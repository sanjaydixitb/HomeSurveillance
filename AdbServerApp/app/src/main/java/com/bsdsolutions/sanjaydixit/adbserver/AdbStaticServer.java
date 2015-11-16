package com.bsdsolutions.sanjaydixit.adbserver;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sanjaydixit on 06/10/15.
 */
public class AdbStaticServer {

    //Log
    private static final String TAG = "AdbServerAppLog";

    //Singleton

    private AdbStaticServer() {
        try {
            mServerSocket = new ServerSocket();
        } catch (IOException e) {
            Log.e(TAG,"Exception while creating socket : " + e.getMessage());
            mServerSocket = null;
        }
    }

    private static AdbStaticServer mInstance = new AdbStaticServer();

    public static AdbStaticServer getInstance( ) {
        //TODO: create a new socket every time ?
        return mInstance;
    }

    private enum ServerState {
        ServerState_Starting,
        ServerState_Started,
        ServerState_Stopping,
        ServerState_Stopped
    };

    private ServerSocket mServerSocket = null;
    private AdbServerListener mListener = null;
    private Thread AsyncAcceptThread = null;
    private volatile ServerState mState = ServerState.ServerState_Stopped;
    //TODO: Remove
    private HashMap<Integer,Socket> clientMap = new HashMap<>();
    private int clientSendingFile = -1;
    private int connectionCounter = 0;

    public void registerListener(AdbServerListener listener) {
        if(listener == null) {
            Log.e(TAG, "listener object is null");
        }
        mListener = listener;
    }

    public void start(int port, Context context) {

        if(mState != ServerState.ServerState_Stopped) {
            Log.e(TAG,"ServerState is not Stopped, state : " + mState.name());
            return;
        }

        mState = ServerState.ServerState_Starting;

        if(mServerSocket == null) {
            Log.e(TAG,"Server socket is null! Creating it");
            try {
                mServerSocket = new ServerSocket();
            } catch (IOException e) {
                Log.e(TAG, "Exception while creating server socket : " + e.getMessage());
                mState = ServerState.ServerState_Stopped;
                mListener.onStarted(-4);
                return;
            }
        }

        if(port < 0 || port > 65535)
        {
            Log.e(TAG, "Invalid port : " + port);
            mState = ServerState.ServerState_Stopped;
            mListener.onStarted(-2);
            return;
        }

        String ip = AdbServerUtils.getIPAddress(context);
        if(ip.equals("")) {
            ip = "192.168.1.2";
            Log.e(TAG,"Force set ip to : " + ip);
        }
        Log.d(TAG, "ip : " + ip);
        SocketAddress address = (SocketAddress)new InetSocketAddress(ip,port);
        try {
            mServerSocket.bind(address);
        } catch (IOException e) {
            Log.e(TAG, "Failed to bind to address : " + ip + ":" + port + " . Exception : " + e.getMessage());
            if(e.getMessage().contains("closed")){
                //Making it null so that it is created in next call
                mServerSocket = null;
            }
            mState = ServerState.ServerState_Stopped;
            mListener.onStarted(-1);
            return;
        }
        Log.d(TAG,"Server bound at port : " + port);

        //TODO: Async Accept
        if(AsyncAcceptThread == null) {
            AsyncAcceptThread = new Thread(new AcceptServerConnections());
            AsyncAcceptThread.start();
        }

        mState = ServerState.ServerState_Started;

        mListener.onStarted(0);

    }

    public void start(int port, AdbServerListener listener, Context context) {
        registerListener(listener);
        start(port,context);
    }

    public boolean isServerRunning() {

        if (mServerSocket == null || !mServerSocket.isBound()) {
            return false;
        }

        return !mServerSocket.isClosed();

    }

    public void stop() {

        if(mState != ServerState.ServerState_Started) {
            Log.e(TAG, "ServerState is not Started, state : " + mState.name());
            return;
        }

        mState = ServerState.ServerState_Stopping;

        if( !mServerSocket.isClosed() ) {
            try {
                mServerSocket.close();
            } catch ( IOException e ) {
                Log.d(TAG, "Exception while closing socket : " + e.getMessage());
                mState = ServerState.ServerState_Started;
                mListener.onStopped(-3);
                return;
            }
            Log.d(TAG, "Closed Server Socket!");
            mListener.onStopped(0);
        } else {
            Log.d(TAG, "Server Socket Already Closed!");
            mListener.onStopped(1);
        }

        mState = ServerState.ServerState_Stopped;

    }

    public int getPort() {

        if(mServerSocket.isClosed()) {
            return -1;
        }

        return mServerSocket.getLocalPort();

    }

