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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EmployeeDashboardActivity extends AppCompatActivity {

    private RecyclerView rvOrderFeed;
    private OrderAdapter adapter;
    private List<Order> pendingOrders;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        db = FirebaseFirestore.getInstance();
        pendingOrders = new ArrayList<>();
        rvOrderFeed = findViewById(R.id.rvOrderFeed);
        rvOrderFeed.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(pendingOrders);
        rvOrderFeed.setAdapter(adapter);

        fetchRealOrders();

        findViewById(R.id.btnViewProduction).setOnClickListener(v -> {
            startActivity(new Intent(EmployeeDashboardActivity.this, ApprovedOrdersActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(EmployeeDashboardActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void fetchRealOrders() {
        db.collection("Orders").whereEqualTo("status", "Pending").addSnapshotListener((value, error) -> {
            if (value != null) {
                pendingOrders.clear();
                int orderNumber = 0;

                for (QueryDocumentSnapshot doc : value) {
                    String docId = doc.getId();
                    String product = doc.getString("productName");
                    String email = doc.getString("customerEmail");
                    String date = doc.getString("orderDate");
                    String imageUrl = doc.getString("designPath");

                    String visualId = String.format("ORD-%06d", orderNumber);
                    orderNumber++;

                    pendingOrders.add(new Order(docId, visualId, product != null ? product : "Custom Order", email != null ? email : "Unknown", date != null ? date : "New", imageUrl));
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private static class Order {
        String documentId, visualId, product, email, time, imageUrl;
        Order(String docId, String vId, String prod, String em, String t, String img) {
            documentId = docId; visualId = vId; product = prod; email = em; time = t; imageUrl = img;
        }
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> orders;
        OrderAdapter(List<Order> orders) { this.orders = orders; }

        @NonNull @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OrderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.tvOrderId.setText(order.visualId);
            holder.tvProductType.setText(order.product);
            holder.tvCustomerEmail.setText(order.email);
            holder.tvOrderDate.setText(order.time);

            if (order.imageUrl != null && !order.imageUrl.isEmpty()) {
                try {
                    holder.ivDesignOverlay.setImageResource(android.R.color.transparent);
                    byte[] decodedString = android.util.Base64.decode(order.imageUrl, android.util.Base64.DEFAULT);
                    Glide.with(holder.itemView.getContext())
                            .asBitmap()
                            .load(decodedString)
                            .into(holder.ivProductImage);
                } catch (Exception e) {
                    holder.ivProductImage.setImageResource(android.R.color.transparent);
                }
            } else {
                holder.ivDesignOverlay.setImageResource(android.R.color.transparent);
                String productName = (order.product != null) ? order.product.toLowerCase() : "";

                if (productName.contains("pillow")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_pillow);
                } else if (productName.contains("mug")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_mug);
                } else if (productName.contains("bottle")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_bottle);
                } else if (productName.contains("phone")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_phone);
                } else if (productName.contains("poster")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_poster);
                } else if (productName.contains("shirt") || productName.contains("hoodie")) {
                    holder.ivProductImage.setImageResource(R.drawable.blank_tshirt);
                } else {
                    holder.ivProductImage.setImageResource(android.R.color.transparent);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(EmployeeDashboardActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", order.visualId);
                intent.putExtra("ORDER_PRODUCT", order.product);
                intent.putExtra("ORDER_EMAIL", order.email);
                intent.putExtra("DOC_ID", order.documentId);
                startActivity(intent);
            });

            holder.btnApprove.setOnClickListener(v -> {
                db.collection("Orders").document(order.documentId).update("status", "Approved")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Order Approved! Moving to production.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Network error. Could not approve.", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        @Override public int getItemCount() { return orders.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvProductType, tvCustomerEmail, tvOrderDate;
            Button btnApprove;
            ImageView ivProductImage, ivDesignOverlay;

            OrderViewHolder(@NonNull View itemView) {
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