/*
 * RPiAlgorithmBaseClass.h
 *
 *  Created on: 22-Oct-2015
 *      Author: sanjaydixit
 */

#include "AdbCommon.h"

#ifndef RPIALGORITHMBASECLASS_H_
#define RPIALGORITHMBASECLASS_H_

namespace RPiAdbClientApp {

namespace RPiAdbClientAlgorithm {

class RPiAlgorithmBaseClass {
public:
	RPiAlgorithmBaseClass();
	virtual ~RPiAlgorithmBaseClass();

	//Pure virtual functions that need to be implemented
	virtual void initAlgorithm() = 0;
	virtual int executeAlgorithm(UINT8* data, UINT32 size) = 0;
	virtual void resetAlgorithm(UINT8 code) = 0;
	virtual void updateAlgorithm(UINT8* data, UINT32 size) = 0;
};

}

}

#endif /* RPIALGORITHMBASECLASS_H_ */
