package kr.ac.hs.farm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
    private Context context;
    private ItemClickListener itemClickListener;

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
        holder.imageView.setImageResource(item.imageRes);

        if (!item.obtained) {
            holder.imageView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            holder.imageView.clearColorFilter();
        }

        if (item.category.equals("먹이")) {
            holder.itemCount.setVisibility(View.VISIBLE);
            holder.itemCount.setText("x" + item.count);
        } else {
            holder.itemCount.setVisibility(View.GONE);
        }

        holder.imageView.setOnClickListener(v -> {
            if (item.category.equals("농장") || item.category.equals("울타리") || item.category.equals("가구") || item.category.equals("작물")) {
                if (!item.obtained) {
                    Toast.makeText(context, "아직 획득하지 않은 아이템입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(context)
                        .setTitle("아이템 적용")
                        .setMessage("이 아이템을 적용하시겠습니까?")
                        .setPositiveButton("네", (dialog, which) -> {
                            Toast.makeText(context, "적용되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("appliedItemImageRes", item.imageRes);
                            context.startActivity(intent);
                        })
                        .setNegativeButton("아니오", (dialog, which) -> {
                            Toast.makeText(context, "취소되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        });
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
