package com.bsdsolutions.sanjaydixit.adbserverapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AdbServerActivity extends AppCompatActivity implements AdbServerListener {

    private static final String TAG = "AdbServerAppLog";
    public static final String EXTRA_NUMBER_OF_CAPTURES = "extra_number_of_captures";
    private static final String Capture_String = "REQUEST_CAMERA_CAPTURE";
    private static final String Data_String = "REQUEST_CAMERA_DATA";
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

        if(mServer == null) {
            mServer = AdbStaticServer.getInstance();
        }

        if(mServer.isServerRunning() && mServer.getPort() != PORT) {
            mServer.stop();
            mStartServerAfterStop = true;
        } else {
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

    private void sendData() {
        AdbStaticServer server = AdbStaticServer.getInstance();
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
            case 0 : break;
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
        if(message.startsWith(Capture_String)) {
            Intent intent = new Intent(this, AdbServerCameraActivity.class);
            if(message.substring(22,23).compareTo(":") == 0) {
                //Received number of captures as well
                try {
                    intent.putExtra(EXTRA_NUMBER_OF_CAPTURES, Integer.parseInt(message.substring(23)));
                } catch (NumberFormatException e) {
                    Log.e(TAG,"Number format exception while converting number of consecutive frames: " + e.getMessage());
                }
            }
            startActivity(intent);
        } else if(message.compareTo(Data_String) == 0) {
            //get List of files in folder(Environment.getExternalStorageDirectory() + File.separator + "CameraServerData" + File.separator + date)
            String path = Environment.getExternalStorageDirectory().toString() + File.separator + "CameraServerData";
            Log.d(TAG, "Path: " + path);
            File f = new File(path);
            File file[] = f.listFiles();
            Log.d(TAG, "Size: " + file.length);
            for (int i=0; i < file.length; i++)
            {
                Log.d(TAG, "FileName:" + file[i].getName());
            }
            sendMessage(clientId,"FileName:"+file[file.length-1].getAbsolutePath()+",SizeInBytes:"+file[file.length-1].length());
            return;
        }
        mTextViewString += "\nClient " + clientId + " : " + message;
        Message msg = mHandler.obtainMessage(UPDATE_TEXTVIEW);
        msg.sendToTarget();
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void sendMessage(int clientId, String message) {
        mServer.sendMessage(clientId, message);
    }

}
