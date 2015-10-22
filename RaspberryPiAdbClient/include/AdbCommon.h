#include <iostream>
#include <fstream>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string>

using namespace std;

#define BUF_SIZE 1024
#define ADB "/Users/sanjaydixit/Library/Android/sdk/platform-tools/adb "
#define System_do(x) system(x.insert(0,"echo ").c_str()); system(x.substr(5).c_str());

#define NUMBER_OF_CAPTURES 2

//data types
typedef unsigned char UINT8;
typedef signed char SINT8;
typedef unsigned short UINT16;
typedef signed short SINT16;
typedef unsigned int UINT32;
typedef signed int SINT32;
typedef unsigned long long UINT64;
typedef signed long long SINT64;

