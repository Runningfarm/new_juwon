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

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<Item> itemList;
    private final Context context;
    private final ItemClickListener itemClickListener;

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
            int pad = dp(holder.imageView.getContext(), 8); // 80dp 셀에서 양쪽 8dp → 64dp 표시영역
            holder.imageView.setPadding(pad, pad, pad, pad);
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            // 다른 아이템은 기존처럼 꽉 차게
            holder.imageView.setPadding(0, 0, 0, 0);
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        // =============================================

        if (!item.obtained) {
            holder.imageView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            holder.imageView.clearColorFilter();
        }

        if ("먹이".equals(item.category)) {
            holder.itemCount.setVisibility(View.VISIBLE);
            holder.itemCount.setText("x" + item.count);
        } else {
            holder.itemCount.setVisibility(View.GONE);
        }

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
                            iBg.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // ★ 추가
                            context.startActivity(iBg);

                        } else if ("fence".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyFenceTool", true);
                            intent.putExtra("fenceAtlasResId", item.imageRes); // ← 아틀라스(fences.png)
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);   // ★ 추가
                            context.startActivity(intent);


                        } else if ("목장_구조물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "목장 설치 끝");
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // ★ 추가
                            context.startActivity(intent);

                        } else if ("건축물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "집 벽 설치 끝");
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // ★ 추가
                            context.startActivity(intent);

                        } else {
                            Toast.makeText(context, "적용되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("appliedItemImageRes", item.imageRes);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // ★ 추가
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemImage);
            itemCount = itemView.findViewById(R.id.itemCount);
        }
    }
}
