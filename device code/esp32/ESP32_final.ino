#include <Arduino.h>
#include <WiFiManager.h>
#include <time.h>

#include <Firebase_ESP_Client.h>

#include <addons/TokenHelper.h>

#include <addons/RTDBHelper.h>
#include "BluetoothSerial.h"

// Check if Bluetooth is availablesasa
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it.
#endif

#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Port Profile for Bluetooth is not available or not enabled. It is only available for the ESP32 chip.
#endif

#define RXp2 16
#define TXp2 17

#define API_KEY "AIzaSyA5b498eUElSD4RUYlHg-LRDMYCkZzg2uI"

#define DATABASE_URL "https://fir-auth-e169c-default-rtdb.firebaseio.com/"

#define USER_EMAIL "jejemon@gmail.com"
#define USER_PASSWORD "jejemon123"

#define MAX_MESSAGE 30

// Firebase Data object
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

BluetoothSerial SerialBT;

unsigned long sendDataPrevMillis1 = 0;
unsigned long sendDataPrevMillis2 = 0;
unsigned long startConnectMillis = 0;
unsigned long previousActiveMillis = 0;

const int batteryLED = 12;
const int firebaseLED = 14;
const int bluetoothLED = 27;
const int wifiLED = 26;
const int powerLED = 25;

const int solarPanelVoltSensor = 35;
const int batteryVoltSensor = 36;
const int relayPin = 13;

const int wetSoil = 277;
const int drySoil = 380;

// Time variables
const char* ntpServer = "asia.pool.ntp.org";
const long gmtOffset_sec = 28800;
const int daylightOffset_sec = 0;

// Battery monitoring and charge control
const float R1 = 30000.0f;
const float R2 = 7500.0f;
const float VREF = 3.3f;
const float OFFSET_VALUE = 1.00f;
const int ADC_RESOLUTION = 4095;
bool isCharging = false;

String prevCommandCode = "";

String parentNode = "jomartolentino2002@gmailcom";

bool isIdle = true;

bool isNoObstacle = true;

bool receivingChunks = false;
String concatString = "";

WiFiManager wm;

void streamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println(F("Stream timeout, resume streaming..."));
  }
}

// Stream callback for custom_controls path
void streamCallback1(FirebaseStream data) {
  String path = data.dataPath();
  String val = data.stringData();

  Serial.println("Data changed in stream: >>>");
  Serial.println("PATH: " + path);
  Serial.println("VALUE: " + val);

  if (!val.isEmpty()) {
    if (path.equals("/command_code")) {
      isIdleCheck(val);
      
      if (val.equals("F") || val.equals("L") || val.equals("R") || val.equals("B")) {
        Serial2.println(val);
        prevCommandCode = val;
      } else if (val != prevCommandCode) {
        Serial2.println(val);
        prevCommandCode = val;
      }
    } else if (path.equals("/is_no_obstacle")) {

      if (val == "true" ) {
        Serial.println("isNoObstacle Updated");
        isNoObstacle = true;
        Serial2.println("T");
        Serial.println("Updated isNoObstacle: " + isNoObstacle);
      } else {
        Serial.println("isNoObstacle NOT Updated");
      }
    }
    digitalWrite(firebaseLED, LOW);
    delay(50);
  }
}

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, RXp2, TXp2);

  pinMode(batteryLED, OUTPUT);
  pinMode(wifiLED, OUTPUT);
  pinMode(firebaseLED, OUTPUT);
  pinMode(bluetoothLED, OUTPUT);
  pinMode(powerLED, OUTPUT);
  pinMode(relayPin, OUTPUT);
  digitalWrite(powerLED, HIGH);

  wifi_connect();
  setup_firebase();
  delay(1500);

  SerialBT.begin("SR_001");
}

