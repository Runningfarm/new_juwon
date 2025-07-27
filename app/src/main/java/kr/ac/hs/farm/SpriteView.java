package kr.ac.hs.farm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
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
    private float speed = 5f;
    private float stopThreshold = 3f;

    private boolean isMoving = false;

    private SurfaceHolder holder;
    private DrawThread thread;

    private SharedPreferences spritePrefs;

    public interface OnSpriteClickListener {
        void onSpriteClick();
    }

    private OnSpriteClickListener onSpriteClickListener;

    public void setOnSpriteClickListener(OnSpriteClickListener listener) {
        this.onSpriteClickListener = listener;
    }

    public SpriteView(Context context) {
        super(context);
        setFocusable(true);
        holder = getHolder();
        holder.addCallback(this);

        spritePrefs = context.getSharedPreferences("SpritePrefs", Context.MODE_PRIVATE);
        String userId = getUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";

        int bgResId = spritePrefs.getInt(bgKey, R.drawable.grass_tiles);
        backgroundImage = BitmapFactory.decodeResource(getResources(), bgResId);

        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.basic_spritesheet);

        frameWidth = spriteSheet.getWidth() / 4;
        frameHeight = spriteSheet.getHeight() / 4;

        srcRect = new Rect(0, 0, frameWidth, frameHeight);
        dstRect = new Rect(0, 0, frameWidth * 2, frameHeight * 2);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        checkAndResetPosition();
        thread = new DrawThread();
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        thread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    private class DrawThread extends Thread {
        private boolean running = false;

        public void setRunning(boolean run) {
            running = run;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            drawFrame(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    sleep(16);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        if (backgroundImage == null) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

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

            float stepX = speed * dx / distance;
            float stepY = speed * dy / distance;
            currentX += stepX;
            currentY += stepY;

            currentX = Math.max(0, Math.min(backgroundImage.getWidth(), currentX));
            currentY = Math.max(0, Math.min(backgroundImage.getHeight(), currentY));

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime > frameDuration) {
                frameIndex = (frameIndex + 1) % frameCount;
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

        Rect bgSrc = new Rect(bgLeft, bgTop, bgRight, bgBottom);
        Rect bgDst = new Rect(0, 0, viewWidth, viewHeight);
        canvas.drawBitmap(backgroundImage, bgSrc, bgDst, null);

        srcRect.left = frameIndex * frameWidth;
        srcRect.top = frameRow * frameHeight;
        srcRect.right = srcRect.left + frameWidth;
        srcRect.bottom = srcRect.top + frameHeight;

        int size = frameWidth * 2;
        dstRect.left = (int) (centerX - size / 2f);
        dstRect.top = (int) (centerY - size / 2f);
        dstRect.right = dstRect.left + size;
        dstRect.bottom = dstRect.top + size;

        canvas.drawBitmap(spriteSheet, srcRect, dstRect, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (dstRect.contains((int) touchX, (int) touchY)) {
                if (onSpriteClickListener != null) {
                    onSpriteClickListener.onSpriteClick();
                    return true;
                }
            }

            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
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

        float defaultX = backgroundImage.getWidth() / 2f;
        float defaultY = backgroundImage.getHeight() / 2f;

        if (!isLoggedIn || savedX == -1 || savedY == -1) {
            currentX = targetX = defaultX;
            currentY = targetY = defaultY;
        } else {
            currentX = targetX = savedX;
            currentY = targetY = savedY;
        }
    }

    public void resetPositionToCenter() {
        float defaultX = backgroundImage.getWidth() / 2f;
        float defaultY = backgroundImage.getHeight() / 2f;
        currentX = targetX = defaultX;
        currentY = targetY = defaultY;
        spritePrefs.edit().remove("lastX").remove("lastY").apply();
    }

    public void reloadBackground() {
        String userId = getUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";
        int bgResId = spritePrefs.getInt(bgKey, R.drawable.grass_tiles);
        backgroundImage = BitmapFactory.decodeResource(getResources(), bgResId);
    }

    private String getUserId() {
        SharedPreferences loginPrefs = getContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);
        return isLoggedIn ? loginPrefs.getString("id", null) : null;
    }
}
