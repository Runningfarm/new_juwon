package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import android.view.View;

public class Tab4Activity extends BaseActivity {

    private static final String PREFS_NAME = "FarmPrefs";
    private static final String KEY_FOOD_COUNT = "foodCount";
    private static final String KEY_LEVEL = "level"; // 현재 레벨 표시/안내용

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;

    private TabLayout tabLayout;

    // ── 농장 카테고리
    private ChipGroup farmCategoryGroup;
    private Chip chipCrop, chipCollect, chipDecor, chipPicnic, chipStructure; // chipFence 제거

    // ── 목장 카테고리 (가구 추가)
    private ChipGroup ranchCategoryGroup;
    private Chip chipAnimal, chipRanchStructure, chipBreeding, chipRanchFurniture;

    private ImageButton tab1Button, tab2Button, tab3Button, tab4Button, tab6Button;

    private SharedPreferences prefs;

    // ── 해금 키(메인과 동일 순서)
    // 10레벨마다 1개씩: 1)농작물 2)장식물 3)채집 4)피크닉 5)농장_구조물
    //                    6)동물 7)사육 8)목장_가구 9)목장_구조물 10)배경
    private static final String[] UNLOCK_KEYS = new String[]{
            "farm_crops","farm_decor","farm_gather","farm_picnic","farm_struct",
            "ranch_animals","ranch_breeding","ranch_furniture","ranch_struct",
            "backgrounds"
    };

    private int lastAllowedTabIndex = 0;  // 배경 탭이 잠금일 때 복귀용
    private boolean ignoreTabCallback = false; // 프로그래매틱 선택 시 재귀 방지

    private String getCurrentUserId() {
        SharedPreferences loginPrefs = getSharedPreferences("login", MODE_PRIVATE);
        return loginPrefs.getBoolean("isLoggedIn", false)
                ? loginPrefs.getString("id", null) : null;
    }
    private String scopedKey(String base) {
        String uid = getCurrentUserId();
        return base + "_" + (uid != null && !uid.trim().isEmpty() ? uid : "guest");
    }

    private boolean isUnlocked(String logicalKey) {
        if (prefs == null || logicalKey == null) return false;
        return prefs.getBoolean(scopedKey("unlock_" + logicalKey), false);
    }

    private int currentLevel() {
        return prefs.getInt(scopedKey(KEY_LEVEL), 1);
    }

    private void migrateUserScopedUnlocksOnce() {
        SharedPreferences.Editor ed = prefs.edit();
        boolean migrated = false;
        for (String k : UNLOCK_KEYS) {
            String oldK = "unlock_" + k;
            String newK = scopedKey(oldK);
            if (!prefs.contains(newK) && prefs.getBoolean(oldK, false)) {
                ed.putBoolean(newK, true);
                ed.remove(oldK);
                migrated = true;
            }
        }
        if (migrated) ed.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab4);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        migrateUserScopedUnlocksOnce();

        ImageButton tab1 = findViewById(R.id.tab1Button);
        ImageButton tab2 = findViewById(R.id.tab2Button);
        ImageButton tab3 = findViewById(R.id.tab3Button);
        ImageButton tab4 = findViewById(R.id.tab4Button);
        ImageButton tab6 = findViewById(R.id.tab6Button);

        // BaseActivity에 등록
        initBottomTabs(java.util.Arrays.asList(tab1, tab2, tab3, tab4, tab6));

        // 현재 탭 강조
        updateBottomBarUI(R.id.tab4Button);

        tab1Button = findViewById(R.id.tab1Button);
        tab2Button = findViewById(R.id.tab2Button);
        tab3Button = findViewById(R.id.tab3Button);
        tab4Button = findViewById(R.id.tab4Button);
        tab6Button = findViewById(R.id.tab6Button);