void loop() {
  // Serial.println("Loooooooppppiiinnggg");
  // Serial.println("Loooooooppppiiinnggg2");

  if (!isNoObstacle) {
    isNoObstacleCheck();
  }

  if (Serial.available()) {
      Serial.println("Message sent!");
      Serial2.println(Serial.readStringUntil('\n'));
    }

  if (Serial2.available()) {
    String val = Serial2.readString();
    Serial.println("Serial2.readString(): ");
    val = removeNewlines(val);
    Serial.println(val);
    if (hasComma(val)) {
      Serial.println("HAS COMMA");
      String key = convertToKeyValue(val, 0);
      String keyValue = convertToKeyValue(val, 1);
      keyValue = removeNewlines(keyValue);
      if (key.equals("moisture")) {
        updateMoistureValue(val);
      } else if (key.equals("is_automatic_mode")) {
        if (SerialBT.hasClient()) {
          SerialBT.println(val);
        }
        if (Firebase.ready()) {
          Serial.printf("Set is_automatic_mode... %s\n", Firebase.RTDB.setString(&fbdo, parentNode + "/Device/is_automatic_mode", keyValue) ? "ok" : fbdo.errorReason().c_str());
        }
      } else if (key.equals("is_no_obstacle")) {
        isNoObstacle = false;
        if (SerialBT.hasClient()) {
          SerialBT.println(val);
        }
        if (Firebase.ready()) {
          Serial.printf("Set is_no_obstacle... %s\n", Firebase.RTDB.setString(&fbdo, parentNode + "/Device/is_no_obstacle", keyValue) ? "ok" : fbdo.errorReason().c_str());
        }
      } else if (key.equals("automatic_status")) {
        if (SerialBT.hasClient()) {
          SerialBT.println(val);
        }
        if (Firebase.ready()) {
          Serial.printf("Set automatic_status... %s\n", Firebase.RTDB.setString(&fbdo, parentNode + "/Device/automatic_status", keyValue) ? "ok" : fbdo.errorReason().c_str());
        }
      } else if (key.equals("obstacle")) {
        if (SerialBT.hasClient()) {
          SerialBT.println(val);
        }
      }
    } else {
      Serial.println("NOT HAVE COMMA");
    }
  }
  // Serial.println("Serial2 END");

  if (SerialBT.hasClient()) {
    digitalWrite(bluetoothLED, HIGH);
    if (SerialBT.available()) {
      // Serial.println("SerialBT.available()");
      String res = SerialBT.readStringUntil('\n');
      
      isIdleCheck(res);

      Serial.println(res);
      Serial2.println(res);
      
      blinkLED(bluetoothLED);
    }
  } else {
    // No BT client
    digitalWrite(bluetoothLED, LOW);
  }
  // Serial.println("SerialBT END");

  if (WiFi.status() == WL_CONNECTED) {
    digitalWrite(wifiLED, HIGH);
    if (Firebase.ready()) {
      digitalWrite(firebaseLED, HIGH);

      if (millis() - sendDataPrevMillis1 >= 10000 && isIdle) {
        setActiveTimestamp();
        sendDataPrevMillis1 = millis();
      }
    } else {
      digitalWrite(firebaseLED, LOW);
    }
  } else {
    // WiFi not connected
    digitalWrite(wifiLED, LOW);
    digitalWrite(firebaseLED, LOW);
  }
  // Serial.println("Firebase END");

  if (millis() - sendDataPrevMillis2 >= 90000 && isIdle) {
    checkBattery();
    solarVoltStatus();
    Serial.println("Check battery END");
    sendDataPrevMillis2 = millis();
  }
  
  if (isCharging) {
    blinkLED(batteryLED);
  } else {
    digitalWrite(batteryLED, LOW);
  }
  // Serial.println("Loop ENDDDDDDDDDDDD");
}

void isNoObstacleCheck() {
  if (Firebase.ready())
  {
    // Retrieve the value from Firebase
    if (Firebase.RTDB.getJSON(&fbdo, parentNode + "/Device"))
    {
        FirebaseJson &json = fbdo.jsonObject();
        FirebaseJsonData jsonData;
        json.get(jsonData, "/is_no_obstacle"); // Get the value of the key "value"

        if (jsonData.success)
        {

          String val = jsonData.to<String>();
          Serial.print("Values: ");
          Serial.println(val);

          if (val == "true" ) {
            Serial.println("isNoObstacle Updated");
            isNoObstacle = true;
            Serial2.println("T");
            Serial.println("Updated isNoObstacle: " + isNoObstacle);
          } else {
            Serial.println("isNoObstacle NOT Updated");
          }
        }
        else
        {
            Serial.println("Failed to get the value");
        }
    }
    else
    {
        Serial.println(fbdo.errorReason());
    }
  }
}

