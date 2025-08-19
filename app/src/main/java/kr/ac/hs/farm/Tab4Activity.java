package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class Tab4Activity extends AppCompatActivity {

    private static final String PREFS_NAME = "FarmPrefs";
    private static final String KEY_FOOD_COUNT = "foodCount";

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;

    private TabLayout tabLayout;

    // 농장 ChipGroup
    private ChipGroup farmCategoryGroup;
    private Chip chipCrop, chipCollect, chipDecor, chipPicnic, chipStructure, chipFence;

    // 목장 ChipGroup
    private ChipGroup ranchCategoryGroup;
    private Chip chipAnimal, chipRanchStructure, chipBreeding, chipEtc;

    // 집 ChipGroup (신규)
    private ChipGroup houseCategoryGroup;
    private Chip chipFurniture, chipArchitecture;

    private ImageButton tab1Button, tab2Button, tab3Button, tab4Button, tab6Button;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab4);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
        tabLayout.addTab(tabLayout.newTab().setText("농장"));
        tabLayout.addTab(tabLayout.newTab().setText("목장"));
        tabLayout.addTab(tabLayout.newTab().setText("집"));     // ★ 신규 탭
        tabLayout.addTab(tabLayout.newTab().setText("배경"));
        tabLayout.addTab(tabLayout.newTab().setText("먹이"));

        // 농장 그룹
        farmCategoryGroup = findViewById(R.id.farmCategoryGroup);
        chipCrop = findViewById(R.id.chip_crop);
        chipCollect = findViewById(R.id.chip_collect);
        chipDecor = findViewById(R.id.chip_decor);
        chipPicnic = findViewById(R.id.chip_picnic);
        chipStructure = findViewById(R.id.chip_structure);
        chipFence = findViewById(R.id.chip_fence);

        // 목장 그룹
        ranchCategoryGroup = findViewById(R.id.ranchCategoryGroup);
        chipAnimal = findViewById(R.id.chip_animal);
        chipRanchStructure = findViewById(R.id.chip_ranch_structure);
        chipBreeding = findViewById(R.id.chip_breeding);
        chipEtc = findViewById(R.id.chip_etc);

        // 집 그룹 (신규)
        houseCategoryGroup = findViewById(R.id.houseCategoryGroup);
        chipFurniture = findViewById(R.id.chip_furniture);
        chipArchitecture = findViewById(R.id.chip_architecture);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        itemList = new ArrayList<>();
        // 초기: 농장-농작물
        showOnlyGroup("농장");
        chipCrop.setChecked(true);
        loadItems("농작물");

        adapter = new ItemAdapter(itemList, this, null);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabName = tab.getText() != null ? tab.getText().toString() : "";
                switch (tabName) {
                    case "농장":
                        showOnlyGroup("농장");
                        chipCrop.setChecked(true);
                        loadItems("농작물");
                        break;
                    case "목장":
                        showOnlyGroup("목장");
                        chipAnimal.setChecked(true);
                        loadItems("동물");
                        break;
                    case "집":
                        showOnlyGroup("집");
                        chipFurniture.setChecked(true);
                        loadItems("가구");
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

        // 농장 chip 클릭
        chipCrop.setOnClickListener(v -> { loadItems("농작물"); adapter.updateList(itemList); });
        chipCollect.setOnClickListener(v -> { loadItems("채집"); adapter.updateList(itemList); });
        chipDecor.setOnClickListener(v -> { loadItems("장식물"); adapter.updateList(itemList); });
        chipPicnic.setOnClickListener(v -> { loadItems("피크닉"); adapter.updateList(itemList); });
        chipStructure.setOnClickListener(v -> { loadItems("구조물"); adapter.updateList(itemList); });
        chipFence.setOnClickListener(v -> { loadItems("울타리"); adapter.updateList(itemList); });

        // 목장 chip 클릭
        chipAnimal.setOnClickListener(v -> { loadItems("동물"); adapter.updateList(itemList); });
        chipRanchStructure.setOnClickListener(v -> { loadItems("목장_구조물"); adapter.updateList(itemList); });
        chipBreeding.setOnClickListener(v -> { loadItems("사육"); adapter.updateList(itemList); });
        chipEtc.setOnClickListener(v -> { loadItems("기타"); adapter.updateList(itemList); });

        // 집 chip 클릭 (신규)
        chipFurniture.setOnClickListener(v -> { loadItems("가구"); adapter.updateList(itemList); });
        chipArchitecture.setOnClickListener(v -> { loadItems("건축물"); adapter.updateList(itemList); });
    }

    private void showOnlyGroup(String which) {
        if ("농장".equals(which)) {
            farmCategoryGroup.setVisibility(View.VISIBLE);
            ranchCategoryGroup.setVisibility(View.GONE);
            houseCategoryGroup.setVisibility(View.GONE);
        } else if ("목장".equals(which)) {
            farmCategoryGroup.setVisibility(View.GONE);
            ranchCategoryGroup.setVisibility(View.VISIBLE);
            houseCategoryGroup.setVisibility(View.GONE);
        } else if ("집".equals(which)) {
            farmCategoryGroup.setVisibility(View.GONE);
            ranchCategoryGroup.setVisibility(View.GONE);
            houseCategoryGroup.setVisibility(View.VISIBLE);
        } else {
            farmCategoryGroup.setVisibility(View.GONE);
            ranchCategoryGroup.setVisibility(View.GONE);
            houseCategoryGroup.setVisibility(View.GONE);
        }
    }

    private void loadItems(String category) {
        itemList.clear();

        // -------- 농장 카테고리 --------
        if (category.equals("농작물")) {
            String[] names = {
                    "wheat", "potato", "cauliflower", "beet", "egg_plant",
                    "cabbage", "corn", "pumpkin", "radish", "blueberry",
                    "starfruit", "pea", "red_mushroom", "red_spotted_mushroom",
                    "purple_mushroom", "purple_spotted_mushroom"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "농작물", 0, resId, true));
            }

        } else if (category.equals("채집")) {
            String[] names = {
                    "grass1", "grass2", "grass3", "grass4",
                    "stone1", "stone2", "stone3", "stone4", "stone5", "stone6",
                    "rock1", "rock2", "thin_tree", "basic_tree", "wide_tree",
                    "small_stump", "basic_stump", "big_stump",
                    "small_fallen_tree", "big_fallen_tree"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "채집", 0, resId, true));
            }

        } else if (category.equals("장식물")) {
            String[] names = {
                    "lotus", "lilac", "sunflower", "blue_tulip", "sky_blue_flower",
                    "blue_flower", "beige_flower", "heart_flower",
                    "small_bush", "big_bush",
                    "long_wooden_path", "wide_wooden_path",
                    "small_stone_path", "long_stone_path", "wide_stone_path",
                    "sign", "left_diagonal_sign", "right_diagonal_sign"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "장식물", 0, resId, true));
            }

        } else if (category.equals("피크닉")) {
            String[] names = {"basket", "blanket"};
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "피크닉", 0, resId, true));
            }

        } else if (category.equals("구조물")) {
            String[] names = {"mailbox", "water_well", "boat"};
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "구조물", 0, resId, true));
            }

        } else if (category.equals("울타리")) {
            for (int i = 1; i <= 16; i++) {
                String name = "fence" + i;
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "울타리", 0, resId, true));
            }

            // -------- 공용 탭 --------
        } else if (category.equals("배경")) {
            String[] bgNames = {"tiles_grass", "tiles_soil", "tiles_stone"};
            for (String bg : bgNames) {
                int resId = getResources().getIdentifier(bg, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item("배경", "배경", 0, resId, true));
            }

        } else if (category.equals("먹이")) {
            int feedImageRes = getResources().getIdentifier("feed_item", "drawable", getPackageName());
            if (feedImageRes == 0) feedImageRes = R.drawable.feed_item; // fallback
            int count = prefs.getInt(KEY_FOOD_COUNT, 3);
            itemList.add(new Item("먹이 아이템", "먹이", count, feedImageRes, true));

            // -------- 목장 카테고리 --------
        } else if (category.equals("동물")) {
            String[] names = { "chicken", "cow" };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "동물", 0, resId, true));
            }

        } else if (category.equals("목장_구조물")) {
            String[] names = {
                    "top_left_wall", "top_center_wall", "top_right_wall",
                    "bottom_left_wall", "bottom_center_wall", "bottom_right_wall",
                    "center_wall",
                    "wide_top_left_wall", "wide_top_right_wall",
                    "wide_bottom_left_wall", "wide_bottom_right_wall",
                    "hole_wall", "floor", "chicken_house"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "목장_구조물", 0, resId, true));
            }

        } else if (category.equals("사육")) {
            String[] names = {
                    "straw", "big_straw", "haystack", "big_haystack",
                    "basket_one", "basket_two", "water_tray", "empty_water_tray"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "사육", 0, resId, true));
            }

        } else if (category.equals("기타")) {
            String[] names = { "chest" };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "기타", 0, resId, true));
            }

            // -------- 집 카테고리 (신규) --------
        } else if (category.equals("가구")) {
            String[] names = {
                    "bed_light_green", "bed_pink", "bed_skyblue",
                    "carpet", "carpet_light_green", "carpet_pink", "carpet_skyblue",
                    "chair_behind", "chair_front", "chair_left", "chair_right",
                    "clock", "clock_edge", "clock_bezel",
                    "frame_flower", "frame_morning", "frame_night",
                    "mood_light_light_green", "mood_light_pink", "mood_light_skyblue",
                    "nightstand",
                    "pot_blue_flower", "pot_sprout", "pot_sunflower",
                    "table_big", "table_small"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "가구", 0, resId, true));
            }

        } else if (category.equals("건축물")) {
            // 아직 제공된 리소스명이 없으니 비워둡니다. 추후 이름 전달되면 여기에 추가.
            String[] names = {
                    // 예: "indoor_wall", "window_large", "door_basic"
            };
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) itemList.add(new Item(name, "건축물", 0, resId, true));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TabLayout.Tab selectedTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (selectedTab != null) {
            String selected = selectedTab.getText() != null ? selectedTab.getText().toString() : "";
            if (selected.equals("농장")) {
                if (chipCollect.isChecked()) loadItems("채집");
                else if (chipDecor.isChecked()) loadItems("장식물");
                else if (chipPicnic.isChecked()) loadItems("피크닉");
                else if (chipStructure.isChecked()) loadItems("구조물");
                else if (chipFence.isChecked()) loadItems("울타리");
                else loadItems("농작물");
            } else if (selected.equals("목장")) {
                if (chipRanchStructure.isChecked()) loadItems("목장_구조물");
                else if (chipBreeding.isChecked()) loadItems("사육");
                else if (chipEtc.isChecked()) loadItems("기타");
                else loadItems("동물");
            } else if (selected.equals("집")) {
                if (chipArchitecture.isChecked()) loadItems("건축물");
                else loadItems("가구");
            } else if (selected.equals("배경")) {
                loadItems("배경");
            } else {
                loadItems("먹이");
            }
            adapter.updateList(itemList);
        }
    }
}
