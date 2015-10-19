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
