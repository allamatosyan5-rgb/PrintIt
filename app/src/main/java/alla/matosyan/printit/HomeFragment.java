package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private TextView tvUserRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvUserRole = view.findViewById(R.id.tvUserRole);
        recyclerView = view.findViewById(R.id.rvProductGrid);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        checkUserRole();
        setupProductList();

        adapter = new ProductAdapter(productList, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(getActivity(), DesignStudioActivity.class);

                intent.putExtra("PRODUCT_TYPE", product.getName());

                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    private void checkUserRole() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            if (currentUser.isAnonymous()) {
                tvUserRole.setText("Logged in as Guest Customer");
            } else {
                tvUserRole.setText("Logged in as Customer");
            }

        } else {
            tvUserRole.setText("Not logged in");
        }
    }

    private void setupProductList() {
        productList = new ArrayList<>();
        productList.add(new Product("T-Shirt", R.drawable.blank_tshirt));
        productList.add(new Product("Mug", R.drawable.blank_mug));
        productList.add(new Product("Water Bottle", R.drawable.blank_bottle));
        productList.add(new Product("Phone Case", R.drawable.blank_phone));
        productList.add(new Product("Pillow", R.drawable.blank_pillow));
        productList.add(new Product("Poster", R.drawable.blank_poster));
    }
}