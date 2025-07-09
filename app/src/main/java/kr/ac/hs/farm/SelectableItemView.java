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
    private ImageView sizeIncreaseButton;  // 크기 증가 버튼
    private ImageView sizeDecreaseButton;  // 크기 감소 버튼

    private float dX, dY;
    private int gridSize = 30; // 격자 스냅 크기

    private float rotationDegrees = 0f;

    private GestureDetectorCompat gestureDetector;

    private int resId;

    private boolean isEditEnabled = false; // 수정 가능 여부

    // 인터페이스: 삭제 이벤트 처리용
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
        // 아이템 이미지뷰 생성
        itemImage = new ImageView(context);
        itemImage.setImageResource(resId);
        LayoutParams imgParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        itemImage.setLayoutParams(imgParams);
        addView(itemImage);

        // 테두리 뷰 생성
        borderView = new View(context);
        borderView.setBackgroundResource(R.drawable.selection_border);
        borderView.setLayoutParams(imgParams);
        addView(borderView);

        // 삭제 버튼 추가 (오른쪽 상단)
        deleteButton = new ImageView(context);
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        LayoutParams delParams = new LayoutParams(60, 60);
        delParams.gravity = Gravity.TOP | Gravity.END;
        deleteButton.setLayoutParams(delParams);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setOnClickListener(v -> {
            if (doubleTapListener != null) doubleTapListener.onDoubleTap();
        });
        addView(deleteButton);

        // 회전 버튼 추가 (오른쪽 하단)
        rotateButton = new ImageView(context);
        rotateButton.setImageResource(android.R.drawable.ic_menu_rotate);
        LayoutParams rotateParams = new LayoutParams(60, 60);
        rotateParams.gravity = Gravity.BOTTOM | Gravity.END;
        rotateButton.setLayoutParams(rotateParams);
        rotateButton.setPadding(8, 8, 8, 8);
        rotateButton.setOnClickListener(v -> {
            rotationDegrees = (rotationDegrees + 45f) % 360;
            setRotation(rotationDegrees);
        });
        addView(rotateButton);

        // 크기 증가 버튼 추가 (왼쪽 상단)
        sizeIncreaseButton = new ImageView(context);
        sizeIncreaseButton.setImageResource(android.R.drawable.arrow_up_float); // 적당한 아이콘으로 변경 가능
        LayoutParams incParams = new LayoutParams(60, 60);
        incParams.gravity = Gravity.TOP | Gravity.START;
        sizeIncreaseButton.setLayoutParams(incParams);
        sizeIncreaseButton.setPadding(8, 8, 8, 8);
        sizeIncreaseButton.setOnClickListener(v -> increaseSize());
        addView(sizeIncreaseButton);

        // 크기 감소 버튼 추가 (왼쪽 하단)
        sizeDecreaseButton = new ImageView(context);
        sizeDecreaseButton.setImageResource(android.R.drawable.arrow_down_float); // 적당한 아이콘으로 변경 가능
        LayoutParams decParams = new LayoutParams(60, 60);
        decParams.gravity = Gravity.BOTTOM | Gravity.START;
        sizeDecreaseButton.setLayoutParams(decParams);
        sizeDecreaseButton.setPadding(8, 8, 8, 8);
        sizeDecreaseButton.setOnClickListener(v -> decreaseSize());
        addView(sizeDecreaseButton);

        // 기본적으로 보이지 않도록 설정
        hideBorderAndButtons();

        // 제스처 설정
        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 더블탭 비활성화 (삭제 버튼으로 대체됨)
                return true;
            }
        });

        // 터치 리스너 설정 (항상 설정, isEditEnabled로 처리)
        setOnTouchListener(touchListener);

        // 터치 이벤트 수신 가능 여부 초기 세팅
        setClickable(false);
        setFocusable(false);
    }

    // 크기 증가 (격자 단위)
    private void increaseSize() {
        int newWidth = getWidth() + gridSize;
        int newHeight = getHeight() + gridSize;
        setSize(newWidth, newHeight);
    }

    // 크기 감소 (격자 단위, 최소 크기 60x60)
    private void decreaseSize() {
        int newWidth = getWidth() - gridSize;
        int newHeight = getHeight() - gridSize;
        if (newWidth < 60) newWidth = 60;
        if (newHeight < 60) newHeight = 60;
        setSize(newWidth, newHeight);
    }

    private void setSize(int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = width;
        params.height = height;
        setLayoutParams(params);
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
    }

    // 수정 가능 여부 세팅
    public void setEditEnabled(boolean enabled) {
        isEditEnabled = enabled;
        // 터치 이벤트 활성화 여부 반영
        setClickable(enabled);
        setFocusable(enabled);
    }

    private OnTouchListener touchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (!isEditEnabled) {
                // 수정 모드가 아닐 땐 터치 이동 불가
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

                    // 격자 단위로 스냅
                    newX = Math.round(newX / gridSize) * gridSize;
                    newY = Math.round(newY / gridSize) * gridSize;

                    // 부모 뷰(farmArea) 경계 제한 추가
                    View parent = (View) getParent();
                    if (parent != null) {
                        int parentWidth = parent.getWidth();
                        int parentHeight = parent.getHeight();

                        int itemWidth = getWidth();
                        int itemHeight = getHeight();

                        // 좌측 상단 경계 제한 (0 이하로 못 가게)
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;

                        // 우측 하단 경계 제한 (부모 크기 - 아이템 크기 이상 못 가게)
                        if (newX > parentWidth - itemWidth) newX = parentWidth - itemWidth;
                        if (newY > parentHeight - itemHeight) newY = parentHeight - itemHeight;
                    }

                    setX(newX);
                    setY(newY);
                    break;

                case MotionEvent.ACTION_UP:
                    // 이동 완료 시 저장할 필요 있으면 콜백 구현 가능
                    break;
            }
            return true;
        }
    };
}