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

public class SelectableSpriteItemView extends SelectableItemView {

    private AnimationDrawable walkAnim;
    private AnimationDrawable idleAnim;
    private AnimationDrawable current;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable wanderTick = this::onWanderTick;
    private boolean wandering = false;
    private ViewGroup wanderBoundsParent;

    private long stepMs = 16;
    private float speedPxPerSec = 30f;
    private float vx = 30f, vy = 0f;

    private float noiseTurnPerSec = 1.0f;
    private float accelFactor = 0.15f;
    private float maxTurnPerTick = 0.25f;

    private boolean paused = false;
    private int minPauseMs = 600, maxPauseMs = 1500;
    private int pauseChancePerSecond = 1;

    private static final float FACE_EPS = 0.1f;
    private int faceDir = 1;

    public SelectableSpriteItemView(Context context, @DrawableRes int baseResId) {
        super(context, baseResId);
    }

    public void applyDualSprite(@DrawableRes int sheetRes,
                                int rows, int cols,
                                int fpsWalk, int fpsIdle,
                                int[] idleRows) {
        boolean[][] idleMask = rowsToMask(rows, cols, idleRows);
        boolean[][] walkMask = invertRowsToMask(rows, cols, idleRows);
        applyDualSpriteWithMasks(sheetRes, rows, cols, fpsWalk, fpsIdle, walkMask, idleMask);
    }

    public void applyDualSpriteWithMasks(@DrawableRes int sheetRes,
                                         int rows, int cols,
                                         int fpsWalk, int fpsIdle,
                                         boolean[][] walkMask,
                                         boolean[][] idleMask) {
        idleAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fpsIdle, idleMask);
        walkAnim = SpriteAnimUtil.buildFromSheet(getContext(), sheetRes, rows, cols, fpsWalk, walkMask);
        switchAnim(idleAnim);
        applyFacingToImageView();
    }

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

    public void enableWander(ViewGroup parent) {
        this.wanderBoundsParent = parent;
        this.wandering = true;
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

    public void setWanderSpeed(float pxPerSec) {
        this.speedPxPerSec = Math.max(8f, pxPerSec);
        float s = (float) Math.hypot(vx, vy);
        if (s > 0.0001f) {
            float k = speedPxPerSec / s;
            vx *= k; vy *= k;
        }
    }

    public void setTurnNoise(float radiansPerSec) {
        this.noiseTurnPerSec = Math.max(0f, radiansPerSec);
    }

    // SelectableSpriteItemView.java 안에 있는 onWanderTick() 메서드를 아래로 교체

    private void onWanderTick() {
        if (!wandering) return;

        final float dt = stepMs / 1000f;

        // 랜덤 일시정지
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
            // 회전/속도 보정
            double angle = Math.atan2(vy, vx);
            double turn = gaussian() * noiseTurnPerSec * dt;
            if (turn >  maxTurnPerTick) turn =  maxTurnPerTick;
            if (turn < -maxTurnPerTick) turn = -maxTurnPerTick;
            angle += turn;

            float cur = (float) Math.hypot(vx, vy);
            float target = speedPxPerSec;
            float newSpeed = cur + (target - cur) * accelFactor;

            vx = (float)(Math.cos(angle) * newSpeed);
            vy = (float)(Math.sin(angle) * newSpeed);

            // ✅ 이동을 '월드 좌표'로 계산
            float nxWorld = getWorldX() + vx * dt;
            float nyWorld = getWorldY() + vy * dt;

            // ✅ 경계(월드 좌표): 현재 카메라 좌상단 + 화면 크기
            Rect b = getMoveBounds();
            int w = getWidth()==0 ? getLayoutParamsSafely().width : getWidth();
            int h = getHeight()==0 ? getLayoutParamsSafely().height : getHeight();
            float minX = b.left,           minY = b.top;
            float maxX = b.right - w,      maxY = b.bottom - h;

            boolean bounced = false;
            if (nxWorld < minX) { nxWorld = minX; vx = Math.abs(vx); bounced = true; }
            if (nxWorld > maxX) { nxWorld = maxX; vx = -Math.abs(vx); bounced = true; }
            if (nyWorld < minY) { nyWorld = minY; vy = Math.abs(vy); bounced = true; }
            if (nyWorld > maxY) { nyWorld = maxY; vy = -Math.abs(vy); bounced = true; }

            // ✅ 월드 좌표로 세팅 → 내부에서 screen = world - camera 로 환산되어 setX/setY됨
            setWorldPosition(nxWorld, nyWorld);

            // 애니/좌우 보기
            if (walkAnim != null) switchAnim(walkAnim);
            if (vx > FACE_EPS && faceDir != 1) { faceDir = 1; applyFacingToImageView(); }
            else if (vx < -FACE_EPS && faceDir != -1) { faceDir = -1; applyFacingToImageView(); }

            // 반사 후 살짝 방향 튕김
            if (bounced) {
                double add = (Math.random() - 0.5) * 0.6;
                double a2 = Math.atan2(vy, vx) + add;
                float s2 = (float) Math.hypot(vx, vy);
                vx = (float)(Math.cos(a2) * s2);
                vy = (float)(Math.sin(a2) * s2);
            }
        }

        handler.postDelayed(wanderTick, stepMs);
    }


    private boolean shouldPauseThisTick(float dt) {
        double p = (pauseChancePerSecond / 100.0) * dt;
        return Math.random() < p;
    }

    private int randomRange(int a, int b) { return a + (int)(Math.random() * (Math.max(1, b - a))); }
    private double gaussian() {
        double u = Math.max(1e-6, Math.random());
        double v = Math.max(1e-6, Math.random());
        return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2 * Math.PI * v);
    }

    private void applyFacingToImageView() {
        ImageView iv = getItemImageView();
        if (iv != null) iv.setScaleX(faceDir);
    }

    // SelectableSpriteItemView.java

    // SelectableSpriteItemView.java 안의 getMoveBounds()를 아래로 교체

    // SelectableSpriteItemView.java (기존 getMoveBounds() 전체 교체)
    private android.graphics.Rect getMoveBounds() {
        // 1순위: 명시된 월드 경계(배경 전체)
        if (worldBounds != null && worldBounds.width() > 0 && worldBounds.height() > 0) {
            return new android.graphics.Rect(worldBounds);
        }

        // 2순위(폴백): 현재 카메라(보이는 화면) 경계
        android.graphics.Rect r = new android.graphics.Rect();
        ViewGroup p = (ViewGroup) getParent();
        if (p == null) return r;

        int viewW = p.getWidth();
        int viewH = p.getHeight();

        int l = (int) getCameraLeft();
        int t = (int) getCameraTop();
        int rr = l + viewW;
        int bb = t + viewH;

        r.set(l, t, rr, bb);
        return r;
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

    // SelectableSpriteItemView.java (필드 구역 어딘가에 추가)
    private android.graphics.Rect worldBounds = null;

    /** 배경(월드) 전체 경계 설정: (0,0) ~ (worldW, worldH) */
    public void setWorldBounds(int worldW, int worldH) {
        if (worldW <= 0 || worldH <= 0) {
            worldBounds = null; // 비정상이면 해제하고 기본(카메라) 경계로
        } else {
            worldBounds = new android.graphics.Rect(0, 0, worldW, worldH);
        }
    }


}
