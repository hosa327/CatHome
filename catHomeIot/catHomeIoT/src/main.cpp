#include "arduino_secrets.h"
#include <Arduino.h>
#include <WiFiNINA.h>
#include <ArduinoBearSSL.h>
#include <ArduinoMqttClient.h>
#include <Arduino_MKRENV.h>  
#include <DFRobot_HX711_I2C.h> 
#include <Wire.h>
#include <string>
#include <ArduinoJson.h>
#include <Ultrasonic.h>
#include <TimeLib.h> 


const char ssid[] = SECRET_SSID; 
const char pass[] = SECRET_PASS; 
const char broker[] = SECRET_BROKER; 
const char* certificate = SECRET_CERTIFICATE;
 

WiFiClient wifiClient; 

// Used for the TCP socket connection 

BearSSLClient sslClient(wifiClient);

// Used for SSL/TLS connection, integrates with ECC508 

MqttClient mqttClient(sslClient);


Ultrasonic ultrasonic(4);
float lastValidDistance = -1;

// Weight sensor
DFRobot_HX711_I2C MyScale;






// data
JsonDocument doc;
JsonDocument incoming_doc;
String message = "";
String incoming_message = "";

String catName = "Mimi"; 

float weight = 0.0;
float ex_weight = 0.0;

float weightThreshold = 100;

bool waterNeeded = false;
bool exWaterNeeded = false;

float ex_temp;
float temp;

float defaultDistance;
float ex_distance;
float distance;

float distanceThreshold = 50;
bool enter;

int poopTime;
long beginTime;
long duration;

String lastDate = "";


float TwoDecimal(float number) {
  return round(number * 100) / 100.0;
}


float ultrasound_sensor(){
  lastValidDistance = ultrasonic.MeasureInCentimeters();
  return lastValidDistance;
}


float temp_sensor(){
  float temperature = ENV.readTemperature();
  return temperature;
}


JsonDocument ini_doc(JsonDocument doc){
  doc.clear();

  doc["catName"] = catName;
  
  return doc;
}




unsigned long getTime() { 
  // get the current time from the WiFi module
  return WiFi.getTime();
}

String getDateTime() {
  unsigned long epoch = 0;
  do {
    epoch = WiFi.getTime();    
    delay(200);
  } while (epoch == 0);

  setTime(epoch);              
  char buf[20];
  snprintf(buf, sizeof(buf),
           "%02d-%02d-%04d %02d:%02d:%02d",
           day(), month(), year(),
           hour(), minute(), second());
  return String(buf);
}

String extractDate(const String& dateTime) {
  return dateTime.substring(0, 10);
}


void connectWiFi(){
  Serial.print("Attempting to connect to the WiFi: "); 
  Serial.print(ssid); 
  Serial.print(" ");

  bool connected = false; int retryCount = 0; const int maxRetries = 10;

  while (!connected && retryCount< maxRetries){
    if (WiFi.begin(ssid, pass) == WL_CONNECTED){
      connected = true;
    }
    else {
      Serial.print("."); 
      delay(1000); retryCount++; 
    } 
  }
  if (connected){ 
    Serial.println(); 
    Serial.println("You're connected to the network"); 
    Serial.println(); } else { Serial.println(); 
    Serial.println("Failed to connect to the network after multiple attempts."); 
  }
}

void connectMQTT() { 
  Serial.print("Attempting to MQTT broker: "); 
  Serial.print(broker); 
  Serial.println(" ");

  while (!mqttClient.connect(broker, 8883)) {
    // failed
    Serial.print("."); 
    delay(3000); 
  }

  Serial.println();
  Serial.println("You're connected to the MQTT broker"); Serial.println();

  // subscribe to a topic
  mqttClient.subscribe("arduino/incoming"); 
}



void publishMessage(JsonDocument doc, String topic) { 
  // send message, the Print interface can be used to set the message contents
  serializeJson(doc, message);

  mqttClient.beginMessage(topic); // topic

  mqttClient.print(message);

  mqttClient.endMessage();                        

}







void onMessageReceived(int messageSize) { 
  // we received a message, print out the topic and contents 
  Serial.print("Received a message with topic '"); 
  Serial.print(mqttClient.messageTopic()); 
  Serial.print("', length "); 
  Serial.print(messageSize); 
  Serial.println(" bytes:");

  String incoming_message = "";
  // use the Stream interface to print the contents 
  while (mqttClient.available()) { 
    char ch = (char)mqttClient.read();
    Serial.print(ch);
    incoming_message += ch;
  } 
  Serial.println();
  Serial.println();

  deserializeJson(incoming_doc, incoming_message);



  if (incoming_doc["catName"].is<const char*>()) {
      catName = incoming_doc["catName"].as<const char*>();
      doc["catName"] = catName;
      Serial.print("catName: ");
      Serial.println(catName);
  }

  incoming_doc.clear();

  Serial.println();
  Serial.println();
}

void tempMeassure(){
  doc = ini_doc(doc);
  ex_temp = temp_sensor();
  ex_temp = TwoDecimal(ex_temp);
  String str_temp = String(ex_temp);
  doc["temperature"] = str_temp;
  doc["time"] = getDateTime();
  publishMessage(doc, "temperature");
}

