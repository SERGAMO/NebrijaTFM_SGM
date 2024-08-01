/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neurosdk.brainbit.demo.thingsboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.catbcn.catthingstunnel.calibration.Calibration;
import com.catbcn.catthingstunnel.calibration.TunnelCalculations;
import com.catbcn.catthingstunnel.calibration.TunnelCalibration;
import com.catbcn.catthingstunnel.connectivity.NetworkUtil;
import com.google.android.things.device.DeviceManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_AUTO_MODE;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_CALIBRATION_POINTS;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_CALIBRATION_STATE;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_CAMERA_STATE;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_CONNECTION_STATUS;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_FACTOR_DP;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_FILTER;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_IP;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_K_MEAN;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_LOG_TIME;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_LOG_TO_THINGSBOARD;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_MANUAL_MODE;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_PHOTO_TIME;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_POINT_TIME;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_SPEED_COUNTS;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_SPEED_FACTOR;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_WORKSHEET;
import static com.catbcn.catthingstunnel.thingsboard.Attributes.ATTR_ZERO;
import static com.catbcn.catthingstunnel.thingsboard.RPC_Commands.RPC_TAKE_PICTURE;


/**
 * Handle publishing sensor data to a Thingsboard IoT MQTT endpoint.
 *
 */
//TODO: Al iniciar, actualizar los atributos por defecto en TB
public class MQTTPublisher implements ThingsboardPublisher {

    private static final String TAG = MQTTPublisher.class.getSimpleName();
    private static final String ACTION_START_STOP_TEST= "com.catbcn.cathings.tunnel.ACTION_START_STOP_TEST";
    private static final String ACTION_START_STOP_MANUAL_TEST= "com.catbcn.cathings.tunnel.ACTION_START_STOP_MANUAL_TEST";
    private static final String ACTION_START_STOP_TUNNEL_CALIBRATION= "com.catbcn.cathings.tunnel.ACTION_START_STOP_TUNNEL_CALIBRATION";
    private static final String ACTION_SET_CURRENT_SPEED = "com.catbcn.cathings.tunnel.ACTION_SET_CURRENT_SPEED";
    private static final String ACTION_GET_CURRENT_SPEED = "com.catbcn.cathings.tunnel.ACTION_GET_CURRENT_SPEED";
    private static final String ACTION_RPC_COMMAND= "com.catbcn.cathings.tunnel.ACTION_RPC_COMMAND";


    private SharedPreferences mSharedPreferences;


    // Indicate if this message should be a MQTT 'retained' message.
    private static final boolean SHOULD_RETAIN = false;

    // Use mqttQos=1 (at least once delivery), mqttQos=0 (at most once delivery) also supported.
    private static final int MQTT_QOS = 1;

    private MqttClient mqttClient;
    private MqttAsyncClient mqttAsyncClient;
    //private MqttAndroidClient mqttClient;
    private ThingsboardIotOptions ThingsboardIotOptions;
    private MqttAuthentication mqttAuth;
    private AtomicBoolean mReady = new AtomicBoolean(false);

    private Context mContext;


    public MQTTPublisher(@NonNull ThingsboardIotOptions options) {
        initialize(options);
    }

    public MQTTPublisher(@NonNull ThingsboardIotOptions options, Context context) {
        initialize(options);
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        syncAttributes();

    }

    @Override
    public void reconfigure(@NonNull ThingsboardIotOptions newOptions) {
        if (newOptions.equals(ThingsboardIotOptions)) {
            return;
        }
        mReady.set(false);
        try {
            close();
        } catch (MqttException e) {
            //FirebaseCrashlytics.getInstance().log(e.getMessage());

            // empty
        }
        initialize(newOptions);
    }

