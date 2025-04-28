package com.example.myapplication;

import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.widget.VideoView;
import android.app.RemoteAction;
import android.graphics.drawable.Icon;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class PipActivity extends AppCompatActivity {

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pip);

        videoView = findViewById(R.id.videoView);

        // Set up the video using the existing sample video in raw directory
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.sam;
        videoView.setVideoPath(videoPath);
        videoView.start();
        
        // Handle back button press to enter PiP mode
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                enterPipMode();
            }
        });
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create PiP params with aspect ratio matching the video
            Rational aspectRatio = new Rational(videoView.getWidth(), videoView.getHeight());
            
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio);
            
            // For Android S (API 31) and above, we can disable the system controls
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(false)
                       .setSeamlessResizeEnabled(true);
            } 
            // For Android P (API 28) through Android R (API 30), set empty actions list
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setActions(new ArrayList<RemoteAction>());
            }
            
            enterPictureInPictureMode(builder.build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        
        // For Android versions before S (API 31), we also need to update PiP params when
        // the mode changes to ensure no controls are shown
        if (isInPictureInPictureMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setActions(new ArrayList<RemoteAction>());
                setPictureInPictureParams(builder.build());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Pause video when app is not visible
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Resume video when app becomes visible again
        videoView.start();
    }
} 