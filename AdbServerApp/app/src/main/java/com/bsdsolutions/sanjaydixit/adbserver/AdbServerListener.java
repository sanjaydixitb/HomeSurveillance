package com.bsdsolutions.sanjaydixit.adbserver;

/**
 * Created by sanjaydixit on 06/10/15.
 */
public interface AdbServerListener {

    /**
     * Error Codes:
     *
     *  1 : Socket Already Closed
     *  0 : Success
     * -1 : Failed to bind
     * -2 : Invalid port
     * -3 : Exception while close
     * -4 : Server Socket is null
     *
     * */

    public void onStarted(int errCode);
    public void onStopped(int errCode);
    public void onNewConnectionEstablished(int clientId);
    public void onConnectionLost(int clientId);
    public void onDataReceived(int clientId, byte[] data);

}
