// —— 一定要最前面定义，不要放到任何 include 之后 —— 
#define TINY_GSM_MODEM_A7672X

#include <TinyGsmClient.h>
#include <ArduinoBearSSL.h>
#include <BearSSLHelpers.h> 
#include <SSLClient.h>
#include <PubSubClient.h>
#include "identification.h"

// 板载串口映射：ESP32 UART1 ↔ A7670X
#define MODEM_TX 27   // ESP32 → 模块 RXD
#define MODEM_RX 26   // ESP32 ← 模块 TXD

HardwareSerial SerialAT(1);
TinyGsm modem(SerialAT);
TinyGsmClient  rawClient(modem);

// 把 PROGMEM 里的字符串拷到 RAM
String caStr   = aws_root_ca;
String certStr = client_cert;
String keyStr  = private_key;

// BearSSL 对象
BearSSLX509List   caList(caStr.c_str());
BearSSLX509List   clientCert(certStr.c_str());
BearSSLPrivateKey pKey(keyStr.c_str());

// SNI Host
const char* mqttHost = AWS_ENDPOINT;

// SSLClient：底层用 rawClient，携带 CA、客户端证书、私钥、SNI
SSLClient secureClient(rawClient, caList, pKey, clientCert, mqttHost);

// MQTT 客户端
PubSubClient mqtt(secureClient);

// 你的运营商 APN
const char apn[]  = "uk.lebara.mobi";
const char user[] = "WAP";
const char pass[] = "WAP";

void setup() {
  Serial.begin(115200);
  delay(100);
  SerialAT.begin(115200, SERIAL_8N1, MODEM_RX, MODEM_TX);

  Serial.println("Modem restart…");
  modem.restart();
  if (!modem.waitForNetwork()) {
    Serial.println("Network registration failed");
    while (true);
  }
  Serial.println("Network OK");

  Serial.print("Connecting GPRS ");
  Serial.print(apn);
  if (!modem.gprsConnect(apn, user, pass)) {
    Serial.println(" fail");
    while (true);
  }
  Serial.println(" success");

  // TLS 握手超时可选调整  
  secureClient.setHandshakeTimeout(15 * 1000);

  // 配置 MQTT
  mqtt.setServer(mqttHost, 8883);
  mqtt.setCallback([](char* topic, byte* payload, unsigned int len) {
    Serial.print("Recv [");
    Serial.print(topic);
    Serial.print("]: ");
    Serial.write(payload, len);
    Serial.println();
  });
}

void loop() {
  if (!mqtt.connected()) {
    Serial.print("MQTT connecting…");
    if (mqtt.connect("TA7670Client")) {
      Serial.println(" connected");
      mqtt.subscribe("test/topic");
    } else {
      Serial.print(" failed, rc=");
      Serial.println(mqtt.state());
      delay(5000);
      return;
    }
  }
  // 发布示例
  mqtt.publish("test/topic", "hello from TA7670");
  mqtt.loop();
  delay(10000);
}