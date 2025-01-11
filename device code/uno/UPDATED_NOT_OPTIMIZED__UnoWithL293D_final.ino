#include <SoftwareSerial.h>
// #include <Smartcar.h>
#include <AFMotor.h>
#include <Smartcar_sensors.h>
#include <Wire.h>

#define MAX_MESSAGE 30

// //initial motors pin
AF_DCMotor motor4(4, MOTOR34_1KHZ);
AF_DCMotor motor3(3, MOTOR34_1KHZ);
AF_DCMotor motor2(2, MOTOR12_64KHZ);

const int Speeed = 255;

long automaticDelayMillis1 = 0;
long automaticDelayMillis2 = 0;
long automaticDelayMillis3 = 0;
long currentDelay = 0;

bool is_no_obstacle = true;
bool fiveSecondsCheck = true;
bool isTurnLeft = false;
int remainingDelay = 0;
int isManual = 1;
int prevIsManual = 1;
int minLightValue = 400;
int forwardDelay = 5000;
int turnCounter = 0;
int lightValue;
String manualMotorCmd = "";

String startTime = "";
String endTime = "";

const int lightSensor = A0;
const int capacitiveSensor = A3;
const int conveyorLight = 13;

Odometer encoder;
Gyroscope gyro;
const int odo_pin = 2;

Sonar sonar;
const int trig_pin = A1;
const int echo_pin = A2;

//Global variables
volatile int counter;
boolean obstacle;
int index = 0;

bool receivingChunks = false;
String concatString = "";

SoftwareSerial espSerial(9, 10); // RX, TX for serial communication

void setup() {
  Serial.begin(9600);
  espSerial.begin(9600); //115200
  pinMode(conveyorLight, OUTPUT);
  digitalWrite(conveyorLight, LOW);
  sonar.attach(trig_pin, echo_pin);
  // encoder.attach(odo_pin);
  gyro.attach();

  // encoder.begin();
  Serial.println("UNO started");
}

void loop() {
  // Serial.println(abs(gyro.getAngularDisplacement()));
  // Serial.println("looping");

  if (Serial.available()) {
    String msg = Serial.readStringUntil('\n');
    espSerial.println(msg);
    Serial.println("Sent!");
  }

  processMotorCmd();

  if (espSerial.available() > 0) {
    Serial.println("espSerial.available()");
    // Serial.println(espSerial.readStringUntil('\n'));

    if (espSerial.peek() == '[' || receivingChunks) {
      String res = espSerial.readStringUntil('\n');
      String trimmedStr = removeNewlines(res);

      // Serial.println(trimmedStr);
      if (trimmedStr.indexOf("]") != -1) {
        receivingChunks = false;
        
        Serial.println("close");
        concatString += trimmedStr;
        Serial.println(concatString);

        reset();
        drawMode();
        concatString = "";
      } else {
        Serial.println("adding");
        receivingChunks = true;
        concatString += trimmedStr;
        delay(100);
      }

    } else {
      String res = espSerial.readStringUntil('\n');
      res = removeNewlines(res);
      Serial.println(res);

      if (res.length() == 1) {
        if (res.equals("F") || res.equals("B") || 
        res.equals("L") || res.equals("R") || res.equals("S")) {
          manualMotorCmd = res;
          Serial.println("Motorrr");
        } else if (res.equals("D")) { // manual
          isManual = 1;
          reset();
          manualMotorCmd = "S";
        } else if (res.equals("M")) { // automatic
          isManual = 0;
          reset();
          manualMotorCmd = "S";
        } else if (res.equals("P")) { // drawing
          isManual = 2;
          reset();
          manualMotorCmd = "S";
        } else if (res.equals("O")) {
          updateMoistureLevel();
        } else if (res.equals("T")) {
          is_no_obstacle = true;
        } else if (res.equals("X")) {
          is_no_obstacle = true;
          isManual = 1;
          manualMotorCmd = "S";
        }
      }
    }
  }

  if (isManual != prevIsManual) {
    if (isManual == 0) {
      digitalWrite(conveyorLight, HIGH);
      automaticMode();
    } else if (isManual == 1) {
      stopAutomaticMode();
      digitalWrite(conveyorLight, LOW);
    } else if (isManual == 2) {
      stopAutomaticMode();
      digitalWrite(conveyorLight, LOW);
    }
    prevIsManual = isManual;
  }
}

