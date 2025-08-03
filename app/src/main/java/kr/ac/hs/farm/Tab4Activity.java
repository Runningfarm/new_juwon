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
    private ChipGroup farmCategoryGroup;
    private Chip chipCrop, chipCollect, chipDecor, chipPicnic, chipStructure, chipFence;
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
        tabLayout.addTab(tabLayout.newTab().setText("배경"));
        tabLayout.addTab(tabLayout.newTab().setText("먹이"));

        farmCategoryGroup = findViewById(R.id.farmCategoryGroup);
        chipCrop = findViewById(R.id.chip_crop);
        chipCollect = findViewById(R.id.chip_collect);
        chipDecor = findViewById(R.id.chip_decor);
        chipPicnic = findViewById(R.id.chip_picnic);
        chipStructure = findViewById(R.id.chip_structure);
        chipFence = findViewById(R.id.chip_fence);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        itemList = new ArrayList<>();
        loadItems("농작물");

        farmCategoryGroup.setVisibility(View.VISIBLE);
        chipCrop.setChecked(true);

        adapter = new ItemAdapter(itemList, this, null);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabName = tab.getText().toString();
                switch (tabName) {
                    case "농장":
                        farmCategoryGroup.setVisibility(View.VISIBLE);
                        chipCrop.setChecked(true);
                        loadItems("농작물");
                        break;
                    case "목장":
                        farmCategoryGroup.setVisibility(View.GONE);
                        loadItems("목장");
                        break;
                    case "배경":
                        farmCategoryGroup.setVisibility(View.GONE);
                        loadItems("배경");
                        break;
                    case "먹이":
                        farmCategoryGroup.setVisibility(View.GONE);
                        loadItems("먹이");
                        break;
                }
                adapter.updateList(itemList);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chipCrop.setOnClickListener(v -> {
            loadItems("농작물");
            adapter.updateList(itemList);
        });

        chipCollect.setOnClickListener(v -> {
            loadItems("채집");
            adapter.updateList(itemList);
        });

        chipDecor.setOnClickListener(v -> {
            loadItems("장식물");
            adapter.updateList(itemList);
        });

        chipPicnic.setOnClickListener(v -> {
            loadItems("피크닉");
            adapter.updateList(itemList);
        });

        chipStructure.setOnClickListener(v -> {
            loadItems("구조물");
            adapter.updateList(itemList);
        });

        chipFence.setOnClickListener(v -> {
            loadItems("울타리");
            adapter.updateList(itemList);
        });
    }

    private void loadItems(String category) {
        itemList.clear();

        if (category.equals("농작물")) {
            String[] names = {
                    "wheat", "potato", "cauliflower", "beet", "egg_plant",
                    "cabbage", "corn", "pumpkin", "radish", "blueberry",
                    "starfruit", "pea", "red_mushroom", "red_spotted_mushroom",
                    "purple_mushroom", "purple_spotted_mushroom"
            };

            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item(name, "농작물", 0, resId, true));
                }
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
                if (resId != 0) {
                    itemList.add(new Item(name, "채집", 0, resId, true));
                }
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
                if (resId != 0) {
                    itemList.add(new Item(name, "장식물", 0, resId, true));
                }
            }

        } else if (category.equals("피크닉")) {
            String[] names = {"basket", "blanket"};
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item(name, "피크닉", 0, resId, true));
                }
            }

        } else if (category.equals("구조물")) {
            String[] names = {"mailbox", "water_well", "boat"};
            for (String name : names) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item(name, "구조물", 0, resId, true));
                }
            }

        } else if (category.equals("울타리")) {
            for (int i = 1; i <= 16; i++) {
                String name = "fence" + i;
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item(name, "울타리", 0, resId, true));
                }
            }

        } else if (category.equals("배경")) {
            String[] bgNames = {"grass_tiles", "soil_tiles", "stone_tiles"};
            for (String bg : bgNames) {
                int resId = getResources().getIdentifier(bg, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item("배경", "배경", 0, resId, true));
                }
            }

        } else if (category.equals("먹이")) {
            int feedImageRes = R.drawable.feed_item;
            int count = prefs.getInt(KEY_FOOD_COUNT, 3);
            itemList.add(new Item("먹이 아이템", "먹이", count, feedImageRes, true));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TabLayout.Tab selectedTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (selectedTab != null) {
            String selected = selectedTab.getText().toString();
            if (selected.equals("농장")) {
                loadItems("농작물");
            } else if (selected.equals("목장")) {
                loadItems("목장");
            } else if (selected.equals("배경")) {
                loadItems("배경");
            } else {
                loadItems("먹이");
            }
            adapter.updateList(itemList);
        }
    }
}