        tab1Button.setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        tab2Button.setOnClickListener(view -> startActivity(new Intent(this, Tab2Activity.class)));
        tab3Button.setOnClickListener(view -> startActivity(new Intent(this, Tab3Activity.class)));
        tab4Button.setOnClickListener(view -> startActivity(new Intent(this, Tab4Activity.class)));
        tab6Button.setOnClickListener(view -> startActivity(new Intent(this, Tab6Activity.class)));

        tabLayout = findViewById(R.id.tabLayout);

        // ── 탭 구성: "집" 탭 제거
        tabLayout.addTab(tabLayout.newTab().setText("농장"));
        tabLayout.addTab(tabLayout.newTab().setText("목장"));
        tabLayout.addTab(tabLayout.newTab().setText("배경"));
        tabLayout.addTab(tabLayout.newTab().setText("먹이"));

        // ── 농장 Chips
        farmCategoryGroup = findViewById(R.id.farmCategoryGroup);
        chipCrop = findViewById(R.id.chip_crop);
        chipCollect = findViewById(R.id.chip_collect);
        chipDecor = findViewById(R.id.chip_decor);
        chipPicnic = findViewById(R.id.chip_picnic);
        chipStructure = findViewById(R.id.chip_structure);

        // ── 목장 Chips (가구 추가)
        ranchCategoryGroup = findViewById(R.id.ranchCategoryGroup);
        chipAnimal = findViewById(R.id.chip_animal);
        chipRanchStructure = findViewById(R.id.chip_ranch_structure);
        chipBreeding = findViewById(R.id.chip_breeding);
        chipRanchFurniture = findViewById(R.id.chip_ranch_furniture);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        itemList = new ArrayList<>();
        showOnlyGroup("농장");
        chipCrop.setChecked(true);
        // 잠금 UI 먼저 반영
        applyLocksToUI();

        // 잠금 가드로 최초 로드
        if (guardChipAndLoad(chipCrop, "농작물", "농장 · 농작물", "farm_crops")) {
            itemList.clear();               // 잠겨 있으면 목록 비우기
        }

        adapter = new ItemAdapter(itemList, this, null);
        recyclerView.setAdapter(adapter);
        adapter.updateList(itemList);

        // ── 잠금 UI 반영
        applyLocksToUI(); // 칩/탭 비활성화 표시

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (ignoreTabCallback) return;

                String t = tab.getText() != null ? tab.getText().toString() : "";
                // 배경 탭 잠금 처리
                if ("배경".equals(t) && !isUnlocked("backgrounds")) {
                    showLockedToast("배경", requiredLevelFor("backgrounds"));
                    // 이전 탭으로 되돌리기
                    ignoreTabCallback = true;
                    TabLayout.Tab prev = tabLayout.getTabAt(lastAllowedTabIndex);
                    if (prev != null) tabLayout.selectTab(prev);
                    ignoreTabCallback = false;
                    return;
                }

                // 정상 전환
                lastAllowedTabIndex = tab.getPosition();
                switch (t) {
                    case "농장":
                        showOnlyGroup("농장");
                        chipCrop.setChecked(true);
                        if (guardChipAndLoad(chipCrop, "농작물", "농장 · 농작물", "farm_crops")) {
                            itemList.clear();
                            adapter.updateList(itemList);
                            return;
                        }
                        adapter.updateList(itemList);
                        break;
                    case "목장":
                        showOnlyGroup("목장");
                        chipAnimal.setChecked(true);
                        if (guardChipAndLoad(chipAnimal, "동물", "목장 · 동물", "ranch_animals")) break;
                        break;
                    case "배경":
                        showOnlyGroup("없음");
                        loadItems("배경");
                        break;
                    case "먹이":
                        showOnlyGroup("없음");
                        loadItems("먹이");
                        break;
                }
                adapter.updateList(itemList);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ── 농장 chips (잠금 가드 포함)
        chipCrop.setOnClickListener(v -> { guardChipAndLoad(chipCrop,"농작물","농장 · 농작물","farm_crops"); adapter.updateList(itemList); });
        chipCollect.setOnClickListener(v -> { guardChipAndLoad(chipCollect,"채집","농장 · 채집","farm_gather"); adapter.updateList(itemList); });
        chipDecor.setOnClickListener(v -> { guardChipAndLoad(chipDecor,"장식물","농장 · 장식물","farm_decor"); adapter.updateList(itemList); });
        chipPicnic.setOnClickListener(v -> { guardChipAndLoad(chipPicnic,"피크닉","농장 · 피크닉","farm_picnic"); adapter.updateList(itemList); });
        chipStructure.setOnClickListener(v -> { guardChipAndLoad(chipStructure,"구조물","농장 · 구조물","farm_struct"); adapter.updateList(itemList); });

