package me.chon.downloader.core;

import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import me.chon.downloader.util.Constants;

/**
 * Created by chon on 2016/10/25.
 * What? How? Why?
 */
public class ConnectThread implements Runnable {

    private final String url;
    private final ConnectListener listener;
    private volatile boolean isRunning;

    public ConnectThread(String url, ConnectListener listener) {
        this.url = url;
        this.listener = listener;
    }

    @Override
    public void run() {
        isRunning = true;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            // FIXME add the header,while download big file(>Integer.MAX_VALUE),contentLength will return -1
//            connection.setRequestProperty("Range","bytes=0-"+Integer.MAX_VALUE);
            connection.setConnectTimeout(Constants.CONNECT_TIME);
            connection.setReadTimeout(Constants.READ_TIME);
            int responseCode = connection.getResponseCode();
            int contentLength = connection.getContentLength();
            boolean isSupportRange;
            // TODO redirect,get the real url.
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String ranges = connection.getHeaderField("Accept-Ranges");
                isSupportRange = "bytes".equals(ranges);
                listener.onConnected(isSupportRange, contentLength);
            } else {
                listener.onConnectError("server error:" + responseCode);
            }
//            if (responseCode == HttpURLConnection.HTTP_PARTIAL){
//                isSupportRange = true;
//            }else {
//
//            }

            isRunning = false;
        } catch (IOException e) {
            isRunning = false;
            String message = e.getMessage();
            if (TextUtils.isEmpty(message)) {
                message = e.toString();
            }
            listener.onConnectError(message);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

    interface ConnectListener {
        void onConnected(boolean isSupportRange, int totalLength);

        void onConnectError(String message);
    }
}

