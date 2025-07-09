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
    private Chip chipFence, chipCrop, chipFurniture;

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
        tabLayout.addTab(tabLayout.newTab().setText("먹이"));

        farmCategoryGroup = findViewById(R.id.farmCategoryGroup);
        chipFence = findViewById(R.id.chip_fence);
        chipCrop = findViewById(R.id.chip_crop);
        chipFurniture = findViewById(R.id.chip_furniture);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        itemList = new ArrayList<>();
        loadItems("울타리");

        farmCategoryGroup.setVisibility(View.VISIBLE);
        chipFence.setChecked(true);

        adapter = new ItemAdapter(itemList, this, null);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                String category = tab.getText().toString();
                if (category.equals("농장")) {
                    farmCategoryGroup.setVisibility(View.VISIBLE);
                    chipFence.setChecked(true);
                    loadItems("울타리");
                } else {
                    farmCategoryGroup.setVisibility(View.GONE);
                    loadItems("먹이");
                }
                adapter.updateList(itemList);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chipFence.setOnClickListener(v -> {
            loadItems("울타리");
            adapter.updateList(itemList);
        });

        chipCrop.setOnClickListener(v -> {
            loadItems("작물");
            adapter.updateList(itemList);
        });

        chipFurniture.setOnClickListener(v -> {
            loadItems("가구");
            adapter.updateList(itemList);
        });
    }

    private void loadItems(String category) {
        itemList.clear();

        if (category.equals("울타리")) {
            for (int i = 0; i < 32; i++) {
                String resName = String.format("tile%03d", i);
                int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item("울타리 " + (i + 1), "울타리", 0, resId, true));
                }
            }

        } else if (category.equals("작물")) {
            String[] cropNames = {
                    "blueberry", "cabbage", "circle", "corn", "flower",
                    "pea", "potato", "pumkin", "purple", "radish",
                    "red", "rice1", "rice2", "sprout", "starfruit", "tulip"
            };

            for (int i = 0; i < cropNames.length; i++) {
                int resId = getResources().getIdentifier(cropNames[i], "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item("작물 " + (i + 1), "작물", 0, resId, true));
                }
            }

        } else if (category.equals("가구")) {
            String[] furnitureNames = {
                    "allcarpet", "bed1", "bed2", "bed3",
                    "carpet1", "carpet2", "carpet3",
                    "frameimg1", "frameimg2", "frameimg3", "nightstand"
            };

            for (int i = 0; i < furnitureNames.length; i++) {
                int resId = getResources().getIdentifier(furnitureNames[i], "drawable", getPackageName());
                if (resId != 0) {
                    itemList.add(new Item("가구 " + (i + 1), "가구", 0, resId, true));
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
        if (tabLayout.getSelectedTabPosition() == 0) {
            loadItems("울타리");
        } else {
            loadItems("먹이");
        }
        adapter.updateList(itemList);
    }
}
