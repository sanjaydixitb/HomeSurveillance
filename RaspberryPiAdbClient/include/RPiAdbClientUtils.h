/*
 * RPiAdbClientUtils.h
 *
 *  Created on: 27-Oct-2015
 *      Author: sanjaydixit
 */

#include "AdbCommon.h"

#ifndef RPIADBCLIENTUTILS_H_
#define RPIADBCLIENTUTILS_H_

namespace RPiAdbClientApp {
namespace RPiAdbClientAlgorithm {

static class RPiAdbClientUtils {
	void loadConfigurationFromFile(const string fileName,const unordered_map<CONFIG_KEYS,string> &configMap);
};

} /* namespace RPiAdbClientAlgorithm */
} /* namespace RPiAdbClientApp */

#endif /* RPIADBCLIENTUTILS_H_ */
