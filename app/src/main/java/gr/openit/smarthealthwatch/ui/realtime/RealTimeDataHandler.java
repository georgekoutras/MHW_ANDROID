/*
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by johnsongar on 3/9/2017.
 */
package gr.openit.smarthealthwatch.ui.realtime;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import com.garmin.device.realtime.*;
import com.garmin.device.realtime.listeners.RealTimeDataListener;
import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.realtime.RealTimeDataManager;

import java.util.EnumSet;
import java.util.HashMap;

public class RealTimeDataHandler implements RealTimeDataListener {

    private static final String TAG = RealTimeDataHandler.class.getSimpleName();

    private static RealTimeDataHandler sInstance;

    private final HashMap<String, HashMap<RealTimeDataType, RealTimeResult>> mLatestData;

    private static long lastAccel = 0;

    public synchronized static RealTimeDataHandler getInstance() {
        if (sInstance == null) {
            sInstance = new RealTimeDataHandler();
        }

        return sInstance;
    }

    private RealTimeDataHandler() {
        mLatestData = new HashMap<>();
    }

    public HashMap<RealTimeDataType, RealTimeResult> getLatestData(String deviceAddress) {
        return mLatestData.get(deviceAddress);
    }

    public void startListening() {
        RealTimeDataManager manager = DeviceManager.getDeviceManager().getRealTimeDataManager();
        manager.addRealTimeDataListener(this, EnumSet.allOf(RealTimeDataType.class));
        for(Device device: DeviceManager.getDeviceManager().getPairedDevices()){
            manager.enableRealTimeData(device.address(), EnumSet.allOf(RealTimeDataType.class));
        }
    }

    public void stopListening() {
        RealTimeDataManager manager = DeviceManager.getDeviceManager().getRealTimeDataManager();
        manager.removeRealTimeDataListener(this, EnumSet.allOf(RealTimeDataType.class));
        for(Device device: DeviceManager.getDeviceManager().getPairedDevices()){
            manager.disableRealTimeData(device.address(), EnumSet.allOf(RealTimeDataType.class));
        }
    }

    static Long lastTime = null;

    @SuppressLint("DefaultLocale")
    public static void logRealTimeData(String tag, String macAddress, RealTimeDataType dataType, RealTimeResult result) {
        String value = "";
        //Log out the main value for each data type

        switch (dataType) {
            case STEPS:
                value = String.valueOf(result.getSteps().getCurrentStepCount());
                break;
            case HEART_RATE_VARIABILITY:
                value = String.valueOf(result.getHeartRateVariability().getHeartRateVariability());
                break;
            case CALORIES:
                value = String.valueOf(result.getCalories().getCurrentTotalCalories());
                break;
            case ASCENT:
                value = String.valueOf(result.getAscent().getCurrentMetersClimbed());
                break;
            case INTENSITY_MINUTES:
                value = String.valueOf(result.getIntensityMinutes().getTotalWeeklyMinutes());
                break;
            case HEART_RATE:
                RealTimeHeartRate heartRate = result.getHeartRate();
                value = heartRate.getCurrentHeartRate()+" "+heartRate.getCurrentRestingHeartRate()+" "+heartRate.getDailyHighHeartRate()+" "+heartRate.getDailyLowHeartRate()+" "+heartRate.getHeartRateSource().name();
                break;
            case STRESS:
                RealTimeStress stress = result.getStress();
                value = String.valueOf(stress.getStressScore());
                break;
            case ACCELEROMETER:
                RealTimeAccelerometer realTimeAccelerometer = result.getAccelerometer();

                int x = realTimeAccelerometer.getAccelerometerSamples().get(0).getX();
                int y = realTimeAccelerometer.getAccelerometerSamples().get(0).getY();
                int z = realTimeAccelerometer.getAccelerometerSamples().get(0).getZ();
                long ts1 = realTimeAccelerometer.getAccelerometerSamples().get(0).getMillisecondTimestamp();
                long ts2 = realTimeAccelerometer.getAccelerometerSamples().get(1).getMillisecondTimestamp();
                long ts3 = realTimeAccelerometer.getAccelerometerSamples().get(2).getMillisecondTimestamp();

                double magnitude = Math.sqrt(x * x + y * y + z * z) / 1000;
                value = "magnitude: " + magnitude + " x: " + x + " y: " + y + " z: " + z;

                long currentTime = realTimeAccelerometer.getAccelerometerSamples().get(0).getMillisecondTimestamp();

                if(lastTime != null)
                {
                }

                lastTime = currentTime;

                break;
            case SPO2:
                RealTimeSpo2 realTimeSpo2 = result.getSpo2();
                value = String.valueOf(realTimeSpo2.getSpo2Reading());
                break;
            case RESPIRATION:
                RealTimeRespiration realTimeRespiration = result.getRespiration();
                value = String.valueOf(realTimeRespiration.getRespirationRate());
                break;
            case BODY_BATTERY:
                RealTimeBodyBattery realTimeBodyBattery = result.getBodyBattery();
                value = String.valueOf(realTimeBodyBattery.getBodyBatteryLevel());
                break;
        }
    }

    @Override
    public void onDataUpdate(@NonNull String macAddress, @NonNull RealTimeDataType dataType, @NonNull RealTimeResult result) {
        logRealTimeData(TAG, macAddress, dataType, result);

        //Cache last received data of each type
        //Used to display values if device loses connection
        HashMap<RealTimeDataType, RealTimeResult> latestData = mLatestData.get(macAddress);
        if (latestData == null) {
            latestData = new HashMap<>();
            mLatestData.put(macAddress, latestData);
        }
        latestData.put(dataType, result);
    }
}
