package com.zancheema.android.p2ptest;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FileClientAsyncTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "FileClientAsyncTask";

    Context context;
    String host;
    int port;
    int len;
    Socket socket = new Socket();
    byte buf[]  = new byte[1024];
    String path;

    public FileClientAsyncTask(Context context, String host, int port, String path) {
        this.context = context;
        this.host = host;
        this.port = port;
        this.path = path;

        Log.d(TAG, "FileClientAsyncTask: path: " + path);
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 2000);

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data is retrieved by the server device.
             */
            OutputStream outputStream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream inputStream = null;
            inputStream = cr.openInputStream(Uri.parse(path));
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "doInBackground: " + e.getMessage());
            return e.getMessage();
            //catch logic
        } catch (IOException e) {
            Log.d(TAG, "doInBackground: " + e.getMessage());
            return e.getMessage();
            //catch logic
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
            return "Image Sent Successfully.";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        Log.d(TAG, "onPostExecute: " + result);
    }
}