    /**
     * Initialize a Thingsboard IoT Endpoint given a set of configuration options.
     *
     * @param options Thingsboard IoT configuration options.
     */
    private void initialize(@NonNull ThingsboardIotOptions options) {
        if (!options.isValid()) {
            Log.w(TAG, "Postponing initialization, since ThingsboardIotOptions is incomplete. " +
                    "Please configure via intent, for example: \n" +
                    "adb shell am startservice -a " +
                    "com.example.androidthings.sensorhub.mqtt.CONFIGURE " +
                    "-e project_id <PROJECT_ID> -e Thingsboard_region <REGION> " +
                    "-e registry_id <REGISTRY_ID> -e device_id <DEVICE_ID> " +
                    "com.example.androidthings.sensorhub/.Thingsboard.ThingsboardPublisherService\n");
            return;
        }
        try {
            ThingsboardIotOptions = options;
            Log.i(TAG, "Device Configuration:");
            Log.i(TAG, " Project ID: "+ThingsboardIotOptions.getProjectId());
            Log.i(TAG, "  Region ID: "+ThingsboardIotOptions.getThingsboardRegion());
            Log.i(TAG, "Registry ID: "+ThingsboardIotOptions.getRegistryId());
            Log.i(TAG, "  Device ID: "+ThingsboardIotOptions.getDeviceId());
            Log.i(TAG, "MQTT Configuration:");
            Log.i(TAG, "Broker: "+ThingsboardIotOptions.getBridgeHostname()+":"+ThingsboardIotOptions.getBridgePort());
            Log.i(TAG, "Publishing to topic: "+ThingsboardIotOptions.getTopicName());
            //mqttAuth = new MqttAuthentication();
            //mqttAuth.initialize();
            /*if( Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                try {
                    mqttAuth.exportPublicKey(new File(Environment.getExternalStorageDirectory(),"Thingsboard_iot_auth_certificate.pem"));
                } catch (GeneralSecurityException | IOException e) {
                    if( e instanceof FileNotFoundException && e.getMessage().contains("Permission denied")) {
                        Log.e(TAG, "Unable to export certificate, may need to reboot to receive WRITE permissions?", e);
                    } else {
                        Log.e(TAG, "Unable to export certificate", e);
                    }
                }
            }*/
            initializeMqttClientSync();
        } catch (MqttException | IOException | GeneralSecurityException e) {
            //FirebaseCrashlytics.getInstance().log(e.getMessage());
            //FirebaseCrashlytics.getInstance().log("Could not initialize MQTT");
            //throw new IllegalArgumentException("Could not initialize MQTT", e);

        }
    }

