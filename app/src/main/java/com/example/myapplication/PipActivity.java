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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class PipActivity extends AppCompatActivity {
    
    // Less extreme aspect ratio (4:3) for better usability while still small
    private static final Rational BETTER_ASPECT_RATIO = new Rational(4, 3);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Request a transparent window with no decorations
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pip);
        
        // Make the window transparent
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Apply the most aggressive immersive mode possible
        applyAggressiveImmersiveMode();
        
        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Force landscape orientation for better PiP appearance
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Handle back button press to enter PiP mode
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                enterPipMode();
            }
        });
        
        // Auto-enter PiP mode after a short delay
        handler.postDelayed(this::enterPipMode, 300);
    }
    
    private void applyAggressiveImmersiveMode() {
        // Use all available window flags to hide UI
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );
        
        // Remove window decorations
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        
        // Make window transparent
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+, use the new WindowInsetsController
            Window window = getWindow();
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Hide all system bars
                controller.hide(
                        WindowInsets.Type.statusBars() | 
                        WindowInsets.Type.navigationBars() | 
                        WindowInsets.Type.systemBars() | 
                        WindowInsets.Type.displayCutout());
                
                // Set behavior to ensure they stay hidden
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // For older Android versions
            View decorView = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN;
            
            // Apply all flags to hide UI elements
            decorView.setSystemUiVisibility(flags);
        }
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode()) {
            return;
        }
        
        try {
            // Try to disable system controls using reflection before entering PiP
            tryDisableSystemControlsWithReflection();
            
            // Create a builder with better usability than before
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            
            // Use a more reasonable aspect ratio
            builder.setAspectRatio(BETTER_ASPECT_RATIO);
            
            // For Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Set controls timeout to zero - critical for hiding icons
//                builder.setHideControlsTimeout(0);
                
                // Use official APIs to disable controls
                builder.setAutoEnterEnabled(false)
                       .setSeamlessResizeEnabled(true);
                
                // Set a minimal source rect hint
                Rect sourceRect = new Rect(0, 0, 10, 10);
                builder.setSourceRectHint(sourceRect);
                
                // Try reflection methods - critical for hiding the icon
                Method[] methods = PictureInPictureParams.Builder.class.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.contains("ShowSystemControls") || 
                        methodName.contains("HideControls") ||
                        methodName.contains("PipControls")) {
                        
                        try {
                            method.setAccessible(true);
                            // Most methods need a boolean parameter, trying both true and false
                            if (method.getParameterTypes().length == 1 && 
                                method.getParameterTypes()[0] == boolean.class) {
                                
                                if (methodName.contains("Show") || methodName.contains("Enable")) {
                                    method.invoke(builder, false);  // For "show" methods, we want false
                                } else if (methodName.contains("Hide") || methodName.contains("Disable")) {
                                    method.invoke(builder, true);   // For "hide" methods, we want true
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                // Try direct field manipulation as well
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
                                    field.set(builder, false);  // For "show" fields, we want false
                                } else if (fieldName.contains("Hide") || fieldName.contains("Disable")) {
                                    field.set(builder, true);   // For "hide" fields, we want true
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } 
            // For Android 9-11
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Empty actions list
                builder.setActions(new ArrayList<>());
            }
            
            // Enter PiP mode
            enterPictureInPictureMode(builder.build());
            
            // Apply aggressive modifications right after entering PiP
            handler.postDelayed(() -> {
                if (isInPictureInPictureMode()) {
                    tryDisableSystemControlsWithReflection();
                    updatePipParams();
                }
            }, 100);
            
            // And again after a longer delay to ensure they stay hidden
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
//                    builder.setHideControlsTimeout(0);
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
            // Try to directly modify Window property
            Field mShouldShowSystemControls = getWindow().getClass()
                    .getDeclaredField("mShouldShowSystemControls");
            mShouldShowSystemControls.setAccessible(true);
            mShouldShowSystemControls.set(getWindow(), false);
        } catch (Exception ignored) {}
        
        try {
            // Try to directly modify Activity property
            Field mShowPictureInPictureMenu = getClass().getSuperclass()
                    .getDeclaredField("mShowPictureInPictureMenu");
            mShowPictureInPictureMenu.setAccessible(true);
            mShowPictureInPictureMenu.set(this, false);
        } catch (Exception ignored) {}
        
        // Try to find and manipulate any control-related fields in window
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
            // Re-apply all methods to hide UI
            applyAggressiveImmersiveMode();
            tryDisableSystemControlsWithReflection();
            
            // Update PiP params again
            updatePipParams();
            
            // Schedule one more attempt after a delay
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
        // Auto-enter PiP mode when user leaves
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
