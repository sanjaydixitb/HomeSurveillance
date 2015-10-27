package com.bsdsolutions.sanjaydixit.adbserverapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bsdsolutions.sanjaydixit.adbserver.AdbServerListener;
import com.bsdsolutions.sanjaydixit.adbserver.AdbStaticServer;
import com.bsdsolutions.sanjaydixit.adbserverapp.AdbServerAppUtils.CONFIGURATION_PARAMETERS;

import java.io.File;
import java.util.HashMap;

public class AdbServerActivity extends AppCompatActivity implements AdbServerListener {

    private static final String TAG = "AdbServerAppLog";
    public static final String EXTRA_NUMBER_OF_CAPTURES = "extra_number_of_captures";
    public static final String CAMERA_SERVER_DATA_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "CameraServerData";
    private static final String Capture_Request = "REQUEST_CAMERA_CAPTURE";
    private static final String Data_Request = "REQUEST_CAMERA_DATA";
    private static final String File_Names_Request = "REQUEST_CAMERA_DATA_FILE_NAMES";
    private static int PORT = 5556;
    private boolean mStartServerAfterStop = false;
    private static AdbStaticServer mServer = null;
    private static TextView mTextView = null;
    private static volatile String mTextViewString = "";
    private static final int UPDATE_TEXTVIEW = 1011;
    private static final int TRY_NEXT_PORT = 1012;
    private static Button mButton = null;
    private static int mClientId = -1;
    private static EditText mEditText = null;
    private PowerManager.WakeLock wl = null;
    private static volatile Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adb_server);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        PowerManager pm = (PowerManager)getSystemService(
                Context.POWER_SERVICE);
        wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                TAG);

        checkWifiStatus();

        if(mServer == null) {
            mServer = AdbStaticServer.getInstance();
        }

        if(mServer.isServerRunning() && mServer.getPort() != PORT) {
            mServer.stop();
            mStartServerAfterStop = true;
        } else {
            clearConfigFile();
            mServer.start(PORT, this, this);
        }

        mTextView = (TextView)findViewById(R.id.textViewLog);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.setTextColor(Color.BLACK);

        mEditText = (EditText)findViewById(R.id.textInput);

        mButton = (Button)findViewById(R.id.sendButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mServer.isServerRunning()) {
                    Toast.makeText(AdbServerActivity.this, "Server not started!", Toast.LENGTH_SHORT).show();
                } else {
                    if (mClientId == -1) {
                        Toast.makeText(AdbServerActivity.this, "No active clients!", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = mEditText.getText().toString();
                        if (message.equals("")) {
                            Toast.makeText(AdbServerActivity.this, "Enter message!", Toast.LENGTH_SHORT).show();
                        } else {
                            sendMessage(mClientId, message);
                            mTextViewString += "\nMe : " + message;
                            mEditText.setText("");
                            Message msg = mHandler.obtainMessage(UPDATE_TEXTVIEW);
                            msg.sendToTarget();
                        }
                    }
                }
            }
        });

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch(msg.what) {
                    case UPDATE_TEXTVIEW:
                        mTextView.setText(mTextViewString);
                        final int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
                        if(scrollAmount > 0)
                            mTextView.scrollTo(0,scrollAmount);
                        return true;
                    case TRY_NEXT_PORT:
                        PORT++;
                        mServer.start(PORT,AdbServerActivity.this,AdbServerActivity.this);
                    default:
                        break;
                }
                return false;
            }
        });

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onResume() {
        super.onResume();
        wl.acquire();
    }

    @Override
    protected void onPause() {
        wl.release();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mServer.stop();
        mHandler.removeMessages(UPDATE_TEXTVIEW);
        super.onDestroy();
    }

    private void clearConfigFile() {
        HashMap<CONFIGURATION_PARAMETERS,String> map =  AdbServerAppUtils.getConfigMap();
        map.clear();
        map.put(CONFIGURATION_PARAMETERS.PORT, "");
        map.put(CONFIGURATION_PARAMETERS.CAMERA_SERVER_DATA_PATH, "");
        AdbServerAppUtils.writeConfigurationToFile();
    }

    private void checkWifiStatus(){
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            wifi.setWifiEnabled(true); // true or false to activate/deactivate wifi
            Log.d(TAG, "Wifi was disabled, so enabled it!");
        }
    }

    public void onStarted(int errCode) {
/*
        if(errCode < 0) {
            //Failed to Start
            //TODO: retry maybe?
            finish();
        }
*/
        switch(errCode) {
            case 0 :
                HashMap<CONFIGURATION_PARAMETERS,String> map =  AdbServerAppUtils.getConfigMap();
                map.clear();
                map.put(CONFIGURATION_PARAMETERS.PORT, String.valueOf(PORT));
                map.put(CONFIGURATION_PARAMETERS.CAMERA_SERVER_DATA_PATH, CAMERA_SERVER_DATA_PATH);
                AdbServerAppUtils.writeConfigurationToFile();
                break;
            case -1 :
                Message msg = mHandler.obtainMessage(TRY_NEXT_PORT);
                msg.sendToTarget();
                break;
            default:
                //Failed to Start
                //TODO: retry maybe?
                finish();
        }
        Log.d(TAG,"Server Started!");
    }

    public void onStopped(int errCode) {

        clearConfigFile();

        if(mStartServerAfterStop) {
            Log.d(TAG,"Starting Server after Stopping!");
            mStartServerAfterStop = false;
            mServer.start(PORT, this, this);
        }
        if(errCode < 0) {
            //Failed to Stop
            //TODO: retry maybe?
        }
        Log.d(TAG,"Server Stopped!");
    }

    public void onNewConnectionEstablished(int clientId) {
        mClientId = clientId;
    }

    public void onConnectionLost(int clientId) {
        if(clientId == mClientId) {
            mClientId = -1;
        }
    }

    public void onDataReceived(int clientId, byte[] data) {
        String message = new String(data);
        if(message.startsWith(Capture_Request)) {
            Intent intent = new Intent(this, AdbServerCameraActivity.class);
            int req_len = Capture_Request.length();
            if(message.length() > req_len && message.substring(req_len,req_len + 1).compareTo(":") == 0) {
                //Received number of captures as well
                try {
                    intent.putExtra(EXTRA_NUMBER_OF_CAPTURES, Integer.parseInt(message.substring(req_len + 1)));
                } catch (NumberFormatException e) {
                    Log.e(TAG,"Number format exception while converting number of consecutive frames: " + e.getMessage());
                }
            }
            startActivity(intent);
        } else if(message.compareTo(Data_Request) == 0) {
            //TODO: Probably redundant. Move to Utils.
            //get List of files in folder(Environment.getExternalStorageDirectory() + File.separator + "CameraServerData" + File.separator + date)
            String path = CAMERA_SERVER_DATA_PATH;
            Log.d(TAG, "Path: " + path);
            File f = new File(path);
            File file[] = f.listFiles();
            Log.d(TAG, "Size: " + file.length);
            for (int i=0; i < file.length; i++)
            {
                Log.d(TAG, "FileName:" + file[i].getName());
            }
            sendMessage(clientId,"FolderName:"+path+",NumberOfFiles:"+file.length);
//            sendMessage(clientId,"FileName:"+file[file.length-1].getAbsolutePath()+",SizeInBytes:"+file[file.length-1].length());
            return;
        } else if(message.compareTo(File_Names_Request) == 0) {
            //TODO: Move to Utils.
            //get List of files in folder(Environment.getExternalStorageDirectory() + File.separator + "CameraServerData" + File.separator + date)
            String path = CAMERA_SERVER_DATA_PATH;
            String fileNames = "";
            Log.d(TAG, "Path: " + path);
            File f = new File(path);
            File file[] = f.listFiles();
            Log.d(TAG, "Size: " + file.length);
            for (int i=0; i < file.length; i++)
            {
                Log.d(TAG, "FileName:" + file[i].getName());
                if(i > 0)
                    fileNames += ":";
                fileNames += file[i].getAbsolutePath();
            }
            sendMessage(clientId,"NumberOfFiles:"+file.length+",FileNames:"+fileNames);
//            sendMessage(clientId,"FileName:"+file[file.length-1].getAbsolutePath()+",SizeInBytes:"+file[file.length-1].length());
            return;
        }
        mTextViewString += "\nClient " + clientId + " : " + message;
        Message msg = mHandler.obtainMessage(UPDATE_TEXTVIEW);
        msg.sendToTarget();
    }

    public void sendMessage(int clientId, String message) {
        mServer.sendMessage(clientId, message);
    }

}
