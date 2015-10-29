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

class RPiAdbClientUtils {

public:
	static void loadConfigurationFromFile(const string fileName, string** configMap, int &len );

};

}/*RPiAdbClientApp*/

#endif /* RPIADBCLIENTUTILS_H_ */
