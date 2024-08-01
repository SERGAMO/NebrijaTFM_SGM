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

import android.os.Parcel;
import android.os.Parcelable;

public class SensorData implements Parcelable {
    private long timestamp;
    private String sensorName;
    private String attributeName;
    private double value;
    private boolean bValue;
    private String attribute;
    private String captureCamera;

    public SensorData(long timestamp, String sensorName, double value) {
        this.timestamp = timestamp;
        this.sensorName = sensorName;
        this.value = value;
    }

    public SensorData(String sensorName, double value) {
        this.sensorName = sensorName;
        this.value = value;
    }
    public SensorData(String attributeName, String attributeValue) {
        this.attributeName = attributeName;
        this.attribute = attributeValue;
    }

    public SensorData(String attributeName, boolean attributeValue) {
        this.attributeName = attributeName;
        this.bValue = attributeValue;
    }



    public long getTimestamp() {
        return timestamp;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public double getValue() {
        return value;
    }

    public String getAttributeValue() {
        return attribute;
    }

    public String getCapture() {
        return captureCamera;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(timestamp);
        out.writeString(sensorName);
        out.writeDouble(value);
        out.writeString(captureCamera);
    }

    public static final Creator<SensorData> CREATOR = new Creator<SensorData>() {
        public SensorData createFromParcel(Parcel in) {
            return new SensorData(in);
        }

        public SensorData[] newArray(int size) {
            return new SensorData[size];
        }
    };

    private SensorData(Parcel in) {
        this(in.readLong(),in.readString(), in.readFloat());
    }

    @Override
    public String toString() {
        return sensorName + " [" + timestamp + "] " + value;
    }
}