// SpriteView.java
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
            } catch (InterruptedException ignored) {}
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
        canvas.drawColor(0xFFFFFAF0);

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

            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int halfSize = (frameWidth * 2) / 2;
            currentX = Math.max(halfSize, Math.min(viewWidth - halfSize, currentX));
            currentY = Math.max(halfSize, Math.min(viewHeight - halfSize, currentY));

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

        srcRect.left = frameIndex * frameWidth;
        srcRect.top = frameRow * frameHeight;
        srcRect.right = srcRect.left + frameWidth;
        srcRect.bottom = srcRect.top + frameHeight;

        int size = frameWidth * 2;
        dstRect.left = (int) (currentX - size / 2f);
        dstRect.top = (int) (currentY - size / 2f);
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
            targetX = touchX;
            targetY = touchY;
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

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        if (!isLoggedIn || savedX == -1 || savedY == -1) {
            currentX = targetX = centerX;
            currentY = targetY = centerY;
        } else {
            currentX = targetX = savedX;
            currentY = targetY = savedY;
        }
    }

    public void resetPositionToCenter() {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        currentX = targetX = centerX;
        currentY = targetY = centerY;
        spritePrefs.edit().remove("lastX").remove("lastY").apply();
    }

    private String getUserId() {
        SharedPreferences loginPrefs = getContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);
        return isLoggedIn ? loginPrefs.getString("id", null) : null;
    }
}
