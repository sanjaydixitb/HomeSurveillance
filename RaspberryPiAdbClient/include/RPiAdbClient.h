/*
 * RPiAdbClient.h
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "AdbCommon.h"
#include "RPiAdbClientUtils.h"
#include "AdbSocket.h"
#include "RPiAlgorithmBaseClass.h"
#include "Algorithms/RPiAdbClientImageAlgorithm.h"

#ifndef RPIADBCLIENT_H_
#define RPIADBCLIENT_H_

using namespace RPiAdbClientApp;
using namespace RPiAdbClientApp::RPiAdbClientSocket;
using namespace RPiAdbClientApp::RPiAdbClientAlgorithm;

namespace RPiAdbClientApp {

typedef enum {
	MODE_NONE = 0,
	MODE_CHAT,
	MODE_CAMERA_CAPTURE,
	MODE_CLOSE_SERVER_APP
}AppMode;

class RPiAdbClient {
public:
	RPiAdbClient();
	RPiAdbClient(char* ip, int port);
	bool adbConnect(char* ip);
	bool clientConnect(char* ip, int port);
	virtual ~RPiAdbClient();
	void run();
	int getMode();
	void setMode(int mode);
	void getIPAndPort(char*ip, int port);
private:
	AdbSocket mSocket;
	AppMode mMode;
	RPiAlgorithmBaseClass* mAlgorithm;
	void runChat();
	void runCamera();
	void closeServerApp();
};

}

#endif /* RPIADBCLIENT_H_ */