void isIdleCheck(String code) {
  if (code.equalsIgnoreCase("D") || code.equalsIgnoreCase("M") || code.equalsIgnoreCase("P")) {
    isIdle = false;
  } else if (code.equalsIgnoreCase("X")) {
    isIdle = true;
  }
}


void updateParentNode() {
  if (Firebase.ready())
  {
    // Retrieve the value from Firebase
    if (Firebase.RTDB.getJSON(&fbdo, "/Released SolaRice"))
    {
        FirebaseJson &json = fbdo.jsonObject();
        FirebaseJsonData jsonData;
        json.get(jsonData, "/SR_001"); // Get the value of the key "value"

        if (jsonData.success)
        {

          String val = jsonData.to<String>();
          Serial.print("Values: ");
          Serial.println(val);

          if (val != parentNode) {
            Serial.println("Parent Node Updated");
            parentNode = val;
            Serial.println("Updated Node: " + parentNode);
          } else {
            Serial.println("Parent Node NOT Updated");
          }
        }
        else
        {
            Serial.println("Failed to get the value");
        }
    }
    else
    {
        Serial.println(fbdo.errorReason());
    }
  }
}


String removeNewlines(String str) {
  String result = "";
  for (int i = 0; i < str.length(); i++) {
    if (str[i] != '\n' && str[i] != '\r') {
      result += str[i];
    }
  }
  return result;
}

// Methods
void updateMoistureValue(String dataString) {
  String value = convertToKeyValue(dataString, 1);
  String msg;

  if (value.toInt() < wetSoil) {
    Serial.println("Status: Rice grain has high moisture");
    msg = "Rice grain has high moisture";
  } else if (value.toInt() >= wetSoil && value.toInt() < drySoil) {
    Serial.println("Status: Rice grain has low moisture");
    msg = "Rice grain has low moisture";
  } else {
    Serial.println("Status: Rice grain moisture is at optimal level!");
    msg = "Rice grain moisture is at optimal level!";
  }

  if (SerialBT.hasClient()) {
    SerialBT.println(dataString + "," + msg);
  }

  if (Firebase.ready()) {
    String cur_time = getCurrentTime();

    FirebaseJson json;
    json.add("value", value);
    json.add("description", msg);
    json.add("timestamp", cur_time);

    Serial.printf("Set moisture... %s\n", Firebase.RTDB.setJSON(&fbdo, parentNode + "/Moisture Level/" + cur_time, &json) ? "ok" : fbdo.errorReason().c_str());
  }
}

String convertToKeyValue(String strData, int index) {
  String listData[3];

  int startPos = 0;
  int separatorPos;
  int currentIndex = 0;

  while ((separatorPos = strData.indexOf(',', startPos)) != -1 && currentIndex < 2) {
    listData[currentIndex] = strData.substring(startPos, separatorPos);
    startPos = separatorPos + 1;
    currentIndex++;
  }

  listData[currentIndex] = strData.substring(startPos);

  if (index >= 0 && index < 3) {
    return listData[index];
  } else {
    return "";
  }
}


