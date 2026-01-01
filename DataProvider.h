#include <Arduino.h>
#include <SoftwareSerial.h>
#include <Delay.h>

struct data_t {
  int ledsState;
  int pwmLeft, pwmRight, leds;
  bool available;
};

class DataProvider {
  private:
    Delay checkTime;
    SoftwareSerial* serial;
    int buffer[8];
    data_t data;
  public:
    DataProvider(SoftwareSerial*);
    data_t retrieve();
};
