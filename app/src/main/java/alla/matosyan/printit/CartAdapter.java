package alla.matosyan.printit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onBuyClick(int position);
        void onDeleteClick(int position);
    }

    public CartAdapter(List<CartItem> cartList, OnItemClickListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText(String.format("$%.2f", item.getPrice()));

        if (item.getImage() != null) {
            holder.ivImage.setImageBitmap(item.getImage());
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();

            Dialog previewDialog = new Dialog(context);
            previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            ImageView fullSizeImage = new ImageView(context);
            if (item.getImage() != null) {
                fullSizeImage.setImageBitmap(item.getImage());
            }
            fullSizeImage.setAdjustViewBounds(true);
            fullSizeImage.setPadding(60, 60, 60, 60);
            fullSizeImage.setBackgroundColor(Color.WHITE);

            previewDialog.setContentView(fullSizeImage);

            if (previewDialog.getWindow() != null) {
                previewDialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                previewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            previewDialog.show();

            if (listener != null) {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(currentPos);
                }
            }
        });

        holder.btnBuy.setOnClickListener(v -> {
            if (listener != null) {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    listener.onBuyClick(currentPos);
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(currentPos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice;
        ImageView ivImage;
        Button btnBuy;
        ImageView btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            ivImage = itemView.findViewById(R.id.ivCartItemImage);
            btnBuy = itemView.findViewById(R.id.btnBuy);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}