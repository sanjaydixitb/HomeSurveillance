package com.bsdsolutions.sanjaydixit.adbserverapp;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;


/**
 * Created by sanjaydixit on 15/10/15.
 */
public class AdbServerCameraActivity extends Activity implements SurfaceHolder.Callback {

    private static String TAG = "CameraServerLogs";
    private static final String CAMERA_SERVER_DATA_PATH = "CameraServerData";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;
    private Camera myCamera = null;
    private static final int DEFAULT_NUMBER_OF_CONSECUTIVE_CAPTURES = 1;
    private static int NUMBER_OF_CONSECUTIVE_CAPTURES = DEFAULT_NUMBER_OF_CONSECUTIVE_CAPTURES;
    private static final int CAPTURE_IMAGE = 1000;

    private Handler mHandler = null;

    private int captureCount = 0;

    TextView testView;

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        Intent intent = getIntent();
        NUMBER_OF_CONSECUTIVE_CAPTURES = intent.getIntExtra(AdbServerActivity.EXTRA_NUMBER_OF_CAPTURES,DEFAULT_NUMBER_OF_CONSECUTIVE_CAPTURES);

        initializeCamera();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case CAPTURE_IMAGE:
                        captureImage();
                        return true;
                }
                return false;
            }
        });

        start_camera();

        Message msg = mHandler.obtainMessage(CAPTURE_IMAGE);
        mHandler.sendMessageDelayed(msg, 500);

    }

    @Override
    protected void onDestroy() {
        stop_camera();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "BackPressed!");
        super.onBackPressed();
    }

    public void initializeCamera(){

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        rawCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d(TAG, "onPictureTaken - raw");
            }
        };

        /** Handles data for jpeg picture */
        shutterCallback = new ShutterCallback() {
            public void onShutter() {
                Log.i(TAG, "onShutter'd");
            }
        };
        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    int mYear = calendar.get(Calendar.YEAR);
                    int mMonth = calendar.get(Calendar.MONTH);
                    int mDay = calendar.get(Calendar.DAY_OF_MONTH);
                    int mHour = calendar.get(Calendar.HOUR_OF_DAY);
                    int mMinute = calendar.get(Calendar.MINUTE);
                    int mSecond = calendar.get(Calendar.SECOND);
                    int mMilliSecond = calendar.get(Calendar.MILLISECOND);
                    outStream = new FileOutputStream(String.format(
                            Environment.getExternalStorageDirectory()+"/"+CAMERA_SERVER_DATA_PATH+"/%d%d%d%d%d%d%d.jpg",mYear,mMonth,mDay,mHour,mMinute,mSecond,mMilliSecond ));
                    outStream.write(data);
                    outStream.close();
                    Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "FileNotFoundException: " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                }
                Log.d(TAG, "onPictureTaken - jpeg");
                captureCount++;
                if(captureCount < NUMBER_OF_CONSECUTIVE_CAPTURES) {
                    Message msg = mHandler.obtainMessage(CAPTURE_IMAGE);
                    mHandler.sendMessage(msg);
                } else {
                    finish();
                }
            }
        };

    }

    private void captureImage() {
        // TODO Auto-generated method stub
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private void start_camera()
    {
        captureCount = 0;
        try{
            camera = Camera.open();
        }catch(RuntimeException e){
            Log.e(TAG, "init_camera: " + e);
            return;
        }
        //TODO: Fix Orientation
        Camera.Parameters param;
        param = camera.getParameters();
        //modify parameter
        param.setJpegQuality(100);
        param.setPictureSize(2592, 1944);
        param.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        param.setAutoExposureLock(false);
        param.setAutoWhiteBalanceLock(false);
        param.setColorEffect("none");
        param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        param.setExposureCompensation(param.getMaxExposureCompensation());
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
//            camera.takePicture(shutter, raw, jpeg);
        } catch (Exception e) {
            Log.e(TAG, "init_camera: " + e);
            return;
        }
    }

    private void stop_camera()
    {
        camera.stopPreview();
        camera.release();
    }


    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

}
