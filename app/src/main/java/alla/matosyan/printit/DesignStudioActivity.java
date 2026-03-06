package alla.matosyan.printit;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DesignStudioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design_studio);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvProductTitle = findViewById(R.id.tv_product_title);

        String productName = getIntent().getStringExtra("PRODUCT_TYPE");
        if (productName != null) {
            tvProductTitle.setText("Design Your " + productName);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}