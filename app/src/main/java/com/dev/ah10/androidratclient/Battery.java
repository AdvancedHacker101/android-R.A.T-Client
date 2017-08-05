package com.dev.ah10.androidratclient;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

class Battery //Class to interact with the battery data
{
    private Intent batteryIntent; //The battery intent containing the battery data

    Battery(Context applicationContext) //Constructor
    {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED); //Create a new intent filter
        batteryIntent = applicationContext.registerReceiver(null, iFilter); //Get the battery intent
    }

    String getBatteryLevel() //Get battery percentage level
    {
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float chargeLevel = level / (float)scale;
        chargeLevel *= 100;

        return Float.toString(chargeLevel);
    }

    boolean isBatteryCharging() //is battery connected to power source?
    {
        int batteryStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        return batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    String getBatteryChargingMethod() //How is the battery charging (usb, ac, wireless)
    {
        int batteryChargeMethod = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String method = "N/A";
        if (!isBatteryCharging()) return method;

        switch (batteryChargeMethod)
        {
            case BatteryManager.BATTERY_PLUGGED_AC:
                method = "Charging with AC";
            break;

            case BatteryManager.BATTERY_PLUGGED_USB:
                method = "Charging with USB";
                break;

            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                method = "Charging with Wireless Charger";
                break;
        }

        return method;
    }

    String getBatteryTemperature() //How hot is the battery? (celsius degree)
    {
        float batteryTemperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        batteryTemperature /= 10; //Temperature is returned in centigrade
        //A centigrade is 10 times more than the actual celsius degree we want

        return Float.toString(batteryTemperature);
    }
}
