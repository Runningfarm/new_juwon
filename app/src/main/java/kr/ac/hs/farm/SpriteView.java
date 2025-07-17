package kr.ac.hs.farm;

import android.content.Context;
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

    private float currentX = 100, currentY = 100;
    private float targetX = 100, targetY = 100;
    private float speed = 5f;
    private float stopThreshold = 3f;

    private boolean isMoving = false;

    private SurfaceHolder holder;
    private DrawThread thread;

    // 콜백 인터페이스 정의
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

        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.basic_spritesheet);

        frameWidth = spriteSheet.getWidth() / 4;
        frameHeight = spriteSheet.getHeight() / 4;

        srcRect = new Rect(0, 0, frameWidth, frameHeight);

        int size = frameWidth * 2;
        dstRect = new Rect(
                (int)(currentX - size / 2f),
                (int)(currentY - size / 2f),
                (int)(currentX + size / 2f),
                (int)(currentY + size / 2f)
        );
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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
            } catch (InterruptedException e) {}
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
                    sleep(16); // 약 60fps
                } catch (InterruptedException e) {}
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(0xFFFFFAF0); // 배경색

        float dx = targetX - currentX;
        float dy = targetY - currentY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > stopThreshold) {
            isMoving = true;

            // 방향에 따른 애니메이션 줄 선택
            if (Math.abs(dx) > Math.abs(dy)) {
                frameRow = dx > 0 ? 3 : 2; // 오른쪽(3), 왼쪽(2)
            } else {
                frameRow = dy > 0 ? 0 : 1; // 아래(0), 위(1)
            }

            float stepX = speed * dx / distance;
            float stepY = speed * dy / distance;
            currentX += stepX;
            currentY += stepY;

            // 화면 경계 제한
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
            frameIndex = 0; // 정지 시 첫 프레임 유지
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
            // 캐릭터 클릭 시 메뉴 열기
            if (dstRect.contains((int) touchX, (int) touchY)) {
                if (onSpriteClickListener != null) {
                    onSpriteClickListener.onSpriteClick();
                    return true;
                }
            }

            // 터치 이동
            targetX = touchX;
            targetY = touchY;
            return true;
        }
        return super.onTouchEvent(event);
    }
}
