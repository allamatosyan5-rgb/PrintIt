package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItemList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_cart);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        cartItemList = new ArrayList<>();

        adapter = new CartAdapter(cartItemList,


                new CartAdapter.OnItemDeleteListener() {
                    @Override
                    public void onDeleteClick(int position, CartItem item) {
                        deleteItemFromCart(position, item);
                    }
                },

                new CartAdapter.OnItemCheckoutListener() {
                    @Override
                    public void onCheckoutClick(CartItem item) {

                        Intent intent = new Intent(requireContext(), CheckoutActivity.class);
                        intent.putExtra("CART_ITEM_ID", item.getDocumentId());
                        startActivity(intent);
                    }
                }
        );

        recyclerView.setAdapter(adapter);

        loadCartItems();

        return view;
    }

    private void loadCartItems() {
        db.collection("cart_items").get().addOnSuccessListener(queryDocumentSnapshots -> {
            cartItemList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                CartItem item = document.toObject(CartItem.class);

                item.setDocumentId(document.getId());

                cartItemList.add(item);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load cart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteItemFromCart(int position, CartItem item) {

        db.collection("cart_items").document(item.getDocumentId()).delete()
                .addOnSuccessListener(aVoid -> {
                    cartItemList.remove(position);
                    adapter.notifyItemRemoved(position);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}