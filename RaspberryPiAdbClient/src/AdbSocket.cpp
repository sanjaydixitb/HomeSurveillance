/*
 * AdbSocket.cpp
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "../include/RPiAdbClient.h"

AdbSocket::AdbSocket() {
	mPort = -1;
	mSocket =  -1;
	mIp = NULL;
	memset(mReadBuffer,0,sizeof(mReadBuffer));
	memset(mWriteBuffer,0,sizeof(mWriteBuffer));
}

AdbSocket::AdbSocket(char* ip, int port) {
	memset(mReadBuffer,0,sizeof(mReadBuffer));
	memset(mWriteBuffer,0,sizeof(mWriteBuffer));
	socketConnect(ip,port);
}

AdbSocket::~AdbSocket() {
	cout << " Closing socket " << endl;
	close(mSocket);
}

void AdbSocket::socketConnect(char* ip, int port) {
	mPort = port;
	mSocket =  socket(AF_INET, SOCK_STREAM, 0);
	if(!mIp)
		mIp = (char*)malloc(sizeof(char)*(strlen(ip)+1));
	mIp = strcpy(mIp,ip);
	if(socketConnect()) {
		cout << "Socket connected to " << ip <<":" << port << endl;
	} else {
		cout << "Socket failed to connect to " << ip << ":" << port << endl;
	}
}

bool AdbSocket::socketConnect() {
	if(mPort == -1 || mSocket == -1 || mIp == NULL) {
		cout << "ERROR, invalid ip, port or socket" << endl;
		return false;
	}
    struct sockaddr_in serv_addr;
    struct hostent *server;
    server = gethostbyname(mIp);
    if (server == NULL) {
    	 cout << "ERROR, no such host : " << mIp << endl;
        //TODO: try adb connect once
    	 return false;
    }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy((char *)server->h_addr,
         (char *)&serv_addr.sin_addr.s_addr,
         server->h_length);
    serv_addr.sin_port = htons(mPort);
    if (connect(mSocket,(struct sockaddr *) &serv_addr,sizeof(serv_addr)) < 0) {
        cout << "ERROR connecting to "<< mIp << ":" << mPort << endl;
        return false;
    }

    return true;
}

int AdbSocket::socketWrite(char* buff, int size) {
    int retVal = 0;
	bzero(mWriteBuffer,BUF_SIZE);
	memcpy(mWriteBuffer,buff,size);
	if(size < BUF_SIZE) {
		mWriteBuffer[size++] = '\n';
	} else {
		cout << "size limit exceeds!" << endl;
	}
    retVal = write(mSocket,mWriteBuffer,size);
    if (retVal < 0) {
         cout << "ERROR writing to socket" << endl;
    }
    return retVal;
}

int AdbSocket::socketRead(char* buff, int size) {
	int retVal = 0;
    bzero(mReadBuffer,BUF_SIZE);
    retVal = read(mSocket,mReadBuffer,BUF_SIZE);
    if (retVal < 0) {
         cout << "ERROR reading from socket" << endl;
    } else {
    	memcpy(buff,mReadBuffer,retVal);
    }
	return retVal;
}


