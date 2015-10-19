/*
 * RPiAdbClient.cpp
 *
 *  Created on: 10-Oct-2015
 *      Author: sanjaydixit
 */

#include "../include/RPiAdbClient.h"

RPiAdbClient::RPiAdbClient() {
	mMode = MODE_NONE;
}

RPiAdbClient::RPiAdbClient(char* ip, int port) : mSocket(ip,port) {
	mMode = MODE_NONE;
}

RPiAdbClient::~RPiAdbClient() {
	// TODO Auto-generated destructor stub
}

void RPiAdbClient::clientConnect(char* ip, int port) {
	mSocket.socketConnect(ip,port);
}

void RPiAdbClient::adbConnect(char* ip){
	string command = "";
	command.append(ADB).append("connect ").append(ip).append(" ");
	System_do(command);
}


void RPiAdbClient::run(){
	string writeBuf, readBuf;
	char dataRead[1024];
	int lenOfRead = 0;
	int numberofCaptures = NUMBER_OF_CAPTURES;
	switch(mMode) {
	case MODE_NONE:
		break;
	case MODE_CHAT:
		cin >> writeBuf;
//		getline(cin,writeBuf);
		mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
		mSocket.socketRead(dataRead,1024);
		readBuf = "";
		readBuf.append(dataRead);
		cout << "Read " << readBuf << endl;
		break;
	case MODE_CAMERA_CAPTURE:
		writeBuf = "REQUEST_CAMERA_CAPTURE";
		writeBuf.append(":");
		writeBuf += numberofCaptures;
		mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
		sleep(10);
		writeBuf = "REQUEST_CAMERA_DATA";
		mSocket.socketWrite((char*)writeBuf.c_str(),writeBuf.length());
		lenOfRead = mSocket.socketRead(dataRead,1024);
		readBuf = "";
		readBuf.append(dataRead,lenOfRead);
		cout << "Read " << readBuf << endl;
		string fileName = readBuf.substr(9,readBuf.find("SizeInBytes:") - 10);
		cout << "FileName : "<<fileName << " and size = " << readBuf.substr(readBuf.find("SizeInBytes:") + 12) <<endl;
		int sizeOfFile = stoi(readBuf.substr(readBuf.find("SizeInBytes:") + 12));
		cout << "FileName : "<<fileName << " and size = " << sizeOfFile <<endl;
		string command = "";
		command.append(ADB).append("pull ").append(fileName).append(" .");
		System_do(command);
		//Remove file after pulling it. Save space!
		command = "";
		command.append(ADB).append("rm ").append(fileName);
		System_do(command);
		break;
//	default:
//		break;
	}
}

void RPiAdbClient::setMode(int mode) {
	mMode = (AppMode)mode;
}

int main() {
	RPiAdbClient client;
	int mode;
	char ip[14] = "192.168.1.100";
	client.adbConnect(ip);
	cout << "ADB Connected!" << endl;
	client.clientConnect(ip,5556);
	cout << "Client Connected!" << endl;
	cout << "Enter Mode: \n1 : Chat\n2: Request Camera capture!" << endl;
	cin >> mode;
	client.setMode(mode);
	client.run();
	return 0;
}
