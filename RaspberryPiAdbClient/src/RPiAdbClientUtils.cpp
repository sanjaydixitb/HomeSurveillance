/*
 * RPiAdbClientUtils.cpp
 *
 *  Created on: 27-Oct-2015
 *      Author: sanjaydixit
 */

#include "../include/RPiAdbClient.h"

namespace RPiAdbClientApp {

void RPiAdbClientUtils::loadConfigurationFromFile(const string fileName, string** configMap, int &len ) {
	ifstream configFile(fileName);

	for(int i=0; i<len; i++) {
		(*configMap)[i] = "";
	}

	string line = "";
	if(configFile.is_open()) {
		int i= 0;
		while(getline(configFile,line)) {
			if(line.find(":") == string::npos) {
				cout << "Invalid config file!" << endl;
				return;
			} else {
				int index = line.find(":");
				cout << line.substr(0,index) << " : " << line.substr(index + 1) << endl;
				(*configMap)[i++] = line.substr(index + 1);
			}
		}
	}
}

}/*RPiAdbClientApp*/
