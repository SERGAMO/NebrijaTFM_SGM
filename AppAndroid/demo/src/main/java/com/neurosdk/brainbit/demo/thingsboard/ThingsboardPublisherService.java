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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handle asynchronous cloud sensor logging requests via a Binder interface. Sensor events are
 * periodically published to the cloud via a {@link ThingsboardPublisher}.
 * <p>
 */
public class ThingsboardPublisherService extends Service {
    private static final String TAG = " ThingsboardPublisherService";

    private static final String INTENT_CONFIGURE_ACTION = "com.catbcn.catthingstunnel.thingsboard.mqtt.CONFIGURE";
    private static final String CONFIG_SHARED_PREFERENCES_KEY = "thingsboard_config";

    // Will store at most this amount of most recent sensor change events, per sensor type
    private static final int BUFFER_SIZE_FOR_ONCHANGE_SENSORS = 10;

    //public static long PUBLISH_INTERVAL_MS = TimeUnit.SECONDS.toMillis(2);
    public static long mInterval = 0;
    private static final long PUBLISH_INTERVAL_MS = 250;

    // After this amount of tentatives, the publish interval will change from PUBLISH_INTERVAL_MS
    // to BACKOFF_INTERVAL_MS until a successful connection has been established.
    private static final long ERRORS_TO_INITIATE_BACKOFF = 20;
    private static final long BACKOFF_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    private Looper mServiceLooper;
    private Handler mServiceHandler;
    private ThingsboardPublisher mPublisher;
    private boolean mEnablePublish = false;

    private AtomicInteger mUnsuccessfulTentatives = new AtomicInteger(0);

    private final ConcurrentHashMap<String, SensorData> mMostRecentData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriorityBlockingQueue<SensorData>> mOnChangeData =
            new ConcurrentHashMap<>();

    //TODO: Se para al iniciar calibración
    private final Runnable mSensorConsumerRunnable = new Runnable() {
        @Override
        public void run() {
            long delayForNextTentative = PUBLISH_INTERVAL_MS;
            //Log.d("PUBLISH", "Tiempo: " + PUBLISH_INTERVAL_MS);
            try {
                initializeIfNeeded();
                processCollectedSensorData();
                mUnsuccessfulTentatives.set(0);
            } catch (Throwable t) {
                if (mUnsuccessfulTentatives.get() >= ERRORS_TO_INITIATE_BACKOFF) {
                    delayForNextTentative = BACKOFF_INTERVAL_MS;
                } else {
                    mUnsuccessfulTentatives.incrementAndGet();
                }
                Log.e(TAG, String.format(Locale.getDefault(),
                        "Cannot publish. %d unsuccessful tentatives, will try again in %d ms",
                        mUnsuccessfulTentatives.get(), delayForNextTentative), t);
            } finally {
                mServiceHandler.postDelayed(this, delayForNextTentative);
            }
        }
    };

    /**
     * Store sensor data so that it can be published in the next publishing cycle. Unlike
     * the other log methods, this method saves the {@link #BUFFER_SIZE_FOR_ONCHANGE_SENSORS} most
     * recent sensor readings per sensor type.
     * @param data
     */
    public void logSensorDataOnChange(SensorData data) {
        PriorityBlockingQueue<SensorData> newQueue =
                new PriorityBlockingQueue<SensorData>(BUFFER_SIZE_FOR_ONCHANGE_SENSORS,
                        new Comparator<SensorData>() {
            @Override
            public int compare(SensorData o1, SensorData o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });
        PriorityBlockingQueue<SensorData> lastData = mOnChangeData.putIfAbsent(
                data.getSensorName(), newQueue);

        if (lastData == null) {
            lastData = newQueue;
        }

        // remove old entries if necessary
        while (lastData.size() >= BUFFER_SIZE_FOR_ONCHANGE_SENSORS) {
           lastData.poll();
        }

        lastData.offer(data);
    }

    /**
     * Store sensor data so that it can be published in the next publishing cycle. For a given
     * {@link SensorData#getSensorName()}, only the most recent data is kept.
     * @param data
     */
    public void logSensorData(SensorData data) {
        mMostRecentData.put(data.getSensorName(), data);
    }

    public void logSensorData(List<SensorData> data) {
        for (SensorData singleData : data) {
            logSensorData(singleData);
        }
    }

    public void publishAttribute(SensorData message){
        mPublisher.publishAttribute(message);
    }

    public void publishTelemetry(List<SensorData> data) {
        mPublisher.publishTelemetry(data);
    }

     public void enablePublishing(boolean enable){
        Log.d(TAG, "Change enable publish to " + enable);
        mEnablePublish = enable;
    }

    @WorkerThread
    private void processCollectedSensorData() {
        if (mPublisher == null || !mPublisher.isReady() || !mEnablePublish) {
            Log.i(TAG, "NO PUBLISH!!!!!!!!!");
            return;
        }
        ArrayList<SensorData> data = new ArrayList<>();

        // get sensorData from continuous sensors
        for (String sensorName : mMostRecentData.keySet()) {
            data.add(mMostRecentData.remove(sensorName));
        }

        // get sensorData from onChange sensors
        for (String sensorName : mOnChangeData.keySet()) {
            mOnChangeData.get(sensorName).drainTo(data);
        }

        Log.i(TAG, "publishing " + data.size() + " sensordata elements");
        mPublisher.publishTelemetry(data);
    }

    // Support for service binding
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ThingsboardPublisherService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ThingsboardPublisherService.this;
        }
    }

    private ThingsboardIotOptions readOptions(Intent intent) {
        ThingsboardIotOptions options = ThingsboardIotOptions.from(
                getSharedPreferences(CONFIG_SHARED_PREFERENCES_KEY, MODE_PRIVATE));
        if (intent != null) {
            options = ThingsboardIotOptions.reconfigure(options, intent.getExtras());
        }
        return options;
    }

    private void saveOptions(ThingsboardIotOptions options) {
        options.saveToPreferences(getSharedPreferences(
                CONFIG_SHARED_PREFERENCES_KEY, MODE_PRIVATE));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeIfNeeded();
        HandlerThread thread = new HandlerThread("ThingsboardPublisherService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new Handler(mServiceLooper);
        mServiceHandler.postDelayed(mSensorConsumerRunnable, PUBLISH_INTERVAL_MS);
    }

    public void setNewLogTime(long time){
        mServiceHandler.postDelayed(mSensorConsumerRunnable, time);
    }

    private void initializeIfNeeded() {
        if (mPublisher == null) {
            try {
                mPublisher = new MQTTPublisher(readOptions(null), getApplicationContext());
            } catch (Throwable t) {
                Log.e(TAG, "Could not create MQTTPublisher. Will try again later", t);
            }
        }
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (INTENT_CONFIGURE_ACTION.equals(action)) {
            Log.i(TAG, "Configuring publisher with intent.");
            ThingsboardIotOptions options = readOptions(intent);
            saveOptions(options);
            if (mPublisher != null) {
                mPublisher.reconfigure(options);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
        mServiceLooper = null;
    }

}
