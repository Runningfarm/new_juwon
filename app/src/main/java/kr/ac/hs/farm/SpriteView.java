package kr.ac.hs.farm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SpriteView extends SurfaceView implements SurfaceHolder.Callback {

    private Bitmap spriteSheet;
    private Bitmap backgroundImage;
    private Rect srcRect, dstRect;
    private int frameWidth, frameHeight;
    private int frameCount = 4;
    private int frameIndex = 0;
    private int frameRow = 0;
    private long lastFrameTime;
    private int frameDuration = 120;

    private float currentX, currentY;
    private float targetX, targetY;
    private float speed = 7f;
    private float stopThreshold = 3f;

    private boolean isMoving = false;

    private SurfaceHolder holder;
    private DrawThread thread;

    private SharedPreferences spritePrefs;

    // 픽셀 스프라이트용 Paint (최근접 보간)
    private final android.graphics.Paint spritePaint = new android.graphics.Paint();


    public interface OnSpriteClickListener {
        void onSpriteClick();
    }
    private OnSpriteClickListener onSpriteClickListener;
    public void setOnSpriteClickListener(OnSpriteClickListener listener) {
        this.onSpriteClickListener = listener;
    }

    // ★ 카메라 변경 콜백
    public interface OnCameraChangeListener {
        void onCameraChanged(int left, int top);
    }
    private OnCameraChangeListener cameraChangeListener;
    public void setOnCameraChangeListener(OnCameraChangeListener l) {
        this.cameraChangeListener = l;
    }

    // 마지막 카메라 상태
    private int lastBgLeft = -1, lastBgTop = -1;

    public SpriteView(Context context) {
        super(context);
        setFocusable(true);
        holder = getHolder();
        holder.addCallback(this);

        spritePrefs = context.getSharedPreferences("SpritePrefs", Context.MODE_PRIVATE);
        String userId = getUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";

        int bgResId = spritePrefs.getInt(bgKey, R.drawable.tiles_grass);
        backgroundImage = BitmapFactory.decodeResource(getResources(), bgResId);
        if (backgroundImage == null) {
            backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.tiles_grass);
        }

        android.graphics.BitmapFactory.Options sOpts = new android.graphics.BitmapFactory.Options();
        sOpts.inScaled = false; // 밀도 자동 스케일링 금지
        sOpts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;

        // 👇 밀도 스케일 완전 차단(있어도 되고 없어도 됨, 넣으면 더 안전)
        sOpts.inDensity = 0;
        sOpts.inTargetDensity = 0;

        spriteSheet = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.basic_spritesheet, sOpts);
        if (spriteSheet == null) {
            // 폴백
            spriteSheet = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888);
            frameCount = 1;
        } else {
            // 밀도 정보 제거(스케일 무시) + mipmap/프리멀티 설정
            spriteSheet.setDensity(0); // DENSITY_NONE
            spriteSheet.setHasMipMap(false);
            try { spriteSheet.setPremultiplied(true); } catch (Throwable ignored) {}
        }

