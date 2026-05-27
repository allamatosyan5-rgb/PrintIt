package alla.matosyan.printit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CartFragment extends Fragment {

    private RecyclerView rvCartItems;
    private TextView tvTotalPrice;
    private Button btnContinueShopping;
    private View bottomCheckoutBar, layoutEmptyCart;

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    CartManager.loadCartFromFirebase(() -> updateCartUI());
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        rvCartItems = view.findViewById(R.id.rvCartItems);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        bottomCheckoutBar = view.findViewById(R.id.bottomCheckoutBar);
        layoutEmptyCart = view.findViewById(R.id.layoutEmptyCart);
        btnContinueShopping = view.findViewById(R.id.btnContinueShopping);

        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));

        if (CartManager.cartList.isEmpty()) {
            tvTotalPrice.setText("Syncing...");
            bottomCheckoutBar.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.GONE);

            CartManager.loadCartFromFirebase(() -> updateCartUI());
        } else {
            updateCartUI();
        }

        btnContinueShopping.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        });

        return view;
    }

    private void updateCartUI() {
        if (!isAdded()) return;

        if (CartManager.cartList.isEmpty()) {
            rvCartItems.setVisibility(View.GONE);
            bottomCheckoutBar.setVisibility(View.GONE);
            layoutEmptyCart.setVisibility(View.VISIBLE);
        } else {
            rvCartItems.setVisibility(View.VISIBLE);
            bottomCheckoutBar.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);

            double total = 0.0;
            for (CartItem item : CartManager.cartList) {
                total += item.getPrice();
            }
            tvTotalPrice.setText(String.format("$%.2f", total));

            CartAdapter adapter = new CartAdapter(CartManager.cartList, new CartAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                }

                @Override
                public void onBuyClick(int position) {
                    Intent intent = new Intent(getContext(), CheckOutActivity.class);
                    intent.putExtra("TARGET_ITEM_INDEX", position);
                    checkoutLauncher.launch(intent);
                }

                @Override
                public void onDeleteClick(int position) {
                    String cloudId = CartManager.cartList.get(position).getId();

                    CartManager.cartList.remove(position);
                    updateCartUI();

                    if (cloudId != null) {
                        CartManager.deleteItemFromFirebase(cloudId);
                    }
                }
            });
            rvCartItems.setAdapter(adapter);
        }
    }
}