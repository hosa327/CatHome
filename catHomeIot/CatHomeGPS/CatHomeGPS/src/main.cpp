#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <TinyGsmClient.h>
#include <PubSubClient.h>
#include <TinyGPSPlus.h>

const char CAT_NAME[]       = "Mimi";        

// --- WiFi credentials ---
const char WIFI_SSID[]      = "YOUR_WIFI_SSID";
const char WIFI_PASSWORD[]  = "YOUR_WIFI_PASSWORD";

// --- 4G (SIM7600G) APN and SIM PIN ---
const char APN[]            = "YOUR_APN";       // e.g. "CMIOT" for China Mobile, "UNINET" for China Unicom
const char SIM_PIN[]        = "";               // Leave empty if no PIN

// --- AWS IoT Core settings ---
static const char AWS_IOT_ENDPOINT[] = "your-aws-endpoint.iot.REGION.amazonaws.com";
static const char THING_NAME[]       = "LilyGoTA7670E";

// --- Amazon Root CA (AmazonRootCA1.pem) ---
const char AWS_CERT_CA[] PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
MIIDQTCCAimgAwIBAgITBmyf3T4S...
... (insert full AmazonRootCA1.pem here) ...
-----END CERTIFICATE-----
)EOF";

// --- Device certificate (.crt.pem) ---
const char AWS_CERT_CRT[] PROGMEM = R"KEY(
-----BEGIN CERTIFICATE-----
MIIDWTCCAkGgAwIBAgIUV...
... (insert your device .crt.pem content here) ...
-----END CERTIFICATE-----
)KEY";

// --- Device private key (.private.key) ---
const char AWS_CERT_PRIVATE[] PROGMEM = R"KEY(
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAv...
... (insert your device .private.key content here) ...
-----END RSA PRIVATE KEY-----
)KEY";

TinyGPSPlus gps;
HardwareSerial gpsSerial(1); // UART1: GPS RX=GPIO12, TX=GPIO9

// Serial2 pins for SIM7600G on LilyGo TA7670E R2
#define MODEM_TX_PIN 26  // TX2 -> GPIO26
#define MODEM_RX_PIN 27  // RX2 -> GPIO27

TinyGsm modem(Serial2);
TinyGsmClientSecure gsmClient(modem);

WiFiClientSecure wifiClient;
PubSubClient mqttWiFi(wifiClient);

PubSubClient mqttGSM(gsmClient);

// Flag to track current network mode: true = WiFi, false = 4G
bool usingWiFi = false;

bool connectWifi() {
  Serial.print("Connecting to WiFi SSID: ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long startAttemptTime = millis();
  // Try for 15 seconds
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 15000) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected, IP address: ");
    Serial.println(WiFi.localIP());
    return true;
  } else {
    Serial.println("WiFi connection failed or timed out.");
    return false;
  }
}

bool connect4G() {
  Serial.println("Initializing 4G modem...");
  Serial2.begin(115200, SERIAL_8N1, MODEM_RX_PIN, MODEM_TX_PIN);
  delay(3000); // Allow modem to power up

  Serial.println("Testing AT commands...");
  if (!modem.restart()) {
    Serial.println("Failed to restart modem. Check wiring and power.");
    return false;
  }
  if (!modem.testAT()) {
    Serial.println("Modem did not respond to AT. Check wiring and power.");
    return false;
  }
  Serial.println("AT OK");

  // Unlock SIM if PIN is provided
  if (strlen(SIM_PIN) && modem.getSimStatus() != 3) {
    Serial.print("Unlocking SIM with PIN: ");
    Serial.println(SIM_PIN);
    modem.simUnlock(SIM_PIN);
  }

  Serial.println("Waiting for network registration...");
  if (!modem.waitForNetwork()) {
    Serial.println("Network registration failed. Check SIM card and antenna.");
    return false;
  }
  Serial.println("Network registered");

  Serial.print("Connecting to GPRS with APN: ");
  Serial.println(APN);
  if (!modem.gprsConnect(APN, "", "")) {
    Serial.println("GPRS connection failed. Check APN settings.");
    return false;
  }
  Serial.print("GPRS connected, IP: ");
  Serial.println(modem.getLocalIP());
  return true;
}

bool connectAwsMqttWifi() {
  Serial.println("Configuring WiFiClientSecure for AWS IoT...");
  wifiClient.setCACert(AWS_CERT_CA);
  wifiClient.setCertificate(AWS_CERT_CRT);
  wifiClient.setPrivateKey(AWS_CERT_PRIVATE);

  mqttWiFi.setServer(AWS_IOT_ENDPOINT, 8883);

  Serial.print("Attempting MQTT connection over WiFi to ");
  Serial.print(AWS_IOT_ENDPOINT);
  Serial.print(":8883 ... ");

  if (mqttWiFi.connect(THING_NAME)) {
    Serial.println("Connected.");
    return true;
  } else {
    Serial.print("Failed, state: ");
    Serial.println(mqttWiFi.state());
    return false;
  }
}

