package com.bsdsolutions.sanjaydixit.adbserverapp;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;


/**
 * Created by sanjaydixit on 15/10/15.
 */
public class AdbServerCameraActivity extends Activity implements SurfaceHolder.Callback {

    private static String TAG = "CameraServerLogs";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;
    private Camera myCamera = null;

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

        captureImageFromHardware();

        start_camera();
        try {
            Thread.sleep(1000,0);
        }
        catch (InterruptedException e ) {
            Log.e(TAG,"Exception : " + e.getMessage());
        }
        captureImage();

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

    public void captureImageFromHardware(){
/*        if(myCamera == null) {
            //TODO: access all available cameras
            myCamera = Camera.open();
            Camera.Parameters params = myCamera.getParameters();
            myCamera.setParameters(params);
//            myCamera.setDisplayOrientation();

        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);*/

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
                    outStream = new FileOutputStream(String.format(
                            "/sdcard/CameraServerData/%d.jpg", System.currentTimeMillis()));
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
                finish();
            }
        };

    }

    public void captureImageFromActivity(){
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }


    public Uri getOutputMediaFileUri() {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
        String date = df.format(Calendar.getInstance().getTime());
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "CameraServerData" + File.separator + date);
        return Uri.fromFile(file);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK ) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image saved to:\n" +
                        data.getData(), Toast.LENGTH_LONG).show();
                Log.d(TAG,"Image saved to:\n" + data.getData());
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
    }

    private void captureImage() {
        // TODO Auto-generated method stub
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private void start_camera()
    {
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
//        param.setPreviewSize(176, 144);
        param.setJpegQuality(100);
        param.setPictureSize(2592, 1944);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        param.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
        param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        param.setAutoExposureLock(false);
        param.setAutoWhiteBalanceLock(false);
        param.set("iso", "ISO800"); //Tried with 400, 800, 600 (values obtained from flatten())
        param.setColorEffect("none");
//        param.setPreviewFrameRate(20);
        param.set("scene-mode", "night-portrait");
        param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        param.setFocusMode("auto");
        param.setExposureCompensation(param.getMaxExposureCompensation());
        camera.setParameters(param);
        setCameraDisplayOrientation(this,0,camera);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
//            camera.takePicture(shutter, raw, jpeg);
        } catch (Exception e) {
            Log.e(TAG, "init_camera: " + e);
            return;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
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
