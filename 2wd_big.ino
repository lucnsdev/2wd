#include <SoftwareSerial.h>
//#include <VelocityMonitor.h>
#include <PinChanger.h>
#include <Fade.h>
#include "DataProvider.h"

#define MOTOR_LEFT_A 9
#define MOTOR_LEFT_B 3
#define MOTOR_RIGHT_A 10
#define MOTOR_RIGHT_B 11

#define BLE_STATE 6
#define LEDS 5
#define LED_BLUE 2
#define BATTERY_MONITOR 16

SoftwareSerial bluetooth(7, 8);
DataProvider dataProvider(&bluetooth);
PinChanger leds;
Fade fade;
Delay delayMotor, delaySend;
data_t data;
int battery = 255;
//VelocityMonitor monitor;

void disableAll() {
  digitalWrite(MOTOR_LEFT_A, LOW);
  digitalWrite(MOTOR_LEFT_B, LOW);
  digitalWrite(MOTOR_RIGHT_A, LOW);
  digitalWrite(MOTOR_RIGHT_B, LOW);
}

void setup() {
  Serial.begin(9600);

  pinMode(MOTOR_LEFT_A, OUTPUT);
  pinMode(MOTOR_LEFT_B, OUTPUT);
  pinMode(MOTOR_RIGHT_A, OUTPUT);
  pinMode(MOTOR_RIGHT_B, OUTPUT );
  pinMode(LEDS, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);  
  pinMode(BLE_STATE, INPUT);
  pinMode(BATTERY_MONITOR, INPUT);

  long timeOn = 25;
  long times[] = {timeOn, 600, timeOn, 100, timeOn, 600};
  leds.setParameters(times, 6, LEDS);
  fade.setParameters(LEDS, 1000);
  fade.insertDelay(500);

  delayMotor.setTime(250);
  delaySend.setTime(1000);

  Serial.println("Ready");
}

void loop() {
  // monitor.loop(); // uS: 184 loops: 5434
  
  if (digitalRead(BLE_STATE)) {
    leds.compute();
    disableAll();
    return;
  } else {
    fade.compute();
  }
  if (delayMotor.gate()) {
    digitalWrite(LED_BLUE, LOW);
    disableAll();
  }

  data = dataProvider.retrieve();
  if (data.available) {
    delayMotor.reset();
    /*
    Serial.print(data.pwmLeft);
    Serial.print(" ");
    Serial.print(data.pwmRight);
    Serial.print(" ");
    Serial.println();
    */

    if (data.pwmLeft < 0) {
      digitalWrite(MOTOR_LEFT_A, LOW);
      analogWrite(MOTOR_LEFT_B, data.pwmLeft * (-1));
    } else {
      digitalWrite(MOTOR_LEFT_B, LOW);
      analogWrite(MOTOR_LEFT_A, data.pwmLeft);
    }

    if (data.pwmRight < 0) {
      digitalWrite(MOTOR_RIGHT_B, LOW);
      analogWrite(MOTOR_RIGHT_A, data.pwmRight * (-1));
    } else {
      digitalWrite(MOTOR_RIGHT_A, LOW);
      analogWrite(MOTOR_RIGHT_B, data.pwmRight);
    }

    if (data.pwmLeft == 0 && data.pwmRight == 0) digitalWrite(LED_BLUE, LOW);
    else digitalWrite(LED_BLUE, HIGH);
  }

  if (delaySend.gate()) {
    delaySend.reset();
    int b = analogRead(BATTERY_MONITOR);
    if (b < battery) battery = b;
    uint8_t payload[3];
    payload[0] = 'b';
    payload[1] = battery >> 8;
    payload[2] = battery & 0xFF;
    bluetooth.write(payload, 3);
  }
}
