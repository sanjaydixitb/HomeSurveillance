/*
 * RPiAdbClientUtils.cpp
 *
 *  Created on: 27-Oct-2015
 *      Author: sanjaydixit
 */

#include "../include/RPiAdbClient.h"

namespace RPiAdbClientApp {
namespace RPiAdbClientAlgorithm {

void loadConfigurationFromFile(const string fileName,const unordered_map<CONFIG_KEYS,string> &configMap) {
	ifstream configFile(fileName);
}

} /* namespace RPiAdbClientAlgorithm */
} /* namespace RPiAdbClientApp */
