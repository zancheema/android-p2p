package com.zancheema.android.p2ptest;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "FileServerAsyncTask";

    private Context context;
    private TextView statusText;

    public FileServerAsyncTask(Context context, View statusText) {
        Log.d(TAG, "FileServerAsyncTask: called");
        this.context = context;
        this.statusText = (TextView) statusText;
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, "doInBackground: called");
        try {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket client = serverSocket.accept();
            Log.d(TAG, "doInBackground: accepted");

            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */


            final File f = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/"
                    + "/wifip2pshared-" + System.currentTimeMillis()
                    + ".jpg");

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            f.createNewFile();
            InputStream inputstream = client.getInputStream();
            copyFile(inputstream, new FileOutputStream(f));
            serverSocket.close();
            return f.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void copyFile(InputStream inputstream, FileOutputStream fileOutputStream) {
            try {
                int c;
                while ((c = inputstream.read()) != -1) {
                    fileOutputStream.write(c);
                }
            } catch (IOException e) {
                Log.d(TAG, "copyFile: " + e.getMessage());
                e.printStackTrace();
            }
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "onPostExecute: called");
        if (result != null) {
            statusText.setText("File copied - " + result);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("content://" + result), "image/*");
            context.startActivity(intent);
        }
    }
}
