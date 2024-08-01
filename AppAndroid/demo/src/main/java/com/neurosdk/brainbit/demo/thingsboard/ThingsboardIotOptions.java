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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Configuration container for the MQTT example
 */
public class ThingsboardIotOptions {
    private static final String TAG = ThingsboardIotOptions.class.getSimpleName();

    private static final String DEFAULT_BRIDGE_HOSTNAME = "mqtt.googleapis.com";
    private static final short DEFAULT_BRIDGE_PORT = 443;

    public static final String UNUSED_ACCOUNT_NAME = "unused";


    public static final String ACCESS_TOKEN_COM= "6nevt6ivOWwDYmJhuGvP"; //Comm dev
    public static final String ACCESS_TOKEN= "fbsUWEHILDqkZrjzYsoK"; //CAT
    public static final String ACCESS_TOKEN_DEMO= "8K4FD5SzeEvxHA9MMVfB"; // Demo TB
    public static final String ACCESS_TOKEN_PRO = "F1A9svliujxol7s2SOjU"; //PRO

    private static final String THINGSBOARD_HOST_LOC = "192.168.1.53"; //COM
    private static final String THINGSBOARD_HOST_PRI = "192.168.1.190"; //PRO
    private static final String THINGSBOARD_HOST= "192.168.10.95"; //CAT TESTS
    private static final String THINGSBOARD_HOST_PRO= "34.228.33.203";

    private static final short DEFAULT_THINGSBOARD_MQTT_PORT = 1883;
    /**
     * Notice that for CloudIoT the topic for telemetry events needs to have the format below.
     * As described <a href="https://cloud.google.com/iot/docs/protocol_bridge_guide#telemetry_events">in docs</a>,
     * messages published to a topic with this format are augmented with extra attributes and
     * forwarded to the Pub/Sub topic specified in the registry resource.
     */
    private static final String MQTT_TOPIC_FORMAT2 = "/devices/%s/events";
    private static final String MQTT_CLIENT_ID_FORMAT = "projects/%s/locations/%s/registries/%s/devices/%s";
    private static final String BROKER_URL_FORMAT = "ssl://%s:%d";


    private static final String THINGSBOARD_URL_FORMAT = "tcp://%s:%d";

    private static final String MQTT_TELEMETRY_FORMAT = "v1/devices/me/telemetry";
    private static final String MQTT_ATTRIBUTE_FORMAT = "v1/devices/me/attributes";

    /**
     * GCP cloud project name.
     */
    private String projectId;

    /**
     * Cloud IoT registry id.
     */
    private String registryId;

    /**
     * Cloud IoT device id.
     */
    private String deviceId;

    /**
     * GCP cloud region.
     */
    private String thingsboardRegion;

    /**
     * MQTT bridge hostname.
     */
    private String bridgeHostname = DEFAULT_BRIDGE_HOSTNAME;

    /**
     * MQTT bridge hostname.
     */
    private String thingsboardHostname = THINGSBOARD_HOST;


    /**
     * MQTT bridge port.
     */
    private short bridgePort = DEFAULT_BRIDGE_PORT;


    /**
     * MQTT bridge port.
     */
    private short thingsboardMqttPort = DEFAULT_THINGSBOARD_MQTT_PORT;


    public String getBrokerUrl() {
        return String.format(Locale.getDefault(), BROKER_URL_FORMAT, bridgeHostname, bridgePort);
    }

    public String getThingsboardHost(){
        return String.format(Locale.getDefault(), THINGSBOARD_URL_FORMAT, thingsboardHostname, thingsboardMqttPort);

    }

    public String getClientId() {
        return String.format(Locale.getDefault(), MQTT_CLIENT_ID_FORMAT, projectId, thingsboardRegion, registryId, deviceId);
    }

    public String getTopicName() {
        return String.format(Locale.getDefault(), MQTT_TELEMETRY_FORMAT, deviceId);
    }

    public String getTopicNameAttribute() {
        return String.format(Locale.getDefault(), MQTT_ATTRIBUTE_FORMAT, deviceId);
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRegistryId() {
        return registryId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getThingsboardRegion() {
        return thingsboardRegion;
    }

    public String getBridgeHostname() {
        return bridgeHostname;
    }

    public short getBridgePort() {
        return bridgePort;
    }

    private ThingsboardIotOptions() {
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(projectId) &&
                !TextUtils.isEmpty(registryId) &&
                !TextUtils.isEmpty(deviceId) &&
                !TextUtils.isEmpty(thingsboardRegion) &&
                !TextUtils.isEmpty(bridgeHostname);
    }

    public void saveToPreferences(SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("project_id", projectId);
        editor.putString("registry_id", registryId);
        editor.putString("device_id", deviceId);
        editor.putString("cloud_region", thingsboardRegion);
        editor.putString("mqtt_bridge_hostname", bridgeHostname);
        editor.putInt("mqtt_bridge_port", bridgePort);
        editor.apply();
    }


    /**
     * Construct a ThingsboardIotOptions object from SharedPreferences.
     */
    public static ThingsboardIotOptions from(SharedPreferences pref) {
        try {
            ThingsboardIotOptions options = new ThingsboardIotOptions();
            options.projectId = pref.getString("project_id", "Wind Tunnel");
            options.registryId = pref.getString("registry_id", "12345");
            options.deviceId = pref.getString("device_id", "Raspbery Pi 3");
            options.thingsboardRegion = pref.getString("cloud_region", "Spain");
            options.bridgeHostname = pref.getString("mqtt_bridge_hostname",
                    THINGSBOARD_HOST);
            options.bridgePort = (short) pref.getInt("mqtt_bridge_port", DEFAULT_THINGSBOARD_MQTT_PORT);
            return options;
        } catch (Exception e) {
            throw new IllegalArgumentException("While processing configuration options", e);
        }
    }

    /**
     * Apply Bundle matched properties.
     */
    public static ThingsboardIotOptions reconfigure(ThingsboardIotOptions original, Bundle bundle) {
        try {
            if (Log.isLoggable(TAG, Log.INFO)) {
                HashSet<String> valid = new HashSet<>(Arrays.asList(new String[] {"project_id",
                        "registry_id", "device_id","cloud_region", "mqtt_bridge_hostname",
                        "mqtt_bridge_port"}));
                valid.retainAll(bundle.keySet());
                Log.i(TAG, "Configuring options using the following intent extras: " + valid);
            }

            ThingsboardIotOptions result = new ThingsboardIotOptions();
            result.projectId = bundle.getString("project_id", original.projectId);
            result.registryId = bundle.getString("registry_id", original.registryId);
            result.deviceId = bundle.getString("device_id", original.deviceId);
            result.thingsboardRegion = bundle.getString("cloud_region", original.thingsboardRegion);
            result.bridgeHostname = bundle.getString("mqtt_bridge_hostname",
                    original.bridgeHostname);
            result.bridgePort = (short) bundle.getInt("mqtt_bridge_port", original.bridgePort);
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("While processing configuration options", e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ThingsboardIotOptions)) {
            return false;
        }
        ThingsboardIotOptions o = (ThingsboardIotOptions) obj;
        return TextUtils.equals(projectId , o.projectId)
            && TextUtils.equals(registryId, o.registryId)
            && TextUtils.equals(deviceId, o.deviceId)
            && TextUtils.equals(thingsboardRegion, o.thingsboardRegion)
            && TextUtils.equals(bridgeHostname, o.bridgeHostname)
            && o.bridgePort == bridgePort;
    }
}
