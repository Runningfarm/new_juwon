package kr.ac.hs.farm;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

public class SelectableFenceGateView extends SelectableItemView {

    public enum Orientation { H, V }

    private final ImageView img;
    private final List<Bitmap> frames;  // 닫힘→열림 순서
    private boolean isOpen = false;
    private int animIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long frameDelayMs = 40; // 프레임 속도

    private final Orientation orientation;
    private final int gateResId;

    public SelectableFenceGateView(Context ctx,
                                   List<Bitmap> frames,
                                   int displaySizePx,
                                   Orientation orientation,
                                   int gateResId) {
        super(ctx, /*dummy*/0);
        this.frames = frames;
        this.orientation = orientation;
        this.gateResId = gateResId;

        img = getItemImageView();
        setCustomBitmap(frames.get(0), gateResId, /*fenceMaskTag*/0);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(displaySizePx, displaySizePx);
        setLayoutParams(lp);

        // 탭으로 토글
        setOnClickListener(v -> toggle());
    }

    public boolean isOpen() { return isOpen; }
    public Orientation getOrientation() { return orientation; }
    public int getGateResId() { return gateResId; }

    public void setFrameDelay(long ms) { frameDelayMs = Math.max(16, ms); }

    public void toggle() {
        if (isOpen) {
            // 열림→닫힘 (역방향 재생)
            play(false);
        } else {
            // 닫힘→열림
            play(true);
        }
    }

    private void play(boolean open) {
        removeCallbacks();
        if (open) animIndex = 0; else animIndex = frames.size() - 1;
        handler.post(new Runnable() {
            @Override public void run() {
                img.setImageBitmap(frames.get(animIndex));
                if (open) animIndex++; else animIndex--;
                boolean cont = open ? animIndex < frames.size() : animIndex >= 0;
                if (cont) {
                    handler.postDelayed(this, frameDelayMs);
                } else {
                    isOpen = open;
                }
            }
        });
    }

    private void removeCallbacks() { handler.removeCallbacksAndMessages(null); }
}
