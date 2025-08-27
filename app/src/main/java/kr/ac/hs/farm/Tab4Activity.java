package kr.ac.hs.farm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import android.view.View;

public class Tab4Activity extends AppCompatActivity {

    private static final String PREFS_NAME = "FarmPrefs";
    private static final String KEY_FOOD_COUNT = "foodCount";

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;

    private TabLayout tabLayout;

    private ChipGroup farmCategoryGroup;
    private Chip chipCrop, chipCollect, chipDecor, chipPicnic, chipStructure, chipFence;

    private ChipGroup ranchCategoryGroup;
    private Chip chipAnimal, chipRanchStructure, chipBreeding, chipEtc;

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
        tabLayout.addTab(tabLayout.newTab().setText("집"));
        tabLayout.addTab(tabLayout.newTab().setText("배경"));
        tabLayout.addTab(tabLayout.newTab().setText("먹이"));

        farmCategoryGroup = findViewById(R.id.farmCategoryGroup);
        chipCrop = findViewById(R.id.chip_crop);
        chipCollect = findViewById(R.id.chip_collect);
        chipDecor = findViewById(R.id.chip_decor);
        chipPicnic = findViewById(R.id.chip_picnic);
        chipStructure = findViewById(R.id.chip_structure);
        chipFence = findViewById(R.id.chip_fence);

        ranchCategoryGroup = findViewById(R.id.ranchCategoryGroup);
        chipAnimal = findViewById(R.id.chip_animal);
        chipRanchStructure = findViewById(R.id.chip_ranch_structure);
        chipBreeding = findViewById(R.id.chip_breeding);
        chipEtc = findViewById(R.id.chip_etc);

        houseCategoryGroup = findViewById(R.id.houseCategoryGroup);
        chipFurniture = findViewById(R.id.chip_furniture);
        chipArchitecture = findViewById(R.id.chip_architecture);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        itemList = new ArrayList<>();
        showOnlyGroup("농장");
        chipCrop.setChecked(true);
        loadItems("농작물");

        adapter = new ItemAdapter(itemList, this, null);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                String t = tab.getText() != null ? tab.getText().toString() : "";
                switch (t) {
                    case "농장": showOnlyGroup("농장"); chipCrop.setChecked(true); loadItems("농작물"); break;
                    case "목장": showOnlyGroup("목장"); chipAnimal.setChecked(true); loadItems("동물"); break;
                    case "집":   showOnlyGroup("집");
                        chipFurniture.setChecked(true);   // 기본 선택을 '가구'로
                        loadItems("가구");                 // 처음 로드도 '가구'
                        break;
                    case "배경": showOnlyGroup("없음"); loadItems("배경"); break;
                    case "먹이": showOnlyGroup("없음"); loadItems("먹이"); break;
                }
                adapter.updateList(itemList);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 농장 chips
        chipCrop.setOnClickListener(v -> { loadItems("농작물"); adapter.updateList(itemList); });
        chipCollect.setOnClickListener(v -> { loadItems("채집"); adapter.updateList(itemList); });
        chipDecor.setOnClickListener(v -> { loadItems("장식물"); adapter.updateList(itemList); });
        chipPicnic.setOnClickListener(v -> { loadItems("피크닉"); adapter.updateList(itemList); });
        chipStructure.setOnClickListener(v -> { loadItems("구조물"); adapter.updateList(itemList); });
        chipFence.setOnClickListener(v -> { loadItems("울타리"); adapter.updateList(itemList); });

        // 목장 chips
        chipAnimal.setOnClickListener(v -> { loadItems("동물"); adapter.updateList(itemList); });
        chipRanchStructure.setOnClickListener(v -> { loadItems("목장_구조물"); adapter.updateList(itemList); });
        chipBreeding.setOnClickListener(v -> { loadItems("사육"); adapter.updateList(itemList); });
        chipEtc.setOnClickListener(v -> { loadItems("기타"); adapter.updateList(itemList); });

        // 집 chips
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
            for (String n : new String[]{"mailbox","water_well","boat"}) addIfExists(n, "구조물");

        } else if (category.equals("울타리")) {
            int resId = getResources().getIdentifier("fences", "drawable", getPackageName());
            if (resId == 0) resId = R.drawable.fences;
            itemList.add(new Item("fence_tool", "울타리", 0, resId, true));

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
            // chicken_house + 집 벽 빌더툴(목장 문구로 실행)
            addIfExists("chicken_house", "목장_구조물");
            int atlasResId = getResources().getIdentifier("wooden_house_walls", "drawable", getPackageName());
            if (atlasResId == 0) atlasResId = R.drawable.wooden_house_walls;
            itemList.add(new Item("house_wall_tool", "목장_구조물", 0, atlasResId, true));

        } else if (category.equals("사육")) {
            String[] names = { "straw","big_straw","haystack","big_haystack",
                    "basket_one","basket_two","water_tray","empty_water_tray" };
            for (String n : names) addIfExists(n, "사육");

        } else if (category.equals("기타")) {
            addIfExists("chest", "기타");

        } else if (category.equals("가구")) {
            String[] names = {
                    "bed_light_green","bed_pink","bed_skyblue",
                    "carpet","carpet_light_green","carpet_pink","carpet_skyblue",
                    "chair_behind","chair_front","chair_left","chair_right",
                    "clock","clock_edge","clock_bezel",
                    "frame_flower","frame_morning","frame_night",
                    "mood_light_light_green","mood_light_pink","mood_light_skyblue",
                    "nightstand",
                    "pot_blue_flower","pot_sprout","pot_sunflower",
                    "table_big","table_small"
            };
            for (String n : names) addIfExists(n, "가구");

        } else if (category.equals("건축물")) {
            // 집 > 건축물: 집 벽 드래그 설치 툴
            int atlasResId = getResources().getIdentifier("wooden_house_walls", "drawable", getPackageName());
            if (atlasResId == 0) atlasResId = R.drawable.wooden_house_walls;
            itemList.add(new Item("house_wall_tool", "건축물", 0, atlasResId, true));
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
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab != null) {
            String t = tab.getText() != null ? tab.getText().toString() : "";
            if (t.equals("농장")) {
                if (chipCollect.isChecked()) loadItems("채집");
                else if (chipDecor.isChecked()) loadItems("장식물");
                else if (chipPicnic.isChecked()) loadItems("피크닉");
                else if (chipStructure.isChecked()) loadItems("구조물");
                else if (chipFence.isChecked()) loadItems("울타리");
                else loadItems("농작물");
            } else if (t.equals("목장")) {
                if (chipRanchStructure.isChecked()) loadItems("목장_구조물");
                else if (chipBreeding.isChecked()) loadItems("사육");
                else if (chipEtc.isChecked()) loadItems("기타");
                else loadItems("동물");
            } else if (t.equals("집")) {
                if (chipArchitecture.isChecked()) loadItems("건축물");
                else loadItems("가구");
            } else if (t.equals("배경")) {
                loadItems("배경");
            } else {
                loadItems("먹이");
            }
            adapter.updateList(itemList);
        }
    }
}
