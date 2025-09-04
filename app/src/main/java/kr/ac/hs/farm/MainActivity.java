// ─────────────────────────────────────────────────────────────────────────────
// MainActivity.java  (풀버전)
// - 울타리(펜스) 게이트: 기존 방식(가로 4칸 / 세로 5칸) 그대로 유지 (fence_gates)
// - 집/목장 구조물 문: door_animation_sprites 사용
//   * 1행(row 0): 집 문 16×16, 프레임 6개 (0=활짝 열림 → 5=완전 닫힘)
//   * 2행(row 1): 목장 구조물 문 48×16 (16×16×3칸), 프레임 6개
// - 기존 배치/연결/저장 로직 유지
// - 추가: 캐릭터 근접 시 울타리 게이트 자동 개폐(히스테리시스 포함)
// - 추가: 캐릭터 근접 시 집/목장 문 자동 개폐(히스테리시스 포함, 반경 고정)
// ─────────────────────────────────────────────────────────────────────────────
package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import android.view.MotionEvent;

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

    private int cameraLeft = 0, cameraTop = 0;

    // Grid
    public static final int GRID_PX = 64;
    private int fenceDisplaySizePx = 64;

    // Fence
    private FencePlacerOverlay fenceOverlay;
    private FenceAtlas fenceAtlas;
    private LinearLayout fenceModeBar;

    // ★ 목장 문 배치 토글/버퍼(= 펜스 설치 모드의 '문 위치' 표식용)
    private boolean ranchDoorPlacementOn = false;
    private final HashSet<Point> ranchDoorCellsBuffer = new HashSet<>();

    // House walls / Ranch structure walls
    private FencePlacerOverlay houseOverlay;
    private HouseAtlas houseAtlas;
    private LinearLayout houseModeBar;

    // ★ 집/구조물 문 배치 토글/버퍼(= 벽 설치 모드의 문 위치 표식용)
    private boolean houseDoorPlacementOn = false;
    private final HashSet<Point> houseDoorCellsBuffer = new HashSet<>();

    // ──[ 울타리 게이트용 태그(기존 유지) ]────────────────────────────────────────
    private static final int TAG_KEY_GATE = 0x7f0A0001;  // Boolean: 게이트 여부
    private static final int TAG_KEY_GATE_FRAME = 0x7f0A0002;  // Integer 0..4
    private static final int TAG_KEY_GATE_OPENING = 0x7f0A0003;  // Boolean
    private static final int TAG_KEY_GATE_LISTENER = 0x7f0A0004;  // Boolean
    private static final int TAG_KEY_GATE_VERTICAL = 0x7f0A0005;  // Boolean: 세로 게이트?
    private static final int TAG_KEY_GATE_GROUP = 0x7f0A0006;  // String: 그룹 ID
    private static final int TAG_KEY_GATE_SLICE = 0x7f0A0007;  // Integer: 조각 index(가로 0..3 / 세로 0..4)

    // ──[ 집/목장 구조물 '문' 태그(신규 애니) ]─────────────────────────────────
    private static final int TAG_IS_DOOR = 0x7f0A1001; // Boolean
    private static final int TAG_DOOR_TYPE = 0x7f0A1002; // String "RANCH" | "HOUSE"
    private static final int TAG_DOOR_GROUP = 0x7f0A1003; // String
    private static final int TAG_DOOR_SLICE = 0x7f0A1004; // Integer (RANCH: 0..2, HOUSE: 0)
    private static final int TAG_DOOR_FRAME = 0x7f0A1005; // Integer 0..5
    private static final int TAG_DOOR_LISTENER = 0x7f0A1006; // Boolean

    // ──[ 울타리 게이트 스프라이트(기존 유지) ]────────────────────────────────
    private Bitmap gateSheet;
    private Bitmap[][] gateHFrameSlices; // [frame 0..4][slice 0..3]
    private Bitmap[][] gateVFrameSlices; // [frame 0..4][slice 0..2]

    // ──[ 집/목장 구조물 문 스프라이트(신규 애니) ]────────────────────────────
    private static final int DOOR_FRAMES = 6;
    private Bitmap doorSheet;
    private Bitmap[] houseDoorFrames;          // [0..5]
    private Bitmap[][] ranchDoorFrameSlices;    // [0..5][slice 0..2]

    // 집/구조물 설치 모드가 '목장(축사/외양간)'인지 여부
    private boolean isRanchBuild = false;

    private final Handler ui = new Handler(Looper.getMainLooper());

    // ===== 자동 개폐 파라미터(히스테리시스) =====
    private static final float GATE_OPEN_RADIUS_PX = 90f;  // 이내로 들어오면 '열기' 목표
    private static final float GATE_CLOSE_RADIUS_PX = 120f; // 이 밖으로 나가면 '닫기' 목표

    // ★ 문(집/목장) 자동 개폐 반경(요청값)
    private static final float HOUSE_DOOR_OPEN_RADIUS = 70f;
    private static final float HOUSE_DOOR_CLOSE_RADIUS = 95f;
    private static final float RANCH_DOOR_OPEN_RADIUS = 80f;
    private static final float RANCH_DOOR_CLOSE_RADIUS = 105f;
    private static final long DOOR_AUTOCHECK_INTERVAL_MS = 120L;

    // 자동 게이트 루프
    private final Runnable gateAutoLoop = new Runnable() {
        @Override
        public void run() {
            try {
                updateGateAutoOpenClose();
            } catch (Throwable ignored) {
            }
            ui.postDelayed(this, 90); // ~11 FPS
        }
    };

    // 문 자동 체크용 상태/루프
    private static final class DoorGroupState {
        float cx, cy;      // 그룹 중심 좌표(월드)
        boolean isOpen;    // 현재 열림 상태(프레임 기준)
        String type;       // "HOUSE" | "RANCH"
    }

    private final HashMap<String, DoorGroupState> doorGroups = new HashMap<>();
    private final Runnable doorAutoCheck = new Runnable() {
        @Override
        public void run() {
            try {
                autoCheckDoors();
            } catch (Throwable ignored) {
            }
            ui.postDelayed(this, DOOR_AUTOCHECK_INTERVAL_MS);
        }
    };

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private void shrinkButton(Button b) {
        b.setMinWidth(0);
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6); // 버튼 사이 간격
        b.setLayoutParams(lp);
    }

    // 설치 세션 UNDO 스택 & 삭제모드 토글
    private final ArrayDeque<ArrayList<View>> fenceUndoStack = new ArrayDeque<>();
    private final ArrayDeque<ArrayList<View>> houseUndoStack = new ArrayDeque<>();
    private boolean fenceDeleteSelectOn = false;
    private boolean houseDeleteSelectOn = false;

    // 삭제 모드 임시 태그
    private static final int TAG_TMP_DELETE_MODE = 0x7f0A2001;

    private static final int ID_TOOLBAR_PANEL = 0x7f0B0010; // 임의 고유값

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets si = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(si.left, si.top, si.right, si.bottom);
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

        spriteView.setOnCameraChangeListener((left, top) -> {
            cameraLeft = left;
            cameraTop = top;
            applyCameraToAllItems();
            if (fenceOverlay != null) fenceOverlay.setCamera(left, top);
            if (houseOverlay != null) houseOverlay.setCamera(left, top);
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
        restoreAppliedItems();
        applyInventoryItem(intent);

        // ==== 툴 모드 진입 ====
        boolean needFenceMode = intent != null && intent.getBooleanExtra("applyFenceTool", false);
        if (needFenceMode) {
            int atlasResId = intent.getIntExtra("fenceAtlasResId", R.drawable.fences);
            enterFenceMode(atlasResId);
        }
        boolean needHouseMode = intent != null && intent.getBooleanExtra("applyHouseTool", false);
        if (needHouseMode) {
            int atlasResId = intent.getIntExtra("houseAtlasResId", R.drawable.wooden_house_walls);
            String okText = intent.getStringExtra("toolOkText");
            enterHouseMode(atlasResId, okText);
        }

        findViewById(R.id.editModeButton).setOnClickListener(v -> {
            setEditMode(true);
            Toast.makeText(this, "수정 모드로 전환되었습니다.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.editCompleteButton).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("수정 완료")
                    .setMessage("수정이 완료되었습니까?")
                    .setPositiveButton("네", (d, w) -> {
                        setEditMode(false);
                        saveAppliedItems();
                        exitFenceMode();
                        exitHouseMode();
                        Toast.makeText(this, "수정이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        resetButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("초기화")
                    .setMessage("현재 진행된 인테리어를 모두 초기화하시겠습니까?")
                    .setPositiveButton("네", (d, w) -> {
                        for (int i = farmArea.getChildCount() - 1; i >= 0; i--) {
                            View child = farmArea.getChildAt(i);
                            if (child instanceof SelectableItemView) farmArea.removeViewAt(i);
                        }
                        String key = getItemKey();
                        if (key != null) prefs.edit().remove(key).apply();

                        SharedPreferences sp = getSharedPreferences("SpritePrefs", MODE_PRIVATE);
                        String userId = getCurrentUserId();
                        String bgKey = userId != null ? "selectedBackground_" + userId : "selectedBackground";
                        sp.edit().putInt(bgKey, R.drawable.tiles_grass).apply();

                        spriteView.reloadBackground();
                        spriteView.resetPositionToCenter();
                        applyWorldBoundsToAnimals();
                        Toast.makeText(this, "인테리어가 모두 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        applyWorldBoundsToAnimals();

        // ★ 자동 게이트 루프 시작
        ui.postDelayed(gateAutoLoop, 300);

        // ★ 문 자동 체크 초기화
        rebuildDoorGroups();
        ui.postDelayed(doorAutoCheck, 200);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (spriteView != null) spriteView.saveCharacterPosition();
        if (fenceAtlas != null) {
            fenceAtlas.dispose();
            fenceAtlas = null;
        }
        if (houseAtlas != null) {
            houseAtlas.dispose();
            houseAtlas = null;
        }

        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).stopAnim();
                ((SelectableSpriteItemView) child).disableWander();
            }
        }

        // ★ 자동 루프 중단
        ui.removeCallbacks(gateAutoLoop);
        ui.removeCallbacks(doorAutoCheck);
        ui.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        ui.removeCallbacksAndMessages(null);
        super.onResume();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableSpriteItemView) {
                ((SelectableSpriteItemView) child).startAnim();
                if (!isEditMode) ((SelectableSpriteItemView) child).enableWander(farmArea);
            }
        }
        applyCameraToAllItems();
        applyWorldBoundsToAnimals();

        // ★ 자동 루프 재시작
        ui.removeCallbacks(gateAutoLoop);
        ui.postDelayed(gateAutoLoop, 300);

        rebuildDoorGroups();
        ui.removeCallbacks(doorAutoCheck);
        ui.postDelayed(doorAutoCheck, 200);
    }

    private String getCurrentUserId() {
        SharedPreferences loginPrefs = getSharedPreferences("login", MODE_PRIVATE);
        return loginPrefs.getBoolean("isLoggedIn", false)
                ? loginPrefs.getString("id", null) : null;
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
        } else Toast.makeText(this, "먹이가 부족합니다! 더 구입해 주세요.", Toast.LENGTH_SHORT).show();
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
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(KEY_FOOD_COUNT, foodCount);
        ed.putInt(KEY_LEVEL, level);
        ed.putInt(KEY_EXPERIENCE, experience);
        saveAppliedItems();
        ed.apply();
    }

    private void loadData() {
        foodCount = prefs.getInt(KEY_FOOD_COUNT, 3);
        level = prefs.getInt(KEY_LEVEL, 1);
        experience = prefs.getInt(KEY_EXPERIENCE, 0);
    }

    // ===== 저장 복원 =====
    private void restoreAppliedItems() {
        String key = getItemKey();
        if (key == null) return;

        int[] cam = spriteView != null ? spriteView.computeCurrentCameraLT() : new int[]{cameraLeft, cameraTop};
        float camLeftNow = cam[0], camTopNow = cam[1];

        String json = prefs.getString(key, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                if (obj.optBoolean("isBackground", false)) {
                    int bgResId = obj.getInt("resId");
                    SharedPreferences sp = getSharedPreferences("SpritePrefs", MODE_PRIVATE);
                    String userId = getCurrentUserId();
                    String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";
                    sp.edit().putInt(bgKey, bgResId).apply();
                    if (spriteView != null) spriteView.reloadBackground();
                    continue;
                }

                int resId = obj.getInt("resId");
                float worldX = (float) obj.optDouble("worldX", Float.NaN);
                float worldY = (float) obj.optDouble("worldY", Float.NaN);
                if (Float.isNaN(worldX) || Float.isNaN(worldY)) {
                    float screenX = (float) obj.getDouble("x");
                    float screenY = (float) obj.getDouble("y");
                    worldX = screenX + camLeftNow;
                    worldY = screenY + camTopNow;
                }

                int width = obj.getInt("width");
                int height = obj.getInt("height");
                float rotation = (float) obj.optDouble("rotation", 0);

                boolean isFence = obj.optBoolean("isFence", false);
                boolean isDoor = obj.optBoolean("isDoor", false);
                boolean isGate = obj.optBoolean("isGate", false); // 구버전/게이트 복원

                if (isFence) {
                    int atlasResId = obj.optInt("fenceAtlasResId", R.drawable.fences);
                    int mask = obj.optInt("fenceMask", 0);
                    int gx = obj.optInt("gridX", Math.round(worldX / GRID_PX));
                    int gy = obj.optInt("gridY", Math.round(worldY / GRID_PX));

                    Bitmap tile16;
                    if (isHouseAtlas(atlasResId)) {
                        if (houseAtlas == null) houseAtlas = new HouseAtlas(this, atlasResId, 16);
                        tile16 = (mask > 15) ? houseAtlas.getFloorBitmap()
                                : houseAtlas.getByMask(mask);
                    } else {
                        if (fenceAtlas == null) fenceAtlas = new FenceAtlas(this, atlasResId);
                        tile16 = fenceAtlas.getByMask(mask);
                    }

                    SelectableFenceView fv = new SelectableFenceView(this, tile16, fenceDisplaySizePx, mask, atlasResId);
                    fv.setFenceGridCell(gx, gy);
                    fv.setFenceMode(true);
                    fv.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
                    fv.setRotation(rotation);
                    fv.setCameraOffset(cameraLeft, cameraTop);
                    fv.setWorldPosition(gx * GRID_PX, gy * GRID_PX);

                    // 게이트 복원
                    if (isGate) {
                        fv.setTag(TAG_KEY_GATE, Boolean.TRUE);
                        fv.setTag(TAG_KEY_GATE_GROUP, obj.optString("gateGroup", gx + "," + gy));
                        fv.setTag(TAG_KEY_GATE_SLICE, obj.optInt("gateSlice", 0));
                        fv.setTag(TAG_KEY_GATE_FRAME, obj.optInt("gateFrame", 0));
                        fv.setTag(TAG_KEY_GATE_VERTICAL, obj.optBoolean("gateVertical", false));
                        fv.setTag(TAG_KEY_GATE_OPENING, obj.optBoolean("gateOpening", false));
                    }

                    // 문(집/구조물) 복원
                    if (isDoor) {
                        fv.setTag(TAG_IS_DOOR, Boolean.TRUE);
                        fv.setTag(TAG_DOOR_GROUP, obj.optString("doorGroup", gx + "," + gy));
                        fv.setTag(TAG_DOOR_SLICE, obj.optInt("doorSlice", 0));
                        fv.setTag(TAG_DOOR_FRAME, obj.optInt("doorFrame", 5)); // 기본 5(닫힘)로 보정
                        String type = obj.optString("doorType", null);
                        if (type == null) type = isHouseAtlas(atlasResId) ? "HOUSE" : "RANCH";
                        fv.setTag(TAG_DOOR_TYPE, type);
                    }

                    fv.setEditEnabled(isEditMode);
                    if (isEditMode) fv.showBorderAndButtons();
                    else fv.hideBorderAndButtons();
                    fv.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
                    farmArea.addView(fv);
                    continue;
                }

                if (isAnimalRes(resId)) {
                    addAnimalToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
                } else {
                    addItemToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        recalcAllGridMasks();
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
                SelectableItemView v = (SelectableItemView) child;
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("resId", v.getResId());
                    obj.put("worldX", v.getWorldX());
                    obj.put("worldY", v.getWorldY());
                    obj.put("x", v.getX());
                    obj.put("y", v.getY());
                    obj.put("width", v.getWidth());
                    obj.put("height", v.getHeight());
                    obj.put("rotation", v.getRotation());

                    Integer fenceMask = v.getFenceMaskTag();
                    Integer atlasTag = v.getAtlasResIdTag();
                    Integer gxTag = v.getFenceGridXTag();
                    Integer gyTag = v.getFenceGridYTag();
                    if (fenceMask != null && atlasTag != null) {
                        obj.put("isFence", true);
                        obj.put("fenceMask", fenceMask);
                        obj.put("fenceAtlasResId", atlasTag);
                        if (gxTag != null) obj.put("gridX", gxTag);
                        if (gyTag != null) obj.put("gridY", gyTag);

                        // 게이트 저장(울타리)
                        if (Boolean.TRUE.equals(v.getTag(TAG_KEY_GATE))) {
                            obj.put("isGate", true);
                            Object grp = v.getTag(TAG_KEY_GATE_GROUP);
                            if (grp != null) obj.put("gateGroup", grp.toString());
                            Object sl = v.getTag(TAG_KEY_GATE_SLICE);
                            if (sl instanceof Integer) obj.put("gateSlice", (Integer) sl);
                            Object fr = v.getTag(TAG_KEY_GATE_FRAME);
                            if (fr instanceof Integer) obj.put("gateFrame", (Integer) fr);
                            Object vt = v.getTag(TAG_KEY_GATE_VERTICAL);
                            if (vt instanceof Boolean) obj.put("gateVertical", (Boolean) vt);
                            Object op = v.getTag(TAG_KEY_GATE_OPENING);
                            if (op instanceof Boolean) obj.put("gateOpening", (Boolean) op);
                        }

                        // 문 저장(집/구조물)
                        if (Boolean.TRUE.equals(v.getTag(TAG_IS_DOOR))) {
                            obj.put("isDoor", true);
                            Object grp = v.getTag(TAG_DOOR_GROUP);
                            if (grp != null) obj.put("doorGroup", grp.toString());
                            Object sl = v.getTag(TAG_DOOR_SLICE);
                            if (sl instanceof Integer) obj.put("doorSlice", (Integer) sl);
                            Object fr = v.getTag(TAG_DOOR_FRAME);
                            if (fr instanceof Integer) obj.put("doorFrame", (Integer) fr);
                            Object tp = v.getTag(TAG_DOOR_TYPE);
                            if (tp != null) obj.put("doorType", tp.toString());
                        }
                    }

                    array.put(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        SharedPreferences sp = getSharedPreferences("SpritePrefs", MODE_PRIVATE);
        String userId = getCurrentUserId();
        String bgKey = (userId != null) ? "selectedBackground_" + userId : "selectedBackground";
        int bgResId = sp.getInt(bgKey, R.drawable.tiles_grass);
        prefs.edit().putString(getItemKey(), array.toString()).apply();

    }

    private void applyInventoryItem(Intent intent) {
        if (intent != null && intent.hasExtra("appliedItemImageRes")) {
            int resId = intent.getIntExtra("appliedItemImageRes", 0);
            if (resId != 0) {
                int[] cam = spriteView != null ? spriteView.computeCurrentCameraLT() : new int[]{cameraLeft, cameraTop};
                float worldX = 300f + cam[0], worldY = 100f + cam[1];

                if (isAnimalRes(resId))
                    addAnimalToFarmAreaWorld(resId, worldX, worldY, 120, 120, 0f);
                else addItemToFarmAreaWorld(resId, worldX, worldY, 120, 120, 0f);

                applyCameraToAllItems();
                applyWorldBoundsToAnimals();
                saveAppliedItems();
                setEditMode(true);
            }
        }
    }

    // ===== 정적 아이템 =====
    private void addItemToFarmAreaWorld(int resId, float worldX, float worldY, int width, int height, float rotation) {
        SelectableItemView v = new SelectableItemView(this, resId);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width, height);
        v.setLayoutParams(p);
        v.setRotation(rotation);
        v.setCameraOffset(cameraLeft, cameraTop);
        v.setWorldPosition(worldX, worldY);
        v.setOnDoubleTapListener(() -> showDeleteConfirmDialog(v));
        v.setEditEnabled(isEditMode);
        if (isEditMode) v.showBorderAndButtons();
        else v.hideBorderAndButtons();
        v.setOnDragEndListener(iv -> {
            iv.setCameraOffset(cameraLeft, cameraTop);
            iv.updateWorldFromScreen();
        });
        farmArea.addView(v);
    }

    // ===== 동물 =====
    private void addAnimalToFarmAreaWorld(int resId, float worldX, float worldY, int width, int height, float rotation) {
        SelectableSpriteItemView itemView = new SelectableSpriteItemView(this, resId);
        String entryName = safeEntryName(resId);
        if ("chicken".equals(entryName)) {
            int rows = 13, cols = 8;
            int[] idleRows = new int[]{10, 11};
            int[][] ex = new int[rows][];
            ex[0] = new int[]{5, 6, 7, 8};
            ex[1] = new int[]{};
            ex[2] = new int[]{};
            ex[3] = new int[]{};
            ex[4] = new int[]{};
            ex[5] = new int[]{};
            ex[6] = new int[]{};
            ex[7] = new int[]{6, 7, 8};
            ex[8] = new int[]{5, 6, 7, 8};
            ex[9] = new int[]{6, 7, 8};
            ex[10] = new int[]{5, 6, 7, 8};
            ex[11] = new int[]{7, 8};
            ex[12] = new int[]{3, 4, 5, 6, 7, 8};
            boolean[][] base = makeIncludeMask(rows, cols, ex);
            boolean[][] idle = filterRows(base, rows, cols, idleRows);
            boolean[][] walk = subtractMasks(base, idle);
            itemView.applyDualSpriteWithMasks(R.drawable.chicken_sprites, rows, cols, 8, 6, walk, idle);

        } else if ("cow".equals(entryName)) {
            int rows = 7, cols = 8;
            int[][] ex = new int[rows][];
            ex[0] = new int[]{4, 5, 6, 7, 8};
            ex[1] = new int[]{};
            ex[2] = new int[]{8};
            ex[3] = new int[]{4, 5, 6, 7, 8};
            ex[4] = new int[]{5, 6, 7, 8};
            ex[5] = new int[]{8};
            ex[6] = new int[]{5, 6, 7, 8};
            boolean[][] base = makeIncludeMask(rows, cols, ex);
            boolean[][] idle = new boolean[rows][cols];
            int r3 = 2;
            for (int c1 : new int[]{1, 3}) {
                int c = c1 - 1;
                if (c >= 0 && c < cols && base[r3][c]) idle[r3][c] = true;
            }
            int r4 = 3;
            for (int c = 0; c < cols; c++) if (base[r4][c]) idle[r4][c] = true;
            int r5 = 4;
            for (int c = 0; c < cols; c++) if (base[r5][c]) idle[r5][c] = true;
            int r7 = 6;
            for (int c = 0; c < cols; c++) if (base[r7][c]) idle[r7][c] = true;
            boolean[][] walk = subtractMasks(base, idle);
            itemView.applyDualSpriteWithMasks(R.drawable.cow_sprites, rows, cols, 8, 6, walk, idle);

        } else {
            addItemToFarmAreaWorld(resId, worldX, worldY, width, height, rotation);
            return;
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        itemView.setLayoutParams(params);
        itemView.setRotation(rotation);
        itemView.setCameraOffset(cameraLeft, cameraTop);
        itemView.setWorldPosition(worldX, worldY);

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

        itemView.setOnDragEndListener(v -> {
            v.setCameraOffset(cameraLeft, cameraTop);
            v.updateWorldFromScreen();
        });
    }

    private static boolean[][] makeIncludeMask(int rows, int cols, int[][] excludedCols1Based) {
        boolean[][] mask = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mask[r][c] = true;
        if (excludedCols1Based != null) {
            for (int r = 0; r < rows; r++) {
                int[] ex = excludedCols1Based[r];
                if (ex == null) continue;
                for (int col1 : ex) {
                    int c = col1 - 1;
                    if (c >= 0 && c < cols) mask[r][c] = false;
                }
            }
        }
        return mask;
    }

    private static boolean[][] filterRows(boolean[][] baseMask, int rows, int cols, int[] keepRows) {
        boolean[] keep = new boolean[rows];
        for (int r : keepRows) if (r >= 0 && r < rows) keep[r] = true;
        boolean[][] out = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            if (keep[r]) for (int c = 0; c < cols; c++) out[r][c] = baseMask[r][c];
        return out;
    }

    private static boolean[][] subtractMasks(boolean[][] baseMask, boolean[][] idleMask) {
        int rows = baseMask.length, cols = baseMask[0].length;
        boolean[][] out = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                out[r][c] = baseMask[r][c] && !(idleMask != null && idleMask[r][c]);
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
                SelectableItemView iv = (SelectableItemView) child;
                iv.setEditEnabled(enabled);
                if (enabled) iv.showBorderAndButtons();
                else iv.hideBorderAndButtons();
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
                .setPositiveButton("종료", (d, w) -> {
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
                .setPositiveButton("네", (d, w) -> {
                    itemView.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(300)
                            .withEndAction(() -> {
                                farmArea.removeView(itemView);
                                saveAppliedItems();
                            })
                            .start();
                })
                .setNegativeButton("아니오", null)
                .show();
    }

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

    // ===== Fence 설치 모드 =====
    private void enterFenceMode(int atlasResId) {
        exitFenceMode();
        if (fenceOverlay != null) exitFenceMode();
        fenceAtlas = new FenceAtlas(this, atlasResId);

        fenceOverlay = new FencePlacerOverlay(this, GRID_PX, masks -> commitFences(masks, atlasResId));
        fenceOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        fenceOverlay.setCamera(cameraLeft, cameraTop);

        View editBtn = findViewById(R.id.editModeButton);
        View doneBtn = findViewById(R.id.editCompleteButton);
        View resetBtn = findViewById(R.id.resetButton);
        fenceOverlay.setExclusionViews(editBtn, doneBtn, resetBtn);

        fenceModeBar = buildTopBar(
                "설치 완료",
                v -> {
                    commitRanchDoorCellsIfAny(atlasResId);
                    saveAppliedItems();
                    exitFenceMode();
                    Toast.makeText(this, "울타리 설치 모드를 종료했습니다.", Toast.LENGTH_SHORT).show();
                },
                "설치 모드 취소",
                v -> {
                    ranchDoorCellsBuffer.clear();
                    if (fenceOverlay != null) fenceOverlay.clearGateCells();
                    exitFenceMode();
                    Toast.makeText(this, "설치 모드를 취소했습니다.", Toast.LENGTH_SHORT).show();
                }
        );

        // 버튼은 panel 에만 추가합니다 (bar 에 다시 addView 금지)
        LinearLayout panel = (LinearLayout) fenceModeBar.getTag(ID_TOOLBAR_PANEL);

        Button btnGate = new Button(this);
        btnGate.setAllCaps(false);
        btnGate.setText(ranchDoorPlacementOn ? "문 배치 ON" : "문 배치 OFF");
        btnGate.setOnClickListener(v -> {
            ranchDoorPlacementOn = !ranchDoorPlacementOn;
            fenceOverlay.setGateMode(ranchDoorPlacementOn);
            btnGate.setText(ranchDoorPlacementOn ? "문 배치 ON" : "문 배치 OFF");

            if (!ranchDoorPlacementOn && !fenceOverlay.getGateCells().isEmpty()) {
                Map<Point, Integer> toCommit = new HashMap<>();
                for (Point p : fenceOverlay.getGateCells()) toCommit.put(new Point(p), 0);
                commitFences(toCommit, atlasResId);
                fenceOverlay.clearGateCells();
            }
        });
        shrinkButton(btnGate);

        Button btnUndo = new Button(this);
        btnUndo.setAllCaps(false);
        btnUndo.setText("되돌리기");
        btnUndo.setOnClickListener(v -> undoFenceOneStep());
        shrinkButton(btnUndo);

        Button btnDel = new Button(this);
        btnDel.setAllCaps(false);
        btnDel.setText("선택 삭제");
        btnDel.setOnClickListener(v -> toggleFenceSelectDelete(atlasResId));
        shrinkButton(btnDel);

        // panel 에만 추가
        panel.addView(btnGate, 0);
        panel.addView(btnUndo, 1);
        panel.addView(btnDel, 2);

        // overlay / bar 는 한 번만 추가
        farmArea.addView(fenceOverlay);
        farmArea.addView(fenceModeBar);

        Toast.makeText(this, "설치할 위치를 드래그하여 배치하세요.", Toast.LENGTH_SHORT).show();
    }


    private void exitFenceMode() {
        if (fenceModeBar != null) {
            farmArea.removeView(fenceModeBar);
            fenceModeBar = null;
        }
        if (fenceOverlay != null) {
            ranchDoorCellsBuffer.addAll(fenceOverlay.getGateCells());
            fenceOverlay.clearGateCells();
            fenceOverlay.setGateMode(false);
            farmArea.removeView(fenceOverlay);
            fenceOverlay = null;
        }
        ranchDoorPlacementOn = false;
        if (fenceAtlas != null) {
            fenceAtlas.dispose();
            fenceAtlas = null;
        }
        detachDeleteListenersForAtlas(-1 /*모두 무시하고 전부 해제하기 위해*/); // atlas 체크는 내부서 함
        fenceDeleteSelectOn = false;
        fenceUndoStack.clear();
    }

    private void commitRanchDoorCellsIfAny(int atlasResId) {
        HashSet<Point> left = new HashSet<>(ranchDoorCellsBuffer);
        if (fenceOverlay != null) left.addAll(fenceOverlay.getGateCells());
        if (left.isEmpty()) return;

        Map<Point, Integer> toCommit = new HashMap<>();
        for (Point p : left) toCommit.put(new Point(p), 0);
        ranchDoorCellsBuffer.clear();
        if (fenceOverlay != null) fenceOverlay.clearGateCells();

        commitFences(toCommit, atlasResId);
    }

    private void commitFences(Map<Point, Integer> cells, int atlasResId) {
        if (fenceAtlas == null) return;

        HashMap<String, SelectableFenceView> current = collectFenceMapByAtlas(atlasResId);

        ArrayList<View> createdThisCommit = new ArrayList<>();

        for (Map.Entry<Point, Integer> e : cells.entrySet()) {
            Point cell = e.getKey();
            int gx = cell.x, gy = cell.y;

            boolean isGateCell = (fenceOverlay != null && fenceOverlay.getGateCells().contains(cell))
                    || ranchDoorCellsBuffer.contains(cell);

            if (isGateCell) {
                boolean vertical = isGateVerticalSpot(current, gx, gy);
                // ▼ 교체: 생성된 뷰들을 받아서 누적
                ArrayList<View> g = placeGateGroup(gx, gy, atlasResId, vertical);
                createdThisCommit.addAll(g);
                continue;
            }

            float worldX = gx * GRID_PX + cameraLeft;
            float worldY = gy * GRID_PX + cameraTop;

            SelectableFenceView fv = new SelectableFenceView(
                    this, fenceAtlas.getByMask(0), fenceDisplaySizePx, 0, atlasResId
            );
            fv.setFenceGridCell(gx, gy);
            fv.setFenceMode(true);
            fv.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
            fv.setCameraOffset(cameraLeft, cameraTop);
            fv.setWorldPosition(worldX, worldY);
            fv.setEditEnabled(isEditMode);
            if (isEditMode) fv.showBorderAndButtons();
            else fv.hideBorderAndButtons();

            fv.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
            fv.setScaleX(0.6f);
            fv.setScaleY(0.6f);
            fv.setAlpha(0f);
            farmArea.addView(fv);
            createdThisCommit.add(fv);

            fv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        }

        recalcAllGridMasks();
        pushFenceUndo(createdThisCommit);
    }

    private void onFenceDragEnd(SelectableFenceView v) {
        v.updateWorldFromScreen();
        int gx = Math.round(v.getWorldX() / GRID_PX);
        int gy = Math.round(v.getWorldY() / GRID_PX);
        v.setFenceGridCell(gx, gy);
        v.setWorldPosition(gx * GRID_PX, gy * GRID_PX);
        v.setCameraOffset(cameraLeft, cameraTop);
        recalcAllGridMasks();
        saveAppliedItems();
    }

    // ===== House/Ranch-Structure 설치 모드 =====
    private void enterHouseMode(int atlasResId, String okTextFromCaller) {
        exitHouseMode();
        if (houseOverlay != null) exitHouseMode();
        houseAtlas = new HouseAtlas(this, atlasResId, 16);

        isRanchBuild = false;
        if (okTextFromCaller != null) {
            String t = okTextFromCaller.toLowerCase();
            if (t.contains("목장") || t.contains("축사") || t.contains("외양간")
                    || t.contains("ranch") || t.contains("barn") || t.contains("stable")) {
                isRanchBuild = true;
            }
        }

        houseOverlay = new FencePlacerOverlay(this, GRID_PX, masks -> commitHouseWalls(masks, atlasResId));
        houseOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        houseOverlay.setCamera(cameraLeft, cameraTop);

        View editBtn = findViewById(R.id.editModeButton);
        View doneBtn = findViewById(R.id.editCompleteButton);
        View resetBtn = findViewById(R.id.resetButton);
        houseOverlay.setExclusionViews(editBtn, doneBtn, resetBtn);

        final String okText = "설치 완료";
        houseModeBar = buildTopBar(
                okText,
                v -> {
                    commitHouseDoorCellsIfAny(atlasResId);
                    saveAppliedItems();
                    exitHouseMode();
                    Toast.makeText(this,
                            (isRanchBuild ? "목장 설치를 완료했습니다." : "집 설치를 완료했습니다."),
                            Toast.LENGTH_SHORT).show();
                },
                "설치 모드 취소",
                v -> {
                    houseDoorCellsBuffer.clear();
                    if (houseOverlay != null) houseOverlay.clearGateCells();
                    exitHouseMode();
                    Toast.makeText(this, "설치 모드를 취소했습니다.", Toast.LENGTH_SHORT).show();
                }
        );

        // 버튼은 panel 에만 추가
        LinearLayout panel = (LinearLayout) houseModeBar.getTag(ID_TOOLBAR_PANEL);

        Button btnDoor = new Button(this);
        btnDoor.setAllCaps(false);
        btnDoor.setText(houseDoorPlacementOn ? "문 배치 ON" : "문 배치 OFF");
        btnDoor.setOnClickListener(v -> {
            houseDoorPlacementOn = !houseDoorPlacementOn;
            houseOverlay.setGateMode(houseDoorPlacementOn);
            btnDoor.setText(houseDoorPlacementOn ? "문 배치 ON" : "문 배치 OFF");

            if (!houseDoorPlacementOn && !houseOverlay.getGateCells().isEmpty()) {
                for (Point p : houseOverlay.getGateCells()) placeHouseDoor(p.x, p.y, atlasResId);
                houseOverlay.clearGateCells();
                recalcAllGridMasks();
            }
        });
        shrinkButton(btnDoor);

        Button btnUndoH = new Button(this);
        btnUndoH.setAllCaps(false);
        btnUndoH.setText("되돌리기");
        btnUndoH.setOnClickListener(v -> undoHouseOneStep());
        shrinkButton(btnUndoH);

        Button btnDelH = new Button(this);
        btnDelH.setAllCaps(false);
        btnDelH.setText("선택 삭제");
        btnDelH.setOnClickListener(v -> toggleHouseSelectDelete(atlasResId));
        shrinkButton(btnDelH);

        // panel 에만 추가
        panel.addView(btnDoor, 0);
        panel.addView(btnUndoH, 1);
        panel.addView(btnDelH, 2);

        // overlay / bar 는 한 번만 추가
        farmArea.addView(houseOverlay);
        farmArea.addView(houseModeBar);

        Toast.makeText(this, "설치할 위치를 드래그하여 배치하세요.", Toast.LENGTH_SHORT).show();
    }


    private void exitHouseMode() {
        if (houseModeBar != null) {
            farmArea.removeView(houseModeBar);
            houseModeBar = null;
        }
        if (houseOverlay != null) {
            houseDoorCellsBuffer.addAll(houseOverlay.getGateCells());
            houseOverlay.clearGateCells();
            houseOverlay.setGateMode(false);
            farmArea.removeView(houseOverlay);
            houseOverlay = null;
        }
        houseDoorPlacementOn = false;
        if (houseAtlas != null) {
            houseAtlas.dispose();
            houseAtlas = null;
        }
        detachDeleteListenersForAtlas(-1);
        houseDeleteSelectOn = false;
        houseUndoStack.clear();
    }

    private void commitHouseDoorCellsIfAny(int atlasResId) {
        HashSet<Point> left = new HashSet<>(houseDoorCellsBuffer);
        if (houseOverlay != null) left.addAll(houseOverlay.getGateCells());
        if (left.isEmpty()) return;

        // ▼ 추가: 이번 배치에서 만들어진 뷰들을 모을 리스트
        ArrayList<View> createdBatch = new ArrayList<>();

        for (Point p : left) {
            // ▼ 문 1개 배치가 만든 모든 파트를 반환받아서 누적
            createdBatch.addAll(placeHouseDoor(p.x, p.y, atlasResId));
        }

        houseDoorCellsBuffer.clear();
        if (houseOverlay != null) houseOverlay.clearGateCells();

        recalcAllGridMasks();

        // ▼ 추가: 문 배치 묶음을 UNDO 스택에 올림
        pushHouseUndo(createdBatch);
    }

    private void commitHouseWalls(Map<Point, Integer> cells, int atlasResId) {
        if (houseAtlas == null) return;

        // ▼ 추가: 이번 커밋에서 생성된 벽(또는 문을 여기서 배치한다면 그 파트들) 모으기
        ArrayList<View> createdThisCommit = new ArrayList<>();

        for (Map.Entry<Point,Integer> e : cells.entrySet()) {
            Point cell = e.getKey();
            int gx = cell.x, gy = cell.y;

            boolean isDoorCell = (houseOverlay != null && houseOverlay.getGateCells().contains(cell))
                    || houseDoorCellsBuffer.contains(cell);
            if (isDoorCell) {
                // 문은 placeHouseDoor에서 생성된 파트들을 되돌리기 묶음으로 수집하려면 이렇게:
                createdThisCommit.addAll(placeHouseDoor(gx, gy, atlasResId));
                continue;
            }

            float worldX = gx * GRID_PX + cameraLeft;
            float worldY = gy * GRID_PX + cameraTop;

            SelectableFenceView wall = new SelectableFenceView(
                    this, houseAtlas.getByMask(1|2), fenceDisplaySizePx, 0, atlasResId
            );
            wall.setFenceGridCell(gx, gy);
            wall.setFenceMode(true);
            wall.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
            wall.setCameraOffset(cameraLeft, cameraTop);
            wall.setWorldPosition(worldX, worldY);
            wall.setEditEnabled(isEditMode);
            if (isEditMode) wall.showBorderAndButtons(); else wall.hideBorderAndButtons();
            wall.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView)v));

            wall.setScaleX(0.6f); wall.setScaleY(0.6f); wall.setAlpha(0f);
            farmArea.addView(wall);
            wall.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();

            // ▼ 추가: 방금 만든 벽을 되돌리기 묶음에 포함
            createdThisCommit.add(wall);
        }

        recalcAllGridMasks();

        // ▼ 추가: 이번 벽/문 배치 묶음을 UNDO 스택에 올림
        pushHouseUndo(createdThisCommit);
    }

    // 기존 buildTopBar(...) 를 이 코드로 교체
    private LinearLayout buildTopBar(String okText, View.OnClickListener ok,
                                     String cancelText, View.OnClickListener cancel) {

        // 컨테이너(툴바)
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(6));
        bar.setBackgroundColor(0xAAFFFFFF);
        bar.setElevation(dp(2));

        // ★ 왼쪽 위에 붙이기
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        barLp.gravity = Gravity.TOP | Gravity.START; // ← START 로 변경
        barLp.topMargin = dp(16);
        barLp.leftMargin = dp(16);                   // ← leftMargin 사용
        bar.setLayoutParams(barLp);

        // (옵션) 드래그로 살짝 옮길 수 있게
        final float[] down = new float[2];
        final int[] start = new int[2];
        bar.setOnTouchListener((v, e) -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down[0] = e.getRawX();
                    down[1] = e.getRawY();
                    start[0] = lp.leftMargin;
                    start[1] = lp.topMargin;
                    return false; // 클릭도 동작하게 false
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - down[0];
                    float dy = e.getRawY() - down[1];
                    lp.leftMargin = Math.max(0, start[0] + (int) dx);
                    lp.topMargin  = Math.max(0, start[1] + (int) dy);
                    v.setLayoutParams(lp);
                    return true;
            }
            return false;
        });

        // ── 토글 헤더(작게) ──
        Button header = new Button(this);
        header.setText("도구 ▾");
        header.setAllCaps(false);
        shrinkButton(header);

        // ── 내부 패널 ──
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 설치 완료
        Button btnOk = new Button(this);
        btnOk.setText(okText);
        btnOk.setAllCaps(false);
        btnOk.setOnClickListener(ok);
        shrinkButton(btnOk);

        // 설치 모드 취소
        Button btnCancel = new Button(this);
        btnCancel.setText(cancelText);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(cancel);
        shrinkButton(btnCancel);

        panel.addView(btnOk);
        panel.addView(btnCancel);

        // 접기/펼치기
        header.setOnClickListener(v -> {
            if (panel.getVisibility() == View.VISIBLE) {
                panel.setVisibility(View.GONE);
                header.setText("도구");
            } else {
                panel.setVisibility(View.VISIBLE);
                header.setText("도구 ▾");
            }
        });

        bar.addView(header);
        bar.addView(panel);

        // 외부에서 panel에 버튼 추가할 수 있도록 태그 저장
        bar.setTag(ID_TOOLBAR_PANEL, panel);

        return bar;
    }

    /**
     * 울타리/집벽 공통: atlas 그룹별로 이웃 연결 재계산 + 문/게이트 프레임 적용
     */
    private void recalcAllGridMasks() {
        HashMap<Integer, HashMap<String, SelectableFenceView>> byAtlas = new HashMap<>();

        // floor(mask>15)는 이웃 계산에서 제외
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableFenceView) {
                SelectableFenceView fv = (SelectableFenceView) child;
                Integer mk = fv.getFenceMaskTag();
                if (mk != null && mk > 15) continue;

                Integer gx = fv.getFenceGridXTag(), gy = fv.getFenceGridYTag();
                if (gx == null || gy == null) {
                    gx = Math.round(fv.getWorldX() / GRID_PX);
                    gy = Math.round(fv.getWorldY() / GRID_PX);
                    fv.setFenceGridCell(gx, gy);
                }
                int atlas = fv.getAtlasResId();
                byAtlas.putIfAbsent(atlas, new HashMap<>());
                byAtlas.get(atlas).put(gx + "," + gy, fv);
            }
        }
        if (byAtlas.isEmpty()) {
            rebuildDoorGroups();
            return;
        }

        final int U = 1, R = 2, D = 4, L = 8;
        ArrayList<SelectableFenceView> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, HashMap<String, SelectableFenceView>> entry : byAtlas.entrySet()) {
            int atlasResId = entry.getKey();
            HashMap<String, SelectableFenceView> map = entry.getValue();

            boolean isHouse = isHouseAtlas(atlasResId);

            FenceAtlas tmpFence = null;
            HouseAtlas tmpHouse = null;
            if (isHouse)
                tmpHouse = (houseAtlas != null ? houseAtlas : new HouseAtlas(this, atlasResId, 16));
            else tmpFence = (fenceAtlas != null ? fenceAtlas : new FenceAtlas(this, atlasResId));

            for (SelectableFenceView fv : map.values()) {
                int gx = fv.getFenceGridXTag(), gy = fv.getFenceGridYTag();
                int m = computeMaskAt(map, gx, gy);

                // === 집/구조물 문 처리 ===
                if (Boolean.TRUE.equals(fv.getTag(TAG_IS_DOOR))) {
                    ensureDoorSpritesLoaded();
                    if (doorSheet != null) {
                        int frameIdx = getDoorFrameIndex(fv);
                        Integer sliceIdx = (Integer) fv.getTag(TAG_DOOR_SLICE);
                        if (sliceIdx == null) sliceIdx = 0;
                        String tp = String.valueOf(fv.getTag(TAG_DOOR_TYPE));
                        Bitmap bmp = "RANCH".equals(tp)
                                ? safeRanchSlice(frameIdx, sliceIdx)
                                : safeHouseFrame(frameIdx);
                        fv.setFenceMaskAndBitmap(m, bmp);

                        if (!isDoorListenerAttached(fv)) {
                            Object grp = fv.getTag(TAG_DOOR_GROUP);
                            if (grp != null && sliceIdx == 0) {
                                String gid = grp.toString();
                                fv.setOnClickListener(v -> animateDoorToggleByGroup(gid));
                                fv.setTag(TAG_DOOR_LISTENER, Boolean.TRUE);
                            } else fv.setOnClickListener(null);
                        }
                        continue;
                    }
                }

                // === 울타리 게이트 처리(기존 유지) ===
                if (Boolean.TRUE.equals(fv.getTag(TAG_KEY_GATE))) {
                    ensureGateSlicesLoaded();
                    if (gateHFrameSlices != null && gateVFrameSlices != null) {
                        boolean verticalNow = (m == (U | D));
                        fv.setTag(TAG_KEY_GATE_VERTICAL, verticalNow);

                        int frameIdx = getGateFrameIndex(fv);
                        Integer sliceIdx = (Integer) fv.getTag(TAG_KEY_GATE_SLICE);
                        if (sliceIdx == null) sliceIdx = 0;

                        Bitmap bmp = verticalNow
                                ? safeVSlice(frameIdx, sliceIdx)
                                : safeHSlice(frameIdx, sliceIdx);
                        fv.setFenceMaskAndBitmap(m, bmp);

                        if (!isGateListenerAttached(fv)) {
                            Object grp = fv.getTag(TAG_KEY_GATE_GROUP);
                            if (grp != null && sliceIdx == 0) {
                                String gid = grp.toString();
                                fv.setOnClickListener(v -> animateGateToggleByGroup(gid));
                                fv.setTag(TAG_KEY_GATE_LISTENER, Boolean.TRUE);
                            } else fv.setOnClickListener(null);
                        }
                    }
                    continue;
                }

                // === 일반 울타리/집벽 ===
                if (isHouse && m == 0) { // 고립 제거
                    toRemove.add(fv);
                    continue;
                }

                if (isHouse) {
                    if (m == (U | D)) {
                        int side = inferVerticalSide(map, gx, gy);
                        Bitmap bm =
                                side > 0 ? tmpHouse.getVerticalRight()
                                        : side < 0 ? tmpHouse.getVerticalLeft()
                                        : tmpHouse.getByMask(m);
                        fv.setFenceMaskAndBitmap(m, bm);
                    } else {
                        fv.setFenceMaskAndBitmap(m, tmpHouse.getByMask(m));
                    }
                } else {
                    fv.setFenceMaskAndBitmap(m, tmpFence.getByMask(m));
                }
            }

            for (SelectableFenceView v : toRemove) farmArea.removeView(v);
            toRemove.clear();

            if (isHouse) fillHouseFloor(atlasResId, map, tmpHouse);
        }

        // ★ 문 그룹 갱신
        rebuildDoorGroups();
    }

    private HashMap<String, SelectableFenceView> collectFenceMapByAtlas(int atlasResId) {
        HashMap<String, SelectableFenceView> map = new HashMap<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableFenceView) {
                SelectableFenceView fv = (SelectableFenceView) child;
                if (fv.getAtlasResId() != atlasResId) continue;
                Integer mk = fv.getFenceMaskTag();
                if (mk != null && mk > 15) continue;
                Integer gx = fv.getFenceGridXTag(), gy = fv.getFenceGridYTag();
                if (gx == null || gy == null) continue;
                map.put(gx + "," + gy, fv);
            }
        }
        return map;
    }

    private boolean isGateVerticalSpot(HashMap<String, SelectableFenceView> map, int gx, int gy) {
        boolean up = map.containsKey(gx + "," + (gy - 1));
        boolean down = map.containsKey(gx + "," + (gy + 1));
        boolean left = map.containsKey((gx - 1) + "," + gy);
        boolean right = map.containsKey((gx + 1) + "," + gy);
        if ((up || down) && !(left || right)) return true;
        return false;
    }

    private int computeMaskAt(HashMap<String, SelectableFenceView> map, int gx, int gy) {
        final int U = 1, R = 2, D = 4, L = 8;
        int m = 0;
        if (map.containsKey(gx + "," + (gy - 1))) m |= U;
        if (map.containsKey((gx + 1) + "," + gy)) m |= R;
        if (map.containsKey(gx + "," + (gy + 1))) m |= D;
        if (map.containsKey((gx - 1) + "," + gy)) m |= L;
        return m;
    }

    private int inferVerticalSide(HashMap<String, SelectableFenceView> map, int gx, int gy) {
        final int U = 1, R = 2, D = 4, L = 8;
        int y = gy - 1;
        while (map.containsKey(gx + "," + y)) {
            int mm = computeMaskAt(map, gx, y);
            if (mm != (U | D)) {
                if ((mm & R) != 0) return +1;
                if ((mm & L) != 0) return -1;
                break;
            }
            y--;
        }
        y = gy + 1;
        while (map.containsKey(gx + "," + y)) {
            int mm = computeMaskAt(map, gx, y);
            if (mm != (U | D)) {
                if ((mm & R) != 0) return +1;
                if ((mm & L) != 0) return -1;
                break;
            }
            y++;
        }
        return 0;
    }

    private void fillHouseFloor(int atlasResId,
                                HashMap<String, SelectableFenceView> wallMap,
                                HouseAtlas atlas) {
        // 1) 기존 floor(mask>15) 제거
        ArrayList<View> floorToRemove = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableFenceView) {
                SelectableFenceView fv = (SelectableFenceView) child;
                if (fv.getAtlasResId() == atlasResId) {
                    Integer mk = fv.getFenceMaskTag();
                    if (mk != null && mk > 15) floorToRemove.add(fv);
                }
            }
        }
        for (View v : floorToRemove) farmArea.removeView(v);

        if (wallMap.isEmpty()) return;

        // 2) 경계 박스 계산
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (String k : wallMap.keySet()) {
            String[] s = k.split(",");
            int x = Integer.parseInt(s[0]);
            int y = Integer.parseInt(s[1]);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        minX -= 1;
        minY -= 1;
        maxX += 1;
        maxY += 1;

        int W = maxX - minX + 1;
        int H = maxY - minY + 1;

        // 3) 점유 맵(벽)
        boolean[][] occ = new boolean[H][W];
        for (String k : wallMap.keySet()) {
            String[] s = k.split(",");
            int gx = Integer.parseInt(s[0]) - minX;
            int gy = Integer.parseInt(s[1]) - minY;
            if (gy >= 0 && gy < H && gx >= 0 && gx < W) occ[gy][gx] = true;
        }

        // 4) 문/게이트도 막힌 셀로 간주 (바닥 새어보임 방지)
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView f = (SelectableFenceView) child;

            Integer mk = f.getFenceMaskTag();
            if (mk != null && mk > 15) continue;

            Integer gxTag = f.getFenceGridXTag(), gyTag = f.getFenceGridYTag();
            if (gxTag == null || gyTag == null) continue;

            boolean blocks = false;
            // 같은 atlas의 벽/타일
            if (f.getAtlasResId() == atlasResId) blocks = true;
            // 집/구조물 문
            if (Boolean.TRUE.equals(f.getTag(TAG_IS_DOOR))) blocks = true;
            // (선택) 울타리 게이트가 내부 사각에 들어온 경우도 차단
            if (Boolean.TRUE.equals(f.getTag(TAG_KEY_GATE))) blocks = true;

            if (blocks
                    && gxTag >= minX && gxTag <= maxX
                    && gyTag >= minY && gyTag <= maxY) {
                occ[gyTag - minY][gxTag - minX] = true;
            }
        }

        // 5) 바깥 flood-fill
        boolean[][] outside = new boolean[H][W];
        ArrayDeque<int[]> dq = new ArrayDeque<>();
        for (int x = 0; x < W; x++) {
            dq.add(new int[]{x, 0});
            dq.add(new int[]{x, H - 1});
        }
        for (int y = 0; y < H; y++) {
            dq.add(new int[]{0, y});
            dq.add(new int[]{W - 1, y});
        }
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!dq.isEmpty()) {
            int[] p = dq.poll();
            int x = p[0], y = p[1];
            if (x < 0 || x >= W || y < 0 || y >= H) continue;
            if (outside[y][x] || occ[y][x]) continue;
            outside[y][x] = true;
            for (int i = 0; i < 4; i++) dq.add(new int[]{x + dx[i], y + dy[i]});
        }

        // 6) 내부에 바닥 채우기
        for (int gy = minY + 1; gy <= maxY - 1; gy++) {
            for (int gx = minX + 1; gx <= maxX - 1; gx++) {
                int ix = gx - minX, iy = gy - minY;
                if (ix < 0 || ix >= W || iy < 0 || iy >= H) continue;
                if (!occ[iy][ix] && !outside[iy][ix]) {
                    float worldX = gx * GRID_PX + cameraLeft;
                    float worldY = gy * GRID_PX + cameraTop;

                    SelectableFenceView floor = new SelectableFenceView(
                            this, atlas.getFloorBitmap(), fenceDisplaySizePx, 16, atlasResId
                    );
                    floor.setFenceGridCell(gx, gy);
                    floor.setFenceMode(true);
                    floor.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
                    floor.setCameraOffset(cameraLeft, cameraTop);
                    floor.setWorldPosition(worldX, worldY);
                    floor.setEditEnabled(isEditMode);
                    if (isEditMode) floor.showBorderAndButtons();
                    else floor.hideBorderAndButtons();
                    floor.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
                    farmArea.addView(floor);
                }
            }
        }
    }

    private boolean isHouseAtlas(int atlasResId) {
        try {
            String name = getResources().getResourceEntryName(atlasResId).toLowerCase();
            if (name.contains("fence")) return false;
            return name.contains("house") || name.contains("wall") || name.contains("structure") || name.contains("ranch") || name.contains("barn") || name.contains("stable");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRanchStructureAtlas(int atlasResId) {
        try {
            String name = getResources().getResourceEntryName(atlasResId).toLowerCase();
            return name.contains("ranch") || name.contains("barn") || name.contains("stable") || name.contains("structure");
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // [A] 울타리 게이트(기존) 스프라이트/유틸/배치/애니메이션
    // =========================
    private void ensureGateSlicesLoaded() {
        if (gateHFrameSlices != null && gateVFrameSlices != null) return;
        if (gateSheet == null) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            gateSheet = BitmapFactory.decodeResource(getResources(), R.drawable.fence_gates, o);
        }
        if (gateSheet == null) return;

        final int cell = 16;

        // 가로: row 0, 한 프레임은 4칸 → colStart=f*4, slice=0..3
        gateHFrameSlices = new Bitmap[5][4];
        for (int f = 0; f < 5; f++) {
            int colStart = f * 4;
            for (int s = 0; s < 4; s++) {
                gateHFrameSlices[f][s] = Bitmap.createBitmap(gateSheet, (colStart + s) * cell, 0 * cell, cell, cell);
            }
        }

        // 세로: row 1~3, frame은 col={0,2,4,6,8}
        gateVFrameSlices = new Bitmap[5][3];
        int[] vCols = {0, 2, 4, 6, 8};
        for (int f = 0; f < 5; f++) {
            int col = vCols[f];
            for (int s = 0; s < 3; s++) { // 0..2 (row1, row2, row3)
                int row = 1 + s;
                gateVFrameSlices[f][s] = Bitmap.createBitmap(gateSheet, col * cell, row * cell, cell, cell);
            }
        }
    }

    private Bitmap safeHSlice(int frame, int slice) {
        frame = Math.max(0, Math.min(4, frame));
        slice = Math.max(0, Math.min(3, slice));
        return gateHFrameSlices[frame][slice];
    }

    private Bitmap safeVSlice(int frame, int slice) {
        frame = Math.max(0, Math.min(4, frame));
        slice = Math.max(0, Math.min(2, slice));
        return gateVFrameSlices[frame][slice];
    }

    private boolean isGateListenerAttached(SelectableFenceView v) {
        Object tag = v.getTag(TAG_KEY_GATE_LISTENER);
        return (tag instanceof Boolean) && (Boolean) tag;
    }

    private int getGateFrameIndex(SelectableFenceView v) {
        Object tag = v.getTag(TAG_KEY_GATE_FRAME);
        int t = 0;
        if (tag instanceof Integer) t = (Integer) tag;
        if (t < 0) t = 0;
        if (t > 4) t = 4;
        return t;
    }

    private void setGateFrameIndex(SelectableFenceView v, int idx) {
        if (idx < 0) idx = 0;
        if (idx > 4) idx = 4;
        v.setTag(TAG_KEY_GATE_FRAME, idx);
    }

    /**
     * 게이트 묶음 설치: 가로(4칸) 또는 세로(5칸)
     */
    private ArrayList<View> placeGateGroup(int gx, int gy, int atlasResId, boolean vertical) {
        ensureGateSlicesLoaded();
        ArrayList<View> created = new ArrayList<>();
        if (gateHFrameSlices == null || gateVFrameSlices == null) return created;

        String groupId = gx + "," + gy + (vertical ? ",GV" : ",GH");

        if (!vertical) {
            for (int s=0; s<4; s++) {
                int cx = gx + s, cy = gy;
                float wx = cx * GRID_PX + cameraLeft;
                float wy = cy * GRID_PX + cameraTop;

                SelectableFenceView part = new SelectableFenceView(
                        this, safeHSlice(0, s), fenceDisplaySizePx, 0, atlasResId
                );
                part.setFenceGridCell(cx, cy);
                part.setFenceMode(true);
                part.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
                part.setCameraOffset(cameraLeft, cameraTop);
                part.setWorldPosition(wx, wy);

                part.setTag(TAG_KEY_GATE, Boolean.TRUE);
                part.setTag(TAG_KEY_GATE_GROUP, groupId);
                part.setTag(TAG_KEY_GATE_SLICE, s);
                part.setTag(TAG_KEY_GATE_FRAME, 0);
                part.setTag(TAG_KEY_GATE_VERTICAL, Boolean.FALSE);
                part.setTag(TAG_KEY_GATE_OPENING, Boolean.FALSE);

                if (s == 0 && !isGateListenerAttached(part)) {
                    part.setOnClickListener(v -> animateGateToggleByGroup(groupId));
                    part.setTag(TAG_KEY_GATE_LISTENER, Boolean.TRUE);
                }

                part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));

                farmArea.addView(part);
                created.add(part);
            }
        } else {
            for (int s=0; s<5; s++) {
                int cx = gx, cy = gy + s;
                float wx = cx * GRID_PX + cameraLeft;
                float wy = cy * GRID_PX + cameraTop;

                SelectableFenceView part = new SelectableFenceView(
                        this, safeVSlice(0, Math.min(s,2)), fenceDisplaySizePx, 0, atlasResId
                );
                part.setFenceGridCell(cx, cy);
                part.setFenceMode(true);
                part.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
                part.setCameraOffset(cameraLeft, cameraTop);
                part.setWorldPosition(wx, wy);

                part.setTag(TAG_KEY_GATE, Boolean.TRUE);
                part.setTag(TAG_KEY_GATE_GROUP, groupId);
                part.setTag(TAG_KEY_GATE_SLICE, Math.min(s,2));
                part.setTag(TAG_KEY_GATE_FRAME, 0);
                part.setTag(TAG_KEY_GATE_VERTICAL, Boolean.TRUE);
                part.setTag(TAG_KEY_GATE_OPENING, Boolean.FALSE);

                if (s == 0 && !isGateListenerAttached(part)) {
                    part.setOnClickListener(v -> animateGateToggleByGroup(groupId));
                    part.setTag(TAG_KEY_GATE_LISTENER, Boolean.TRUE);
                }

                part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));

                farmArea.addView(part);
                created.add(part);
            }
        }
        return created;
    }

    /**
     * 그룹 ID 기준으로 게이트 열림/닫힘 애니메이션
     */
    private void animateGateToggleByGroup(String groupId) {
        ensureGateSlicesLoaded();
        if (gateHFrameSlices == null || gateVFrameSlices == null) return;

        ArrayList<SelectableFenceView> parts = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableFenceView) {
                Object g = child.getTag(TAG_KEY_GATE_GROUP);
                if (g != null && groupId.equals(g.toString()))
                    parts.add((SelectableFenceView) child);
            }
        }
        if (parts.isEmpty()) return;

        SelectableFenceView head = parts.get(0);
        boolean vertical = Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE_VERTICAL));
        int cur = getGateFrameIndex(head);
        boolean opening = !(Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE_OPENING)));
        for (SelectableFenceView p : parts) p.setTag(TAG_KEY_GATE_OPENING, opening);

        final boolean verticalF = vertical;
        final boolean openingF = opening;

        ui.post(new Runnable() {
            int idx = cur;

            @Override
            public void run() {
                if (openingF) {
                    if (idx >= 4) return;
                    idx++;
                } else {
                    if (idx <= 0) return;
                    idx--;
                }

                for (SelectableFenceView p : parts) {
                    Integer slice = (Integer) p.getTag(TAG_KEY_GATE_SLICE);
                    if (slice == null) slice = 0;
                    Bitmap bmp = verticalF ? safeVSlice(idx, slice) : safeHSlice(idx, slice);

                    int m = 0;
                    Integer mt = p.getFenceMaskTag();
                    if (mt != null) m = mt;
                    setGateFrameIndex(p, idx);
                    p.setFenceMaskAndBitmap(m, bmp);
                }
                ui.postDelayed(this, 60);
            }
        });
    }

    // =========================
    // [B] 집/목장 구조물 '문' 스프라이트 로더/유틸/배치/애니메이션
    // =========================
    private void ensureDoorSpritesLoaded() {
        if (houseDoorFrames != null && ranchDoorFrameSlices != null) return;
        if (doorSheet == null) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            doorSheet = BitmapFactory.decodeResource(getResources(), R.drawable.door_animation_sprites, o);
        }
        if (doorSheet == null) return;

        final int cell = 16;

        // 1행: 집 문(1칸) 6프레임
        houseDoorFrames = new Bitmap[DOOR_FRAMES];
        for (int f = 0; f < DOOR_FRAMES; f++) {
            houseDoorFrames[f] = Bitmap.createBitmap(doorSheet, f * cell, 0, cell, cell);
        }

        // 2행: 목장 구조물 문(가로 3칸=48×16) 6프레임 → 각 프레임의 3조각
        ranchDoorFrameSlices = new Bitmap[DOOR_FRAMES][3];
        for (int f = 0; f < DOOR_FRAMES; f++) {
            int colStart = f * 3;
            for (int s = 0; s < 3; s++) {
                ranchDoorFrameSlices[f][s] = Bitmap.createBitmap(doorSheet, (colStart + s) * cell, 1 * cell, cell, cell);
            }
        }
    }

    private Bitmap safeHouseFrame(int frame) {
        if (houseDoorFrames == null) return null;
        frame = Math.max(0, Math.min(DOOR_FRAMES - 1, frame));
        return houseDoorFrames[frame];
    }

    private Bitmap safeRanchSlice(int frame, int slice) {
        if (ranchDoorFrameSlices == null) return null;
        frame = Math.max(0, Math.min(DOOR_FRAMES - 1, frame));
        slice = Math.max(0, Math.min(2, slice));
        return ranchDoorFrameSlices[frame][slice];
    }

    private boolean isDoorListenerAttached(SelectableFenceView v) {
        return Boolean.TRUE.equals(v.getTag(TAG_DOOR_LISTENER));
    }

    private int getDoorFrameIndex(SelectableFenceView v) {
        Object tag = v.getTag(TAG_DOOR_FRAME);
        int t = 5; // 기본 닫힘
        if (tag instanceof Integer) t = (Integer) tag;
        if (t < 0) t = 0;
        if (t > DOOR_FRAMES - 1) t = DOOR_FRAMES - 1;
        return t;
    }

    private ArrayList<View> placeHouseDoor(int gx, int gy, int atlasResId) {
        ensureDoorSpritesLoaded();
        ArrayList<View> created = new ArrayList<>();

        boolean useRanchDoor = isRanchBuild || isRanchStructureAtlas(atlasResId);

        if (useRanchDoor) {
            String groupId = "R:" + gx + "," + gy;
            int startFrame = DOOR_FRAMES - 1;
            for (int s = 0; s < 3; s++) {
                int cx = gx + s, cy = gy;
                float wx = cx * GRID_PX + cameraLeft;
                float wy = cy * GRID_PX + cameraTop;

                SelectableFenceView part = new SelectableFenceView(
                        this, safeRanchSlice(startFrame, s), fenceDisplaySizePx, 0, atlasResId
                );
                part.setFenceGridCell(cx, cy);
                part.setFenceMode(true);
                part.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
                part.setCameraOffset(cameraLeft, cameraTop);
                part.setWorldPosition(wx, wy);

                part.setTag(TAG_IS_DOOR, Boolean.TRUE);
                part.setTag(TAG_DOOR_TYPE, "RANCH");
                part.setTag(TAG_DOOR_GROUP, groupId);
                part.setTag(TAG_DOOR_SLICE, s);
                part.setTag(TAG_DOOR_FRAME, startFrame);

                if (s == 0 && !isDoorListenerAttached(part)) {
                    part.setOnClickListener(v -> animateDoorToggleByGroup(groupId));
                    part.setTag(TAG_DOOR_LISTENER, Boolean.TRUE);
                }

                part.setEditEnabled(isEditMode);
                if (isEditMode) part.showBorderAndButtons(); else part.hideBorderAndButtons();
                part.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
                farmArea.addView(part);
                created.add(part);
            }
        } else {
            String groupId = "H:" + gx + "," + gy;
            int startFrame = DOOR_FRAMES - 1;
            SelectableFenceView door = new SelectableFenceView(
                    this, safeHouseFrame(startFrame), fenceDisplaySizePx, 0, atlasResId
            );
            door.setFenceGridCell(gx, gy);
            door.setFenceMode(true);
            door.setLayoutParams(new FrameLayout.LayoutParams(GRID_PX, GRID_PX));
            door.setCameraOffset(cameraLeft, cameraTop);
            door.setWorldPosition(gx * GRID_PX + cameraLeft, gy * GRID_PX + cameraTop);

            door.setTag(TAG_IS_DOOR, Boolean.TRUE);
            door.setTag(TAG_DOOR_TYPE, "HOUSE");
            door.setTag(TAG_DOOR_GROUP, groupId);
            door.setTag(TAG_DOOR_SLICE, 0);
            door.setTag(TAG_DOOR_FRAME, startFrame);

            if (!isDoorListenerAttached(door)) {
                door.setOnClickListener(v -> animateDoorToggleByGroup(groupId));
                door.setTag(TAG_DOOR_LISTENER, Boolean.TRUE);
            }

            door.setEditEnabled(isEditMode);
            if (isEditMode) door.showBorderAndButtons(); else door.hideBorderAndButtons();
            door.setOnDragEndListener(v -> onFenceDragEnd((SelectableFenceView) v));
            farmArea.addView(door);
            created.add(door);
        }

        rebuildDoorGroups();
        return created;
    }


    /**
     * 그룹 ID 단위로 집/구조물 문 수동 토글 애니메이션
     */
    private void animateDoorToggleByGroup(String groupId) {
        ensureDoorSpritesLoaded();
        if (doorSheet == null) return;

        ArrayList<SelectableFenceView> parts = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (child instanceof SelectableFenceView) {
                Object g = child.getTag(TAG_DOOR_GROUP);
                if (g != null && groupId.equals(g.toString()))
                    parts.add((SelectableFenceView) child);
            }
        }
        if (parts.isEmpty()) return;

        int cur = getDoorFrameIndex(parts.get(0));
        final boolean closing = (cur <= 2); // 0~2면 닫히는 방향 → 5

        ui.post(new Runnable() {
            int idx = cur;

            @Override
            public void run() {
                if (closing) {
                    if (idx >= DOOR_FRAMES - 1) return; // 5
                    idx++;
                } else {
                    if (idx <= 0) return; // 0
                    idx--;
                }
                for (SelectableFenceView p : parts) {
                    p.setTag(TAG_DOOR_FRAME, idx);
                    int slice = 0;
                    Object sl = p.getTag(TAG_DOOR_SLICE);
                    if (sl instanceof Integer) slice = (Integer) sl;
                    String tp = String.valueOf(p.getTag(TAG_DOOR_TYPE));
                    Bitmap bmp = "RANCH".equals(tp) ? safeRanchSlice(idx, slice) : safeHouseFrame(idx);
                    int m = 0;
                    Integer mt = p.getFenceMaskTag();
                    if (mt != null) m = mt;
                    p.setFenceMaskAndBitmap(m, bmp);
                }
                ui.postDelayed(this, 70);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // ★★★ 추가: 캐릭터 근접 시 울타리 게이트 자동 개폐 업데이트 ★★★
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 캐릭터와 각 게이트 그룹의 중심 거리로 목표 프레임을 정하고 한 틱씩 근접
     */
    private void updateGateAutoOpenClose() {
        // 캐릭터 월드 좌표(캐릭터는 화면 중앙에 그려지므로 카메라 + 화면중앙이 곧 캐릭터 좌표)
        float charX = cameraLeft + (spriteView != null ? spriteView.getWidth() / 2f : 0f);
        float charY = cameraTop + (spriteView != null ? spriteView.getHeight() / 2f : 0f);

        // 그룹별 헤드(slice==0)만 스캔
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView head = (SelectableFenceView) child;
            if (!Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE))) continue;

            // 헤드만(슬라이스 0) 처리
            Object sl = head.getTag(TAG_KEY_GATE_SLICE);
            if (!(sl instanceof Integer) || ((Integer) sl) != 0) continue;

            Object gidObj = head.getTag(TAG_KEY_GATE_GROUP);
            if (gidObj == null) continue;
            String gid = gidObj.toString();

            boolean vertical = Boolean.TRUE.equals(head.getTag(TAG_KEY_GATE_VERTICAL));

            // 게이트 그룹의 중심 좌표 계산
            float gx = head.getWorldX();
            float gy = head.getWorldY();
            float centerX, centerY;
            if (!vertical) {
                // 가로 4칸: head(왼쪽) 기준 + 1.5칸
                centerX = gx + GRID_PX * 1.5f;
                centerY = gy + GRID_PX * 0.5f;
            } else {
                // 세로 5칸: head(위쪽) 기준 + 2칸
                centerX = gx + GRID_PX * 0.5f;
                centerY = gy + GRID_PX * 2.0f;
            }

            float dx = charX - centerX, dy = charY - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            // 목표 프레임: 4=완전 열림, 0=완전 닫힘  (animateGateToggleByGroup의 증가 방향과 일치)
            int targetFrame;
            if (dist <= GATE_OPEN_RADIUS_PX) targetFrame = 4;
            else if (dist >= GATE_CLOSE_RADIUS_PX) targetFrame = 0;
            else continue; // 히스테리시스 내부: 유지

            // 그룹의 모든 파트 수집
            ArrayList<SelectableFenceView> parts = new ArrayList<>();
            for (int j = 0; j < farmArea.getChildCount(); j++) {
                View v = farmArea.getChildAt(j);
                if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_KEY_GATE))) {
                    Object g2 = v.getTag(TAG_KEY_GATE_GROUP);
                    if (g2 != null && gid.equals(g2.toString())) parts.add((SelectableFenceView) v);
                }
            }
            if (parts.isEmpty()) continue;

            int cur = getGateFrameIndex(head);
            if (cur == targetFrame) continue;

            int next = cur + (targetFrame > cur ? +1 : -1);
            // 프레임 한 틱 적용
            for (SelectableFenceView p : parts) {
                Integer slice = (Integer) p.getTag(TAG_KEY_GATE_SLICE);
                if (slice == null) slice = 0;
                Bitmap bmp = vertical ? safeVSlice(next, slice) : safeHSlice(next, slice);
                setGateFrameIndex(p, next);
                int m = 0;
                Integer mt = p.getFenceMaskTag();
                if (mt != null) m = mt;
                p.setFenceMaskAndBitmap(m, bmp);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ★★★ 추가: 캐릭터 근접 시 집/목장 문 자동 개폐 업데이트 ★★★
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 현재 배치된 문 그룹 스캔(중심 좌표/타입/상태)
     */
    private void rebuildDoorGroups() {
        doorGroups.clear();
        HashMap<String, ArrayList<SelectableFenceView>> tmp = new HashMap<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View ch = farmArea.getChildAt(i);
            if (!(ch instanceof SelectableFenceView)) continue;
            SelectableFenceView f = (SelectableFenceView) ch;
            if (!Boolean.TRUE.equals(f.getTag(TAG_IS_DOOR))) continue;
            Object g = f.getTag(TAG_DOOR_GROUP);
            if (g == null) continue;
            String gid = g.toString();
            tmp.putIfAbsent(gid, new ArrayList<>());
            tmp.get(gid).add(f);
        }
        for (Map.Entry<String, ArrayList<SelectableFenceView>> e : tmp.entrySet()) {
            String gid = e.getKey();
            ArrayList<SelectableFenceView> parts = e.getValue();
            if (parts.isEmpty()) continue;
            float sx = 0, sy = 0;
            int n = 0;
            String type = "HOUSE";
            for (SelectableFenceView p : parts) {
                float cx = p.getWorldX() + GRID_PX / 2f;
                float cy = p.getWorldY() + GRID_PX / 2f;
                sx += cx;
                sy += cy;
                n++;
                Object tp = p.getTag(TAG_DOOR_TYPE);
                if (tp != null) type = tp.toString();
            }
            DoorGroupState st = new DoorGroupState();
            st.cx = sx / n;
            st.cy = sy / n;
            st.type = type;
            int cur = getDoorFrameIndex(parts.get(0));
            // 프레임 0~2(열림 방향)면 "열림으로 향함"이므로 isOpen=true로 취급
            st.isOpen = (cur <= 2);
            doorGroups.put(gid, st);
        }
    }

    /**
     * 거리 체크 후 자동 열림/닫힘(히스테리시스)
     */
    private void autoCheckDoors() {
        if (spriteView == null || doorGroups.isEmpty()) return;
        float heroX = cameraLeft + spriteView.getWidth() / 2f;
        float heroY = cameraTop + spriteView.getHeight() / 2f;

        for (Map.Entry<String, DoorGroupState> e : doorGroups.entrySet()) {
            String gid = e.getKey();
            DoorGroupState st = e.getValue();
            float dx = heroX - st.cx, dy = heroY - st.cy;
            float d2 = dx * dx + dy * dy;

            boolean isRanch = "RANCH".equals(st.type);
            float openR = isRanch ? RANCH_DOOR_OPEN_RADIUS : HOUSE_DOOR_OPEN_RADIUS;
            float closeR = isRanch ? RANCH_DOOR_CLOSE_RADIUS : HOUSE_DOOR_CLOSE_RADIUS;
            float openR2 = openR * openR, closeR2 = closeR * closeR;

            if (!st.isOpen && d2 <= openR2) {
                animateDoorOpenByGroup(gid);
                st.isOpen = true;
            } else if (st.isOpen && d2 >= closeR2) {
                animateDoorCloseByGroup(gid);
                st.isOpen = false;
            }
        }
    }

    private void animateDoorOpenByGroup(String gid) {
        animateDoorTowards(gid, 0);
    }               // 0 = 활짝 열림

    private void animateDoorCloseByGroup(String gid) {
        animateDoorTowards(gid, DOOR_FRAMES - 1);
    } // 5 = 완전 닫힘

    private void animateDoorTowards(String gid, int target) {
        ensureDoorSpritesLoaded();
        if (doorSheet == null) return;

        ArrayList<SelectableFenceView> parts = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View c = farmArea.getChildAt(i);
            if (c instanceof SelectableFenceView) {
                Object g = c.getTag(TAG_DOOR_GROUP);
                if (g != null && gid.equals(g.toString())) parts.add((SelectableFenceView) c);
            }
        }
        if (parts.isEmpty()) return;

        int cur = getDoorFrameIndex(parts.get(0));
        final int start = cur;

        ui.post(new Runnable() {
            int idx = start;

            @Override
            public void run() {
                if (idx == target) return;
                idx += target > idx ? +1 : -1;

                for (SelectableFenceView p : parts) {
                    p.setTag(TAG_DOOR_FRAME, idx);
                    int slice = 0;
                    Object sl = p.getTag(TAG_DOOR_SLICE);
                    if (sl instanceof Integer) slice = (Integer) sl;
                    String tp = String.valueOf(p.getTag(TAG_DOOR_TYPE));
                    Bitmap bmp = "RANCH".equals(tp) ? safeRanchSlice(idx, slice) : safeHouseFrame(idx);
                    int m = 0;
                    Integer mt = p.getFenceMaskTag();
                    if (mt != null) m = mt;
                    p.setFenceMaskAndBitmap(m, bmp);
                }
                ui.postDelayed(this, 70);
            }
        });
    }

    private void pushFenceUndo(ArrayList<View> group) {
        if (group != null && !group.isEmpty()) fenceUndoStack.push(group);
    }

    private void pushHouseUndo(ArrayList<View> group) {
        if (group != null && !group.isEmpty()) houseUndoStack.push(group);
    }

    private void undoFenceOneStep() {
        if (!fenceUndoStack.isEmpty()) {
            for (View v : fenceUndoStack.pop()) farmArea.removeView(v);
            recalcAllGridMasks();
            saveAppliedItems();
            Toast.makeText(this, "울타리 최근 배치를 되돌렸습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoHouseOneStep() {
        if (!houseUndoStack.isEmpty()) {
            for (View v : houseUndoStack.pop()) farmArea.removeView(v);
            recalcAllGridMasks();
            saveAppliedItems();
            Toast.makeText(this, "집/목장 최근 배치를 되돌렸습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeGateGroup(String gid) {
        ArrayList<View> del = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View v = farmArea.getChildAt(i);
            if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_KEY_GATE))) {
                Object g2 = v.getTag(TAG_KEY_GATE_GROUP);
                if (g2 != null && gid.equals(g2.toString())) del.add(v);
            }
        }
        for (View v : del) farmArea.removeView(v);
    }

    private void removeDoorGroup(String gid) {
        ArrayList<View> del = new ArrayList<>();
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View v = farmArea.getChildAt(i);
            if (v instanceof SelectableFenceView && Boolean.TRUE.equals(v.getTag(TAG_IS_DOOR))) {
                Object g2 = v.getTag(TAG_DOOR_GROUP);
                if (g2 != null && gid.equals(g2.toString())) del.add(v);
            }
        }
        for (View v : del) farmArea.removeView(v);
    }

    private void attachDeleteListenersForAtlas(int atlasResId, boolean includeGatesDoors) {
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView fv = (SelectableFenceView) child;
            if (fv.getAtlasResId() != atlasResId) continue;

            child.setTag(TAG_TMP_DELETE_MODE, Boolean.TRUE);
            child.setOnClickListener(v -> {
                if (includeGatesDoors && Boolean.TRUE.equals(fv.getTag(TAG_KEY_GATE))) {
                    Object gid = fv.getTag(TAG_KEY_GATE_GROUP);
                    if (gid != null) removeGateGroup(gid.toString());
                } else if (includeGatesDoors && Boolean.TRUE.equals(fv.getTag(TAG_IS_DOOR))) {
                    Object gid = fv.getTag(TAG_DOOR_GROUP);
                    if (gid != null) removeDoorGroup(gid.toString());
                } else {
                    farmArea.removeView(fv);
                }
                recalcAllGridMasks();
                saveAppliedItems();
            });
        }
    }

    private void detachDeleteListenersForAtlas(int atlasResId) {
        for (int i = 0; i < farmArea.getChildCount(); i++) {
            View child = farmArea.getChildAt(i);
            if (!(child instanceof SelectableFenceView)) continue;
            SelectableFenceView fv = (SelectableFenceView) child;
            if (fv.getAtlasResId() != atlasResId) continue;

            if (Boolean.TRUE.equals(child.getTag(TAG_TMP_DELETE_MODE))) {
                child.setOnClickListener(null);
                child.setTag(TAG_TMP_DELETE_MODE, null);
            }
        }
    }

    private void toggleFenceSelectDelete(int atlasResId) {
        fenceDeleteSelectOn = !fenceDeleteSelectOn;
        if (fenceOverlay != null)
            fenceOverlay.setVisibility(fenceDeleteSelectOn ? View.GONE : View.VISIBLE);
        if (fenceDeleteSelectOn) {
            attachDeleteListenersForAtlas(atlasResId, true);
            Toast.makeText(this, "삭제 모드: 삭제할 울타리를 탭하세요.", Toast.LENGTH_SHORT).show();
        } else {
            detachDeleteListenersForAtlas(atlasResId);
            recalcAllGridMasks();
            Toast.makeText(this, "삭제 모드 해제", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleHouseSelectDelete(int atlasResId) {
        houseDeleteSelectOn = !houseDeleteSelectOn;
        if (houseOverlay != null)
            houseOverlay.setVisibility(houseDeleteSelectOn ? View.GONE : View.VISIBLE);
        if (houseDeleteSelectOn) {
            attachDeleteListenersForAtlas(atlasResId, true);
            Toast.makeText(this, "삭제 모드: 삭제할 벽을 탭하세요.", Toast.LENGTH_SHORT).show();
        } else {
            detachDeleteListenersForAtlas(atlasResId);
            recalcAllGridMasks();
            Toast.makeText(this, "삭제 모드 해제", Toast.LENGTH_SHORT).show();
        }
    }
}