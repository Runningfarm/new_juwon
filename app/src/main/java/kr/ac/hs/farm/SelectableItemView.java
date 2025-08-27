package kr.ac.hs.farm;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.view.GestureDetectorCompat;
import android.view.GestureDetector;

public class SelectableItemView extends FrameLayout {

    private ImageView itemImage;
    private View borderView;
    private ImageView deleteButton;
    private ImageView rotateButton;
    private ImageView sizeIncreaseButton;
    private ImageView sizeDecreaseButton;

    private float dX, dY;
    private int gridSize = 30; // 일반 아이템 스냅

    private float rotationDegrees = 0f;

    private GestureDetectorCompat gestureDetector;

    private int resId;

    private boolean isEditEnabled = false;

    // 월드/카메라
    private float worldX = 0f, worldY = 0f;
    private float cameraLeft = 0f, cameraTop = 0f;

    // 커스텀 비트맵/울타리 태그
    private android.graphics.Bitmap customBitmap = null;
    private Integer fenceMaskTag = null;
    private Integer atlasResIdTag = null;
    private Integer fenceGridXTag = null; // 그리드 좌표 보존
    private Integer fenceGridYTag = null;

    // fence 전용 모드(버튼 숨김)
    private boolean isFenceMode = false;

    public float getCameraLeft() { return cameraLeft; }
    public float getCameraTop()  { return cameraTop;  }

    public interface OnDoubleTapListener { void onDoubleTap(); }
    private OnDoubleTapListener doubleTapListener;
    public void setOnDoubleTapListener(OnDoubleTapListener l){ this.doubleTapListener = l; }

    public interface OnDragEndListener { void onDragEnd(SelectableItemView v); }
    private OnDragEndListener dragEndListener;
    public void setOnDragEndListener(OnDragEndListener l) { this.dragEndListener = l; }

    public SelectableItemView(Context context, int resId) {
        super(context);
        this.resId = resId;
        init(context);
    }