        // ── 목장 chips (잠금 가드 포함)
        chipAnimal.setOnClickListener(v -> { guardChipAndLoad(chipAnimal,"동물","목장 · 동물","ranch_animals"); adapter.updateList(itemList); });
        chipRanchStructure.setOnClickListener(v -> { guardChipAndLoad(chipRanchStructure,"목장_구조물","목장 · 구조물","ranch_struct"); adapter.updateList(itemList); });
        chipBreeding.setOnClickListener(v -> { guardChipAndLoad(chipBreeding,"사육","목장 · 사육","ranch_breeding"); adapter.updateList(itemList); });
        chipRanchFurniture.setOnClickListener(v -> { guardChipAndLoad(chipRanchFurniture,"목장_가구","목장 · 가구","ranch_furniture"); adapter.updateList(itemList); });
    }

    /** 잠긴 칩이면 토스트 안내 후 체크 해제 및 로딩 취소, 아니면 로딩 */
    private boolean guardChipAndLoad(Chip chip, String category, String humanLabel, String unlockKey) {
        if (!isUnlocked(unlockKey)) {
            showLockedToast(humanLabel, requiredLevelFor(unlockKey));
            chip.setChecked(false);
            return true; // 가드에 걸림(로딩하지 않음)
        }
        loadItems(category);
        return false;
    }

    private void showOnlyGroup(String which) {
        if ("농장".equals(which)) {
            farmCategoryGroup.setVisibility(View.VISIBLE);
            ranchCategoryGroup.setVisibility(View.GONE);
        } else if ("목장".equals(which)) {
            farmCategoryGroup.setVisibility(View.GONE);
            ranchCategoryGroup.setVisibility(View.VISIBLE);
        } else {
            farmCategoryGroup.setVisibility(View.GONE);
            ranchCategoryGroup.setVisibility(View.GONE);
        }
    }

    /** 칩/탭에 잠금 상태 시각 반영 */
    private void applyLocksToUI() {
        // 칩 비활성화/투명도
        setChipLockVisual(chipCrop,          isUnlocked("farm_crops"));
        setChipLockVisual(chipDecor,         isUnlocked("farm_decor"));
        setChipLockVisual(chipCollect,       isUnlocked("farm_gather"));
        setChipLockVisual(chipPicnic,        isUnlocked("farm_picnic"));
        setChipLockVisual(chipStructure,     isUnlocked("farm_struct"));

        setChipLockVisual(chipAnimal,        isUnlocked("ranch_animals"));
        setChipLockVisual(chipBreeding,      isUnlocked("ranch_breeding"));
        setChipLockVisual(chipRanchFurniture,isUnlocked("ranch_furniture"));
        setChipLockVisual(chipRanchStructure,isUnlocked("ranch_struct"));

        // 배경 탭 라벨에 잠금 뱃지 느낌(선택은 리스너에서 가드)
        TabLayout.Tab bgTab = tabLayout.getTabAt(2); // 농장(0), 목장(1), 배경(2), 먹이(3)
        if (bgTab != null) {
            if (!isUnlocked("backgrounds")) bgTab.setText("배경");
            else bgTab.setText("배경");
        }
    }

    private void setChipLockVisual(Chip chip, boolean unlocked) {
        chip.setEnabled(unlocked);
        chip.setAlpha(unlocked ? 1.0f : 0.45f);
    }

    /** 이 logicalKey가 해금되려면 필요한 레벨(10,20,...) */
    private int requiredLevelFor(String logicalKey) {
        int idx = -1;
        for (int i = 0; i < UNLOCK_KEYS.length; i++) {
            if (UNLOCK_KEYS[i].equals(logicalKey)) { idx = i; break; }
        }
        if (idx < 0) return 0;
        return (idx + 1) * 10;
    }

    private void showLockedToast(String label, int needLevel) {
        int fixedLv;

        if (label.contains("농장")) {
            fixedLv = 10;
        } else if (label.contains("목장")) {
            fixedLv = 60;
        } else if (label.contains("배경")) {
            fixedLv = 100;
        } else {
            fixedLv = needLevel; // fallback
        }

        String msg = "해금 조건: LV " + fixedLv + "이 필요합니다.";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void loadItems(String category) {
        itemList.clear();

        if (category.equals("농작물")) {
            String[] names = { "wheat","potato","cauliflower","beet","egg_plant",
                    "cabbage","corn","pumpkin","radish","blueberry",
                    "starfruit","pea","red_mushroom","red_spotted_mushroom",
                    "purple_mushroom","purple_spotted_mushroom" };
            for (String n : names) addIfExists(n, "농작물");

        } else if (category.equals("채집")) {
            String[] names = { "grass1","grass2","grass3","grass4",
                    "stone1","stone2","stone3","stone4","stone5","stone6",
                    "rock1","rock2","thin_tree","basic_tree","wide_tree",
                    "small_stump","basic_stump","big_stump",
                    "small_fallen_tree","big_fallen_tree" };
            for (String n : names) addIfExists(n, "채집");

        } else if (category.equals("장식물")) {
            String[] names = { "lotus","lilac","sunflower","blue_tulip","sky_blue_flower",
                    "blue_flower","beige_flower","heart_flower",
                    "small_bush","big_bush",
                    "long_wooden_path","wide_wooden_path",
                    "small_stone_path","long_stone_path","wide_stone_path",
                    "sign","left_diagonal_sign","right_diagonal_sign" };
            for (String n : names) addIfExists(n, "장식물");

        } else if (category.equals("피크닉")) {
            for (String n : new String[]{"basket","blanket"}) addIfExists(n, "피크닉");

        } else if (category.equals("구조물")) {
            // ── 농장-구조물에 울타리 툴 포함 (울타리 카테고리 삭제됨)
            for (String n : new String[]{"mailbox","water_well","boat"}) addIfExists(n, "구조물");
            int fencesResId = getResources().getIdentifier("fences", "drawable", getPackageName());
            if (fencesResId == 0) fencesResId = R.drawable.fences;
            itemList.add(new Item("fence", "구조물", 0, fencesResId, true));

        } else if (category.equals("배경")) {
            for (String n : new String[]{"tiles_grass","tiles_soil","tiles_stone"}) addIfExistsAs("배경", n, "배경");

        } else if (category.equals("먹이")) {
            int feedImageRes = getResources().getIdentifier("feed_item", "drawable", getPackageName());
            if (feedImageRes == 0) feedImageRes = R.drawable.feed_item;
            int count = prefs.getInt(KEY_FOOD_COUNT, 3);
            itemList.add(new Item("먹이 아이템", "먹이", count, feedImageRes, true));

        } else if (category.equals("동물")) {
            for (String n : new String[]{"chicken","cow"}) addIfExists(n, "동물");

        } else if (category.equals("목장_구조물")) {
            addIfExists("chicken_house", "목장_구조물");
            int atlasResId = getResources().getIdentifier("wooden_house_walls", "drawable", getPackageName());
            if (atlasResId == 0) atlasResId = R.drawable.wooden_house_walls;
            itemList.add(new Item("house_wall_tool", "목장_구조물", 0, atlasResId, true));

        } else if (category.equals("사육")) {
            String[] names = { "straw","big_straw","haystack","big_haystack",
                    "basket_one","basket_two","water_tray","empty_water_tray" };
            for (String n : names) addIfExists(n, "사육");

        } else if (category.equals("목장_가구")) {
            String[] names = {
                    "bed_light_green","bed_pink","bed_skyblue",
                    "carpet","carpet_light_green","carpet_pink","carpet_skyblue",
                    "chair_behind","chair_front","chair_left","chair_right",
                    "clock","clock_edge","clock_bezel",
                    "frame_flower","frame_morning","frame_night",
                    "mood_light_light_green","mood_light_pink","mood_light_skyblue",
                    "nightstand",
                    "pot_blue_flower","pot_sprout","pot_sunflower",
                    "table_big","table_small",
                    "chest"
            };
            for (String n : names) addIfExists(n, "목장_가구");
        }
    }

    private void addIfExists(String entryName, String category) {
        int resId = getResources().getIdentifier(entryName, "drawable", getPackageName());
        if (resId != 0) itemList.add(new Item(entryName, category, 0, resId, true));
    }
    private void addIfExistsAs(String name, String entry, String category) {
        int resId = getResources().getIdentifier(entry, "drawable", getPackageName());
        if (resId != 0) itemList.add(new Item(name, category, 0, resId, true));
    }

    @Override protected void onResume() {
        super.onResume();
        // 레벨업 후 돌아왔을 수 있으니 UI 잠금 재반영
        applyLocksToUI();

        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab != null) {
            String t = tab.getText() != null ? tab.getText().toString().replace(" 🔒","") : "";
            if (t.equals("농장")) {
                if (chipRanchStructure.isChecked()) chipRanchStructure.setChecked(false); // 잘못 켜져 있으면 초기화
                if (chipBreeding.isChecked()) chipBreeding.setChecked(false);
                if (chipRanchFurniture.isChecked()) chipRanchFurniture.setChecked(false);

                if (chipCollect.isChecked()) {
                    guardChipAndLoad(chipCollect,"채집","농장 · 채집","farm_gather");
                } else if (chipDecor.isChecked()) {
                    guardChipAndLoad(chipDecor,"장식물","농장 · 장식물","farm_decor");
                } else if (chipPicnic.isChecked()) {
                    guardChipAndLoad(chipPicnic,"피크닉","농장 · 피크닉","farm_picnic");
                } else if (chipStructure.isChecked()) {
                    guardChipAndLoad(chipStructure,"구조물","농장 · 구조물","farm_struct");
                } else {
                    guardChipAndLoad(chipCrop,"농작물","농장 · 농작물","farm_crops"); // 기본
                }
            } else if (t.equals("목장")) {
                if (chipAnimal.isChecked()) {
                    guardChipAndLoad(chipAnimal,"동물","목장 · 동물","ranch_animals");
                } else if (chipRanchStructure.isChecked()) {
                    guardChipAndLoad(chipRanchStructure,"목장_구조물","목장 · 구조물","ranch_struct");
                } else if (chipBreeding.isChecked()) {
                    guardChipAndLoad(chipBreeding,"사육","목장 · 사육","ranch_breeding");
                } else if (chipRanchFurniture.isChecked()) {
                    guardChipAndLoad(chipRanchFurniture,"목장_가구","목장 · 가구","ranch_furniture");
                } else {
                    chipAnimal.setChecked(true);
                    guardChipAndLoad(chipAnimal,"동물","목장 · 동물","ranch_animals");
                }
            } else if (t.equals("배경")) {
                if (!isUnlocked("backgrounds")) {
                    showLockedToast("배경", requiredLevelFor("backgrounds"));
                    ignoreTabCallback = true;
                    TabLayout.Tab prev = tabLayout.getTabAt(lastAllowedTabIndex);
                    if (prev != null) tabLayout.selectTab(prev);
                    ignoreTabCallback = false;
                } else {
                    loadItems("배경");
                }
            } else { // 먹이
                loadItems("먹이");
            }
            adapter.updateList(itemList);
        }
    }
}
