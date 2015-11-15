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
	case MODE_CLOSE_SERVER_APP:
		closeServerApp();
		break;
	default:
		cout << "Invalid Mode set : "<< mMode <<endl;
		break;
	}
}

void RPiAdbClient::setMode(int mode) {
	mMode = (AppMode)mode;
}

int RPiAdbClient::getMode() {
	return mMode;
}

void RPiAdbClient::runChat() {
	string writeBuf, readBuf;
	unsigned char dataRead[1024];
	cin >> writeBuf;
	while(writeBuf.compare("Q") != 0) {
	//		getline(cin,writeBuf);
		mSocket.socketWrite((unsigned char*)writeBuf.c_str(),writeBuf.length());
		mSocket.socketRead(dataRead,1024);
		readBuf = "";
		readBuf.append((char*)dataRead);
		cout << "Read " << readBuf << endl;
		cin >> writeBuf;
	}
}

void RPiAdbClient::runCamera() {
	string writeBuf, readBuf;
	unsigned char dataRead[1025];
	long long lenOfRead = 0;
	int numberofCaptures = NUMBER_OF_CAPTURES, numberOfFiles = 0;
	string command = "", folderName = "";
	if(mAlgorithm == NULL) {
		mAlgorithm = new RPiAdbClientImageAlgorithm();
	}
	writeBuf = "REQUEST_CAMERA_CAPTURE";
	writeBuf.append(":");
	stringstream sNumberOfCaptures;
	sNumberOfCaptures << numberofCaptures;
	writeBuf.append(sNumberOfCaptures.str());
	mSocket.socketWrite((unsigned char*)writeBuf.c_str(),writeBuf.length());
	sleep(TIME_TO_WAIT_PER_CAPTURE*numberofCaptures);
	writeBuf = "REQUEST_CAMERA_DATA";
	mSocket.socketWrite((unsigned char*)writeBuf.c_str(),writeBuf.length());
	lenOfRead = mSocket.socketRead(dataRead,1024);
	readBuf = "";
	readBuf.append((char*)dataRead,lenOfRead);
	cout << "Read " << readBuf << endl;
	folderName = readBuf.substr(11,readBuf.find("NumberOfFiles:") - 12);
	cout << "folderName : "<<folderName << " and number of files = " << readBuf.substr(readBuf.find("NumberOfFiles:") + 14) <<endl;
	numberOfFiles = stoi(readBuf.substr(readBuf.find("NumberOfFiles:") + 14));
	cout << "folderName : "<<folderName << " and number of files = " << numberOfFiles <<endl;
	//TODO: Pull entire folder and save in local folder. Clear remote folder
	command = "";
	command.append(ADB).append("pull ").append(folderName).append(" ").append(DESTINATION_FOLDER_PATH);
	System_do(command);

	//TEMP:
	writeBuf = "REQUEST_CAMERA_DATA_FILE_NAMES";
	mSocket.socketWrite((unsigned char*)writeBuf.c_str(),writeBuf.length());
	lenOfRead = mSocket.socketRead(dataRead,1024);
	readBuf = "";
	readBuf.append((char*)dataRead,lenOfRead);
	cout << "Read " << readBuf << endl;
	string fileNamesList = readBuf.substr(readBuf.find(",FileNames:") + 11);
	string file = fileNamesList.substr(0,fileNamesList.find(':'));

	while(file.find("/") != string::npos) {
		file = file.substr(file.find("/") + 1);
	}

	file.insert(0,"out_");

	cout << " Writing to file : " << file << endl;

	FILE* pFile;
	pFile = fopen(file.c_str(),"w+");
	writeBuf = "OK";

	lenOfRead = mSocket.socketRead(dataRead,1024);
	while(lenOfRead > 0) {
		fwrite (dataRead , sizeof(unsigned char), lenOfRead, pFile);
		mSocket.socketWrite((unsigned char*)writeBuf.c_str(), writeBuf.length());
		lenOfRead = mSocket.socketRead(dataRead,1024);
	}

	cout << " Done writing to file!" << endl;

	fclose(pFile);

	//Remove file after pulling it. Save space!
	//TODO: Remove only if image copied
//	command = "";
//	command.append(ADB).append("shell rm ").append(folderName).append("/*");
//	System_do(command);



}

void RPiAdbClient::closeServerApp() {
	string writeBuf;
	if(mAlgorithm != NULL) {
		delete mAlgorithm;
		mAlgorithm = NULL;
	}
	writeBuf = "CLOSE_SERVER_APP";
	mSocket.socketWrite((unsigned char*)writeBuf.c_str(),writeBuf.length());
	sleep(3);
	mMode = MODE_NONE;
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

	//Find Port
	string command = "";
	command.append(ADB).append("pull ").append("/sdcard/adbServer.config .");
	System_do(command);

	string* configParams = new string[7];
	int len = 7;
	RPiAdbClientUtils::loadConfigurationFromFile("adbServer.config",&configParams,len);

	cout << "Connecting to port : " << configParams[1] << endl;

	if(client.clientConnect(ip,atoi(configParams[1].c_str()))) {
	cout << "Client Connected!" << endl;
	} else {
		cout << "Failed to connect to client!" << endl;
		return 0;
	}
	do {
		cout
				<< "Enter Mode: \n1 : Chat\n2: Request Camera capture!\n3: Close Server APP!\n*****************************\n0: Quit"
				<< endl;
		cin >> mode;
		client.setMode(mode);
		client.run();
	} while (client.getMode() != 0);
	return 0;
}