void processMotorCmd() {
  if (isManual) {
    if (manualMotorCmd.equals("F") || manualMotorCmd.equals("L") || manualMotorCmd.equals("R")) {
      checkObstacle();
      if (is_no_obstacle) {
        if (manualMotorCmd.equals("F")) {
          isManual = 1;
          forward();
        }

        if (manualMotorCmd.equals("L")) {
          isManual = 1;
          left();
        }

        if (manualMotorCmd.equals("R")) {
          isManual = 1;
          right();
        }
        delay(100);
      }
    } else {
      if (manualMotorCmd.equals("B")) {
        isManual = 1;
        backward();
        delay(50);
      }
      
      if (manualMotorCmd.equals("S")) {
        stop();
      }
    }
  }
}

void drawMode() {
  Serial.println("Drawing Mode! 1");

  String inputData = concatString;
  Serial.println("Before Input: ");
  Serial.println(inputData);
  
  // Initialize a queue based on the predefined input string
  int arrayLength = inputData.substring(1, inputData.indexOf('!')).toInt(); // Extract the number of instructions
  String queue[arrayLength];
  int k = 0;

  Serial.print("arrayLength: ");
  Serial.println(arrayLength);

  // Parse and store instructions in the queue array
  int currentIndex = inputData.indexOf('!') + 1; // Start parsing after the '!'

  while (k < arrayLength) {
      int nextAsterisk = inputData.indexOf('*', currentIndex);
      if (nextAsterisk == -1) break; // No more instructions
      queue[k] = inputData.substring(currentIndex, nextAsterisk);
      currentIndex = nextAsterisk + 1; // Move to the next instruction
      k++;
  }

  // Serial.print("queue: ");
  // Serial.println(queue);

  // Interpret and execute instructions
  for (index = 0; index < arrayLength && !obstacle; index++) {
    // Interpret
    String instr = "";
    String parameter = "";

    Serial.print("queue[index]: ");
    Serial.println(queue[index]);
    // Serial.println(queue[index].length());

    int spaceIndex = queue[index].indexOf(' '); // Find the index of the space character
    Serial.println(queue[index]);
    if (spaceIndex != -1) {
        instr = queue[index].substring(0, spaceIndex); // Extract the instruction (before the space)
        parameter = queue[index].substring(spaceIndex + 1); // Extract the parameter (after the space)
    } else {
        instr = queue[index]; // If no space, the entire string is the instruction
    }

    Serial.print("Instr: ");
    instr.replace(",","");
    Serial.println(instr);
    

    Serial.print("Parameter: ");
    Serial.println(parameter);

    // Execute
    int value = parameter.toInt(); // Convert the parameter to an integer
    if (instr.equals("f")) {
        goForwardSafe(value);
        Serial.print("for loop: 1");
    } else if (instr == "rc") {
      Serial.print("for loop: 2");
        rotateClockwise(value);
        Serial.print("for loop: 3");
    } else if (instr == "rcc") {
      Serial.print("for loop: 4");
      rotateCounterClockwise(value);
      Serial.print("for loop: 5");
    }
  }
}

void rotateCounterClockwise(int desiredDisplacement){
  gyro.begin();
  while(abs(gyro.getAngularDisplacement()) <= desiredDisplacement){
    Serial.println(abs(gyro.getAngularDisplacement()));
    left();
  }
  stop();
  gyro.stop();
}

void rotateClockwise(int desiredDisplacement) {
  gyro.begin();
  while(abs(gyro.getAngularDisplacement()) <= desiredDisplacement) {
    Serial.println(abs(gyro.getAngularDisplacement()));
    right();
  }
  stop();
  gyro.stop();
}

//go forward with detect obstacle function working
void goForwardSafe(int desiredDistance){
  encoder.attach(odo_pin);
  encoder.begin();
  delay(200);
  while(encoder.getDistance() <= desiredDistance && isFrontClear()){
    Serial.println("encoder inside");
    Serial.println(encoder.getDistance());
    forward();
  }
  stop();
  encoder.detach();
}

//detect obstacle function.
boolean isFrontClear() {
  int distance = sonar.getDistance();
  
  if(distance < 25 && distance != 0){
    counter++;
  }
  
  if(counter >= 3){
    
    String str = "obstacle,";
    str += index;

    brake();

    Serial.println(str);
    espSerial.println(str);
    
    obstacle = true;
    return false;
  }
  return true;
}

//brake safe in front of the obstacle.
void brake() {
  stop();
  delay(50);
  backward();
  delay(75);
  stop();
}

