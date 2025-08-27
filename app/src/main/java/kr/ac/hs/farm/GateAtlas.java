package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

/** fence_gates.png 전용
 *  - 셀 16px 고정, 전체 크기: 가로 20칸 × 세로 5칸
 *  - row0(가로문): [닫힘×4][열림1×4][열림2×4][열림3×4][완전열림×4] → 5프레임
 *  - row1~row3(세로문): 각 row의 1,3,5,7,9번째 칸만 유효 → 5프레임 × 3row = 15프레임
 *  - drawable-nodpi/ 에 두는 것을 권장(inScaled=false)
 */
public class GateAtlas {

    public static final int CELL = 16;
    public static final int COLS = 20;
    public static final int ROWS = 5;

    // 가로문 5프레임(닫힘→완전열림)
    private final Bitmap[] horiz = new Bitmap[5];
    // 세로문 15프레임(닫힘→완전열림) * 3줄
    private final Bitmap[] vertical = new Bitmap[15];

    private final Bitmap src;

    public GateAtlas(Context ctx, int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        src = BitmapFactory.decodeResource(ctx.getResources(), resId, o);
        if (src == null) throw new RuntimeException("Gate atlas decode failed");

        // ---- row0: 20칸을 4칸씩 5그룹으로 묶어 첫 칸만 취함 ----
        int[] startColsRow0 = new int[]{0, 4, 8, 12, 16};
        for (int i = 0; i < 5; i++) {
            horiz[i] = cutIfNotEmpty(startColsRow0[i], 0);
            if (horiz[i] == null) horiz[i] = makeEmptyTile(); // 안전장치
        }

        // ---- row1~row3: 각 row에서 1,3,5,7,9번째 칸(1-base) → 0,2,4,6,8 ----
        int[] validCols = new int[]{0, 2, 4, 6, 8};
        int k = 0;
        for (int r = 1; r <= 3; r++) {
            for (int c : validCols) {
                Bitmap b = cutIfNotEmpty(c, r);
                vertical[k++] = (b != null) ? b : makeEmptyTile();
            }
        }
    }

    // ───────────── Public API ─────────────

    /** 가로문: 0=닫힘 … 4=완전열림 */
    public Bitmap getHorizontalFrame(int stage /*0..4*/) {
        if (stage < 0) stage = 0; if (stage > 4) stage = 4;
        return horiz[stage];
    }
    /** 가로문 전체 프레임(닫힘→완전열림) */
    public Bitmap[] getHorizontalFrames() { return horiz.clone(); }

    /** 세로문 전체 프레임(닫힘→완전열림) * 3줄 연결(총 15프레임) */
    public Bitmap[] getVerticalFrames() { return vertical.clone(); }

    public void dispose() {
        if (src != null && !src.isRecycled()) src.recycle();
    }

    // ───────────── Internal ─────────────

    private Bitmap cut(int col, int row) {
        int sx = col * CELL, sy = row * CELL;
        Rect sr = new Rect(sx, sy, sx + CELL, sy + CELL);
        Bitmap b = Bitmap.createBitmap(CELL, CELL, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawBitmap(src, sr, new Rect(0, 0, CELL, CELL), null);
        b.setDensity(0);
        return b;
    }

    /** 완전 투명(빈칸)인 경우 null 리턴 */
    private Bitmap cutIfNotEmpty(int col, int row) {
        Bitmap b = cut(col, row);
        // 투명 여부 간단 체크: 네 모서리 + 중앙 샘플
        int[] px = new int[]{
                b.getPixel(0,0), b.getPixel(CELL-1,0),
                b.getPixel(0,CELL-1), b.getPixel(CELL-1,CELL-1),
                b.getPixel(CELL/2, CELL/2)
        };
        boolean allTransparent = true;
        for (int p : px) {
            if (Color.alpha(p) != 0) { allTransparent = false; break; }
        }
        if (allTransparent) { b.recycle(); return null; }
        return b;
    }

    private Bitmap makeEmptyTile() {
        Bitmap e = Bitmap.createBitmap(CELL, CELL, Bitmap.Config.ARGB_8888);
        e.setDensity(0);
        return e;
    }
}
