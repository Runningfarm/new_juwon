package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

public class HeroOverlayView extends View {
    private final SpriteView sprite;
    private final Rect src = new Rect();
    private final Rect dst = new Rect();
    private final android.graphics.Paint paint = new android.graphics.Paint();

    public HeroOverlayView(Context ctx, SpriteView sprite) {
        super(ctx);
        this.sprite = sprite;
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
        // 픽셀 스프라이트 또렷하게
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);

        // 화면 갱신 루프 (부담 적음)
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap sheet = sprite.getSpriteSheetBitmap();
        if (sheet == null || sheet.getWidth() == 0 || sheet.getHeight() == 0) {
            postInvalidateOnAnimation();
            return;
        }

        int fw = Math.max(1, sprite.getFrameWidthPx());
        int fh = Math.max(1, sprite.getFrameHeightPx());
        int row = sprite.getFrameRow();
        int idx = sprite.getFrameIndex();

        src.set(idx * fw, row * fh,
                Math.min((idx + 1) * fw, sheet.getWidth()),
                Math.min((row + 1) * fh, sheet.getHeight()));

        int vw = Math.max(1, getWidth());
        int vh = Math.max(1, getHeight());
        float cx = vw / 2f;
        float cy = vh / 2f;

        int size = fw * 6; // SpriteView와 동일 스케일
        dst.set((int)(cx - size/2f), (int)(cy - size/2f),
                (int)(cx + size/2f), (int)(cy + size/2f));

        canvas.drawBitmap(sheet, src, dst, paint);

        // 다음 프레임 예약
        postInvalidateOnAnimation();
    }
}