// 픽셀 렌더링용 Paint 설정
        spritePaint.setFilterBitmap(false);  // 최근접 보간(블러 방지)
        spritePaint.setAntiAlias(false);     // 안티앨리어싱 끔(픽셀 또렷)
        spritePaint.setDither(false);        // 디더링 끔

        frameWidth = Math.max(1, spriteSheet.getWidth() / 4);
        frameHeight = Math.max(1, spriteSheet.getHeight() / 4);

        srcRect = new Rect(0, 0, frameWidth, frameHeight);
        dstRect = new Rect(0, 0, frameWidth * 2, frameHeight * 2);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holderParam) {
        checkAndResetPosition();
        if (thread == null) {
            thread = new DrawThread();
            thread.setRunning(true);
            thread.start();
        } else {
            thread.setRunning(true);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holderParam) {
        if (thread != null) {
            thread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    thread.join(200);
                    retry = false;
                } catch (InterruptedException ignored) {
                }
            }
            thread = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    private class DrawThread extends Thread {
        private volatile boolean running = false;
        public void setRunning(boolean run) { running = run; }

        @Override
        public void run() {
            while (running) {
                if (holder == null || holder.getSurface() == null || !holder.getSurface().isValid()) {
                    SystemClock.sleep(16);
                    continue;
                }
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            drawFrame(canvas);
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (canvas != null) {
                        try { holder.unlockCanvasAndPost(canvas); } catch (Throwable ignored) {}
                    }
                }
                SystemClock.sleep(16);
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        if (backgroundImage == null || backgroundImage.getWidth() <= 0 || backgroundImage.getHeight() <= 0) {
            canvas.drawColor(0xFFFFFFFF);
            return;
        }

        int viewWidth = Math.max(1, getWidth());
        int viewHeight = Math.max(1, getHeight());

        float dx = targetX - currentX;
        float dy = targetY - currentY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > stopThreshold) {
            isMoving = true;

            if (Math.abs(dx) > Math.abs(dy)) {
                frameRow = dx > 0 ? 3 : 2;
            } else {
                frameRow = dy > 0 ? 0 : 1;
            }

            float stepX = speed * dx / Math.max(1f, distance);
            float stepY = speed * dy / Math.max(1f, distance);
            currentX += stepX;
            currentY += stepY;

            currentX = Math.max(0, Math.min(backgroundImage.getWidth(), currentX));
            currentY = Math.max(0, Math.min(backgroundImage.getHeight(), currentY));

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime > frameDuration) {
                frameIndex = (frameIndex + 1) % Math.max(1, frameCount);
                lastFrameTime = currentTime;
            }
        } else {
            isMoving = false;
            currentX = targetX;
            currentY = targetY;
            frameIndex = 0;
        }

        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;

        float offsetX = currentX - centerX;
        float offsetY = currentY - centerY;

        int bgLeft = (int) offsetX;
        int bgTop = (int) offsetY;
        int bgRight = bgLeft + viewWidth;
        int bgBottom = bgTop + viewHeight;

        if (bgLeft < 0) {
            bgRight += -bgLeft;
            bgLeft = 0;
        }
        if (bgTop < 0) {
            bgBottom += -bgTop;
            bgTop = 0;
        }
        if (bgRight > backgroundImage.getWidth()) {
            bgLeft -= (bgRight - backgroundImage.getWidth());
            bgRight = backgroundImage.getWidth();
        }
        if (bgBottom > backgroundImage.getHeight()) {
            bgTop -= (bgBottom - backgroundImage.getHeight());
            bgBottom = backgroundImage.getHeight();
        }

        bgLeft = Math.max(0, bgLeft);
        bgTop = Math.max(0, bgTop);
        bgRight = Math.min(backgroundImage.getWidth(), bgRight);
        bgBottom = Math.min(backgroundImage.getHeight(), bgBottom);

        if (bgRight <= bgLeft || bgBottom <= bgTop) {
            bgLeft = 0; bgTop = 0;
            bgRight = backgroundImage.getWidth();
            bgBottom = backgroundImage.getHeight();
        }

        Rect bgSrc = new Rect(bgLeft, bgTop, bgRight, bgBottom);
        Rect bgDst = new Rect(0, 0, viewWidth, viewHeight);
        canvas.drawBitmap(backgroundImage, bgSrc, bgDst, null);

        // ★ 카메라 변경 알림 (final 캡처)
        if (cameraChangeListener != null && (bgLeft != lastBgLeft || bgTop != lastBgTop)) {
            lastBgLeft = bgLeft;
            lastBgTop = bgTop;

            final int camLeft = bgLeft;
            final int camTop  = bgTop;

            post(() -> {
                OnCameraChangeListener l = cameraChangeListener;
                if (l != null) l.onCameraChanged(camLeft, camTop);
            });
        }

        if (spriteSheet != null && spriteSheet.getWidth() > 0 && spriteSheet.getHeight() > 0) {
            int safeFrameW = Math.max(1, frameWidth);
            int safeFrameH = Math.max(1, frameHeight);

            srcRect.left = frameIndex * safeFrameW;
            srcRect.top = frameRow * safeFrameH;
            srcRect.right = Math.min(srcRect.left + safeFrameW, spriteSheet.getWidth());
            srcRect.bottom = Math.min(srcRect.top + safeFrameH, spriteSheet.getHeight());

            int size = safeFrameW * 6;
            dstRect.left = (int) (centerX - size / 2f);
            dstRect.top = (int) (centerY - size / 2f);
            dstRect.right = dstRect.left + size;
            dstRect.bottom = dstRect.top + size;

            canvas.drawBitmap(spriteSheet, srcRect, dstRect, spritePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (dstRect != null && dstRect.contains((int) touchX, (int) touchY)) {
                if (onSpriteClickListener != null) {
                    onSpriteClickListener.onSpriteClick();
                    return true;
                }
            }

            float centerX = Math.max(1, getWidth()) / 2f;
            float centerY = Math.max(1, getHeight()) / 2f;
            float dx = touchX - centerX;
            float dy = touchY - centerY;

            targetX = currentX + dx;
            targetY = currentY + dy;
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void saveCharacterPosition() {
        spritePrefs.edit()
                .putFloat("lastX", currentX)
                .putFloat("lastY", currentY)
                .apply();
    }

    public void checkAndResetPosition() {
        float savedX = spritePrefs.getFloat("lastX", -1);
        float savedY = spritePrefs.getFloat("lastY", -1);
        boolean isLoggedIn = getUserId() != null;

        float defaultX = (backgroundImage != null) ? backgroundImage.getWidth() / 2f : 0f;
        float defaultY = (backgroundImage != null) ? backgroundImage.getHeight() / 2f : 0f;

        if (!isLoggedIn || savedX == -1 || savedY == -1) {
            currentX = targetX = defaultX;
            currentY = targetY = defaultY;
        } else {
            currentX = targetX = savedX;
            currentY = targetY = savedY;
        }
    }

    public void resetPositionToCenter() {
        float defaultX = (backgroundImage != null) ? backgroundImage.getWidth() / 2f : 0f;
        float defaultY = (backgroundImage != null) ? backgroundImage.getHeight() / 2f : 0f;
        currentX = targetX = defaultX;
        currentY = targetY = defaultY;
        spritePrefs.edit().remove("lastX").remove("lastY").apply();

        final int camLeft = (int)(currentX - Math.max(1, getWidth())/2f);
        final int camTop  = (int)(currentY - Math.max(1, getHeight())/2f);

        post(() -> {
            if (cameraChangeListener != null) {
                cameraChangeListener.onCameraChanged(camLeft, camTop);
            }
        });
    }

    public void reloadBackground() {
        String userId = getUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";
        int bgResId = spritePrefs.getInt(bgKey, R.drawable.tiles_grass);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), bgResId);
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.tiles_grass);
        }
        backgroundImage = bmp;
    }

    private String getUserId() {
        SharedPreferences loginPrefs = getContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);
        return isLoggedIn ? loginPrefs.getString("id", null) : null;
    }

    // SpriteView.java 안에 추가
    public int[] computeCurrentCameraLT() {
        if (backgroundImage == null) return new int[]{0, 0};

        int viewWidth = Math.max(1, getWidth());
        int viewHeight = Math.max(1, getHeight());

        // centerX/centerY는 화면 중앙
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;

        // drawFrame과 동일 로직: currentX/currentY 기준
        float offsetX = currentX - centerX;
        float offsetY = currentY - centerY;

        int bgLeft = (int) offsetX;
        int bgTop = (int) offsetY;
        int bgRight = bgLeft + viewWidth;
        int bgBottom = bgTop + viewHeight;

        if (bgLeft < 0) { bgRight += -bgLeft; bgLeft = 0; }
        if (bgTop < 0)  { bgBottom += -bgTop; bgTop = 0; }
        if (bgRight > backgroundImage.getWidth()) {
            bgLeft -= (bgRight - backgroundImage.getWidth());
            bgRight = backgroundImage.getWidth();
        }
        if (bgBottom > backgroundImage.getHeight()) {
            bgTop -= (bgBottom - backgroundImage.getHeight());
            bgBottom = backgroundImage.getHeight();
        }

        bgLeft = Math.max(0, bgLeft);
        bgTop = Math.max(0, bgTop);

        return new int[]{ bgLeft, bgTop };
    }

    public int getWorldWidth() {
        return (backgroundImage != null) ? backgroundImage.getWidth() : 0;
    }

    public int getWorldHeight() {
        return (backgroundImage != null) ? backgroundImage.getHeight() : 0;
    }

}