void checkBattery() {
  // Update data every 30 seconds
  // if the percentage is equal or less than the minimum voltage, then charge the battery
  // otherwise, update stop charging
  float percentList[5];
  for (int i = 0; i < 5; i++) {
    int analogValue = analogRead(batteryVoltSensor);
    float voltageAtPin = (analogValue * VREF) / ADC_RESOLUTION;
    float batteryVoltage = voltageAtPin * (R1 + R2) / R2;
    if (batteryVoltage != 0) {
      batteryVoltage += OFFSET_VALUE;
    }
    int batteryPercentage;

    if (batteryVoltage < 12.89 && batteryVoltage > 12.79) {
      batteryPercentage = mapFloat(batteryVoltage, 12.79, 12.89, 91, 100);
    } else if (batteryVoltage < 12.78 && batteryVoltage > 12.66) {
      batteryPercentage = mapFloat(batteryVoltage, 12.66, 12.78, 81, 90);
    } else if (batteryVoltage < 12.65 && batteryVoltage > 12.77) {
      batteryPercentage = mapFloat(batteryVoltage, 12.52, 12.65, 71, 80);
    } else if (batteryVoltage < 12.51 && batteryVoltage > 12.42) {
      batteryPercentage = mapFloat(batteryVoltage, 12.42, 12.51, 61, 70);
    } else if (batteryVoltage < 12.41 && batteryVoltage > 12.24) {
      batteryPercentage = mapFloat(batteryVoltage, 12.24, 12.41, 51, 60);
    } else if (batteryVoltage < 12.23 && batteryVoltage > 12.12) {
      batteryPercentage = mapFloat(batteryVoltage, 12.12, 12.23, 41, 50);
    } else if (batteryVoltage < 12.11 && batteryVoltage > 11.97) {
      batteryPercentage = mapFloat(batteryVoltage, 11.97, 12.11, 31, 40);
    } else if (batteryVoltage < 11.96 && batteryVoltage > 11.82) {
      batteryPercentage = mapFloat(batteryVoltage, 11.82, 11.96, 21, 30);
    } else if (batteryVoltage < 11.81 && batteryVoltage > 11.71) {
      batteryPercentage = mapFloat(batteryVoltage, 11.71, 11.81, 11, 20);
    } else if (batteryVoltage < 11.70 && batteryVoltage > 11.64) {
      batteryPercentage = mapFloat(batteryVoltage, 11.64, 11.70, 1, 10);
    } else {
      batteryPercentage = 0;
    }
    percentList[i] = batteryPercentage;
    // Print the values
    // Serial.print("i: ");
    // Serial.println(i);
    // Serial.print("Battery Voltage: ");
    // Serial.println(batteryVoltage, 2);
  }
  float percentSum = percentList[0] + percentList[1] + percentList[2] + percentList[3] + percentList[4];
  float averagePercentage = percentSum / 5;
  String msg = "";
  String cur_time = getCurrentTime();

  Serial.print("Battery percent: ");
  Serial.print(averagePercentage);
  Serial.println("%");

  if (averagePercentage <= 70.00) {
    // Charge the battery
    msg = "Charging started!";
    Serial.println("Charging started!");
    digitalWrite(relayPin, LOW);
    isCharging = true;
  } else {
    // Stop charging
    msg = "Charging stopped!";
    Serial.println("Charging stopped!");
    digitalWrite(relayPin, HIGH);
    isCharging = false;
  }
  String strPercent = String(averagePercentage);

  if (Firebase.ready()) {
    FirebaseJson json;
    json.add("percentage", strPercent);
    json.add("status", msg);
    json.add("timestamp", cur_time);

    Serial.printf("Set Battery Status History... %s\n", Firebase.RTDB.setJSON(&fbdo, parentNode + "/Battery/" + cur_time, &json) ? "ok" : fbdo.errorReason().c_str());
  }

  if (SerialBT.hasClient()) {
    SerialBT.println("percentage," + strPercent);
    delay(500);
  }

  // Serial2.println("percentage " + strPercent);
  
}

void solarVoltStatus() {
  int analogValue = analogRead(solarPanelVoltSensor);
  float voltageAtPin = (analogValue * VREF) / ADC_RESOLUTION;
  float solarpanelVoltage = voltageAtPin * (R1 + R2) / R2;

  String voltStatus = "";
  if (solarpanelVoltage >= 12.00) {
    voltStatus = "normal";
  } else if (solarpanelVoltage >= 11.00 && solarpanelVoltage <= 12.00) {
    voltStatus = "low";
  } else {
    voltStatus = "critical";
  }
  if (SerialBT.hasClient()) {
    SerialBT.println("panel_status," + voltStatus);
    delay(500);
  }

  if (Firebase.ready()) {
    // Serial.printf("Set solar_panel_volt_status... %s\n", Firebase.RTDB.setString(&fbdo, "/Device/solar_panel_volt_status", " ") ? "ok" : fbdo.errorReason().c_str());
    Serial.printf("Set solar_panel_volt_status... %s\n", Firebase.RTDB.setString(&fbdo, parentNode + "/Device/solar_panel_volt_status", voltStatus) ? "ok" : fbdo.errorReason().c_str());
  }
  
}

