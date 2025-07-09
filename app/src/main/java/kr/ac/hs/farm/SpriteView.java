package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
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

    private SurfaceHolder holder;
    private DrawThread thread;

    public SpriteView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.basic_spritesheet);

        frameWidth = spriteSheet.getWidth() / 4;
        frameHeight = spriteSheet.getHeight() / 4;

        srcRect = new Rect(0, 0, frameWidth, frameHeight);

        int size = frameWidth * 4;
        dstRect = new Rect(0, 0, size, size);
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
                    sleep(16);
                } catch (InterruptedException e) {}
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(0xFFFFFAF0); // 배경

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > frameDuration) {
            frameIndex = (frameIndex + 1) % frameCount;
            lastFrameTime = currentTime;
        }

        srcRect.left = frameIndex * frameWidth;
        srcRect.top = frameRow * frameHeight;
        srcRect.right = srcRect.left + frameWidth;
        srcRect.bottom = srcRect.top + frameHeight;

        // 중앙 좌표 계산
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        int size = (int)(frameWidth * 2); // 크기를 2배로 축소 (필요시 1.5, 1.2 등 조정 가능)

        int left = (canvasWidth - size) / 2;
        int top = (canvasHeight - size) / 2;

        dstRect.left = left;
        dstRect.top = top;
        dstRect.right = left + size;
        dstRect.bottom = top + size;

        canvas.drawBitmap(spriteSheet, srcRect, dstRect, null);
    }
}