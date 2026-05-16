package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ApprovedOrdersActivity extends AppCompatActivity {

    private RecyclerView rvApprovedFeed;
    private ApprovedAdapter adapter;
    private List<Order> approvedOrders;
    private FirebaseFirestore db;
    private TextView tvEmptyOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approved_orders);

        db = FirebaseFirestore.getInstance();
        approvedOrders = new ArrayList<>();
        rvApprovedFeed = findViewById(R.id.rvApprovedFeed);
        rvApprovedFeed.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovedAdapter(approvedOrders);
        rvApprovedFeed.setAdapter(adapter);

        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fetchApprovedOrders();
    }

    private void fetchApprovedOrders() {
        db.collection("Orders").whereEqualTo("status", "Approved").addSnapshotListener((value, error) -> {
            if (value != null) {
                approvedOrders.clear();
                for (QueryDocumentSnapshot doc : value) {
                    String docId = doc.getId();
                    String product = doc.getString("productName");
                    String email = doc.getString("customerEmail");
                    String date = doc.getString("orderDate");
                    String imageUrl = doc.getString("designPath");
                    String visualId = "PRD-" + docId.substring(0, 5).toUpperCase();

                    approvedOrders.add(new Order(docId, visualId, product != null ? product : "Custom Order", email != null ? email : "Unknown", date != null ? date : "New", imageUrl));
                }
                adapter.notifyDataSetChanged();

                if (tvEmptyOrders != null) {
                    if (approvedOrders.isEmpty()) {
                        rvApprovedFeed.setVisibility(View.GONE);
                        tvEmptyOrders.setVisibility(View.VISIBLE);
                    } else {
                        rvApprovedFeed.setVisibility(View.VISIBLE);
                        tvEmptyOrders.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private static class Order {
        String documentId, visualId, product, email, time, imageUrl;
        Order(String docId, String vId, String prod, String em, String t, String img) {
            documentId = docId; visualId = vId; product = prod; email = em; time = t; imageUrl = img;
        }
    }

    private class ApprovedAdapter extends RecyclerView.Adapter<ApprovedAdapter.ApprovedViewHolder> {
        private List<Order> orders;
        ApprovedAdapter(List<Order> orders) { this.orders = orders; }

        @NonNull @Override
        public ApprovedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ApprovedViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ApprovedViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.tvOrderId.setText(order.visualId);
            holder.tvProductType.setText(order.product);
            holder.tvCustomerEmail.setText(order.email);
            holder.tvOrderDate.setText(order.time);

            if (order.imageUrl != null && !order.imageUrl.isEmpty()) {
                try {
                    holder.ivDesignOverlay.setImageResource(android.R.color.transparent);
                    byte[] decodedString = android.util.Base64.decode(order.imageUrl, android.util.Base64.DEFAULT);
                    Glide.with(holder.itemView.getContext()).asBitmap().load(decodedString).into(holder.ivProductImage);
                } catch (Exception e) {
                    holder.ivProductImage.setImageResource(android.R.color.transparent);
                }
            } else {
                holder.ivDesignOverlay.setImageResource(android.R.color.transparent);
                holder.ivProductImage.setImageResource(R.drawable.blank_tshirt);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ApprovedOrdersActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", order.visualId);
                intent.putExtra("ORDER_PRODUCT", order.product);
                intent.putExtra("ORDER_EMAIL", order.email);
                intent.putExtra("DOC_ID", order.documentId);
                startActivity(intent);
            });

            holder.btnApprove.setText("READY FOR CARRIER");
            holder.btnApprove.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));

            holder.btnApprove.setOnClickListener(v -> {
                db.collection("Orders").document(order.documentId).update("status", "Ready for Carrier")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), "Order ready for external carrier!", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        @Override public int getItemCount() { return orders.size(); }

        class ApprovedViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvProductType, tvCustomerEmail, tvOrderDate;
            Button btnApprove;
            ImageView ivProductImage, ivDesignOverlay;

            ApprovedViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvProductType = itemView.findViewById(R.id.tvProductType);
                tvCustomerEmail = itemView.findViewById(R.id.tvCustomerEmail);
                tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                ivProductImage = itemView.findViewById(R.id.ivProductImage);
                ivDesignOverlay = itemView.findViewById(R.id.ivDesignOverlay);
            }
        }
    }
}