int mapFloat(float x, float in_min, float in_max, int out_min, int out_max) {
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

bool hasComma(const String& str) {
  const char* str_char_array = str.c_str();
  return strchr(str_char_array, ',') != nullptr;
}

void setup_firebase() {
  Serial.println("setup_firebase STARTEDDDDD");
  if (WiFi.status() == WL_CONNECTED) {
    digitalWrite(wifiLED, HIGH);
    blinkLED(firebaseLED);

    // Serial.printf("Firebase Client v%s\n\n" FIREBASE_CLIENT_VERSION);
    config.api_key = API_KEY;

    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;

    config.database_url = DATABASE_URL;

    config.token_status_callback = tokenStatusCallback;

    Firebase.begin(&config, &auth);

    updateParentNode();
    delay(3000);
    String currentParentNode = "/" + parentNode + "/Device";
    Serial.print("currentParentNode: ");
    Serial.println(currentParentNode);

    if (!Firebase.RTDB.beginStream(&fbdo, currentParentNode)) {
      Serial.println("Could not begin stream for command_code");
      Serial.println("REASON: " + fbdo.errorReason());
      blinkLED(firebaseLED);
    }

    // Set stream callbacks for both paths
    Firebase.RTDB.setStreamCallback(&fbdo, streamCallback1, streamTimeoutCallback);

  } else {
    digitalWrite(wifiLED, LOW);
    digitalWrite(firebaseLED, LOW);
    WiFi.disconnect(true);

  }
  // Serial.println("setup_firebase ENDDDDDDD");
}

void wifi_connect() {
  // Set WiFiManager configurations
  wm.setConfigPortalTimeout(30);
  wm.setBreakAfterConfig(true);  // Exit after configuration is saved

  // Start the AP if WiFi is not connected
  if (!wm.autoConnect("SR_001_AP")) {
    Serial.println("Failed to connect to WiFi. Starting AP...");
    // isAPActive = true;
    // startTime = millis();
  } else {
    if (WiFi.status() == WL_CONNECTED) {
      configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
      delay(200);
      String ret = getCurrentTime();
      Serial.println(ret);

      Serial.print("Connected with IP: ");
      Serial.println(WiFi.localIP());
      digitalWrite(wifiLED, HIGH);

    } else {
      digitalWrite(wifiLED, LOW);
      // WiFi.disconnect(true);
      // wm.
      Serial.println("WiFi not connected");
      Serial.println("Firebase functions are unavailable");
    }
  }
}

void setActiveTimestamp() {
  int64_t curr_timestamp = getTimestamp();
  Serial.print("5: Set json...: ");
  bool isSetSuccess = Firebase.RTDB.setString(&fbdo, parentNode + "/Device/last_active_time", String(curr_timestamp));
  Serial.print("isSetSuccess: ");
  Serial.println(bool(isSetSuccess));
  if (isSetSuccess) {
    Serial.println("ok");
    digitalWrite(firebaseLED, HIGH);
  } else {
    digitalWrite(firebaseLED, LOW);
    Serial.println(fbdo.errorReason().c_str());
  }
}

int64_t getTimestamp() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return (tv.tv_sec * 1000LL + (tv.tv_usec / 1000LL));
}

String getCurrentTime() {
  struct tm timeinfo;
  if (getLocalTime(&timeinfo)) {
    char timeString[30];
    strftime(timeString, sizeof(timeString), "%Y-%m-%d;%H:%M:%S", &timeinfo);

    unsigned long currentMillis = millis();
    int milliseconds = currentMillis % 1000;

    char milliStr[4];
    sprintf(milliStr, "%03d", milliseconds);

    strcat(timeString, ":");
    strcat(timeString, milliStr);

    Serial.println(timeString);
    return String(timeString);
  } else {
    return String("error");
  }
}

void blinkLED(int pin) {
  digitalWrite(pin, HIGH);
  delay(50);
  digitalWrite(pin, LOW);
  delay(50);
}
