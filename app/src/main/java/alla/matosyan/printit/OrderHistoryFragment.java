package alla.matosyan.printit;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryFragment extends Fragment {

    private RecyclerView rvMyOrders;
    private CustomerOrderAdapter adapter;
    private List<CustomerOrder> myOrdersList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_history, container, false);

        rvMyOrders = view.findViewById(R.id.rvMyOrders);
        rvMyOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        myOrdersList = new ArrayList<>();
        adapter = new CustomerOrderAdapter(myOrdersList);
        rvMyOrders.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchMyOrders();

        return view;
    }

    private void fetchMyOrders() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String mySecretId = currentUser.getUid();

        db.collection("Orders")
                .whereEqualTo("customerId", mySecretId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if(getContext() != null) Toast.makeText(getContext(), "Failed to load orders.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        myOrdersList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            String orderId = doc.getId();

                            String product = doc.getString("name");
                            String status = doc.getString("status");
                            String date = doc.getString("orderDate");
                            String imageUrl = doc.getString("imageUrl");

                            String price = "";
                            if (doc.get("price") != null) {
                                price = String.valueOf(doc.get("price"));
                            }

                            myOrdersList.add(new CustomerOrder(
                                    orderId,
                                    product != null ? product : "Unknown Item",
                                    status != null ? status : "Processing",
                                    date != null ? date : "Recently",
                                    "$" + price,
                                    imageUrl
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class CustomerOrder {
        String orderId, product, status, date, price, imageUrl;
        CustomerOrder(String orderId, String product, String status, String date, String price, String imageUrl) {
            this.orderId = orderId;
            this.product = product;
            this.status = status;
            this.date = date;
            this.price = price;
            this.imageUrl = imageUrl;
        }
    }

    private class CustomerOrderAdapter extends RecyclerView.Adapter<CustomerOrderAdapter.OrderViewHolder> {
        private List<CustomerOrder> orders;

        CustomerOrderAdapter(List<CustomerOrder> orders) { this.orders = orders; }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            CustomerOrder order = orders.get(position);
            holder.tvProductName.setText(order.product);
            holder.tvDate.setText(order.date);
            holder.tvPrice.setText(order.price);

            if (order.imageUrl != null && !order.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(order.imageUrl)
                        .placeholder(new ColorDrawable(Color.LTGRAY))
                        .into(holder.ivProductImage);
            } else {
                holder.ivProductImage.setImageDrawable(new ColorDrawable(Color.LTGRAY));
            }



            if ("Completed".equalsIgnoreCase(order.status)) {
                holder.tvStatus.setText("Completed, check e-mail for shipping details.");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else if ("Pending".equalsIgnoreCase(order.status)) {
                holder.tvStatus.setText("Status: Pending");
                holder.tvStatus.setTextColor(Color.parseColor("#1976D2"));
            } else {
                holder.tvStatus.setText("Status: " + order.status);
                holder.tvStatus.setTextColor(Color.DKGRAY);
            }

            holder.itemView.setOnClickListener(v -> {
                Dialog previewDialog = new Dialog(holder.itemView.getContext());
                previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                ImageView fullSizeImage = new ImageView(holder.itemView.getContext());

                if (order.imageUrl != null && !order.imageUrl.isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(order.imageUrl)
                            .placeholder(new ColorDrawable(Color.LTGRAY))
                            .into(fullSizeImage);
                } else {
                    fullSizeImage.setImageDrawable(new ColorDrawable(Color.LTGRAY));
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
        public int getItemCount() { return orders.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvStatus, tvDate, tvPrice;
            ImageView ivProductImage;
            RatingBar rbOrderRating;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvHistoryProductName);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvPrice = itemView.findViewById(R.id.tvHistoryPrice);
                ivProductImage = itemView.findViewById(R.id.ivHistoryProductImage);
                rbOrderRating = itemView.findViewById(R.id.rbOrderRating);
            }
        }
    }
}