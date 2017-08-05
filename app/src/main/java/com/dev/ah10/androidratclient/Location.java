package com.dev.ah10.androidratclient;


import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

class Location //Class to interact with location and gps
{
    private Context ctx; //The context the class will use

    Location(Context applicationContext) //Constructor
    {
        ctx = applicationContext; //Set the application context
    }

    android.location.Location GetLocationData() //Get the device location
    {
        LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE); //Get the location service
        try
        {
            Log.e("Location", "Location acquired");
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //Request a single location object
        }
        catch (SecurityException ex)
        {
            Log.e("Location", "Error: " + ex.toString()); //Failed to get location
            return null;
        }
    }
}
