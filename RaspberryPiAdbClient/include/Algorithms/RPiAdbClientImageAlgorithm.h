/*
 * RPiAdbClientImageAlgorithm.h
 *
 *  Created on: 22-Oct-2015
 *      Author: sanjaydixit
 */

#include "../RPiAlgorithmBaseClass.h"

#ifndef RPIADBCLIENTIMAGEALGORITHM_H_
#define RPIADBCLIENTIMAGEALGORITHM_H_

namespace RPiAdbClientApp {
namespace RPiAdbClientAlgorithm {

class RPiAdbClientImageAlgorithm : public RPiAlgorithmBaseClass {
public:
	RPiAdbClientImageAlgorithm();
	virtual ~RPiAdbClientImageAlgorithm();

	virtual void initAlgorithm();
	virtual int executeAlgorithm(UINT8* data, UINT32 size);
	virtual void resetAlgorithm(UINT8 code);
	virtual void updateAlgorithm(UINT8* data, UINT32 size);
};

} /* namespace RPiAdbClientAlgorithm */
} /* namespace RPiAdbClientApp */

#endif /* RPIADBCLIENTIMAGEALGORITHM_H_ */