    public void sendMessage(int clientId, String message) {
        Socket socket = clientMap.get(clientId);
        if(socket == null) {
            Log.e(TAG,"Socket not found in map!");
            return;
        }

        PrintWriter out = null;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            out.write(message);
            out.flush();
        } catch (IOException e) {
            Log.e(TAG,"Exception while handling outstream : " + e.getMessage());
        }

    }

    public boolean sendFile(int clientId, String fileName) {
        if(clientSendingFile != -1) {
            Log.e(TAG, "Already sending file! Busy!");
            return false;
        } else {
            clientSendingFile = clientId;
            Log.d(TAG,"Client " + clientSendingFile + " is sending a file!");
        }
        Socket socket = clientMap.get(clientId);
        if(socket == null || socket.isClosed() || !socket.isConnected()) {
                Log.e(TAG,"Invalid socket to sendFile!");
                clientSendingFile = -1;
                return false;
            }

            if(fileName == null || fileName.length() == 0) {
                Log.e(TAG,"Invalid filename to sendFile!");
                clientSendingFile = -1;
                return false;
            }

            File file = new File(fileName);
            if(!file.exists()) {
                Log.e(TAG,"File " + fileName + " does not exist! Error in sendFile!");
                clientSendingFile = -1;
                return false;
            }

            try {
                FileInputStream fis = new FileInputStream(file);
                long fileSize = file.length();
                long dataRead = 0;
                byte[] buffer = new byte[8000];
                int dr = 0,sleepCount = 0;
                String ackRead = "";
                BufferedReader in = null;
                DataOutputStream out = null;
                socket.setSendBufferSize(8004);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new DataOutputStream(socket.getOutputStream());

                while(dataRead != fileSize) { // && !socket.isOutputShutdown() && !socket.isClosed()) {
                    dr = fis.read(buffer);
                    if(dr == -1) {
                        Log.d(TAG,"End of File reached!");
                        break;
                    } else {
                        dataRead += (long)dr;
                    }
//                    if(dataRead < 100) {
//                        Log.d(TAG,"read data : " + buffer + " dataRead : " + dataRead);
//                    }
//                    Log.d(TAG, "Read [" + dataRead + "/" + fileSize + "]");

                    if(dr == buffer.length)
                        out.write(buffer);
                    else
                        out.write(buffer,0,dr);
                    out.flush();

                    ackRead = in.readLine();
                    sleepCount = 0;
                    while(ackRead == null) {
                        //Sleep for some time
                        sleepCount++;
                        try {
                            Thread.sleep(0, 50);
                        } catch (InterruptedException e) {
                            Log.e(TAG,"Exception while sleeping : " + e.getMessage());
                        }
                        Log.d(TAG,"SleepCount : " + sleepCount);
                        ackRead = in.readLine();
                        if(sleepCount > 50) {
                            break;
                        }
                    }

                    if(ackRead == null) {
                        break;
                    } else if(ackRead.compareToIgnoreCase("OK") == 0) {
                        continue;
                    }

                }

                Log.d(TAG, "While Ended with dataRead : " + dataRead + " and fileSize = " + fileSize);

                out.flush();

            } catch (FileNotFoundException e) {
                Log.e(TAG,"File not found exception for file " + file + " with message : "+ e.getMessage());
                clientSendingFile = -1;
                return false;
            } catch (IOException e) {
                Log.e(TAG,"IO exception for file " + file + " with message : "+ e.getMessage());
                clientSendingFile = -1;
                return false;
            }

            clientSendingFile = -1;
            return true;
    }

    public class AcceptServerConnections implements Runnable {

        @Override
        public void run() {

            Log.d(TAG,"Async accept thread started!");

            while(mState == ServerState.ServerState_Starting || mState == ServerState.ServerState_Started) {
                try {
                    Log.d(TAG,"Waiting for a new Connection!");
                    Socket newConnection = mServerSocket.accept();
                    Log.d(TAG, "Accepted new Connection!");
/*
                    if(clientMap.containsKey(connectionCounter)) {
                        //TODO: Already Exists
                    }
*/
                    Thread connection = new Thread(new ServerConnection(connectionCounter,newConnection));
                    connection.start();
                    clientMap.put(connectionCounter, newConnection);
                    mListener.onNewConnectionEstablished(connectionCounter);
                    connectionCounter++;

                } catch (IOException e) {
                    Log.e(TAG,"Exception while server socket accept : " + e.getMessage());
                }
            }
            Log.d(TAG,"Closing all existing sockets!");

            for(Socket socket : clientMap.values()) {
                try {
                    if (!socket.isClosed())
                        socket.close();
                } catch (IOException e) {
                    Log.e(TAG,"Exception while closing Socket : " + e.getMessage());
                }
            }

            Log.d(TAG,"Exiting Async Accept Thread!");
        }
    }

    public class ServerConnection implements Runnable {

        private int mClientId;
        private Socket mSocket;

        public ServerConnection(int id, Socket socket) {
            mClientId = id;
            mSocket = socket;
        }

        @Override
        public void run() {

            if(!mSocket.isConnected() || !mSocket.isBound() || mSocket.isClosed() || mSocket.isInputShutdown()) {
                Log.e(TAG,"Socket error!");
                return;
            }
            BufferedReader inp = null;
            try {
                inp = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            } catch (IOException e) {
                Log.e(TAG,"Exception while getting input stream!");
                return;
            }

            String buffer;

            do {
                try {
                    while(clientSendingFile == mClientId) {
                        Thread.sleep(50,0);
                    }
                    buffer = inp.readLine();
                    Log.d(TAG, "Read Line : " + buffer);
                    if(buffer != null) {
                        mListener.onDataReceived(mClientId, buffer.getBytes());
                    }
                } catch (IOException e) {
                    buffer = null;
                    Log.e(TAG,"Exception while reading line : " + e.getMessage());
                } catch (InterruptedException e) {
                    buffer = null;
                    Log.e(TAG,"Exception while sleeping : " + e.getMessage());
                }
            } while(buffer != null && !mSocket.isInputShutdown());

            Log.d(TAG, "Closing connection id : " + mClientId);
            mListener.onConnectionLost(mClientId);
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while closing reading socket : " + e.getMessage());
            }

        }
    }

}
