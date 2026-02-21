package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView introImage;
    private TextView introText;
    private int step = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        introImage = findViewById(R.id.introImage);
        introText = findViewById(R.id.introText);

        runAnimationSequence();
    }

    private void runAnimationSequence() {
        updateContent(R.drawable.photo1, "Premium Custom Apparel");

        new Handler().postDelayed(() -> {
            updateContent(R.drawable.photo2, "Branded Merchandise");
        }, 2000);

        new Handler().postDelayed(() -> {
            updateContent(R.drawable.photo3, "Industrial Precision");
        }, 4000);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 6500);
    }

    private void updateContent(int imageRes, String description) {
        AlphaAnimation fadeIn = new AlphaAnimation(0.2f, 1.0f);
        fadeIn.setDuration(800);

        introImage.setImageResource(imageRes);
        introText.setText(description);

        introImage.startAnimation(fadeIn);
        introText.startAnimation(fadeIn);
    }
}