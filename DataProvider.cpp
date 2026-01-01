#include "DataProvider.h"

DataProvider::DataProvider(SoftwareSerial* serial) {
  serial->begin(9600);
  this->serial = serial;
  checkTime.setTime(100);
}

data_t DataProvider::retrieve() {
  data.available = false;
  if (serial->available()) {
    char type = (char) serial->read();
    checkTime.reset();
    if (type == 's') {
      data.available = true;
      data.pwmLeft = 0;
      data.pwmRight = 0;
    } else if (type == 'm') {
      int index = 0;
      while (!checkTime.isOverTime()) {
        if (serial->available()) {
          checkTime.reset();
          buffer[index] = serial->read();
          index++;
        }
        if (index == 4) break;
      }
      if (index == 4) {
        data.pwmLeft = buffer[0] << 8 | buffer[1];
        data.pwmRight = buffer[2] << 8 | buffer[3];
        data.available = true;
      }
    }
  }
  return data;
}
