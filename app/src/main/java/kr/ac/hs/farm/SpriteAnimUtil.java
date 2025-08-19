package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.DrawableRes;

public class SpriteAnimUtil {

    public static AnimationDrawable buildFromSheet(Context ctx,
                                                   @DrawableRes int sheetRes,
                                                   int rows,
                                                   int cols,
                                                   int fps) {
        return buildFromSheet(ctx, sheetRes, rows, cols, fps, null);
    }

    public static AnimationDrawable buildFromSheet(Context ctx,
                                                   @DrawableRes int sheetRes,
                                                   int rows,
                                                   int cols,
                                                   int fps,
                                                   boolean[][] includeMask) {
        // 자동 스케일링 방지 + 명시적 포맷
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false; // 밀도 스케일링 금지
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap sheet = BitmapFactory.decodeResource(ctx.getResources(), sheetRes, opts);
        if (sheet == null || rows <= 0 || cols <= 0) return null;

        // 밀도 정보 제거(스케일 무시), mipmap/프리멀티 설정
        sheet.setDensity(0); // DENSITY_NONE
        sheet.setHasMipMap(false);
        try { sheet.setPremultiplied(true); } catch (Throwable ignored) {}

        int frameW = sheet.getWidth() / cols;
        int frameH = sheet.getHeight() / rows;

        AnimationDrawable anim = new AnimationDrawable();
        int frameDurationMs = Math.max(1, 1000 / Math.max(1, fps));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (includeMask != null) {
                    if (r >= includeMask.length || c >= includeMask[r].length) continue;
                    if (!includeMask[r][c]) continue;
                }
                Bitmap frame = Bitmap.createBitmap(sheet, c * frameW, r * frameH, frameW, frameH);
                BitmapDrawable drawable = new BitmapDrawable(ctx.getResources(), frame);
                // 픽셀아트 품질 설정
                drawable.setAntiAlias(false);
                drawable.setFilterBitmap(false);
                drawable.setDither(false);
                drawable.setBounds(0, 0, frameW, frameH);
                anim.addFrame(drawable, frameDurationMs);
            }
        }
        anim.setOneShot(false);
        return anim;
    }

    public static AnimationDrawable buildFromRows(Context ctx,
                                                  @DrawableRes int sheetRes,
                                                  int rows,
                                                  int cols,
                                                  int fps,
                                                  int[] includeRows) {
        boolean[][] mask = null;
        if (includeRows != null) {
            mask = new boolean[rows][cols];
            for (int r : includeRows) {
                if (r >= 0 && r < rows) {
                    for (int c = 0; c < cols; c++) mask[r][c] = true;
                }
            }
        }
        return buildFromSheet(ctx, sheetRes, rows, cols, fps, mask);
    }
}