void weightMeassure(){
  doc = ini_doc(doc);
  ex_weight = MyScale.readWeight();
  ex_weight = TwoDecimal(ex_weight);
  String str_weight = String(ex_weight);

  if (ex_weight < weightThreshold){
    exWaterNeeded = true;
  }
  String strWaterRefill = exWaterNeeded ? "true" : "false";

  doc["weight"] = str_weight;
  doc["waterNeeded"] = strWaterRefill;
  doc["time"] = getDateTime();
  
  publishMessage(doc, "water");
}



void setup() { 
  Serial.begin(115200);
  delay(5000);
  Serial.println("Setting up...");
  if (!ENV.begin()) {
      Serial.println("MKR ENV Shield Initialise Failed");
  }

  if (!MyScale.begin()) {
      Serial.println("Weight Sensor Initialise failed");
  } 
  else {
      Serial.println("Weight Sensor Initialise success");
      MyScale.setCalWeight(220);
      MyScale.setThreshold(5);
      MyScale.enableCal();
      // MyScale.setCalibration(2300); 
      
      MyScale.setCalibration(MyScale.getCalibration());
      
  }




  // Set a callback to get the current time 
  // used to validate the servers certificate

  ArduinoBearSSL.onGetTime(getTime);

  // Set the ECCX08 slot to use for the private key 
  // and the accompanying public certificate for it 

  sslClient.setEccSlot(0, certificate);

  // Optional, set the client id used for MQTT, 
  // each device that is connected to the broker 
  // must have a unique client id. The MQTTClient will generate 
  // a client id for you based on the millis() value if not set

  mqttClient.setId("catHomeArduino");

  // Set the message callback, this function is 
  // called when the MQTTClient receives a message 

  mqttClient.onMessage(onMessageReceived); 

  while (WiFi.status() != WL_CONNECTED) { 
    int numNetworks = WiFi.scanNetworks(); 
    Serial.println("Discovered " + String(numNetworks) + " Networks"); 
    connectWiFi(); 
  }
  
  while (!mqttClient.connected()) { 
    // MQTT client is disconnected, connect 
    connectMQTT(); 
  }

  Wire.begin();

  doc = ini_doc(doc);

  enter = false;
  poopTime = 0;
  duration = 0;  

  tempMeassure();
  weightMeassure();

  Serial.print("Temperature: ");
  Serial.println(ex_temp);
  Serial.print("Weight: ");
  Serial.println(ex_weight);
  Serial.print("WaterNeeded: ");
  Serial.println(exWaterNeeded ? "true" : "false");

  String now = getDateTime();
  lastDate = extractDate(now);
}








void loop() {
  if (WiFi.status() != WL_CONNECTED) { 
    int numNetworks = WiFi.scanNetworks(); 
    Serial.println("Discovered " + String(numNetworks) + " Networks"); 
    connectWiFi(); 
  }
  
  if (!mqttClient.connected()) { 
    // MQTT client is disconnected, connect 
    connectMQTT(); 
  }


  temp = temp_sensor();
  temp = TwoDecimal(temp);
  if(abs(temp - ex_temp) > 0.1){
    doc = ini_doc(doc);
    Serial.print("Temperature: ");
    Serial.println(temp);
    
    String str_temp = String(temp);
    doc["temperature"] = str_temp + "(℃)";
    doc["time"] = getDateTime();
    publishMessage(doc, "temperature");
    ex_temp = temp;
  }


  distance = ultrasound_sensor();
  if(distance < distanceThreshold && !enter){
    enter = true;
    Serial.println(distance);
    beginTime = millis();;
  }else if((distance > distanceThreshold) && (enter == true)){
    duration = millis() - beginTime;
    if (duration > 1000) {
      poopTime += 1;
      enter = false;

      doc = ini_doc(doc);
      doc["poopTime"] = String(poopTime);
      doc["duration"] = String(duration/1000) + "(s)";
      doc["time"] = getDateTime();
      publishMessage(doc, "poop");
      Serial.println("out");
    }else{
      enter = false;
    }

  }


  weight = MyScale.readWeight();
  ex_weight = TwoDecimal(ex_weight);
  if(abs(weight - ex_weight) >= 1){
    doc = ini_doc(doc);
    
    Serial.print("weight: ");
    Serial.println(weight);

    String str_weight = String(weight);
    doc["weight"] = str_weight + ("g");

    ex_weight = weight;

    waterNeeded = (weight < weightThreshold);
    String strWaterRefill = waterNeeded ? "true" : "false";
    doc["waterNeeded"] = strWaterRefill;
    doc["time"] = getDateTime();
    publishMessage(doc, "water");
  }
  


  String now = getDateTime();  
  String today = extractDate(now);
  if (today != lastDate) {
    // newDay
    lastDate = today;
    poopTime = 0;

    doc = ini_doc(doc);
    doc["poopTime"] = String(0);
    doc["time"]     = 0;
    publishMessage(doc, "poop");
  }
  // poll for new MQTT messages and send keep alives 
  mqttClient.poll();
  
} 