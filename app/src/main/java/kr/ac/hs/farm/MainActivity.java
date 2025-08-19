package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FarmPrefs";
    private static final String KEY_FOOD_COUNT = "foodCount";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_EXPERIENCE = "experience";

    private ImageButton characterButton;
    private LinearLayout characterMenu;
    private Button feedButton;
    private ImageButton exitButton;
    private ProgressBar levelProgressBar;
    private TextView levelText;
    private TextView foodCountText;
    private FrameLayout farmArea;
    private Button resetButton;

    private SharedPreferences prefs;
    private boolean isMenuVisible = false;
    private boolean isEditMode = false;

    private int foodCount = 3;
    private int level = 1;
    private int experience = 0;
    private final int MAX_EXPERIENCE = 100;

    private SpriteView spriteView;

    // 카메라 오프셋(배경 소스 좌상단)
    private int cameraLeft = 0;
    private int cameraTop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemInsets.left, systemInsets.top, systemInsets.right, systemInsets.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        characterButton = findViewById(R.id.characterButton);
        characterMenu = findViewById(R.id.characterMenu);
        feedButton = findViewById(R.id.feedButton);
        exitButton = findViewById(R.id.exitButton);
        levelProgressBar = findViewById(R.id.levelProgressBar);
        levelText = findViewById(R.id.levelText);
        foodCountText = findViewById(R.id.foodCountText);
        farmArea = findViewById(R.id.farmArea);
        resetButton = findViewById(R.id.resetButton);

        loadData();

        characterButton.setVisibility(View.GONE);

        spriteView = new SpriteView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        spriteView.setLayoutParams(params);
        farmArea.addView(spriteView, 0);
        spriteView.setOnSpriteClickListener(this::toggleCharacterMenu);

        // 카메라 변경 리스너: 카메라 오프셋 갱신 → 모든 아이템 화면좌표 재계산
        spriteView.setOnCameraChangeListener((left, top) -> {
            cameraLeft = left;
            cameraTop = top;
            applyCameraToAllItems();
        });

        spriteView.checkAndResetPosition();

        characterButton.setOnClickListener(v -> toggleCharacterMenu());
        feedButton.setOnClickListener(v -> giveFood());
        exitButton.setOnClickListener(v -> showExitDialog());

        findViewById(R.id.tab1Button).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.tab2Button).setOnClickListener(v -> startActivity(new Intent(this, Tab2Activity.class)));
        findViewById(R.id.tab3Button).setOnClickListener(v -> startActivity(new Intent(this, Tab3Activity.class)));
        findViewById(R.id.tab4Button).setOnClickListener(v -> startActivity(new Intent(this, Tab4Activity.class)));
        findViewById(R.id.tab6Button).setOnClickListener(v -> startActivity(new Intent(this, Tab6Activity.class)));

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("reward")) {
            int reward = intent.getIntExtra("reward", 0);
            foodCount += reward;
            saveData();
            Toast.makeText(this, "보상으로 먹이 " + reward + "개를 받았습니다!", Toast.LENGTH_SHORT).show();
        }

        updateUI();
        restoreAppliedItems();     // 저장 복원 (월드→화면)
        applyInventoryItem(intent);// 인벤토리에서 방금 적용한 항목 처리

        findViewById(R.id.editModeButton).setOnClickListener(v -> {
            setEditMode(true);
            Toast.makeText(this, "수정 모드로 전환되었습니다.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.editCompleteButton).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("수정 완료")
                    .setMessage("수정이 완료되었습니까?")
                    .setPositiveButton("네", (dialog, which) -> {
                        setEditMode(false);
                        saveAppliedItems(); // 월드 좌표로 저장
                        Toast.makeText(this, "수정이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        resetButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("초기화")
                    .setMessage("현재 진행된 인테리어를 모두 초기화하시겠습니까?")
                    .setPositiveButton("네", (dialog, which) -> {
                        for (int i = farmArea.getChildCount() - 1; i >= 0; i--) {
                            View child = farmArea.getChildAt(i);
                            if (child instanceof SelectableItemView) {
                                farmArea.removeViewAt(i);
                            }
                        }

                        String key = getItemKey();
                        if (key != null) {
                            prefs.edit().remove(key).apply();
                        }

                        SharedPreferences spritePrefs = getSharedPreferences("SpritePrefs", MODE_PRIVATE);
                        String userId = getCurrentUserId();
                        String bgKey = userId != null ? "selectedBackground_" + userId : "selectedBackground";
                        spritePrefs.edit().putInt(bgKey, R.drawable.tiles_grass).apply();

                        spriteView.reloadBackground();
                        spriteView.resetPositionToCenter();

                        // 배경 바뀐 뒤 월드 경계 재적용
                        applyWorldBoundsToAnimals();

                        Toast.makeText(this, "인테리어가 모두 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        // 시작 시 한 번 월드 경계 세팅
        applyWorldBoundsToAnimals();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (spriteView != null) spriteView.saveCharacterPosition();

        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).stopAnim();
                ((SelectableSpriteItemView) child).disableWander();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).startAnim();
                if (!isEditMode) {
                    ((SelectableSpriteItemView) child).enableWander(farmArea);
                }
            }
        }
        // 복귀 시 카메라 오프셋 다시 반영
        applyCameraToAllItems();
        // 월드 경계도 보장
        applyWorldBoundsToAnimals();
    }

    private String getCurrentUserId() {
        SharedPreferences loginPrefs = getSharedPreferences("login", MODE_PRIVATE);
        return loginPrefs.getBoolean("isLoggedIn", false)
                ? loginPrefs.getString("id", null)
                : null;
    }

    private String getItemKey() {
        String userId = getCurrentUserId();
        return userId != null ? "appliedItems_" + userId : null;
    }

    private void toggleCharacterMenu() {
        isMenuVisible = !isMenuVisible;
        characterMenu.setVisibility(isMenuVisible ? View.VISIBLE : View.GONE);
    }

    private void giveFood() {
        if (foodCount > 0) {
            foodCount--;
            experience += 20;
            Toast.makeText(this, "냥이에게 먹이를 줬어요! 남은 먹이: " + foodCount, Toast.LENGTH_SHORT).show();
            if (experience >= MAX_EXPERIENCE) {
                level++;
                experience -= MAX_EXPERIENCE;
                Toast.makeText(this, "레벨업! 현재 레벨: " + level, Toast.LENGTH_SHORT).show();
            }
            updateUI();
            saveData();
        } else {
            Toast.makeText(this, "먹이가 부족합니다! 더 구입해 주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        levelText.setText("LV " + level);
        levelProgressBar.setMax(MAX_EXPERIENCE);
        levelProgressBar.setProgress(experience);
        foodCountText.setText("먹이: " + foodCount);
        feedButton.setEnabled(foodCount > 0);
        feedButton.setAlpha(foodCount > 0 ? 1.0f : 0.5f);
    }

    private void saveData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FOOD_COUNT, foodCount);
        editor.putInt(KEY_LEVEL, level);
        editor.putInt(KEY_EXPERIENCE, experience);
        saveAppliedItems(); // 월드 좌표 저장
        editor.apply();
    }

    private void loadData() {
        foodCount = prefs.getInt(KEY_FOOD_COUNT, 3);
        level = prefs.getInt(KEY_LEVEL, 1);
        experience = prefs.getInt(KEY_EXPERIENCE, 0);
    }

    private void restoreAppliedItems() {
        String key = getItemKey();
        if (key == null) return;

        // 현재 카메라 오프셋 즉시 계산(첫 프레임 전에도 일관)
        int[] cam = spriteView != null ? spriteView.computeCurrentCameraLT() : new int[]{cameraLeft, cameraTop};
        float camLeftNow = cam[0];
        float camTopNow  = cam[1];

        String json = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                if (obj.has("isBackground") && obj.getBoolean("isBackground")) {
                    int bgResId = obj.getInt("resId");
                    prefs.edit().putInt("selectedBackground", bgResId).apply();
                    continue;
                }

                int resId = obj.getInt("resId");

                float worldX = (float) obj.optDouble("worldX", Float.NaN);
                float worldY = (float) obj.optDouble("worldY", Float.NaN);
                if (Float.isNaN(worldX) || Float.isNaN(worldY)) {
                    // 구버전 저장본 호환: x/y는 화면좌표
                    float screenX = (float) obj.getDouble("x");
                    float screenY = (float) obj.getDouble("y");
                    worldX = screenX + camLeftNow;
                    worldY = screenY + camTopNow;
                }

                int width = obj.getInt("width");
                int height = obj.getInt("height");
                float rotation = (float) obj.optDouble("rotation", 0);

                if (isAnimalRes(resId)) {
                    addAnimalToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
                } else {
                    addItemToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 카메라/월드 반영
        applyCameraToAllItems();
        applyWorldBoundsToAnimals();
    }

    private void saveAppliedItems() {
        String key = getItemKey();
        if (key == null) return;

        JSONArray array = new JSONArray();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableItemView) {
                SelectableItemView itemView = (SelectableItemView) child;
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("resId", itemView.getResId());
                    obj.put("worldX", itemView.getWorldX());
                    obj.put("worldY", itemView.getWorldY());
                    // 호환용 화면좌표도 함께 저장
                    obj.put("x", itemView.getX());
                    obj.put("y", itemView.getY());
                    obj.put("width", itemView.getWidth());
                    obj.put("height", itemView.getHeight());
                    obj.put("rotation", itemView.getRotation());
                    array.put(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        int bgResId = prefs.getInt("selectedBackground", R.drawable.tiles_grass);
        JSONObject bgObj = new JSONObject();
        try {
            bgObj.put("resId", bgResId);
            bgObj.put("isBackground", true);
            array.put(bgObj);
        } catch (Exception e) {
            e.printStackTrace();
        }

        prefs.edit().putString(key, array.toString()).apply();
    }

    private void applyInventoryItem(Intent intent) {
        if (intent != null && intent.hasExtra("appliedItemImageRes")) {
            int resId = intent.getIntExtra("appliedItemImageRes", 0);
            if (resId != 0) {
                // 현재 카메라 오프셋 즉시 계산해서 월드좌표 산출
                int[] cam = spriteView != null ? spriteView.computeCurrentCameraLT() : new int[]{cameraLeft, cameraTop};
                float camLeftNow = cam[0];
                float camTopNow  = cam[1];

                float initialScreenX = 300f;
                float initialScreenY = 100f;
                float worldX = initialScreenX + camLeftNow;
                float worldY = initialScreenY + camTopNow;

                if (isAnimalRes(resId)) {
                    addAnimalToFarmAreaWorld(resId, worldX, worldY, 120, 120, 0f);
                } else {
                    addItemToFarmAreaWorld(resId, worldX, worldY, 120, 120, 0f);
                }

                // 즉시 반영
                applyCameraToAllItems();
                applyWorldBoundsToAnimals();

                saveAppliedItems();
                setEditMode(true);
            }
        }
    }

    // 정적 아이템(월드 좌표)
    private void addItemToFarmAreaWorld(int resId, float worldX, float worldY, int width, int height, float rotation) {
        SelectableItemView itemView = new SelectableItemView(this, resId);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        itemView.setLayoutParams(params);
        itemView.setRotation(rotation);

        // 카메라/월드 초기화
        itemView.setCameraOffset(cameraLeft, cameraTop);
        itemView.setWorldPosition(worldX, worldY);

        itemView.setOnDoubleTapListener(() -> showDeleteConfirmDialog(itemView));
        itemView.setEditEnabled(isEditMode);
        if (isEditMode) itemView.showBorderAndButtons(); else itemView.hideBorderAndButtons();

        // 드래그 종료 시 월드 좌표 갱신
        itemView.setOnDragEndListener(v -> {
            v.setCameraOffset(cameraLeft, cameraTop);
            v.updateWorldFromScreen();
        });

        farmArea.addView(itemView);
    }

    // 동물(월드 좌표)
    private void addAnimalToFarmAreaWorld(int resId, float worldX, float worldY, int width, int height, float rotation) {
        SelectableSpriteItemView itemView = new SelectableSpriteItemView(this, resId);

        String entryName = safeEntryName(resId);
        if ("chicken".equals(entryName)) {
            int rows = 13, cols = 8;
            int[] idleRows = new int[]{10, 11};

            int[][] exclude = new int[rows][];
            exclude[0]  = new int[]{5,6,7,8};
            exclude[1]  = new int[]{8};
            exclude[2]  = new int[]{};
            exclude[3]  = new int[]{8};
            exclude[4]  = new int[]{8};
            exclude[5]  = new int[]{8};
            exclude[6]  = new int[]{8};
            exclude[7]  = new int[]{6,7,8};
            exclude[8]  = new int[]{5,6,7,8};
            exclude[9]  = new int[]{6,7,8};
            exclude[10] = new int[]{5,6,7,8};
            exclude[11] = new int[]{7,8};
            exclude[12] = new int[]{3,4,5,6,7,8};

            boolean[][] baseMask = makeIncludeMask(rows, cols, exclude);
            boolean[][] idleMask = filterRows(baseMask, rows, cols, idleRows);
            boolean[][] walkMask = subtractMasks(baseMask, idleMask);

            itemView.applyDualSpriteWithMasks(
                    R.drawable.chicken_sprites,
                    rows, cols, 8, 6,
                    walkMask, idleMask
            );

        } else if ("cow".equals(entryName)) {
            int rows = 7, cols = 8;

            int[][] exclude = new int[rows][];
            exclude[0] = new int[]{4,5,6,7,8};
            exclude[1] = new int[]{};
            exclude[2] = new int[]{8};
            exclude[3] = new int[]{4,5,6,7,8};
            exclude[4] = new int[]{5,6,7,8};
            exclude[5] = new int[]{8};
            exclude[6] = new int[]{5,6,7,8};

            boolean[][] baseMask = makeIncludeMask(rows, cols, exclude);
            boolean[][] idleMask = new boolean[rows][cols];

            int r3 = 2;
            int[] r3cols = new int[]{1, 3};
            for (int c1 : r3cols) {
                int c = c1 - 1;
                if (c >= 0 && c < cols && baseMask[r3][c]) idleMask[r3][c] = true;
            }
            int r4 = 3;
            for (int c = 0; c < cols; c++) if (baseMask[r4][c]) idleMask[r4][c] = true;
            int r5 = 4;
            for (int c = 0; c < cols; c++) if (baseMask[r5][c]) idleMask[r5][c] = true;
            int r7 = 6;
            for (int c = 0; c < cols; c++) if (baseMask[r7][c]) idleMask[r7][c] = true;

            boolean[][] walkMask = subtractMasks(baseMask, idleMask);

            itemView.applyDualSpriteWithMasks(
                    R.drawable.cow_sprites,
                    rows, cols, 8, 6,
                    walkMask, idleMask
            );

        } else {
            addItemToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
            return;
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        itemView.setLayoutParams(params);
        itemView.setRotation(rotation);

        // 카메라/월드 초기화
        itemView.setCameraOffset(cameraLeft, cameraTop);
        itemView.setWorldPosition(worldX, worldY);

        // ★ 월드 전체 경계 설정 (배경 전체에서 돌아다니도록)
        int worldW = spriteView.getWorldWidth();
        int worldH = spriteView.getWorldHeight();
        itemView.setWorldBounds(worldW, worldH);

        itemView.setOnDoubleTapListener(() -> showDeleteConfirmDialog(itemView));
        itemView.setEditEnabled(isEditMode);
        if (isEditMode) itemView.showBorderAndButtons();
        else itemView.hideBorderAndButtons();
        farmArea.addView(itemView);

        if (!isEditMode) {
            itemView.enableWander(farmArea);
            itemView.setWanderSpeed(15f);
        }

        // 드래그 종료 시 월드 좌표 갱신
        itemView.setOnDragEndListener(v -> {
            v.setCameraOffset(cameraLeft, cameraTop);
            v.updateWorldFromScreen();
        });
    }

    // ====== 마스크 유틸 ======
    private static boolean[][] makeIncludeMask(int rows, int cols, int[][] excludedCols1Based) {
        boolean[][] mask = new boolean[rows][cols];
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) mask[r][c]=true;
        if (excludedCols1Based != null) {
            for (int r=0;r<rows;r++) {
                int[] ex = excludedCols1Based[r];
                if (ex == null) continue;
                for (int col1 : ex) {
                    int c = col1 - 1;
                    if (c>=0 && c<cols) mask[r][c] = false;
                }
            }
        }
        return mask;
    }
    private static boolean[][] filterRows(boolean[][] baseMask, int rows, int cols, int[] keepRows) {
        boolean[] keep = new boolean[rows];
        for (int r : keepRows) if (r>=0 && r<rows) keep[r]=true;
        boolean[][] out = new boolean[rows][cols];
        for (int r=0;r<rows;r++) if (keep[r]) for (int c=0;c<cols;c++) out[r][c]=baseMask[r][c];
        return out;
    }
    private static boolean[][] subtractMasks(boolean[][] baseMask, boolean[][] idleMask) {
        int rows = baseMask.length;
        int cols = baseMask[0].length;
        boolean[][] out = new boolean[rows][cols];
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            out[r][c] = baseMask[r][c] && !(idleMask != null && idleMask[r][c]);
        }
        return out;
    }

    private boolean isAnimalRes(int resId) {
        String name = safeEntryName(resId);
        return "chicken".equals(name) || "cow".equals(name);
    }

    private String safeEntryName(int resId) {
        try {
            return getResources().getResourceEntryName(resId);
        } catch (Exception e) {
            return "";
        }
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableItemView) {
                SelectableItemView itemView = (SelectableItemView) child;
                itemView.setEditEnabled(enabled);
                if (enabled) itemView.showBorderAndButtons();
                else itemView.hideBorderAndButtons();
            }
            if (child instanceof SelectableSpriteItemView) {
                if (enabled) ((SelectableSpriteItemView) child).disableWander();
                else ((SelectableSpriteItemView) child).enableWander(farmArea);
            }
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("종료", (dialog, which) -> {
                    saveData();
                    finishAffinity();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteConfirmDialog(SelectableItemView itemView) {
        new AlertDialog.Builder(this)
                .setTitle("아이템 삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("네", (dialog, which) -> {
                    itemView.animate()
                            .alpha(0f).scaleX(0f).scaleY(0f).setDuration(300)
                            .withEndAction(() -> {
                                farmArea.removeView(itemView);
                                saveAppliedItems();
                            })
                            .start();
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    // 카메라 오프셋이 바뀔 때 모든 아이템의 화면 좌표 다시 계산
    private void applyCameraToAllItems() {
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableItemView) {
                SelectableItemView iv = (SelectableItemView) child;
                iv.setCameraOffset(cameraLeft, cameraTop);
                iv.applyScreenFromWorld();
            }
        }
    }

    // 배경(월드) 전체 크기를 모든 동물에게 전달
    private void applyWorldBoundsToAnimals() {
        if (spriteView == null) return;
        int worldW = spriteView.getWorldWidth();
        int worldH = spriteView.getWorldHeight();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).setWorldBounds(worldW, worldH);
            }
        }
    }
}
