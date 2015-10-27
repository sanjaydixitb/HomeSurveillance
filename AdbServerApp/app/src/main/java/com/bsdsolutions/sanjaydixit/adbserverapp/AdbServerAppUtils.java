package com.bsdsolutions.sanjaydixit.adbserverapp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Created by sanjaydixit on 25/10/15.
 */
public class AdbServerAppUtils {
    public static final String DEFAULT_CONFIGURATION_FILE_PATH = Environment.getExternalStorageDirectory()+"/adbServer.config";
    public static final String CONFIG_SEPARATOR = ":";
    public static final String TAG = "AdbServerAppLog";
    private static String mFilePath = DEFAULT_CONFIGURATION_FILE_PATH;
    private static HashMap<CONFIGURATION_PARAMETERS,String> mConfigMap = new HashMap<>();

    public enum CONFIGURATION_PARAMETERS{
        PORT,
        CAMERA_SERVER_DATA_PATH
    };

    public static void setConfigurationFilePath(String path) {
        mFilePath = path;
    }

    public static String getConfigurationFilePath() {
        return mFilePath;
    }

    public static boolean writeConfigurationToFile() {
        if(mFilePath.length()== 0) {
            Log.e(TAG,"Invalid configuration file path set!");
            return false;
        }

        File file = new File(mFilePath);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG,"File does not exist and failed to create it!");
                return false;
            }
            Log.e(TAG,"Configuration file does not exist! Created it!");
        }

        try {
            FileOutputStream f = new FileOutputStream(file,false);
            PrintWriter pw = new PrintWriter(f);
            for( Entry<CONFIGURATION_PARAMETERS,String> values : mConfigMap.entrySet()) {
                pw.println(values.getKey().name() + ":" + values.getValue());
            }
            pw.flush();
            pw.close();
            f.close();
        } catch (Exception e) {
            Log.e(TAG,"Exception while writing to file : "+e.getMessage());
            return false;
        }


        return true;
    }

    public static void loadConfigurationFromFile() {

        mConfigMap.clear();

        if(mFilePath.length()== 0) {
            Log.e(TAG,"Invalid configuration file path set!");
        }

        File file = new File(mFilePath);
        if(!file.exists()) {
            Log.e(TAG,"Configuration file does not exist!");
        }

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                //ParseLine
                String key = getKey(line), value = getValue(line);
                if(key.length() != 0 && value.length() != 0) {
                    mConfigMap.put(CONFIGURATION_PARAMETERS.valueOf(key), value);
                }
            }
            br.close();
        }
        catch (IOException e) {
            Log.e(TAG,"Exception while reading Configuration File : " + e.getMessage());
        }

    }

    public static HashMap<CONFIGURATION_PARAMETERS,String> getConfigMap() {
        return mConfigMap;
    }

    private static String getKey(String line) {
        String retVal = "";
        int index = line.indexOf(CONFIG_SEPARATOR);
        if(line.length() != 0 && index != -1) {
            return line.substring(0,index);
        }
        return retVal;
    }

    private static String getValue(String line) {
        String retVal = "";
        int index = line.indexOf(CONFIG_SEPARATOR);
        if(line.length() != 0 && index != -1) {
            return line.substring(index+1);
        }
        return retVal;
    }

}
