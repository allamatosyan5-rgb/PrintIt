package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        CardView cardTshirt = view.findViewById(R.id.card_tshirt);
        CardView cardMug = view.findViewById(R.id.card_mug);
        CardView cardBottle = view.findViewById(R.id.card_bottle);
        CardView cardPhone = view.findViewById(R.id.card_phone);
        CardView cardPillow = view.findViewById(R.id.card_pillow);
        CardView cardPoster = view.findViewById(R.id.card_poster);

        cardTshirt.setOnClickListener(v -> openDesignStudio("T-Shirt"));
        cardMug.setOnClickListener(v -> openDesignStudio("Mug"));
        cardBottle.setOnClickListener(v -> openDesignStudio("Water Bottle"));
        cardPhone.setOnClickListener(v -> openDesignStudio("Phone Case"));
        cardPillow.setOnClickListener(v -> openDesignStudio("Pillow"));
        cardPoster.setOnClickListener(v -> openDesignStudio("Poster"));

        return view;
    }

    private void openDesignStudio(String productType) {
        Intent intent = new Intent(getActivity(), DesignStudioActivity.class);
        intent.putExtra("PRODUCT_TYPE", productType);
        startActivity(intent);
    }
}