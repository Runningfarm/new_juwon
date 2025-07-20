// MainActivity.java
package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
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
        restoreAppliedItems();
        applyInventoryItem(intent);

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
                        saveAppliedItems();
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
                        prefs.edit().putString(getItemKey(), "[]").apply();

                        spriteView.resetPositionToCenter();

                        Toast.makeText(this, "인테리어가 모두 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (spriteView != null) spriteView.saveCharacterPosition();
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
        saveAppliedItems();
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

        String json = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int resId = obj.getInt("resId");
                float x = (float) obj.getDouble("x");
                float y = (float) obj.getDouble("y");
                int width = obj.getInt("width");
                int height = obj.getInt("height");
                float rotation = (float) obj.optDouble("rotation", 0);
                addItemToFarmArea(resId, x, y, width, height, rotation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        prefs.edit().putString(key, array.toString()).apply();
    }

    private void applyInventoryItem(Intent intent) {
        if (intent != null && intent.hasExtra("appliedItemImageRes")) {
            int resId = intent.getIntExtra("appliedItemImageRes", 0);
            if (resId != 0) {
                addItemToFarmArea(resId, 300f, 100f, 200, 200, 0f);
                saveAppliedItems();
                setEditMode(true);
            }
        }
    }

    private void addItemToFarmArea(int resId, float x, float y, int width, int height, float rotation) {
        SelectableItemView itemView = new SelectableItemView(this, resId);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        itemView.setLayoutParams(params);
        itemView.setX(x);
        itemView.setY(y);
        itemView.setRotation(rotation);
        itemView.setOnDoubleTapListener(() -> showDeleteConfirmDialog(itemView));
        itemView.setEditEnabled(isEditMode);
        if (isEditMode) itemView.showBorderAndButtons();
        else itemView.hideBorderAndButtons();
        farmArea.addView(itemView);
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
}
