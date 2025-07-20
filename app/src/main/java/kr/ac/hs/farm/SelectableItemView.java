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
    private int gridSize = 30;

    private float rotationDegrees = 0f;

    private GestureDetectorCompat gestureDetector;

    private int resId;

    private boolean isEditEnabled = false;

    public interface OnDoubleTapListener {
        void onDoubleTap();
    }

    private OnDoubleTapListener doubleTapListener;

    public void setOnDoubleTapListener(OnDoubleTapListener listener) {
        this.doubleTapListener = listener;
    }

    public SelectableItemView(Context context, int resId) {
        super(context);
        this.resId = resId;
        init(context);
    }

    private void init(Context context) {
        itemImage = new ImageView(context);
        itemImage.setImageResource(resId);
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

        deleteButton.setOnClickListener(v -> {
            if (doubleTapListener != null) doubleTapListener.onDoubleTap();
        });

        rotateButton.setOnClickListener(v -> {
            rotationDegrees = (rotationDegrees + 45f) % 360;
            setRotation(rotationDegrees);
        });

        sizeIncreaseButton.setOnClickListener(v -> increaseSize());
        sizeDecreaseButton.setOnClickListener(v -> decreaseSize());

        hideBorderAndButtons();

        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }
        });

        setOnTouchListener(touchListener);

        setClickable(false);
        setFocusable(false);
    }

    // ★ 수정: 버튼 크기 자동 조정
    private void updateButtonSizes() {
        int itemSize = Math.min(getWidth(), getHeight());
        int buttonSize = Math.max(30, itemSize / 4);  // 최소 30

        FrameLayout.LayoutParams layoutParams;

        layoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        deleteButton.setLayoutParams(layoutParams);

        layoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        rotateButton.setLayoutParams(layoutParams);

        layoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        sizeIncreaseButton.setLayoutParams(layoutParams);

        layoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        layoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        sizeDecreaseButton.setLayoutParams(layoutParams);
    }

    private void increaseSize() {
        int newWidth = getWidth() + gridSize;
        int newHeight = getHeight() + gridSize;
        setSize(newWidth, newHeight);
    }

    private void decreaseSize() {
        int newWidth = getWidth() - gridSize;
        int newHeight = getHeight() - gridSize;
        if (newWidth < 60) newWidth = 60;
        if (newHeight < 60) newHeight = 60;
        setSize(newWidth, newHeight);
    }

    // ★ 수정: 크기 변경 시 버튼 크기 반영
    private void setSize(int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = width;
        params.height = height;
        setLayoutParams(params);
        updateButtonSizes(); // 버튼 크기 재조정
    }

    public int getResId() {
        return resId;
    }

    public void hideBorderAndButtons() {
        borderView.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        rotateButton.setVisibility(View.GONE);
        sizeIncreaseButton.setVisibility(View.GONE);
        sizeDecreaseButton.setVisibility(View.GONE);
    }

    public void showBorderAndButtons() {
        borderView.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);
        rotateButton.setVisibility(View.VISIBLE);
        sizeIncreaseButton.setVisibility(View.VISIBLE);
        sizeDecreaseButton.setVisibility(View.VISIBLE);
        updateButtonSizes(); // ★ 수정: 보여줄 때도 크기 조정
    }

    public void setEditEnabled(boolean enabled) {
        isEditEnabled = enabled;
        setClickable(enabled);
        setFocusable(enabled);
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (!isEditEnabled) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = getX() - event.getRawX();
                    dY = getY() - event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;

                    newX = Math.round(newX / gridSize) * gridSize;
                    newY = Math.round(newY / gridSize) * gridSize;

                    View parent = (View) getParent();
                    if (parent != null) {
                        int parentWidth = parent.getWidth();
                        int parentHeight = parent.getHeight();

                        int itemWidth = getWidth();
                        int itemHeight = getHeight();

                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX > parentWidth - itemWidth) newX = parentWidth - itemWidth;
                        if (newY > parentHeight - itemHeight) newY = parentHeight - itemHeight;
                    }

                    setX(newX);
                    setY(newY);
                    break;

                case MotionEvent.ACTION_UP:
                    break;
            }
            return true;
        }
    };
}
