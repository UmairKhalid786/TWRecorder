package com.tekwhizz.twsc;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import android.os.Handler;
import android.os.ResultReceiver;
import android.util.DisplayMetrics;
import android.util.Log;


import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TWRecorder implements MyListener {
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private final Context context;
    private int resultCode;
    private boolean isAudioEnabled = true;
    private boolean isVideoHDEnabled = true;
    private Activity activity;
    private String outputPath;
    private String fileName;
    private String notificationTitle;
    private String notificationDescription;
    private String notificationButtonText;
    private int audioBitrate = 0;
    private int audioSamplingRate = 0;
    private FileObserver observer;
    private final TWRecorderListener TWRecorderListener;
    private byte[] byteArray;
    private String audioSource = "MIC";
    private String videoEncoder = "DEFAULT";
    private boolean enableCustomSettings = false;
    private int videoFrameRate = 30;
    private int videoBitrate = 40000000;
    private String outputFormat = "DEFAULT";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TWRecorder(Context context, TWRecorderListener listener) {
        this.context = context.getApplicationContext();
        this.TWRecorderListener = listener;
        setScreenDensity();
    }

    /*Set output path*/
    public void setOutputPath(String path) {
        outputPath = path;
    }

    /*Set file name*/
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /*Set audio bitrate*/
    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;

    }

    /*Set audio sample rate*/
    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    /*Enable/Disable audio*/
    public void isAudioEnabled(boolean bool) {
        this.isAudioEnabled = bool;
    }

    /*Set Audio Source*/
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.AudioSource.html
    public void setAudioSource(String source){
        audioSource = source;

    }

    /*Enable/Disable HD recording*/
    public void recordHDVideo(boolean bool) {
        this.isVideoHDEnabled = bool;
    }

    /*Set Video Encoder*/
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.VideoEncoder.html
    public void setVideoEncoder(String encoder){
        videoEncoder = encoder;

    }

    //Enable Custom Settings
    public void enableCustomSettings(){
        enableCustomSettings = true;

    }

    //Set Video Frame Rate
    public void setVideoFrameRate(int fps){
        videoFrameRate = fps;
    }

    //Set Video BitRate
    public void setVideoBitrate(int bitrate){
        videoBitrate = bitrate;
    }

    //Set Output Format
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.OutputFormat.html
    public void setOutputFormat(String format){
        outputFormat = format;
    }

    // Set screen densityDpi
    private void setScreenDensity() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mScreenDensity = metrics.densityDpi;
    }

    //Set Custom Dimensions (NOTE - YOUR DEVICE MIGHT NOT SUPPORT THE SIZE YOU PASS IT)
    public void setScreenDimensions(int heightInPX, int widthInPX){
        mScreenHeight = heightInPX;
        mScreenWidth = widthInPX;
    }

    /*Get file path including file name and extension*/
    public String getFilePath() {
        return ScreenRecordService.getFilePath();
    }

    /*Get file name and extension*/
    public String getFileName() {
        return ScreenRecordService.getFileName();
    }

    /*Start screen recording*/
    public void startScreenRecording(Intent data, int resultCode, Activity activity) {
        this.resultCode = resultCode;
        this.activity = activity;
        startService(data);
    }

    /*Stop screen recording*/
    public void stopScreenRecording() {
        Intent service = new Intent(context, ScreenRecordService.class);
        context.stopService(service);
    }

    /*Check if recording is in progress*/
    public boolean isBusyRecording() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (ScreenRecordService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*Change notification icon*/
    public void setNotificationSmallIcon(int drawable) {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), drawable);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byteArray = stream.toByteArray();
    }

    /*Set notification title*/
    public void setNotificationTitle(String Title) {
        notificationTitle = Title;
    }

    /*Set notification description*/
    public void setNotificationDescription(String Description) {
        notificationDescription = Description;
    }

    public void setNotificationButtonText(String string){
        notificationButtonText = string;
    }

    /*Start recording service*/
    private void startService(Intent data) {
        try {
            if (outputPath!=null){
                File file = new File(outputPath);
                String parent = file.getParent();
                observer = new FileObserver(parent, activity, TWRecorder.this);
            }else {
                observer = new FileObserver(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)), activity, TWRecorder.this);
            }
            observer.startWatching();

            Intent service = new Intent(context, ScreenRecordService.class);
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            service.putExtra("audio", isAudioEnabled);
            service.putExtra("width", mScreenWidth);
            service.putExtra("height", mScreenHeight);
            service.putExtra("density", mScreenDensity);
            service.putExtra("quality", isVideoHDEnabled);
            service.putExtra("path", outputPath);
            service.putExtra("fileName", fileName);
            service.putExtra("audioBitrate", audioBitrate);
            service.putExtra("audioSamplingRate", audioSamplingRate);
            service.putExtra("notificationSmallBitmap", byteArray);
            service.putExtra("notificationTitle", notificationTitle);
            service.putExtra("notificationDescription", notificationDescription);
            service.putExtra("notificationButtonText", notificationButtonText);
            service.putExtra("enableCustomSettings", enableCustomSettings);
            service.putExtra("audioSource",audioSource);
            service.putExtra("videoEncoder", videoEncoder);

            service.putExtra("videoFrameRate", videoFrameRate);
            service.putExtra("videoBitrate", videoBitrate);
            service.putExtra("outputFormat", outputFormat);
            service.putExtra(ScreenRecordService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {
                        String result = resultData.getString("errorReason");
                        if (result != null) {
                            observer.stopWatching();
                            TWRecorderListener.TWRecorderOnError(100, result);
                            try {
                                Intent mservice = new Intent(context, ScreenRecordService.class);
                                context.stopService(mservice);
                            }catch (Exception e){

                            }

                        }
                    }
                }
            });


            context.startService(service);
        }catch (Exception e){
            TWRecorderListener.TWRecorderOnError(0, Log.getStackTraceString(e));
        }

    }

    /*Complete callback method*/
    @Override
    public void callback() {
        observer.stopWatching();
        TWRecorderListener.TWRecorderOnComplete();
    }
}