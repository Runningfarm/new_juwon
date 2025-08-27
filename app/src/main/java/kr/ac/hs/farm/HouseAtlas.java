package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class HouseAtlas {

    private final Bitmap[] tiles;
    private final Paint nearest = new Paint();
    private final int resId;
    private final int tileSizePx;

    // ===== 인덱스 매핑 (4×4 권장 배치 기준) =====
    public int H_CORNER_UL = 0;   // (↑←)
    public int H_STRAIGHT_H = 1;  // (←→)
    public int H_CORNER_UR = 2;   // (↑→)
    public int H_WINDOW_ON_H = 3; // 창문(가로)

    // 세로 직선: 좌/우용 분리 ★핵심
    public int H_STRAIGHT_V_LEFT  = 4; // 왼쪽 벽면용
    public int H_FLOOR_UNUSED     = 5; // 바닥(연결엔 미사용)

    public int H_STRAIGHT_V_RIGHT = 6; // 오른쪽 벽면용

    public int H_CORNER_DL = 8;   // (←↓)
    public int H_STRAIGHT_H_ALT = 9;  // 가로 복제(없으면 H_STRAIGHT_H)
    public int H_CORNER_DR = 10;  // (→↓)

    // 얇은 코너/여분은 선택사항
    public int H_THIN_UL = 12, H_THIN_UR = 13, H_THIN_DL = 14, H_THIN_DR = 15;

    // T/교차 폴백
    public int H_T_UP    = 1;   // ←→
    public int H_T_RIGHT = 4;   // ↑↓(좌/우 구분 없는 세로가 필요할 땐 왼쪽 것으로 폴백)
    public int H_T_DOWN  = 1;   // ←→
    public int H_T_LEFT  = 4;   // ↑↓

    public int H_CROSS   = 1;   // ←→

    private boolean useWindowOnHorizontal = false;

    public HouseAtlas(Context ctx, int resId, int tileSizePx) {
        this.resId = resId;
        this.tileSizePx = tileSizePx <= 0 ? 16 : tileSizePx;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap sheet = BitmapFactory.decodeResource(ctx.getResources(), resId, o);
        if (sheet == null) throw new RuntimeException("house atlas decode failed: resId=" + resId);
        sheet.setDensity(0);

        nearest.setFilterBitmap(false);
        nearest.setAntiAlias(false);
        nearest.setDither(false);

        int cols = sheet.getWidth()  / this.tileSizePx;
        int rows = sheet.getHeight() / this.tileSizePx;
        int count = Math.max(1, cols * rows);
        tiles = new Bitmap[count];

        Rect src = new Rect();
        for (int i = 0; i < count; i++) {
            int c = i % cols;
            int r = i / cols;
            src.set(c * tileSizePx, r * tileSizePx, (c + 1) * tileSizePx, (r + 1) * tileSizePx);
            Bitmap b = Bitmap.createBitmap(tileSizePx, tileSizePx, Bitmap.Config.ARGB_8888);
            new Canvas(b).drawBitmap(sheet, src, new Rect(0, 0, tileSizePx, tileSizePx), nearest);
            b.setDensity(0);
            tiles[i] = b;
        }
        sheet.recycle();
    }

    public int getResId() { return resId; }

    /** 표준 룰(4방 마스크)로 고른 비트맵. 세로 좌/우는 외부에서 오버라이드 가능. */
    public Bitmap getByMask(int mask) {
        final int U=1,R=2,D=4,L=8;
        mask &= 0x0F;
        switch (Integer.bitCount(mask)) {
            case 0: return getSafe(H_STRAIGHT_H);
            case 1:
                if (mask == U) return getSafe(H_T_RIGHT); // ↑ 끝 -> 세로로 폴백
                if (mask == R) return getSafe(H_STRAIGHT_H);
                if (mask == D) return getSafe(H_T_RIGHT);
                return getSafe(H_STRAIGHT_H);
            case 2:
                if (mask == (U|D)) return getSafe(H_STRAIGHT_V_LEFT); // 기본은 '왼쪽형' 사용
                if (mask == (R|L)) {
                    if (useWindowOnHorizontal && H_WINDOW_ON_H >= 0) return getSafe(H_WINDOW_ON_H);
                    return getSafe(H_STRAIGHT_H);
                }
                if (mask == (U|L)) return getSafe(H_CORNER_UL);
                if (mask == (U|R)) return getSafe(H_CORNER_UR);
                if (mask == (D|L)) return getSafe(H_CORNER_DL);
                return getSafe(H_CORNER_DR);
            case 3:
                if (mask == (R|D|L)) return getSafe(H_T_UP);
                if (mask == (U|D|L)) return getSafe(H_T_RIGHT);
                if (mask == (U|R|L)) return getSafe(H_T_DOWN);
                return getSafe(H_T_LEFT);
            default:
                return getSafe(H_CROSS);
        }
    }

    /** 세로 좌/우 전용 비트맵(외부에서 방향 판단 후 사용) */
    public Bitmap getVerticalLeft()  { return getSafe(H_STRAIGHT_V_LEFT); }
    public Bitmap getVerticalRight() { return getSafe(H_STRAIGHT_V_RIGHT); }

    private Bitmap getSafe(int idx) {
        if (idx < 0 || idx >= tiles.length || tiles[idx] == null) return tiles[0];
        return tiles[idx];
    }

    public void setUseWindowOnHorizontal(boolean enable, int windowIndex) {
        this.useWindowOnHorizontal = enable;
        this.H_WINDOW_ON_H = windowIndex;
    }

    public void dispose() {
        for (Bitmap b : tiles) if (b != null && !b.isRecycled()) b.recycle();
    }

    public Bitmap getFloorBitmap() {
        return getSafe(H_FLOOR_UNUSED);
    }

}
