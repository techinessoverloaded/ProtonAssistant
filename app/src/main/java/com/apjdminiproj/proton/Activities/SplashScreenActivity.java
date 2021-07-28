package com.apjdminiproj.proton.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.apjdminiproj.proton.R;

public class SplashScreenActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private int progress;
    private Intent intent;
    private Handler handler;
    private Animation bounceAnimation;
    private ImageView logoView;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)
        {
            WindowInsetsController controller = getWindow().getInsetsController();
            if(controller!=null)
            {
                controller.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else
            {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_splash_screen);
        progressBar = findViewById(R.id.progressBar);
        logoView = findViewById(R.id.logoSplash);
        bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.logo_bounce_anim);
        progress = 0;
        handler = new Handler(getMainLooper());
        intent = new Intent(SplashScreenActivity.this, MainActivity.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        logoView.startAnimation(bounceAnimation);
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    while(progress<100)
                    {
                        progress+=1;
                        handler.post(new Runnable() {
                            @Override
                            public void run()
                            {
                                progressBar.setProgress(progress,true);
                            }
                        });
                        try
                        {
                            Thread.sleep(30);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        if(progress==100)
                        {
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            }).start();
    }
}