bool connectAwsMqttGSM() {
  Serial.println("Configuring GSM TLS client for AWS IoT...");
  gsmClient.setCACert(AWS_CERT_CA);
  gsmClient.setCertificate(AWS_CERT_CRT);
  gsmClient.setPrivateKey(AWS_CERT_PRIVATE);

  mqttGSM.setServer(AWS_IOT_ENDPOINT, 8883);

  Serial.print("Attempting MQTT connection over 4G to ");
  Serial.print(AWS_IOT_ENDPOINT);
  Serial.print(":8883 ... ");

  if (mqttGSM.connect(THING_NAME)) {
    Serial.println("Connected.");
    return true;
  } else {
    Serial.print("Failed, state: ");
    Serial.println(mqttGSM.state());
    return false;
  }
}

void publishLocation(double latitude, double longitude) {
  char payload[256];
  int len = snprintf(
    payload, sizeof(payload),
    "{\"catName\":\"%s\",\"latitude\":%.6f,\"longitude\":%.6f}",
    CAT_NAME, latitude, longitude
  );
  String topic = String("device/") + THING_NAME + "/location";

  if (usingWiFi) {
    if (!mqttWiFi.connected()) {
      Serial.println("WiFi MQTT disconnected. Attempting to reconnect...");
      if (!connectAwsMqttWifi()) {
        Serial.println("Failed to reconnect over WiFi, will try 4G next.");
        usingWiFi = false;
        // Attempt 4G fallback immediately
        if (connect4G()) {
          connectAwsMqttGSM();
        }
      }
    }
    if (mqttWiFi.connected()) {
      bool ok = mqttWiFi.publish(topic.c_str(), (uint8_t*)payload, len, false);
      if (ok) {
        Serial.print("Published over WiFi: ");
        Serial.println(payload);
      } else {
        Serial.println("WiFi publish failed.");
      }
      return;
    }
  }

  // If not using WiFi or WiFi publish failed, try 4G
  if (!modem.isGprsConnected()) {
    Serial.println("4G is not connected. Attempting to connect GPRS...");
    if (connect4G()) {
      connectAwsMqttGSM();
    } else {
      Serial.println("4G connection failed. Cannot publish.");
      return;
    }
  }
  if (!mqttGSM.connected()) {
    Serial.println("4G MQTT disconnected. Attempting to reconnect...");
    connectAwsMqttGSM();
  }
  if (mqttGSM.connected()) {
    bool ok = mqttGSM.publish(topic.c_str(), (uint8_t*)payload, len, false);
    if (ok) {
      Serial.print("Published over 4G: ");
      Serial.println(payload);
    } else {
      Serial.println("4G publish failed.");
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println();
  Serial.println("=== LilyGo TA7670E R2: WiFi first, fallback to 4G ===");

  // Initialize GPS UART (9600 baud, RX=GPIO12, TX=GPIO9)
  gpsSerial.begin(9600, SERIAL_8N1, 12, 9);
  Serial.println("GPS UART initialized");

  // 1) Try connecting to WiFi
  if (connectWifi()) {
    usingWiFi = true;
    if (!connectAwsMqttWifi()) {
      Serial.println("Failed to connect MQTT over WiFi. Will try 4G.");
      usingWiFi = false;
    }
  } else {
    usingWiFi = false;
    Serial.println("Skipping WiFi MQTT. Attempting 4G...");
  }

  // 2) If WiFi is not used or MQTT over WiFi failed, connect 4G & MQTT
  if (!usingWiFi) {
    if (connect4G()) {
      connectAwsMqttGSM();
    } else {
      Serial.println("4G initialization failed. System will run with GPS only.");
    }
  }

  Serial.println("Setup complete. Entering main loop...");
}

// =======================================================
//                 FUNCTION: MAIN LOOP
// =======================================================
unsigned long lastPublishMs = 0;
const unsigned long PUBLISH_INTERVAL = 15UL * 1000UL; // publish every 15 seconds

void loop() {
  // Periodically check if WiFi became available when using 4G
  if (!usingWiFi && WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi became available. Switching to WiFi...");
    if (connectAwsMqttWifi()) {
      usingWiFi = true;
      Serial.println("Switched to WiFi MQTT.");
    }
  }

  // If using WiFi, maintain mqttWiFi.loop()
  if (usingWiFi) {
    if (mqttWiFi.connected()) {
      mqttWiFi.loop();
    }
    // If WiFi disconnected at any point, switch to 4G
    if (WiFi.status() != WL_CONNECTED) {
      Serial.println("WiFi disconnected. Switching to 4G...");
      usingWiFi = false;
      if (connect4G()) {
        connectAwsMqttGSM();
      }
    }
  } else {
    // If using 4G, ensure GPRS and mqttGSM are connected
    if (modem.isGprsConnected()) {
      if (mqttGSM.connected()) {
        mqttGSM.loop();
      } else {
        Serial.println("4G MQTT lost. Reconnecting...");
        connectAwsMqttGSM();
      }
    } else {
      Serial.println("4G GPRS lost. Reconnecting...");
      if (connect4G()) {
        connectAwsMqttGSM();
      }
    }
  }

  // Read GPS data
  while (gpsSerial.available() > 0) {
    char c = gpsSerial.read();
    gps.encode(c);
  }

  // Publish location at intervals if we have a valid fix
  if (gps.location.isUpdated() && millis() - lastPublishMs >= PUBLISH_INTERVAL) {
    if (gps.location.isValid()) {
      double lat = gps.location.lat();
      double lon = gps.location.lng();
      publishLocation(lat, lon);
      lastPublishMs = millis();
    } else {
      Serial.println("GPS data not valid yet. Waiting for next update...");
    }
  }
}