//reset variables.
void reset(){
  counter = 0;
  obstacle = false;
}

void  updateMoistureLevel() {
  // Read the Analog Input and print it
  int moisture = analogRead(capacitiveSensor);
  // Serial.print("Analog output: ");
  // Serial.println(moisture);
  
  String result = "moisture," + String(moisture);
  // Serial.println(result);

  espSerial.print(result);
  delay(200);
}

void notifyAautomaticStop() {
  espSerial.println("is_automatic_mode,false");
}

void notifyAautomaticStatusFailed() {
  espSerial.println("automatic_status,failed");
}

void notifyAautomaticStatusSuccess() {
  espSerial.println("automatic_status,success");
}

void notifyObstacleDetected() {
  // Serial.println("notifyObstacleDetected()!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
  espSerial.println("is_no_obstacle,false");
  delay(200);
}

void checkObstacle() {
  int distance = sonar.getDistance();
  // Serial.print("Ultrasonic sensor: ");
  // Serial.println(distance);

  if (distance > 0 && distance <= 20) {
    // Serial.println("Alert: There's an obstacle in front!");
    if (is_no_obstacle) {
      notifyObstacleDetected();
      is_no_obstacle = false;
      brake();
      
    }
  } else {
    // Serial.println("No obstacle detected!");
    if (!is_no_obstacle) {
      is_no_obstacle = true;
    }
  }
  delay(50);
}

void automaticMode() {
  // Serial.println("automaticMode Looping");
  if (is_no_obstacle) {
    int lightValue;
    if (fiveSecondsCheck) {
      // Serial.println("fiveSecondsCheck!!!!");
      long currentDelay = millis() - automaticDelayMillis1;
      if (automaticDelayMillis1 == 0 || currentDelay <= forwardDelay) {
        if (automaticDelayMillis1 == 0) {
          automaticDelayMillis1 = millis();
        }

        // Serial.print("currentDelay1: ");
        // Serial.println(currentDelay);
        checkObstacle();
        if (!is_no_obstacle) {
          // Serial.println("There's OBSTACLE!!");
          remainingDelay = currentDelay;
          automaticDelayMillis1 = 0;
          stop();
          return;
        }
        if (remainingDelay != 0) {
          // Serial.println("There's remaining delay");
          currentDelay = remainingDelay + (millis() - automaticDelayMillis1);
          if (currentDelay >= forwardDelay) {
            automaticDelayMillis1 = automaticDelayMillis1 - forwardDelay;
            remainingDelay = 0;
            // Serial.println("Remaining delay is 0");
          }
        }
        // Initial movement
        runConveyorMotor();
        forward();
        delay(300);
      } else {
        lightValue = analogRead(lightSensor);
        // Serial.print("lightValue: ");
        // Serial.println(lightValue);
        if (lightValue > minLightValue) {
          if (turnCounter < 5) {
            notifyAautomaticStatusFailed();
          } else {
            notifyAautomaticStatusSuccess();
          }
          isManual = 1;
          return;
        }
        fiveSecondsCheck = false;
      }
    } else {
      if (!isTurnLeft) {
        // FORWARDDDDDDDDDDDDDDDD
        lightValue = analogRead(lightSensor);
        // Serial.print("lightValue: ");
        // Serial.print(lightValue);
        if (lightValue < minLightValue) {
          checkObstacle();
          if (!is_no_obstacle) {
            // Serial.println("There's OBSTACLE!!");
            stop();
            return;
          }
          // Serial.println("There's Rice");
          // Serial.println("There's Rice");
        
          runConveyorMotor();
          forward();
          delay(300);
          // FORWARDDDDDDDDDDDDDDDD END
        } else {
          // Serial.println("No Rice");
          isTurnLeft = true;
        }
      } else {
        // LEFTTTTTTTTTTTTTTTTTT
        long currentDelay = millis() - automaticDelayMillis2;
        if (automaticDelayMillis2 == 0 || currentDelay <= forwardDelay) {
          if (automaticDelayMillis2 == 0) {
            automaticDelayMillis2 = millis();
          }

          // Serial.print("currentDelay1: ");
          // Serial.println(currentDelay);
          checkObstacle();
          if (!is_no_obstacle) {
            // Serial.println("There's OBSTACLE!!");
            remainingDelay = currentDelay;
            automaticDelayMillis2 = 0;
            stop();
            return;
          }
          if (remainingDelay != 0) {
            // Serial.println("There's remaining delay");
            currentDelay = remainingDelay + (millis() - automaticDelayMillis2);
            if (currentDelay >= forwardDelay) {
              automaticDelayMillis2 = automaticDelayMillis2 - forwardDelay;
              remainingDelay = 0;
              // Serial.println("Remaining delay is 0");
            }
          }
          // Initial movement
          left();
          delay(300);
        } else {
          isTurnLeft = false;
          fiveSecondsCheck = true;
          resetDelayMillis();
          turnCounter++;
        }
      }
    }
  } else {
    // Serial.println("May HARANNGGGGGGGGGGGGGGGGG!!!!!!!!!");
  }
}

