package com.tekwhizz.twsc;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ScreenRecordService extends Service {

    private static final String TAG = "ScreenRecordService";

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;
    private boolean isVideoHD;
    private boolean isAudioEnabled;
    private String path;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;
    private String name;
    private int audioBitrate;
    private int audioSamplingRate;
    private static String filePath;
    private static String fileName;
    private int audioSourceAsInt;
    private int videoEncoderAsInt;
    private boolean isCustomSettingsEnabled;
    private int videoFrameRate;
    private int videoBitrate;
    private int outputFormatAsInt;

    public final static String BUNDLED_LISTENER = "listener";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //Get intent extras
        byte[] notificationSmallIcon = intent.getByteArrayExtra("notificationSmallBitmap");
        String notificationTitle = intent.getStringExtra("notificationTitle");
        String notificationDescription = intent.getStringExtra("notificationDescription");
        String notificationButtonText = intent.getStringExtra("notificationButtonText");
        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        mScreenWidth = intent.getIntExtra("width", 0);
        mScreenHeight = intent.getIntExtra("height", 0);

        if (mScreenHeight == 0 || mScreenWidth == 0) {
            TWRecorderCodecInfo TWRecorderCodecInfo = new TWRecorderCodecInfo();
            TWRecorderCodecInfo.setContext(this);
            mScreenHeight = TWRecorderCodecInfo.getMaxSupportedHeight();
            mScreenWidth = TWRecorderCodecInfo.getMaxSupportedWidth();
        }

        mScreenDensity = intent.getIntExtra("density", 1);
        isVideoHD = intent.getBooleanExtra("quality", false);
        isAudioEnabled = intent.getBooleanExtra("audio", false);
        path = intent.getStringExtra("path");
        name = intent.getStringExtra("fileName");
        String audioSource = intent.getStringExtra("audioSource");
        String videoEncoder = intent.getStringExtra("videoEncoder");
        videoFrameRate = intent.getIntExtra("videoFrameRate", 30);
        videoBitrate = intent.getIntExtra("videoBitrate", 40000000);

        if (audioSource != null) {
            setAudioSourceAsInt(audioSource);
        }
        if (videoEncoder != null) {
            setvideoEncoderAsInt(videoEncoder);
        }

        filePath = name;
        audioBitrate = intent.getIntExtra("audioBitrate", 128000);
        audioSamplingRate = intent.getIntExtra("audioSamplingRate", 44100);
        String outputFormat = intent.getStringExtra("outputFormat");
        if (outputFormat != null) {
            setOutputformatAsInt(outputFormat);
        }

        isCustomSettingsEnabled = intent.getBooleanExtra("enableCustomSettings", false);

        //Set notification notification button text if developer did not
        if (notificationButtonText == null) {
            notificationButtonText = "STOP RECORDING";
        }
        //Set notification bitrate if developer did not
        if (audioBitrate == 0) {
            audioBitrate = 128000;
        }
        //Set notification sampling rate if developer did not
        if (audioSamplingRate == 0) {
            audioSamplingRate = 44100;
        }
        //Set notification title if developer did not
        if (notificationTitle == null || notificationTitle.equals("")) {
            notificationTitle = "Recording your screen";
        }
        //Set notification description if developer did not
        if (notificationDescription == null || notificationDescription.equals("")) {
            notificationDescription = "Drag down to stop the recording";
        }

        //Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "001";
            String channelName = "RecordChannel";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Notification notification;

                Intent myIntent = new Intent(this, NotificationReceiver.class);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, 0);

                Notification.Action action = new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.presence_video_online),
                        notificationButtonText,
                        pendingIntent).build();

                if (notificationSmallIcon != null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(notificationSmallIcon, 0, notificationSmallIcon.length);
                    //Modify notification badge
                    notification = new Notification.Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(Icon.createWithBitmap(bmp)).setContentTitle(notificationTitle).setContentText(notificationDescription).addAction(action).build();

                } else {
                    //Modify notification badge
                    notification = new Notification.Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(notificationTitle).setContentText(notificationDescription).addAction(action).build();
                }
                startForeground(101, notification);
            }
        } else {
            startForeground(101, new Notification());
        }


        if (path == null) {
            path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
        }

        //Init MediaRecorder
        try {
            initRecorder();
        } catch (Exception e) {
            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString("errorReason", Log.getStackTraceString(e));
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }

        //Init MediaProjection
        try {
            initMediaProjection();
        } catch (Exception e) {
            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString("errorReason", Log.getStackTraceString(e));
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }

        //Init VirtualDisplay
        try {
            initVirtualDisplay();
        } catch (Exception e) {
            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString("errorReason", Log.getStackTraceString(e));
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
                Bundle bundle = new Bundle();
                bundle.putString("error", "38");
                bundle.putString("errorReason", String.valueOf(i));
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle);
                }
            }
        });

        //Start Recording
        try {
            mMediaRecorder.start();
        } catch (Exception e) {
            // From the tests I've done, this can happen if another application is using the mic or if an unsupported video encoder was selected
            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString("error", "38");
            bundle.putString("errorReason", Log.getStackTraceString(e));
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }


        return Service.START_STICKY;
    }

    //Set output format as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setOutputformatAsInt(String outputFormat) {
        switch (outputFormat) {
            case "DEFAULT":
                outputFormatAsInt = 0;
                break;
            case "THREE_GPP":
                outputFormatAsInt = 1;
                break;
            case "MPEG_4":
                outputFormatAsInt = 2;
                break;
            case "AMR_NB":
                outputFormatAsInt = 3;
                break;
            case "AMR_WB":
                outputFormatAsInt = 4;
                break;
            case "AAC_ADTS":
                outputFormatAsInt = 6;
                break;
            case "MPEG_2_TS":
                outputFormatAsInt = 8;
                break;
            case "WEBM":
                outputFormatAsInt = 9;
                break;
            case "OGG":
                outputFormatAsInt = 11;
                break;
            default:
                outputFormatAsInt = 2;
        }
    }

    //Set video encoder as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setvideoEncoderAsInt(String encoder) {
        switch (encoder) {
            case "DEFAULT":
                videoEncoderAsInt = 0;
                break;
            case "H263":
                videoEncoderAsInt = 1;
                break;
            case "H264":
                videoEncoderAsInt = 2;
                break;
            case "MPEG_4_SP":
                videoEncoderAsInt = 3;
                break;
            case "VP8":
                videoEncoderAsInt = 4;
                break;
            case "HEVC":
                videoEncoderAsInt = 5;
                break;
        }
    }

    //Set audio source as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private void setAudioSourceAsInt(String audioSource) {
        switch (audioSource) {
            case "DEFAULT":
                audioSourceAsInt = 0;
                break;
            case "MIC":
                audioSourceAsInt = 1;
                break;
            case "VOICE_UPLINK":
                audioSourceAsInt = 2;
                break;
            case "VOICE_DOWNLINK":
                audioSourceAsInt = 3;
                break;
            case "VOICE_CALL":
                audioSourceAsInt = 4;
                break;
            case "CAMCODER":
                audioSourceAsInt = 5;
                break;
            case "VOICE_RECOGNITION":
                audioSourceAsInt = 6;
                break;
            case "VOICE_COMMUNICATION":
                audioSourceAsInt = 7;
                break;
            case "REMOTE_SUBMIX":
                audioSourceAsInt = 8;
                break;
            case "UNPROCESSED":
                audioSourceAsInt = 9;
                break;
            case "VOICE_PERFORMANCE":
                audioSourceAsInt = 10;
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaProjection() {
        mMediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(mResultCode, mResultData);
    }

    //Return the output file path as string
    public static String getFilePath() {
        return filePath;
    }

    //Return the name of the output file
    public static String getFileName() {
        return fileName;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initRecorder() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate).replace(" ", "");
        String videoQuality = "HD";
        if (!isVideoHD) {
            videoQuality = "SD";
        }
        if (name == null) {
            name = videoQuality + curTime;
        }

        filePath = path + "/" + name + ".mp4";

        fileName = name + ".mp4";

        mMediaRecorder = new MediaRecorder();


//        if (isAudioEnabled) {
//            mMediaRecorder.setAudioSource(audioSourceAsInt);
//        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(outputFormatAsInt);
//        if (isAudioEnabled) {
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            mMediaRecorder.setAudioEncodingBitRate(audioBitrate);
//            mMediaRecorder.setAudioSamplingRate(audioSamplingRate);
//        }

        mMediaRecorder.setVideoEncoder(videoEncoderAsInt);
        mMediaRecorder.setOutputFile(path + "/" + name + ".mp4");
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);

        if (!isCustomSettingsEnabled) {
            if (!isVideoHD) {
                mMediaRecorder.setVideoEncodingBitRate(12000000);
                mMediaRecorder.setVideoFrameRate(30);
            } else {
                mMediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
                mMediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
            }
        } else {
            mMediaRecorder.setVideoEncodingBitRate(videoBitrate);
            mMediaRecorder.setVideoFrameRate(videoFrameRate);
        }


        mMediaRecorder.prepare();

        //return mediaRecorder;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        resetAll();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resetAll() {
        stopForeground(true);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);

            mMediaProjection.stop();
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