    @Override
    public void publishTelemetry(List<SensorData> data) {
        try {
            if (isReady()) {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    // if for some reason the mqtt client has disconnected, we should try to connect
                    // it again.
                    try {
                        initializeMqttClientSync();
                    } catch (MqttException | IOException | GeneralSecurityException e) {
                        //FirebaseCrashlytics.getInstance().log(e.getMessage());
                        //throw new IllegalArgumentException("Could not initialize MQTT", e);
                    }
                }
                String payload = MessagePayload.createMessagePayload(data);
                //String payload = MessagePayload.createMessagePayloadWithTs(data);
                Log.d(TAG, "Publishing: " + payload);
                sendMessage(ThingsboardIotOptions.getTopicName(), payload.getBytes());
            }
        } catch (MqttException e) {
            //FirebaseCrashlytics.getInstance().log(e.getMessage());
            //throw new IllegalArgumentException("Could not send message", e);
        }
    }

    @Override
    public void publishAttribute(SensorData message){
        try {
            if (isReady()) {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    // if for some reason the mqtt client has disconnected, we should try to connect
                    // it again.
                    try {
                        initializeMqttClientSync();
                    } catch (MqttException | IOException | GeneralSecurityException e) {
                        //FirebaseCrashlytics.getInstance().log(e.getMessage());
                        throw new IllegalArgumentException("Could not initialize MQTT", e);
                    }
                }

                String payload = MessagePayload.createMessagePayload(message);
                Log.d(TAG, "Publishing: "+payload);
                sendMessage(ThingsboardIotOptions.getTopicNameAttribute(), payload.getBytes());

                //mqttClient.publish("v1/devices/me/attributes", new MqttMessage(mNextPoint.getText().toString().getBytes()));
                //JSONObject tmrStatus = new JSONObject();
                //try {
                //    tmrStatus.put(message.getAttributeName(),message.getAttributeValue());
                //} catch (JSONException e) {
                //    e.printStackTrace();
                // }

                //String payload = MessagePayload.createMessagePayload(message);
                //Log.d(TAG, "Publishing: "+payload);
                //sendMessage(ThingsboardIotOptions.getTopicNameAttribute(),tmrStatus.toString().getBytes());
            }
        } catch (MqttException e) {
            //FirebaseCrashlytics.getInstance().log(e.getMessage());
            //throw new IllegalArgumentException("Could not send message", e);
        }
    }

    @Override
    public boolean isReady() {
        return mReady.get();
    }

    @Override
    public void close() throws MqttException {
        ThingsboardIotOptions = null;
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient = null;
        }
    }

    private void initializeMqttClientAync() {
        //mqttClient = new MqttClient(ThingsboardIotOptions.getBrokerUrl(), ThingsboardIotOptions.getClientId(), new MemoryPersistence());
        //mqttClient = new MqttClient(ThingsboardIotOptions.getThingsboardHost(), "Raspberry Pi 3", new MemoryPersistence());
        try {
            mqttAsyncClient = new MqttAsyncClient(ThingsboardIotOptions.getThingsboardHost(), "imx6", new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }


        MqttConnectOptions options = new MqttConnectOptions();
        // Note that the the Google Thingsboard IoT only supports MQTT 3.1.1, and Paho requires that we
        // explicitly set this. If you don't set MQTT version, the server will immediately close its
        // connection to your device.
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT);
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setMaxInflight(500);
        //options.setUserName(ThingsboardIotOptions.UNUSED_ACCOUNT_NAME);
        options.setUserName(ThingsboardIotOptions.ACCESS_TOKEN);
        mqttAsyncClient.setCallback(mMqttCallback);
        try {
            mqttAsyncClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT client connected!");
                    try {
                        mqttAsyncClient.subscribe("v1/devices/me/rpc/request/+", 0);
                        mqttAsyncClient.subscribe("v1/devices/me/attributes",0);
                        //updateData();
                        mReady.set(true);

                    } catch (MqttException ex) {
                        Log.e(TAG, "Unable to subscribe to rpc requests topic", ex);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    if (e instanceof MqttException) {
                        MqttException mqttException = (MqttException) e;
                        Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s, code: %d", mqttException.getMessage(),
                                mqttException.getReasonCode()), e);
                    } else {
                        Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s", e.getMessage()), e);
                    }
                }
            });
        } catch (MqttException ex) {
            Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s, code: %d", ex.getMessage(), ex.getReasonCode()), ex);
        }
    }

    private void initializeMqttClientSync() throws MqttException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        //mqttClient = new MqttClient(ThingsboardIotOptions.getBrokerUrl(), ThingsboardIotOptions.getClientId(), new MemoryPersistence());
        mqttClient = new MqttClient(ThingsboardIotOptions.getThingsboardHost(), "Raspberry Pi 3", new MemoryPersistence());
        //mqttClient = new MqttAsyncClient(ThingsboardIotOptions.getThingsboardHost(), "imx6", new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        // Note that the the Google Thingsboard IoT only supports MQTT 3.1.1, and Paho requires that we
        // explicitly set this. If you don't set MQTT version, the server will immediately close its
        // connection to your device.
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT);
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setMaxInflight(500);
        //options.setUserName(ThingsboardIotOptions.UNUSED_ACCOUNT_NAME);
        options.setUserName(ThingsboardIotOptions.ACCESS_TOKEN);

        mqttClient.setCallback(mMqttCallback );


        // generate the jwt password
        //options.setPassword(mqttAuth.createJwt(ThingsboardIotOptions.getProjectId()));

        try {
            mqttClient.connect(options);
            Log.i(TAG, "MQTT client connecting...");

            try {
                mqttClient.subscribe("v1/devices/me/rpc/request/+", 0);
                mqttClient.subscribe("v1/devices/me/attributes",0);
                Log.i(TAG, "MQTT client subscribed to RPC and Attributes");


                //updateData();
            } catch (MqttException ex) {
                //FirebaseCrashlytics.getInstance().log(ex.getMessage());
                Log.e(TAG, "Unable to subscribe to rpc requests topic", ex);
            }
            try {


                //mqttClient.publish("v1/devices/me/attributes", getTimerStatusMessage());

            } catch (Exception ex) {
                //FirebaseCrashlytics.getInstance().log(ex.getMessage());
                Log.e(TAG, "Unable to publish status to Thingsboard server", ex);
            }


        } catch (MqttException ex) {
            //FirebaseCrashlytics.getInstance().log(String.format("Unable to connect to Thingsboard server: %s, code: %d", ex.getMessage(), ex.getReasonCode()));
            Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s, code: %d", ex.getMessage(), ex.getReasonCode()), ex);
        }
        mReady.set(true);
    }

    private void syncAttributes(){
        //Sync attributes with Thingsboard

        try {
            mqttClient.publish("v1/devices/me/attributes", getPointTimerStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getLogTimerStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getCameraStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getFilterStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getPhotoTimeMessage());
            mqttClient.publish("v1/devices/me/attributes", getCalibrationState());
            mqttClient.publish("v1/devices/me/attributes", getKmStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getWorksheetStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getManualMode(mSharedPreferences.getBoolean(ATTR_MANUAL_MODE, false)));
            mqttClient.publish("v1/devices/me/attributes", getAutoMode(mSharedPreferences.getBoolean(ATTR_AUTO_MODE, true)));
            mqttClient.publish("v1/devices/me/attributes", getLogToThingsboard());
            mqttClient.publish("v1/devices/me/attributes", getSpeedFactorStatusMessage());
            mqttClient.publish("v1/devices/me/attributes", getCurrentSpeed());
            mqttClient.publish("v1/devices/me/attributes", getConnectionStatusMessage());
        } catch (Exception ex) {
            //FirebaseCrashlytics.getInstance().log("Unable to publish status to Thingsboard server");
            Log.e(TAG, "Unable to publish status to Thingsboard server", ex);
        }
    }
    private void sendMessage(String mqttTopic, byte[] mqttMessage) throws MqttException {
        mqttClient.publish(mqttTopic, mqttMessage, MQTT_QOS, SHOULD_RETAIN);
    }


    /*
    IMQTT Connection methods
     */


    /*
    IMQTT Callback
     */
    private MqttCallback mMqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //endTest();
            //mqttConnect();
            Log.e(TAG, "Disconnected from Thingsboard server", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, String.format("Received message from topic [%s]", topic));
            Intent rpcIntent;
            try{
                if(topic.equals("errors")) {
                    JSONObject messageData = new JSONObject(new String(message.getPayload()));
                    //FirebaseCrashlytics.getInstance().log(messageData.toString());
                }else if (topic.contains("v1/devices/me/attributes")){
                    JSONObject messageData = new JSONObject(new String(message.getPayload()));
                    JSONArray arrayData = messageData.names();

                    if(arrayData.length() == 0)
                        return;

                    String attr = arrayData.getString(0);



                    switch (attr){
                        case ATTR_SPEED_COUNTS:
                            if(Calibration.getManualMode()) {
                                rpcIntent = new Intent(ACTION_SET_CURRENT_SPEED);
                                rpcIntent.putExtra("SPEED", messageData.getInt(attr));
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                            }
                            break;

                        case ATTR_WORKSHEET:
                            Calibration.setWorksheet(messageData.getInt(attr));
                            mSharedPreferences.edit().putInt(ATTR_WORKSHEET,Calibration.getWorksheet()).apply();
                            updateWorksheetAttribute(Calibration.getWorksheet());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_POINT_TIME:
                            Calibration.setPointTimer(messageData.getInt(attr));
                            mSharedPreferences.edit().putInt(ATTR_POINT_TIME,Calibration.getPointTimer()).apply();
                            updatePointTimerAttribute(Calibration.getPointTimer());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_LOG_TIME:
                            Calibration.setLogTimer(messageData.getInt(attr));
                            mSharedPreferences.edit().putInt(ATTR_LOG_TIME,Calibration.getLogTimer()).apply();
                            updateLogTimerAttribute(Calibration.getLogTimer());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_FILTER:
                            Calibration.setFilterLength(messageData.getInt(attr));
                            mSharedPreferences.edit().putInt(ATTR_FILTER,Calibration.getFilterLength()).apply();
                            updateFilterAttribute(Calibration.getFilterLength());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_K_MEAN:
                            Calibration.setkMean(messageData.getDouble(attr));
                            mSharedPreferences.edit().putFloat(ATTR_K_MEAN,(float)Calibration.getkMean()).apply();
                            updateKmAttribute();
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_CALIBRATION_POINTS:
                            String[] points = messageData.getString(attr).split(",");
                            Calibration.deleteAllPoints();

                            for (String point : points)
                                Calibration.addNewPoint(Integer.valueOf(point.trim()));

                            updatePointsAttribute(Calibration.getPoints());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_CALIBRATION_STATE:
                            Calibration.setCalibrationState(messageData.getBoolean(attr));
                            mSharedPreferences.edit().putBoolean(ATTR_CALIBRATION_STATE,Calibration.getCalibrationState()).apply();
                            updateManualCalibrationStateAttribute(Calibration.getCalibrationState());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_LOG_TO_THINGSBOARD:
                            Calibration.setLogToThingsboard(messageData.getBoolean(attr));
                            mSharedPreferences.edit().putBoolean(ATTR_LOG_TO_THINGSBOARD,Calibration.getLogToThingsboard()).apply();
                            updateLogAttribute(Calibration.getLogToThingsboard());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_CAMERA_STATE:
                            Calibration.setCameraStatus(messageData.getBoolean(attr));
                            mSharedPreferences.edit().putBoolean(ATTR_CAMERA_STATE,Calibration.getCameraStatus()).apply();
                            updateLogAttribute(Calibration.getCameraStatus());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_PHOTO_TIME:
                            Calibration.setPhotoTime(messageData.getInt(attr));
                            mSharedPreferences.edit().putInt(ATTR_PHOTO_TIME, Calibration.getPhotoTime()).apply();
                            updateLogTimerAttribute(Calibration.getLogTimer());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_FACTOR_DP:
                            Calibration.setFactorDP(messageData.getDouble(attr));
                            mSharedPreferences.edit().putFloat(ATTR_FACTOR_DP, (float)Calibration.getFactorDP()).apply();
                            updateFactorDPAttribute(Calibration.getFactorDP());
                            sendLocalBroadcast(attr);
                            break;

                        case ATTR_ZERO:
                            updateZeroAttribute();
                            sendLocalBroadcast(attr);
                            break;
                    }

                }else if (topic.contains("v1/devices/me/rpc/request/")) {
                    String requestId = topic.substring("v1/devices/me/rpc/request/".length());
                    JSONObject messageData = new JSONObject(new String(message.getPayload()));
                    String method = messageData.getString("method");
                    //Intent rpcIntent;
                    Log.d(TAG, "Message " + method);

                    if(method != null){
                        switch (method){

                            case "checkConnectionStatus":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getConnectionStatusMessage());
                                break;

                            case "getIP":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getLocalIP());
                                break;

                            case "setManualMode":
                                Calibration.setManualMode(messageData.getBoolean("params"));
                                updateManualModeStatus(Calibration.getManualMode(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getManualMode":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getManualMode(Calibration.getManualMode()));
                                break;

                            case "setAutoMode":
                                Calibration.setManualMode(!messageData.getBoolean("params"));
                                updateAutoModeStatus(!Calibration.getManualMode(), requestId);
                                sendLocalBroadcast("setManualMode");
                                break;

                            case "getAutoMode":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getAutoMode(!Calibration.getManualMode()));
                                break;

                            case "setLogToThingsboard":
                                Calibration.setLogToThingsboard(messageData.getBoolean("params"));
                                updateLogStatus(Calibration.getLogToThingsboard(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getLogToThingsboard":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getLogToThingsboard());

                                break;

                            case "setAutoCalibrationState":
                                rpcIntent = new Intent(ACTION_START_STOP_TEST);
                                rpcIntent.putExtra("ACTION", messageData.getBoolean("params"));
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                Calibration.setCalibrationState(true);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                break;


                            case "setManualCalibrationState":
                                rpcIntent = new Intent(ACTION_START_STOP_MANUAL_TEST);
                                rpcIntent.putExtra("ACTION", messageData.getBoolean("params"));
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                Calibration.setCalibrationState(messageData.getBoolean("params"));
                                //updateManualCalibrationStateAttribute(Calibration.getCalibrationState());
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                break;

                            case "getManualCalibrationState":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());

                                break;

                            case "startManualCalibration": ;
                                Calibration.setCalibrationState(true);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                sendLocalBroadcast(method);
                                break;

                            case "stopManualCalibration":
                                Calibration.setCalibrationState(false);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                sendLocalBroadcast(method);
                                break;


                            case "starTunnelCalibration":
                                rpcIntent = new Intent(ACTION_START_STOP_TUNNEL_CALIBRATION);
                                rpcIntent.putExtra("ACTION", true);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                Calibration.setCalibrationState(true);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                break;

                            case "stopTunnelCalibration":
                                rpcIntent = new Intent(ACTION_START_STOP_TUNNEL_CALIBRATION);
                                rpcIntent.putExtra("ACTION", false);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                Calibration.setCalibrationState(false);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationState());
                                break;

                            case "setCurrentSpeed":
                                if(Calibration.getManualMode()) {
                                    rpcIntent = new Intent(ACTION_SET_CURRENT_SPEED);
                                    rpcIntent.putExtra("SPEED", messageData.getInt("params"));
                                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                    mqttClient.publish("v1/devices/me/rpc/response/" + requestId, setCurrentSpeed(messageData.getInt("params")));
                                }else
                                    mqttClient.publish("v1/devices/me/rpc/response/" + requestId, noManualMode());
                                break;

                            case "getCurrentSpeed":
                                //rpcIntent = new Intent(ACTION_GET_CURRENT_SPEED);
                                //LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCurrentSpeed());

                                break;

                            case "setPoints":
                                String[] points = messageData.getString("params").split(",");
                                Calibration.deleteAllPoints();

                                for (String point : points)
                                    Calibration.addNewPoint(Integer.valueOf(point));

                                updatePointsStatus(Calibration.getPoints(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getPoints":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCalibrationPoints());
                                break;

                            case "setCalibrationTunnelPoints":
                                String[] tunnelPoints = messageData.getString("params").split(",");
                                TunnelCalibration.deleteAllPoints();

                                for (String point : tunnelPoints)
                                    TunnelCalibration.addNewPoint(Integer.valueOf(point));

                                updatePointsStatus(TunnelCalibration.getPoints(), requestId);
                                sendLocalBroadcast(method);

                                break;

                            case "getCalibrationTunnelPoints":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getTunnelCalibrationPoints());
                                break;


                            case "setWorksheet":
                                Calibration.setWorksheet(messageData.getInt("params"));
                                updateWorksheetStatus(Calibration.getWorksheet(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getWorksheet":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getWorksheetStatusMessage());
                                break;

                            case "setCalibrationPointTimer":
                                Calibration.setPointTimer(messageData.getInt("params"));
                                updatePointTimerStatus(Calibration.getPointTimer(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getCalibrationPointTimer":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getPointTimerStatusMessage());
                                break;

                            case "setLogTimer":
                                Calibration.setLogTimer(messageData.getInt("params"));
                                updatePhotoTimeStatus(Calibration.getPhotoTime(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getLogTimer":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getLogTimerStatusMessage());
                                break;

                            case "setFilter":
                                Calibration.setFilterLength(messageData.getInt("params"));
                                updateFilterStatus(requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getFilter":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getFilterStatusMessage());
                                break;

                            case "getSpeedFactor":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getSpeedFactorStatusMessage());
                                break;

                            case "setKmean":
                                Calibration.setkMean(messageData.getDouble("params"));
                                updateKmStatus(requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getKmean":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getKmStatusMessage());
                                break;

                            case "setCameraStatus":
                                Calibration.setCameraStatus(messageData.getBoolean("params"));
                                updateCameraStatus(requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getCameraStatus":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getCameraStatusMessage());
                                break;

                            case "setPhotoTime":
                                Calibration.setPhotoTime(messageData.getInt("params"));
                                updateLogTimerStatus(Calibration.getLogTimer(), requestId);
                                sendLocalBroadcast(method);
                                break;

                            case "getPhotoTime":
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getPhotoTimeMessage());
                                break;

                            case RPC_TAKE_PICTURE:
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getPicMessage());
                                sendLocalBroadcast(method);
                                break;


                            case "setZero":
                                updateZeroAttribute();
                                sendLocalBroadcast(ATTR_ZERO);
                                break;

                            case "reboot":
                                updateRebootStatus(requestId);
                                DeviceManager.getInstance().reboot();
                                break;

                            //Echo
                            default:
                                mqttClient.publish("v1/devices/me/rpc/response/" + requestId, msgNotSupported());
                                break;
                        }
                    }
                }
            }catch (Exception ex){
                //FirebaseCrashlytics.getInstance().log(ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Delivery completed to Thingsboard Server for Token: " + token.getMessageId());
        }
    };


    private void sendConnectionStatus(String requestId) throws Exception{
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, getConnectionStatusMessage());
    }



    private void updateCameraStatus(String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("camera", Calibration.getCameraStatus());
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }


    private void updateKmStatus(String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("k_media", Calibration.getkMean());
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateKmAttribute() throws Exception {
        JSONObject response = new JSONObject();
        response.put("k_media", Calibration.getkMean());
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }



    private void updateFilterStatus(String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("filter", Calibration.getFilterLength());
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }



    private MqttMessage getKmStatusMessage() throws Exception {
        JSONObject kmStatus = new JSONObject();
        kmStatus.put(ATTR_K_MEAN, (double)mSharedPreferences.getFloat(ATTR_K_MEAN,1));
        return new MqttMessage(kmStatus.toString().getBytes());
    }


    private MqttMessage getFilterStatusMessage() throws Exception {
        JSONObject filterStatus = new JSONObject();
        filterStatus.put(ATTR_FILTER, mSharedPreferences.getInt(ATTR_FILTER,200));
        return new MqttMessage(filterStatus.toString().getBytes());
    }

    private MqttMessage getSpeedFactorStatusMessage() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_SPEED_FACTOR, mSharedPreferences.getInt(ATTR_SPEED_FACTOR,70));
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getCameraStatusMessage() throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_CAMERA_STATE, mSharedPreferences.getBoolean(ATTR_CAMERA_STATE,false));
        return new MqttMessage(response.toString().getBytes());
    }

    private MqttMessage getPhotoTimeMessage() throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_PHOTO_TIME, mSharedPreferences.getInt(ATTR_PHOTO_TIME,1));
        return new MqttMessage(response.toString().getBytes());
    }

    private MqttMessage getPicMessage() throws Exception {
        JSONObject response = new JSONObject();
        response.put("get_picture", "Solicitando captura");
        return new MqttMessage(response.toString().getBytes());
    }

    private MqttMessage msgNotSupported() throws Exception {
        JSONObject response = new JSONObject();
        response.put("msg_not_supported", "Comando no soportado");
        return new MqttMessage(response.toString().getBytes());
    }

    private MqttMessage getConnectionStatusMessage() throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_CONNECTION_STATUS, mqttClient.isConnected());

        return new MqttMessage(response.toString().getBytes());
    }


    private void updateWorksheetStatus(int worksheet, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_WORKSHEET, worksheet);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateWorksheetAttribute(int worksheet) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_WORKSHEET, worksheet);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateManualModeStatus(boolean mode, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_MANUAL_MODE, mode);
        response.put(ATTR_AUTO_MODE, !mode);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateAutoModeStatus(boolean mode, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_MANUAL_MODE, !mode);
        response.put(ATTR_AUTO_MODE, mode);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateLogStatus(boolean log, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("Log to Thingsboard", log);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateLogAttribute(boolean log) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_LOG_TO_THINGSBOARD, log);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updatePointTimerStatus(long timer, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("point_time", timer);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updatePointTimerAttribute(int timer) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_POINT_TIME, timer);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updatePointsStatus(String points, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("points", points);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updatePointsAttribute(String points) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_CALIBRATION_POINTS, points);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateManualCalibrationStateAttribute(boolean state) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_CALIBRATION_STATE, state);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }


    private void updateLogTimerStatus(long timer, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("log_time", timer);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateLogTimerAttribute(int timer) throws Exception {
        JSONObject response = new JSONObject();
        response.put(ATTR_LOG_TIME, timer);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateFilterAttribute(int filter) throws Exception {
        JSONObject response = new JSONObject();
        response.put("filter", filter);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateFactorDPAttribute(double factor) throws Exception {
        JSONObject response = new JSONObject();
        response.put("factor_dp", factor);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateZeroAttribute() throws Exception {
        JSONObject response = new JSONObject();
        response.put("zero_dp", false);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/attributes", message);
    }


    private void updatePhotoTimeStatus(long timer, String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("photo_time", timer);
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private void updateRebootStatus(String requestId) throws Exception {
        JSONObject response = new JSONObject();
        response.put("reboot", "Rebooting");
        MqttMessage message = new MqttMessage(response.toString().getBytes());
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mqttClient.publish("v1/devices/me/attributes", message);
    }

    private MqttMessage getWorksheetStatusMessage() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_WORKSHEET, mSharedPreferences.getInt(ATTR_WORKSHEET,1));
        return new MqttMessage(status.toString().getBytes());
    }


    private MqttMessage getCalibrationState() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_CALIBRATION_STATE, mSharedPreferences.getBoolean(ATTR_CALIBRATION_STATE, false));
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getCalibrationPoints() throws Exception {
        JSONObject status = new JSONObject();
        status.put("points", Calibration.getPoints());
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getTunnelCalibrationPoints() throws Exception {
        JSONObject status = new JSONObject();
        status.put("points", TunnelCalibration.getPoints());
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getLocalIP() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_IP, NetworkUtil.getIP(mContext));
        return new MqttMessage(status.toString().getBytes());
    }


    private MqttMessage getManualMode(boolean mode) throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_MANUAL_MODE, mode);
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getAutoMode(boolean mode) throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_AUTO_MODE, mode);
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getLogToThingsboard() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_LOG_TO_THINGSBOARD, mSharedPreferences.getBoolean(ATTR_LOG_TO_THINGSBOARD,true));
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getCurrentSpeed() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_SPEED_COUNTS, 0);
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage setCurrentSpeed(int speed) throws Exception {
        JSONObject status = new JSONObject();
        if (speed < 0)
            speed = 0;
        if (speed > 2731)
            speed = 2731;
        status.put("New speed Hz", TunnelCalculations.getTunnelSpeedHz(speed));
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage noManualMode() throws Exception {
        JSONObject status = new JSONObject();
        status.put("New speed Hz", "Modo Manual requerido");
        return new MqttMessage(status.toString().getBytes());
    }


    private MqttMessage getPointTimerStatusMessage() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_POINT_TIME, mSharedPreferences.getInt(ATTR_POINT_TIME,3));
        return new MqttMessage(status.toString().getBytes());
    }

    private MqttMessage getLogTimerStatusMessage() throws Exception {
        JSONObject status = new JSONObject();
        status.put(ATTR_LOG_TIME, mSharedPreferences.getInt(ATTR_LOG_TIME,250));
        return new MqttMessage(status.toString().getBytes());
    }


    private void sendLocalBroadcast(String command){
        Intent rpcIntent = new Intent(ACTION_RPC_COMMAND);
        rpcIntent.putExtra("COMMAND", command);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(rpcIntent);
    }
}