package alla.matosyan.printit;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.storage.FirebaseStorage;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order currentOrder = orderList.get(position);

        holder.tvOrderId.setText("Order #" + currentOrder.getOrderId());
        holder.tvOrderStatus.setText(currentOrder.getStatus());
        holder.tvOrderAddress.setText("Shipping to: " + currentOrder.getAddress());

        if (currentOrder.getImageUrl() != null && !currentOrder.getImageUrl().isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(currentOrder.getImageUrl())
                    .getBytes(1024 * 1024 * 5)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        holder.ivOrderImage.setImageBitmap(bmp);
                        holder.ivOrderImage.setTag(bmp);
                    });
        }

        holder.itemView.setOnClickListener(v -> {
            Dialog previewDialog = new Dialog(holder.itemView.getContext());
            previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            ImageView fullSizeImage = new ImageView(holder.itemView.getContext());

            Bitmap savedBmp = (Bitmap) holder.ivOrderImage.getTag();
            if (savedBmp != null) {
                fullSizeImage.setImageBitmap(savedBmp);
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
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvOrderAddress;
        ImageView ivOrderImage;
        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderAddress = itemView.findViewById(R.id.tvOrderAddress);

            ivOrderImage = itemView.findViewById(R.id.ivOrderImage);
        }
    }
}