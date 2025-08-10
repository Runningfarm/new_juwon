package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

/**
 * 스프라이트: 걷기/대기 분리 + 랜덤 워크(연속 조향) + 진행방향 바라보기 + 마스크 지원
 * - 목표점 직선 이동 대신, 속도 벡터에 랜덤 조향을 계속 주는 자유 이동
 * - 경계 반사, 랜덤 일시정지(Idle) 지원
 */
public class SelectableSpriteItemView extends SelectableItemView {

    // 애니 2종
    private AnimationDrawable walkAnim;
    private AnimationDrawable idleAnim;
    private AnimationDrawable current;

    // wander (연속 조향)
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable wanderTick = this::onWanderTick;
    private boolean wandering = false;
    private ViewGroup wanderBoundsParent;

    private long stepMs = 16;              // ~60fps
    private float speedPxPerSec = 30f;     // 기본 속도 (느리게)
    private float vx = 30f, vy = 0f;       // 현재 속도 벡터(px/s)

    // 조향 파라미터
    private float noiseTurnPerSec = 1.0f;  // 초당 방향 변동(라디안) 세기 (0.5~2.0 권장)
    private float accelFactor = 0.15f;     // 속도 크기 보정(목표 속도로 복원)
    private float maxTurnPerTick = 0.25f;  // 프레임당 최대 회전(라디안) 클램프

    // 랜덤 일시정지
    private boolean paused = false;
    private int minPauseMs = 600, maxPauseMs = 1500;
    private int pauseChancePerSecond = 1;  // 초당 ~1% 확률 (원하면 올려/내려)

    // 바라보기(좌우 반전)
    private static final float FACE_EPS = 0.1f;
    private int faceDir = 1;               // 1: 오른쪽, -1: 왼쪽

    public SelectableSpriteItemView(Context context, @DrawableRes int baseResId) {
        super(context, baseResId);
    }

    /** 걷기/대기 애니를 한 번에 세팅(행 기반) */
    public void applyDualSprite(@DrawableRes int sheetRes,
                                int rows, int cols,
                                int fpsWalk, int fpsIdle,
                                int[] idleRows) {
        boolean[][] idleMask = rowsToMask(rows, cols, idleRows);
        boolean[][] walkMask = invertRowsToMask(rows, cols, idleRows);
        applyDualSpriteWithMasks(sheetRes, rows, cols, fpsWalk, fpsIdle, walkMask, idleMask);
    }

