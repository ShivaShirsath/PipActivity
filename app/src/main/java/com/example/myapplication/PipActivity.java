package com.example.myapplication;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class PipActivity extends AppCompatActivity {
    
    private static final Rational BETTER_ASPECT_RATIO = new Rational(4, 3);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pip);
        
        TextView helloText = findViewById(R.id.helloText);
        
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        applyAggressiveImmersiveMode();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                enterPipMode();
            }
        });
        
        handler.postDelayed(this::enterPipMode, 300);
    }
    
    private void applyAggressiveImmersiveMode() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars() | 
                        WindowInsets.Type.navigationBars() | 
                        WindowInsets.Type.systemBars() | 
                        WindowInsets.Type.displayCutout());
                
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN;
            
            decorView.setSystemUiVisibility(flags);
        }
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode()) {
            return;
        }
        
        try {
            tryDisableSystemControlsWithReflection();
            
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            
            builder.setAspectRatio(BETTER_ASPECT_RATIO);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                builder.setAutoEnterEnabled(false)
                       .setSeamlessResizeEnabled(true);
                
                Rect sourceRect = new Rect(0, 0, 10, 10);
                builder.setSourceRectHint(sourceRect);
                
                Method[] methods = PictureInPictureParams.Builder.class.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.contains("ShowSystemControls") || 
                        methodName.contains("HideControls") ||
                        methodName.contains("PipControls")) {
                        
                        try {
                            method.setAccessible(true);
                            if (method.getParameterTypes().length == 1 && 
                                method.getParameterTypes()[0] == boolean.class) {
                                
                                if (methodName.contains("Show") || methodName.contains("Enable")) {
                                    method.invoke(builder, false);  
                                } else if (methodName.contains("Hide") || methodName.contains("Disable")) {
                                    method.invoke(builder, true);   
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                Field[] fields = PictureInPictureParams.Builder.class.getDeclaredFields();
                for (Field field : fields) {
                    String fieldName = field.getName();
                    if (fieldName.contains("SystemControls") || 
                        fieldName.contains("Controls") ||
                        fieldName.contains("Menu")) {
                        
                        try {
                            field.setAccessible(true);
                            if (field.getType() == boolean.class) {
                                if (fieldName.contains("Show") || fieldName.contains("Enable")) {
                                    field.set(builder, false);  
                                } else if (fieldName.contains("Hide") || fieldName.contains("Disable")) {
                                    field.set(builder, true);   
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } 
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setActions(new ArrayList<>());
            }
            
            enterPictureInPictureMode(builder.build());
            
            handler.postDelayed(() -> {
                if (isInPictureInPictureMode()) {
                    tryDisableSystemControlsWithReflection();
                    updatePipParams();
                }
            }, 100);
            
            handler.postDelayed(() -> {
                if (isInPictureInPictureMode()) {
                    tryDisableSystemControlsWithReflection();
                    updatePipParams();
                }
            }, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updatePipParams() {
        if (!isInPictureInPictureMode()) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                builder.setAspectRatio(BETTER_ASPECT_RATIO);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    builder.setActions(new ArrayList<>());
                }
                
                setPictureInPictureParams(builder.build());
            }
        } catch (Exception ignored) {}
    }
    
    private void tryDisableSystemControlsWithReflection() {
        try {
            Field mShouldShowSystemControls = getWindow().getClass()
                    .getDeclaredField("mShouldShowSystemControls");
            mShouldShowSystemControls.setAccessible(true);
            mShouldShowSystemControls.set(getWindow(), false);
        } catch (Exception ignored) {}
        
        try {
            Field mShowPictureInPictureMenu = getClass().getSuperclass()
                    .getDeclaredField("mShowPictureInPictureMenu");
            mShowPictureInPictureMenu.setAccessible(true);
            mShowPictureInPictureMenu.set(this, false);
        } catch (Exception ignored) {}
        
        Field[] windowFields = getWindow().getClass().getDeclaredFields();
        for (Field field : windowFields) {
            String fieldName = field.getName();
            if (fieldName.contains("Control") || 
                fieldName.contains("Menu") || 
                fieldName.contains("Show")) {
                
                try {
                    field.setAccessible(true);
                    if (field.getType() == boolean.class) {
                        field.set(getWindow(), false);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        
        if (isInPictureInPictureMode) {
            applyAggressiveImmersiveMode();
            tryDisableSystemControlsWithReflection();
            
            updatePipParams();
            
            handler.postDelayed(() -> {
                if (isInPictureInPictureMode()) {
                    tryDisableSystemControlsWithReflection();
                    updatePipParams();
                }
            }, 300);
        }
    }
    
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode()) {
            enterPipMode();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        applyAggressiveImmersiveMode();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
} 
