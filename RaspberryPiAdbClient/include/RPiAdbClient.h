/*
 * RPiAdbClient.h
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "AdbCommon.h"
#include "AdbSocket.h"

#ifndef RPIADBCLIENT_H_
#define RPIADBCLIENT_H_

typedef enum {
	MODE_NONE = 0,
	MODE_CHAT,
	MODE_CAMERA_CAPTURE
}AppMode;

class RPiAdbClient {
public:
	RPiAdbClient();
	RPiAdbClient(char* ip, int port);
	void adbConnect(char* ip);
	void clientConnect(char* ip, int port);
	virtual ~RPiAdbClient();
	void run();
	void setMode(int mode);
private:
	AdbSocket mSocket;
	AppMode mMode;
};

#endif /* RPIADBCLIENT_H_ */
