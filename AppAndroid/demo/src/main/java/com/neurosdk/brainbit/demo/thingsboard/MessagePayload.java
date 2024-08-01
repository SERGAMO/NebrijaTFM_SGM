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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * This class handles the serialization of the SensorData objects into a String
 */
public class MessagePayload {

    /**
     * Serialize a List of SensorData objects into a JSON string, for sending to the cloud
     * @param data List of SensorData objects to serialize
     * @return JSON String
     */
    public static String createMessagePayload(List<SensorData> data) {
        try {
            JSONObject messagePayload = new JSONObject();
            for (SensorData el : data) {
                messagePayload.put(el.getSensorName(), el.getValue());
            }
            return messagePayload.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid message");
        }
    }

    /**
     * Serialize a List of SensorData objects into a JSON string, for sending to the cloud
     * @param data List of SensorData objects to serialize
     * @return JSON String
     */
    public static String createMessagePayloadWithTs(List<SensorData> data) {
        try {
            JSONObject messagePayload = new JSONObject();
            JSONObject sensor = new JSONObject();

            for (SensorData el : data) {
                sensor.put(el.getSensorName(), el.getValue());
            }
            messagePayload.put("ts",data.get(0).getTimestamp());
            messagePayload.put("values", sensor);
            return messagePayload.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid message");
        }
    }

    /**
     * Serialize a List of SensorData objects into a JSON string, for sending to the cloud
     * @param attribute List of SensorData objects to serialize
     * @return JSON String
     */
    public static String createMessagePayload(SensorData attribute) {
        try {
            JSONObject messagePayload = new JSONObject();
            JSONArray dataArray = new JSONArray();
            JSONObject sensor = new JSONObject();

            //sensor.put("timestamp_" + el.getSensorName(), el.getTimestamp());
            sensor.put(attribute.getAttributeName(), attribute.getAttributeValue());
            dataArray.put(sensor);

            //messagePayload.put("data", dataArray);
            //return messagePayload.toString();
            return sensor.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid message");
        }
    }
}
