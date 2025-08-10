package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.DrawableRes;

public class SpriteAnimUtil {

    // 기존 전체/마스크 버전 유지
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
        Bitmap sheet = BitmapFactory.decodeResource(ctx.getResources(), sheetRes);
        if (sheet == null || rows <= 0 || cols <= 0) return null;

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

    // 행 선택(0-based)으로 프레임 구성
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
