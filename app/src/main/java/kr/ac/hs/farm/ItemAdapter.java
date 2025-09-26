package kr.ac.hs.farm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<Item> itemList;
    private final Context context;
    private final ItemClickListener itemClickListener;

    // 표시용 이름 매핑: 리소스명 -> "한글"
    private static final Map<String, String> nameMap = buildNameMap();

    public interface ItemClickListener {
        void onFarmItemClick(Item item);
    }

    public ItemAdapter(List<Item> itemList, Context context, ItemClickListener listener) {
        this.itemList = itemList;
        this.context = context;
        this.itemClickListener = listener;
    }

    public void updateList(List<Item> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itemList.get(position);

        // === 인벤토리 썸네일만 치환 ===
        int thumbRes = item.imageRes;

        // 울타리 툴(인벤토리 아이콘만 별도)
        if ("fence".equals(item.name)) {
            Integer icon = resolveDrawableId("fence", "fence_tool_icon", "fence_icon");
            if (icon != null) thumbRes = icon;
        }

        // 집 설치 툴(목장/집 공통) → house.png 로 보이게
        if ("house_wall_tool".equals(item.name)
                && ("목장_구조물".equals(item.category) || "건축물".equals(item.category))) {
            Integer icon = resolveDrawableId(
                    "house",
                    "house_tool_icon",
                    "house_install_icon",
                    "house_wall_tool_icon",
                    "house_icon"
            );
            if (icon != null) thumbRes = icon;
        }

        holder.imageView.setImageResource(thumbRes);

        // === 먹이만 64x64처럼 보이도록 패딩(8dp) 적용 ===
        if ("먹이".equals(item.category)) {
            int pad = dp(holder.imageView.getContext(), 8);
            holder.imageView.setPadding(pad, pad, pad, pad);
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            holder.imageView.setPadding(0, 0, 0, 0);
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        // 획득 여부 시각 처리
        if (!item.obtained) {
            holder.imageView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            holder.imageView.clearColorFilter();
        }

        // 이름/수량 표시 로직
        if ("먹이".equals(item.category)) {
            // 이름 숨김
            holder.itemName.setVisibility(View.GONE);
            // 수량 표시
            holder.itemCount.setVisibility(View.VISIBLE);
            holder.itemCount.setText("x" + item.count);
        } else {
            // 이름 표시
            holder.itemName.setVisibility(View.VISIBLE);
            String label = nameMap.get(item.name);
            if (label == null) {
                if ("배경".equals(item.category)) {
                    label = item.name;
                } else {
                    label = item.name;
                }
            }
            holder.itemName.setText(label);

            // 수량 숨김
            holder.itemCount.setVisibility(View.GONE);
        }

        // 클릭 이벤트
        holder.imageView.setOnClickListener(v -> {
            if (!item.obtained) {
                Toast.makeText(context, "아직 획득하지 않은 아이템입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(context)
                    .setTitle("아이템 적용")
                    .setMessage("이 아이템을 적용하시겠습니까?")
                    .setPositiveButton("네", (dialog, which) -> {
                        if ("배경".equals(item.category)) {
                            SharedPreferences loginPrefs = context.getSharedPreferences("login", Context.MODE_PRIVATE);
                            boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);
                            String userId = isLoggedIn ? loginPrefs.getString("id", null) : null;

                            String bgKey = userId != null ? "selectedBackground_" + userId : "selectedBackground_default";

                            SharedPreferences prefs = context.getSharedPreferences("SpritePrefs", Context.MODE_PRIVATE);
                            prefs.edit().putInt(bgKey, item.imageRes).apply();

                            Toast.makeText(context, "배경이 변경되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent iBg = new Intent(context, MainActivity.class);
                            iBg.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(iBg);

                        } else if ("fence".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyFenceTool", true);
                            intent.putExtra("fenceAtlasResId", item.imageRes);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);

                        } else if ("목장_구조물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "목장 설치 끝");
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);

                        } else if ("건축물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "집 벽 설치 끝");
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);

                        } else {
                            Toast.makeText(context, "적용되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("appliedItemImageRes", item.imageRes);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton("아니오", (dialog, which) ->
                            Toast.makeText(context, "취소되었습니다.", Toast.LENGTH_SHORT).show()
                    )
                    .show();
        });
    }

    /** 주어진 이름들 중 존재하는 drawable의 첫 번째 id 반환. 없으면 null */
    private Integer resolveDrawableId(String... names) {
        for (String n : names) {
            int id = context.getResources().getIdentifier(n, "drawable", context.getPackageName());
            if (id != 0) return id;
        }
        return null;
    }

    private static int dp(Context ctx, int value) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView itemCount;
        TextView itemName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemImage);
            itemCount = itemView.findViewById(R.id.itemCount);
            itemName = itemView.findViewById(R.id.itemName);
        }
    }

    private static Map<String, String> buildNameMap() {
        HashMap<String, String> m = new HashMap<>();

        // ── 농장(작물)
        m.put("wheat", "밀");
        m.put("potato", "감자");
        m.put("cauliflower", "콜리플라워");
        m.put("beet", "비트");
        m.put("egg_plant", "가지");
        m.put("cabbage", "양배추");
        m.put("corn", "옥수수");
        m.put("pumpkin", "호박");
        m.put("radish", "무");
        m.put("blueberry", "블루베리");
        m.put("starfruit", "스타후르츠");
        m.put("pea", "완두콩");
        m.put("red_mushroom", "빨간 버섯");
        m.put("red_spotted_mushroom", "빨간 얼룩 버섯");
        m.put("purple_mushroom", "보라 버섯");
        m.put("purple_spotted_mushroom", "보라 얼룩 버섯");

        // ── 농장(피크닉/구조물)
        m.put("boat", "보트");
        m.put("mailbox", "우체통");
        m.put("basket", "바구니");
        m.put("blanket", "돗자리");
        m.put("fence", "울타리");
        m.put("water_well", "우물");

        // ── 농장(장식물)
        m.put("lotus", "연꽃");
        m.put("lilac", "라일락");
        m.put("sunflower", "해바라기");
        m.put("blue_tulip", "파란 튤립");
        m.put("sky_blue_flower", "하늘색 꽃");
        m.put("blue_flower", "파란 꽃");
        m.put("beige_flower", "베이지 꽃");
        m.put("heart_flower", "하트꽃");
        m.put("small_bush", "작은 덤불");
        m.put("big_bush", "큰 덤불");
        m.put("long_wooden_path", "긴 나무길");
        m.put("wide_wooden_path", "넓은 나무길");
        m.put("small_stone_path", "작은 돌길");
        m.put("long_stone_path", "긴 돌길");
        m.put("wide_stone_path", "넓은 돌길");
        m.put("sign", "표지판");
        m.put("left_diagonal_sign", "표지판 (왼쪽)");
        m.put("right_diagonal_sign", "표지판 (오른쪽)");

        // ── 농장(채집)
        m.put("stone1", "돌멩이");
        m.put("stone2", "돌멩이");
        m.put("stone3", "돌멩이");
        m.put("stone4", "돌멩이");
        m.put("stone5", "돌멩이");
        m.put("stone6", "돌멩이");
        m.put("rock1", "바위");
        m.put("rock2", "바위");
        m.put("grass1", "풀");
        m.put("grass2", "풀");
        m.put("grass3", "풀");
        m.put("grass4", "풀");
        m.put("thin_tree", "얇은 나무");
        m.put("basic_tree", "평범한 나무");
        m.put("wide_tree", "넓은 나무");
        m.put("small_stump", "작은 그루터기");
        m.put("basic_stump", "평범한 그루터기");
        m.put("big_stump", "큰 그루터기");
        m.put("small_fallen_tree", "쓰러진 작은 나무");
        m.put("big_fallen_tree", "쓰러진 큰 나무");

        // ── 목장(가구)
        m.put("bed_light_green", "연두색 침대");
        m.put("bed_pink", "분홍색 침대");
        m.put("bed_skyblue", "하늘색 침대");
        m.put("carpet", "카펫");
        m.put("carpet_light_green", "연두색 카펫");
        m.put("carpet_pink", "분홍색 카펫");
        m.put("carpet_skyblue", "하늘색 카펫");
        m.put("chair_behind", "의자(뒤)");
        m.put("chair_front", "의자(앞)");
        m.put("chair_left", "의자(왼쪽)");
        m.put("chair_right", "의자(오른쪽)");
        m.put("clock", "시계");
        m.put("clock_edge", "엣지 시계");
        m.put("clock_bezel", "테두리 시계");
        m.put("frame_flower", "꽃 풍경 액자");
        m.put("frame_morning", "아침 풍경 액자");
        m.put("frame_night", "밤 풍경 액자");
        m.put("mood_light_light_green", "연두색 무드등");
        m.put("mood_light_pink", "분홍색 무드등");
        m.put("mood_light_skyblue", "하늘색 무드등");
        m.put("nightstand", "탁상");
        m.put("pot_blue_flower", "파란꽃 화분");
        m.put("pot_sprout", "새싹 화분");
        m.put("pot_sunflower", "해바라기 화분");
        m.put("table_big", "큰 탁자");
        m.put("table_small", "작은 탁자");
        m.put("chest", "상자");

        // ── 목장(구조물/집)
        m.put("chicken_house", "닭집");
        m.put("house_wall_tool", "집");

        // ── 목장(동물)
        m.put("chicken", "닭");
        m.put("cow", "소");

        // ── 목장(사육)
        m.put("straw", "볏짚");
        m.put("big_straw", "큰 볏짚");
        m.put("haystack", "건초더미");
        m.put("big_haystack", "큰 건초더미");
        m.put("basket_one", "사료 바구니");
        m.put("basket_two", "사료 바구니 (2)");
        m.put("water_tray", "물통");
        m.put("empty_water_tray", "빈 물통");

        // ── 배경
        m.put("tiles_grass", "초원 타일");
        m.put("tiles_soil", "사막 타일");
        m.put("tiles_stone", "암석 타일");

        // ── 먹이
        m.put("feed_item", "먹이");

        return m;
    }
}
