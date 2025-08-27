package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.FrameLayout;

/** 울타리 전용: 회전/사이즈 버튼 숨김, 그리드 좌표/마스크 보존 */
public class SelectableFenceView extends SelectableItemView {

    private int fenceMask = 0;
    private int atlasResId = 0;

    public SelectableFenceView(Context ctx, Bitmap bmp16, int displaySizePx, int fenceMask, int atlasResId) {
        super(ctx, /*dummy*/0);
        this.fenceMask = fenceMask;
        this.atlasResId = atlasResId;

        setCustomBitmap(bmp16, atlasResId, fenceMask);
        setFenceMode(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(displaySizePx, displaySizePx);
        setLayoutParams(lp);
    }

    public int getFenceMask() { return fenceMask; }
    public int getAtlasResId() { return atlasResId; }
    public void setFenceMaskAndBitmap(int mask, Bitmap bmp16) {
        this.fenceMask = mask;
        setCustomBitmap(bmp16, atlasResId, mask);
    }
}
