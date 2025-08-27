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
        if ("울타리".equals(item.category) && "fence_tool".equals(item.name)) {
            Integer icon = resolveDrawableId("fence_tool_icon", "fence", "fence_icon");
            if (icon != null) thumbRes = icon;
        }

        // 집 설치 툴(목장/집 공통) → house.png 로 보이게
        if ("house_wall_tool".equals(item.name)
                && ("목장_구조물".equals(item.category) || "건축물".equals(item.category))) {
            // 가장 먼저 "house" 를 찾고, 없을 때만 예비 이름들로 대체
            Integer icon = resolveDrawableId(
                    "house",                // <-- 여기 파일만 두면 이게 바로 쓰여요
                    "house_tool_icon",
                    "house_install_icon",
                    "house_wall_tool_icon",
                    "house_icon"
            );
            if (icon != null) thumbRes = icon;
        }

        holder.imageView.setImageResource(thumbRes);

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
                            context.startActivity(new Intent(context, MainActivity.class));

                        } else if ("울타리".equals(item.category) && "fence_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyFenceTool", true);
                            intent.putExtra("fenceAtlasResId", item.imageRes);
                            context.startActivity(intent);

                        } else if ("목장_구조물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "목장 설치 끝");
                            context.startActivity(intent);

                        } else if ("건축물".equals(item.category) && "house_wall_tool".equals(item.name)) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("applyHouseTool", true);
                            intent.putExtra("houseAtlasResId", item.imageRes);
                            intent.putExtra("toolOkText", "집 벽 설치 끝");
                            context.startActivity(intent);

                        } else {
                            Toast.makeText(context, "적용되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("appliedItemImageRes", item.imageRes);
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
