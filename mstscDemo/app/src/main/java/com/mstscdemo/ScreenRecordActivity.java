/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mstscdemo;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mstsc.MstscClient;
import com.mstsc.inf.RtmpListener;
import com.mstsc.utils.constant.Constants;


import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.task.RtmpStreamingSender;
import net.yrom.screenrecorder.task.ScreenRecorder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author hzy
 * @time 2019.3-2019.6
 */
public class ScreenRecordActivity extends Activity implements View.OnClickListener,RtmpListener {


    private static final int REQUEST_CODE = 1;

    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecorder mVideoRecorder;
    //private RESAudioClient audioClient;
    private RtmpStreamingSender streamingSender;
    private ExecutorService executorService;

    private boolean isRecording;
    private RESCoreParameters coreParameters;
    private String rtmpAddr = Constants.RTMP_PORT;
    private String deviceId = "DEMO8";


    private Button  startControl;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_remote_control);

        initSDK(deviceId);
        initView();
        initFunction();
    }

    private void initSDK(String deviceId)
    {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        createScreenCapture();

        //load socket control
        MstscClient mstscClient = new MstscClient();
        mstscClient.init(deviceId,this);

    }



    private void initView()
    {
        startControl = findViewById(R.id.remote_start_control);
    }

    private void initFunction()
    {
        startControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }
        rtmpAddr = rtmpAddr+deviceId;
        if (TextUtils.isEmpty(rtmpAddr)) {
            Toast.makeText(this, "rtmp address cannot be null", Toast.LENGTH_SHORT).show();
            return;
        }
        streamingSender = new RtmpStreamingSender();
        streamingSender.sendStart(rtmpAddr);
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                streamingSender.sendFood(flvData, type);
            }
        };
        coreParameters = new RESCoreParameters();


        mVideoRecorder = new ScreenRecorder(collecter, RESFlvData.VIDEO_WIDTH, RESFlvData.VIDEO_HEIGHT, RESFlvData.VIDEO_BITRATE, 1, mediaProjection);
        mVideoRecorder.start();


        executorService = Executors.newCachedThreadPool();
        executorService.execute(streamingSender);

        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        if (mVideoRecorder != null) {
            stopScreenRecord();
        } else {
            createScreenCapture();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* if (mVideoRecorder != null) {
            stopScreenRecord();
        }*/
    }


    private void createScreenCapture() {
        isRecording = true;
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    private void stopScreenRecord() {
        mVideoRecorder.quit();
        mVideoRecorder = null;
        if (streamingSender != null) {
            streamingSender.sendStop();
            streamingSender.quit();
            streamingSender = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }


    /**
     * 关闭录屏
     */
    @Override
    public void stopPush() {

       Log.e("关闭录屏","XXXXXXXXXXXXXXXXXXXXXXX");
        finish();

        if (mVideoRecorder != null) {
            stopScreenRecord();
        }
    }
}