void stopAutomaticMode() {
  // Serial.println("stopAutomaticMode()!!!!!!!!!!!!!");
  stop();
  // Serial.println("MOTOR STOPEDDD!");
  notifyAautomaticStop();
  isManual = 1;
  fiveSecondsCheck = true;
  isTurnLeft = false;
  resetDelayMillis();
  // Serial.println("stopAutomaticMode()!!!!!!!!!!!!!");
}

void resetDelayMillis() {
  automaticDelayMillis1 = 0;
  automaticDelayMillis2 = 0;
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

void runConveyorMotor() {
  Serial.println("runConveyorMotor");
  motor2.setSpeed(Speeed);  //Define maximum velocity
  motor2.run(FORWARD);      //rotate the motor clockwise
}

void forward() {
  Serial.println("forward!");
  motor4.setSpeed(Speeed);  //Define maximum velocity
  motor4.run(FORWARD);      //rotate the motor clockwise
  motor3.setSpeed(Speeed);  //Define maximum velocity
  motor3.run(FORWARD);      //rotate the motor clockwise
}

void backward() {
  Serial.println("backward!");
  motor4.setSpeed(Speeed);  //Define maximum velocity
  motor4.run(BACKWARD);     //rotate the motor anti-clockwise
  motor3.setSpeed(Speeed);  //Define maximum velocity
  motor3.run(BACKWARD);     //rotate the motor anti-clockwise
}

void left() {
  Serial.println("left!");
  motor4.setSpeed(Speeed);  //Define maximum velocity
  motor4.run(BACKWARD);     //rotate the motor anti-clockwise
  motor3.setSpeed(Speeed);  //Define maximum velocity
  motor3.run(FORWARD);      //rotate the motor clockwise
}

void right() {
  Serial.println("right!");
  motor4.setSpeed(Speeed);  //Define maximum velocity
  motor4.run(FORWARD);      //rotate the motor clockwise
  motor3.setSpeed(Speeed);  //Define maximum velocity
  motor3.run(BACKWARD);     //rotate the motor anti-clockwise
}

void topleft() {
  Serial.println("topleft!");
  motor4.setSpeed(Speeed);        //Define maximum velocity
  motor4.run(FORWARD);            //rotate the motor clockwise
  motor3.setSpeed(Speeed / 3.1);  //Define maximum velocity
  motor3.run(FORWARD);            //rotate the motor clockwise
}

void topright() {
  Serial.println("topright!");
  motor4.setSpeed(Speeed / 3.1);  //Define maximum velocity
  motor4.run(FORWARD);            //rotate the motor clockwise
  motor3.setSpeed(Speeed);        //Define maximum velocity
  motor3.run(FORWARD);            //rotate the motor clockwise
}

void bottomleft() {
  Serial.println("bottomleft!");
  motor4.setSpeed(Speeed);        //Define maximum velocity
  motor4.run(BACKWARD);           //rotate the motor anti-clockwise
  motor3.setSpeed(Speeed / 3.1);  //Define maximum velocity
  motor3.run(BACKWARD);           //rotate the motor anti-clockwise
}

void bottomright() {
  Serial.println("bottomright!");
  motor4.setSpeed(Speeed / 3.1);  //Define maximum velocity
  motor4.run(BACKWARD);           //rotate the motor anti-clockwise
  motor3.setSpeed(Speeed);        //Define maximum velocity
  motor3.run(BACKWARD);           //rotate the motor anti-clockwise
}

void stop() {
  // Serial.println("stop!");
  motor4.setSpeed(0);   //Define minimum velocity
  motor4.run(RELEASE);  //stop the motor when release the button
  motor2.setSpeed(0);   //Define minimum velocity
  motor2.run(RELEASE);  //stop the motor when release the button
  motor3.setSpeed(0);   //Define minimum velocity
  motor3.run(RELEASE);  //stop the motor when release the button
}
