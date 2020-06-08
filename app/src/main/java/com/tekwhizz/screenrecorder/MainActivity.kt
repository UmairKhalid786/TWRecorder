package com.tekwhizz.screenrecorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tekwhizz.twsc.TWRecorder
import com.tekwhizz.twsc.TWRecorderListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() , TWRecorderListener {

    private val SCREEN_RECORD_REQUEST_CODE: Int = 0;
    var twRecorder: TWRecorder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionBtn.setOnClickListener {
            if (actionBtn.text.toString().equals("start" , ignoreCase = true)){
                startRecordingScreen()
            }else{
                twRecorder?.stopScreenRecording()
                actionBtn.text = "start"
            }
        }

        //Init TWRecorder
        //Init TWRecorder
        twRecorder = TWRecorder(this, this)
        twRecorder?.enableCustomSettings()

        twRecorder?.setVideoFrameRate(24);
//The bitrate is also dependent on the device and the frame rate that is set
        twRecorder?.setVideoBitrate(32);
    }

    override fun TWRecorderOnError(errorCode: Int, reason: String?) {
        Log.e("Error" , errorCode.toString() + reason)
    }

    override fun TWRecorderOnComplete() {
        Log.e("FilePath" , twRecorder?.filePath)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(twRecorder?.filePath))
        intent.setDataAndType(Uri.parse(twRecorder?.filePath), "video/mp4")
        startActivity(intent)
    }


    private fun startRecordingScreen() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent =
            mediaProjectionManager?.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) { //Start screen recording
                twRecorder!!.startScreenRecording(data, resultCode, this)
                actionBtn.text = "stop"
            }
        }
    }
}
