package alla.matosyan.printit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private OnItemDeleteListener deleteListener;
    private OnItemCheckoutListener checkoutListener;

    public interface OnItemDeleteListener {
        void onDeleteClick(int position, CartItem item);
    }

    public interface OnItemCheckoutListener void onCheckoutClick(CartItem item);
    }

    public CartAdapter(List<CartItem> cartItems, OnItemDeleteListener deleteListener, OnItemCheckoutListener checkoutListener) {
        this.cartItems = cartItems;
        this.deleteListener = deleteListener;
        this.checkoutListener = checkoutListener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.tvTextPreview.setText("Text: " + item.getText());
        holder.tvItemType.setText(item.getItemType());
        holder.tvItemPrice.setText(String.format("$%.2f", item.getPrice()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .into(holder.ivItemImage);
        } else {
            String type = item.getItemType();

            if (type != null) {
                switch (type) {
                    case "Mug":
                        holder.ivItemImage.setImageResource(R.drawable.blank_mug);
                        break;
                    case "Water Bottle":
                        holder.ivItemImage.setImageResource(R.drawable.blank_bottle);
                        break;
                    case "Phone Case":
                        holder.ivItemImage.setImageResource(R.drawable.blank_phone);
                        break;
                    case "Pillow":
                        holder.ivItemImage.setImageResource(R.drawable.blank_pillow);
                        break;
                    case "Poster":
                        holder.ivItemImage.setImageResource(R.drawable.blank_poster);
                        break;
                    default:
                        holder.ivItemImage.setImageResource(R.drawable.blank_tshirt);
                        break;
                }
            } else {
                holder.ivItemImage.setImageResource(R.drawable.blank_tshirt);
            }
        }
        // ---------------------------------------------------------------------

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(position, item);
            }
        });

        holder.btnCheckout.setOnClickListener(v -> {
            if (checkoutListener != null) {
                checkoutListener.onCheckoutClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemType, tvTextPreview, tvItemPrice;
        ImageButton btnDelete;
        Button btnCheckout;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.cart_item_image);
            tvItemType = itemView.findViewById(R.id.cart_item_type);
            tvTextPreview = itemView.findViewById(R.id.cart_item_text_preview);
            btnDelete = itemView.findViewById(R.id.btn_delete_item);
            tvItemPrice = itemView.findViewById(R.id.cart_item_price);
            btnCheckout = itemView.findViewById(R.id.btn_checkout_item);
        }
    }
}