    private void init(Context context) {
        itemImage = new ImageView(context);
        itemImage.setImageResource(resId);
        itemImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams imgParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        itemImage.setLayoutParams(imgParams);
        addView(itemImage);

        borderView = new View(context);
        borderView.setBackgroundResource(R.drawable.selection_border);
        borderView.setLayoutParams(imgParams);
        addView(borderView);

        deleteButton = new ImageView(context);
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        addView(deleteButton);

        rotateButton = new ImageView(context);
        rotateButton.setImageResource(android.R.drawable.ic_menu_rotate);
        addView(rotateButton);

        sizeIncreaseButton = new ImageView(context);
        sizeIncreaseButton.setImageResource(android.R.drawable.arrow_up_float);
        addView(sizeIncreaseButton);

        sizeDecreaseButton = new ImageView(context);
        sizeDecreaseButton.setImageResource(android.R.drawable.arrow_down_float);
        addView(sizeDecreaseButton);

        deleteButton.setOnClickListener(v -> { if (doubleTapListener != null) doubleTapListener.onDoubleTap(); });
        rotateButton.setOnClickListener(v -> { rotationDegrees = (rotationDegrees + 45f) % 360; setRotation(rotationDegrees); });
        sizeIncreaseButton.setOnClickListener(v -> increaseSize());
        sizeDecreaseButton.setOnClickListener(v -> decreaseSize());

        hideBorderAndButtons();

        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) { return true; }
        });

        setOnTouchListener(touchListener);
        setClickable(false);
        setFocusable(false);
    }

    // ===== 월드/카메라 =====
    public void setCameraOffset(float left, float top) { this.cameraLeft = left; this.cameraTop = top; }
    public void setWorldPosition(float wx, float wy) { this.worldX = wx; this.worldY = wy; applyScreenFromWorld(); }
    public void updateWorldFromScreen() { this.worldX = getX() + cameraLeft; this.worldY = getY() + cameraTop; }
    public void applyScreenFromWorld() { setX(worldX - cameraLeft); setY(worldY - cameraTop); }
    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }

    // ===== 버튼 크기 =====
    private void updateButtonSizes() {
        int itemSize = Math.min(getWidth(), getHeight());
        int buttonSize = Math.max(30, itemSize / 4);

        FrameLayout.LayoutParams lp;
        lp = new FrameLayout.LayoutParams(buttonSize, buttonSize); lp.gravity = Gravity.TOP    | Gravity.END;  deleteButton.setLayoutParams(lp);
        lp = new FrameLayout.LayoutParams(buttonSize, buttonSize); lp.gravity = Gravity.BOTTOM | Gravity.END;  rotateButton.setLayoutParams(lp);
        lp = new FrameLayout.LayoutParams(buttonSize, buttonSize); lp.gravity = Gravity.TOP    | Gravity.START; sizeIncreaseButton.setLayoutParams(lp);
        lp = new FrameLayout.LayoutParams(buttonSize, buttonSize); lp.gravity = Gravity.BOTTOM | Gravity.START; sizeDecreaseButton.setLayoutParams(lp);
    }

    private void increaseSize() { setSize(getWidth()+gridSize, getHeight()+gridSize); }
    private void decreaseSize() {
        int nw = getWidth()-gridSize, nh = getHeight()-gridSize;
        if (nw < 60) nw = 60; if (nh < 60) nh = 60; setSize(nw, nh);
    }
    private void setSize(int w, int h) {
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) getLayoutParams();
        p.width = w; p.height = h; setLayoutParams(p); updateButtonSizes();
    }

    public int getResId() { return resId; }

    public Integer getFenceMaskTag() { return fenceMaskTag; }
    public Integer getAtlasResIdTag() { return atlasResIdTag; }
    public Integer getFenceGridXTag() { return fenceGridXTag; }
    public Integer getFenceGridYTag() { return fenceGridYTag; }

    public void setFenceGridCell(int gx, int gy) { this.fenceGridXTag = gx; this.fenceGridYTag = gy; }
    public void setFenceMode(boolean enable) {
        isFenceMode = enable;
        if (enable) { // 울타리는 회전/크기 버튼 숨김
            rotateButton.setVisibility(View.GONE);
            sizeIncreaseButton.setVisibility(View.GONE);
            sizeDecreaseButton.setVisibility(View.GONE);
        }
    }

    public void hideBorderAndButtons() {
        borderView.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        if (!isFenceMode) {
            rotateButton.setVisibility(View.GONE);
            sizeIncreaseButton.setVisibility(View.GONE);
            sizeDecreaseButton.setVisibility(View.GONE);
        } else {
            rotateButton.setVisibility(View.GONE);
            sizeIncreaseButton.setVisibility(View.GONE);
            sizeDecreaseButton.setVisibility(View.GONE);
        }
    }

    public void showBorderAndButtons() {
        borderView.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);
        if (!isFenceMode) {
            rotateButton.setVisibility(View.VISIBLE);
            sizeIncreaseButton.setVisibility(View.VISIBLE);
            sizeDecreaseButton.setVisibility(View.VISIBLE);
        } else {
            rotateButton.setVisibility(View.GONE);
            sizeIncreaseButton.setVisibility(View.GONE);
            sizeDecreaseButton.setVisibility(View.GONE);
        }
        updateButtonSizes();
    }

    public void setEditEnabled(boolean enabled) {
        isEditEnabled = enabled;
        setClickable(enabled);
        setFocusable(enabled);
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override public boolean onTouch(View v, MotionEvent e) {
            if (!isEditEnabled) return false;

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = getX() - e.getRawX();
                    dY = getY() - e.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE: {
                    float newX = e.getRawX() + dX;
                    float newY = e.getRawY() + dY;

                    // 일반 아이템은 gridSize, 울타리는 GRID_PX(=48) 스냅 되도록 외부에서 보정
                    newX = Math.round(newX / gridSize) * gridSize;
                    newY = Math.round(newY / gridSize) * gridSize;

                    View parent = (View) getParent();
                    if (parent != null) {
                        int pw = parent.getWidth(), ph = parent.getHeight();
                        int iw = getWidth(), ih = getHeight();
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX > pw - iw) newX = pw - iw;
                        if (newY > ph - ih) newY = ph - ih;
                    }
                    setX(newX); setY(newY);
                    break;
                }

                case MotionEvent.ACTION_UP:
                    updateWorldFromScreen();
                    if (dragEndListener != null) dragEndListener.onDragEnd(SelectableItemView.this);
                    break;
            }
            return true;
        }
    };

    // 외부 제어용
    public void setItemImageDrawable(android.graphics.drawable.Drawable d) { itemImage.setImageDrawable(d); }
    public void setItemImageVisible(boolean visible) { itemImage.setVisibility(visible ? View.VISIBLE : View.GONE); }
    public void clearItemImage() { itemImage.setImageDrawable(null); }
    public ImageView getItemImageView() { return itemImage; }

    public void setCustomBitmap(android.graphics.Bitmap bm, int atlasResIdTag, int fenceMaskTag) {
        this.customBitmap = bm;
        this.atlasResIdTag = atlasResIdTag;
        this.fenceMaskTag = fenceMaskTag;
        this.itemImage.setImageBitmap(bm);
    }
}