    /** 걷기/대기 애니를 마스크로 직접 세팅(빈 프레임 제외 가능) */
    public void applyDualSpriteWithMasks(@DrawableRes int sheetRes,
                                         int rows, int cols,
                                         int fpsWalk, int fpsIdle,
                                         boolean[][] walkMask,
                                         boolean[][] idleMask) {
        idleAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fpsIdle, idleMask);
        walkAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fpsWalk, walkMask);
        switchAnim(idleAnim); // 초기: 대기
        applyFacingToImageView();
    }

    // 단일 API 유지(필요 시)
    public void applySprite(@DrawableRes int sheetRes, int rows, int cols, int fps) {
        walkAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fps);
        idleAnim = walkAnim;
        switchAnim(walkAnim);
        applyFacingToImageView();
    }

    public void applySpriteMasked(@DrawableRes int sheetRes, int rows, int cols, int fps, boolean[][] includeMask) {
        walkAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fps, includeMask);
        idleAnim = walkAnim;
        switchAnim(walkAnim);
        applyFacingToImageView();
    }

    private void switchAnim(AnimationDrawable next) {
        if (next == null || next == current) return;
        clearItemImage();
        setBackground(null);
        setItemImageDrawable(next);
        setItemImageVisible(true);
        current = next;
        post(() -> {
            if (walkAnim != null) walkAnim.stop();
            if (idleAnim != null) idleAnim.stop();
            current.start();
        });
    }

    public void startAnim() { if (current != null && !current.isRunning()) current.start(); }
    public void stopAnim()  { if (current != null && current.isRunning()) current.stop();  }

    // ===== 랜덤 워크(연속 조향) =====

    public void enableWander(ViewGroup parent) {
        this.wanderBoundsParent = parent;
        this.wandering = true;
        // 초기 방향 랜덤
        double th = Math.random() * Math.PI * 2;
        vx = (float)(Math.cos(th) * speedPxPerSec);
        vy = (float)(Math.sin(th) * speedPxPerSec);
        paused = false;
        switchAnim(walkAnim != null ? walkAnim : current);
        handler.post(wanderTick);
    }

    public void disableWander() {
        this.wandering = false;
        handler.removeCallbacks(wanderTick);
    }

    /** 외부에서 속도 조절(px/sec) */
    public void setWanderSpeed(float pxPerSec) {
        this.speedPxPerSec = Math.max(8f, pxPerSec);
        // 현재 속도 크기도 새 목표에 맞춰 보정
        float s = (float)Math.hypot(vx, vy);
        if (s > 0.0001f) {
            float k = speedPxPerSec / s;
            vx *= k; vy *= k;
        }
    }

    /** 조향 강도(라디안/초). 기본 1.0 */
    public void setTurnNoise(float radiansPerSec) {
        this.noiseTurnPerSec = Math.max(0f, radiansPerSec);
    }

    private void onWanderTick() {
        if (!wandering) return;

        final float dt = stepMs / 1000f;

        // 랜덤 일시정지 트리거
        if (!paused && shouldPauseThisTick(dt)) {
            paused = true;
            switchAnim(idleAnim != null ? idleAnim : current);
            handler.postDelayed(() -> {
                paused = false;
                switchAnim(walkAnim != null ? walkAnim : current);
                handler.post(wanderTick);
            }, randomRange(minPauseMs, maxPauseMs));
            return;
        }

        if (!paused) {
            // 현재 속도 → 각도로
            double angle = Math.atan2(vy, vx);

            // 랜덤 조향(가우시안 작은 각도) + 클램프
            double turn = gaussian() * noiseTurnPerSec * dt;
            if (turn >  maxTurnPerTick) turn =  maxTurnPerTick;
            if (turn < -maxTurnPerTick) turn = -maxTurnPerTick;
            angle += turn;

            // 속도 크기는 목표(speedPxPerSec)로 점진 보정
            float cur = (float)Math.hypot(vx, vy);
            float target = speedPxPerSec;
            float newSpeed = cur + (target - cur) * accelFactor;

            vx = (float)(Math.cos(angle) * newSpeed);
            vy = (float)(Math.sin(angle) * newSpeed);

            // 위치 업데이트
            float nx = getX() + vx * dt;
            float ny = getY() + vy * dt;

            // 경계 반사
            Rect b = getMoveBounds();
            int w = getWidth()==0 ? getLayoutParamsSafely().width : getWidth();
            int h = getHeight()==0 ? getLayoutParamsSafely().height : getHeight();
            float minX = b.left, minY = b.top;
            float maxX = b.right - w, maxY = b.bottom - h;

            boolean bounced = false;
            if (nx < minX) { nx = minX; vx = Math.abs(vx); bounced = true; }
            if (nx > maxX) { nx = maxX; vx = -Math.abs(vx); bounced = true; }
            if (ny < minY) { ny = minY; vy = Math.abs(vy); bounced = true; }
            if (ny > maxY) { ny = maxY; vy = -Math.abs(vy); bounced = true; }

            setX(nx);
            setY(ny);

            // 걷는 중 → 걷기 애니 보장
            if (walkAnim != null) switchAnim(walkAnim);

            // 바라보기(좌우 반전)
            if (vx > FACE_EPS && faceDir != 1) { faceDir = 1; applyFacingToImageView(); }
            else if (vx < -FACE_EPS && faceDir != -1) { faceDir = -1; applyFacingToImageView(); }

            // 반사 직후엔 살짝 조향 세게 줘서 벽 타기 방지
            if (bounced) {
                double add = (Math.random() - 0.5) * 0.6; // -0.3~0.3rad
                double a2 = Math.atan2(vy, vx) + add;
                float s2 = (float)Math.hypot(vx, vy);
                vx = (float)(Math.cos(a2) * s2);
                vy = (float)(Math.sin(a2) * s2);
            }
        }

        handler.postDelayed(wanderTick, stepMs);
    }

    private boolean shouldPauseThisTick(float dt) {
        // 초당 pauseChancePerSecond% 확률 → 프레임 단위 확률
        // 예: 1%/s 이면 dt=0.016에서 약 0.00016 확률
        double p = (pauseChancePerSecond / 100.0) * dt;
        return Math.random() < p;
    }

    private int randomRange(int a, int b) { return a + (int)(Math.random() * (Math.max(1, b - a))); }
    private double gaussian() {
        // Box–Muller
        double u = Math.max(1e-6, Math.random());
        double v = Math.max(1e-6, Math.random());
        return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2 * Math.PI * v);
    }

    // ===== 유틸 =====

    private void applyFacingToImageView() {
        ImageView iv = getItemImageView();
        if (iv != null) iv.setScaleX(faceDir);
    }

    private Rect getMoveBounds() {
        Rect r = new Rect();
        if (wanderBoundsParent == null) {
            View p = (View) getParent();
            if (p == null) return r;
            r.set(0, 0, p.getWidth(), p.getHeight()); return r;
        }
        int l = wanderBoundsParent.getPaddingLeft();
        int t = wanderBoundsParent.getPaddingTop();
        int rr = wanderBoundsParent.getWidth() - wanderBoundsParent.getPaddingRight();
        int bb = wanderBoundsParent.getHeight() - wanderBoundsParent.getPaddingBottom();
        r.set(l, t, rr, bb); return r;
    }

    private ViewGroup.LayoutParams getLayoutParamsSafely() {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) lp = new ViewGroup.LayoutParams(100,100);
        return lp;
    }

    private static boolean[][] rowsToMask(int rows, int cols, int[] includeRows) {
        boolean[][] m = new boolean[rows][cols];
        if (includeRows == null) return m;
        for (int r : includeRows) if (r>=0 && r<rows) for (int c=0;c<cols;c++) m[r][c]=true;
        return m;
    }
    private static boolean[][] invertRowsToMask(int rows, int cols, int[] excludeRows) {
        boolean[] ex = new boolean[rows];
        if (excludeRows != null) for (int r: excludeRows) if (r>=0 && r<rows) ex[r]=true;
        boolean[][] m = new boolean[rows][cols];
        for (int r=0;r<rows;r++) if (!ex[r]) for (int c=0;c<cols;c++) m[r][c]=true;
        return m;
    }
}
