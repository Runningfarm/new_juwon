package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * fences.png (4x4=16칸) 고정 매핑:
 *  행우선 인덱스(0..15) == 비트마스크(0..15, ↑=1,→=2,↓=4,←=8)
 */
public class FenceAtlas {

    private final Bitmap[] tiles = new Bitmap[16];
    private final Paint nearest = new Paint();
    private final int resId;

    public FenceAtlas(Context ctx, int resId) {
        this.resId = resId;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap atlas = BitmapFactory.decodeResource(ctx.getResources(), resId, o);
        if (atlas == null) throw new RuntimeException("fences.png decode failed: resId=" + resId);
        atlas.setDensity(0);

        nearest.setFilterBitmap(false);
        nearest.setAntiAlias(false);
        nearest.setDither(false);

        final int cols = 4, rows = 4;
        final int cellW = atlas.getWidth()  / cols;
        final int cellH = atlas.getHeight() / rows;

        Rect src = new Rect();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c; // == mask
                src.set(c * cellW, r * cellH, (c + 1) * cellW, (r + 1) * cellH);
                Bitmap b = Bitmap.createBitmap(cellW, cellH, Bitmap.Config.ARGB_8888);
                new Canvas(b).drawBitmap(atlas, src, new Rect(0, 0, cellW, cellH), nearest);
                b.setDensity(0);
                tiles[idx] = b;
            }
        }
        atlas.recycle();
    }

    /** mask: 0..15 (↑=1,→=2,↓=4,←=8) */
    public Bitmap getByMask(int mask) { return tiles[mask & 0x0F]; }

    public int getResId() { return resId; }

    public void dispose() {
        for (Bitmap b : tiles) if (b != null && !b.isRecycled()) b.recycle();
    }
}
