/*
 * AdbSocket.h
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "AdbCommon.h"

#ifndef AdbSocket_H_
#define AdbSocket_H_

namespace RPiAdbClientApp {

namespace RPiAdbClientSocket {

#define DEFAULT_IP "192.168.1.100"
#define DEFAULT_PORT 5556

class AdbSocket {
public:
	AdbSocket();
	AdbSocket(char* ip, int port);
	bool socketConnect(char* ip, int port);
	virtual ~AdbSocket();
	int socketWrite(unsigned char* buff, int size);
	int socketRead(unsigned char* buff, int size, unsigned char* ack = NULL, int ackSize = 0);
	bool isConnected();
private:
	int mSocket,mPort;
	char* mIp;
	char mReadBuffer[BUF_SIZE];
	char mWriteBuffer[BUF_SIZE];
	bool mIsConnected;
	bool socketConnect();
};

}

}

#endif /* AdbSocket_H_ */
