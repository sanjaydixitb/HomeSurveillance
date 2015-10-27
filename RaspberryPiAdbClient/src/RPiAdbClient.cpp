/*
 * RPiAdbClient.cpp
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "../include/RPiAdbClient.h"

namespace RPiAdbClientApp {

RPiAdbClient::RPiAdbClient() {
	mAlgorithm = NULL;
	mMode = MODE_NONE;
}

RPiAdbClient::RPiAdbClient(char* ip, int port) : mSocket(ip,port) {
	mAlgorithm = NULL;
	mMode = MODE_NONE;
}

RPiAdbClient::~RPiAdbClient() {
	// TODO Auto-generated destructor stub
}

void RPiAdbClient::getIPAndPort(char*ip, int port) {
	string command = "";
	//TODO: get file using adb and parse it to get ip and port (verifying if server has started)
	//		else, start the server application by launching it.
}


bool RPiAdbClient::clientConnect(char* ip, int port) {
	mSocket.socketConnect(ip,port);
	return mSocket.isConnected();
}

bool RPiAdbClient::adbConnect(char* ip){
	string command = "";
	command.append(ADB).append("connect ").append(ip).append(" ");
	System_do(command);

	command = "";
	command.append(ADB).append("devices");
	System_do_with_log(command);
	ifstream command_log("command_log");
	string result;
	bool connected = false;

	while(getline(command_log,result)) {
		if(result.find(ip) != string::npos) {
			//Success
			connected = true;
			break;
		}
	}
	return connected;
}


void RPiAdbClient::run(){

	switch(mMode) {
	case MODE_NONE:
		break;
	case MODE_CHAT:
		runChat();
		break;
	case MODE_CAMERA_CAPTURE:
		runCamera();
		break;
	default:
		cout << "Invalid Mode set : "<< mMode <<endl;
		break;
	}
}

void RPiAdbClient::setMode(int mode) {
	mMode = (AppMode)mode;
}

void RPiAdbClient::runChat() {
	string writeBuf, readBuf;
	char dataRead[1024];
	cin >> writeBuf;
	while(writeBuf.compare("Q") != 0) {
	//		getline(cin,writeBuf);
		mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
		mSocket.socketRead(dataRead,1024);
		readBuf = "";
		readBuf.append(dataRead);
		cout << "Read " << readBuf << endl;
		cin >> writeBuf;
	}
}

void RPiAdbClient::runCamera() {
	string writeBuf, readBuf;
	char dataRead[1024];
	int lenOfRead = 0;
	int numberofCaptures = NUMBER_OF_CAPTURES, numberOfFiles = 0;
	string command = "", folderName = "";
	mAlgorithm = new RPiAdbClientImageAlgorithm();
	writeBuf = "REQUEST_CAMERA_CAPTURE";
	writeBuf.append(":");
	stringstream sNumberOfCaptures;
	sNumberOfCaptures << numberofCaptures;
	writeBuf.append(sNumberOfCaptures.str());
	mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
	sleep(TIME_TO_WAIT_PER_CAPTURE*numberofCaptures);
	writeBuf = "REQUEST_CAMERA_DATA";
	mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
	lenOfRead = mSocket.socketRead(dataRead,1024);
	readBuf = "";
	readBuf.append(dataRead,lenOfRead);
	cout << "Read " << readBuf << endl;
	folderName = readBuf.substr(11,readBuf.find("NumberOfFiles:") - 12);
	cout << "folderName : "<<folderName << " and number of files = " << readBuf.substr(readBuf.find("NumberOfFiles:") + 14) <<endl;
	numberOfFiles = stoi(readBuf.substr(readBuf.find("NumberOfFiles:") + 14));
	cout << "folderName : "<<folderName << " and number of files = " << numberOfFiles <<endl;
	//TODO: Pull entire folder and save in local folder. Clear remote folder
	command = "";
	command.append(ADB).append("pull ").append(folderName).append(" ").append(DESTINATION_FOLDER_PATH);
	System_do(command);
	//Remove file after pulling it. Save space!
	//TODO: Remove only if image copied
	command = "";
	command.append(ADB).append("shell rm ").append(folderName).append("/*");
	System_do(command);
}

}//RPiAdbClientApp

int main() {
	RPiAdbClient client;
	int mode;
	char ip[14] = "192.168.1.100";
	if(client.adbConnect(ip)) {
		cout << "ADB Connected!" << endl;
	} else {
		cout << "Failed to connect to ADB!" << endl;
		return 0;
	}
	if(client.clientConnect(ip,5556)) {
	cout << "Client Connected!" << endl;
	} else {
		cout << "Failed to connect to client!" << endl;
		return 0;
	}
	do {
		cout
				<< "Enter Mode: \n1 : Chat\n2: Request Camera capture!\n*****************************\n0: Quit"
				<< endl;
		cin >> mode;
		client.setMode(mode);
		client.run();
	} while (mode != 0);
	return 0;
}
