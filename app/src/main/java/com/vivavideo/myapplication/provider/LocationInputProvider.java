package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.RongIM;
import com.vivavideo.myapplication.model.Event;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.rong.common.RLog;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.LocationMessage;

public class LocationInputProvider extends InputProvider.ExtendProvider {
    private final static String TAG = "LocationInputProvider";
    Message mCurrentMessage;

    public LocationInputProvider(RongContext context) {
        super(context);
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_location);
    }

    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.rc_plugins_location);
    }

    @Override
    public void onPluginClick(View view) {
        if (RongContext.getInstance() != null && RongContext.getInstance().getLocationProvider() != null) {
            RongContext.getInstance().getLocationProvider().onStartLocation(getContext(), new RongIM.LocationProvider.LocationCallback() {

                @Override
                public void onSuccess(final LocationMessage locationMessage) {

                    RongIM.getInstance().insertMessage(mCurrentConversation.getConversationType(), mCurrentConversation.getTargetId(), RongIM.getInstance().getCurrentUserId(), locationMessage, new RongIMClient.ResultCallback<Message>() {
                        @Override
                        public void onSuccess(Message message) {
                            if (locationMessage.getImgUri() != null) {
                                if (locationMessage.getImgUri().getScheme().equals("http")) {
                                    message.setContent(locationMessage);
                                    getContext().executorBackground(new DownloadRunnable(message, locationMessage.getImgUri()));
                                } else if(locationMessage.getImgUri().getScheme().equals("file")){
                                    RongIM.getInstance().sendMessage(message, null, null, new IRongCallback.ISendMessageCallback() {
                                        @Override
                                        public void onAttached(Message message) {

                                        }

                                        @Override
                                        public void onSuccess(Message message) {

                                        }

                                        @Override
                                        public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                                        }
                                    });
                                } else {
                                    RLog.e(TAG, "onPluginClick " + locationMessage.getImgUri().getScheme() + " scheme does not support!");
                                }
                            } else {
                                RLog.e(TAG, "onPluginClick File does not exist!");
                            }
                        }
                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                }
                @Override
                public void onFailure(String msg) {

                }
            });
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
    }

    class DownloadRunnable implements Runnable {
        private Message message;
        private Uri uri;

        public DownloadRunnable(Message message, Uri uri) {
            this.message = message;
            this.uri = uri;
        }

        @Override
        public void run() {
            mCurrentMessage = message;
            final Event.OnReceiveMessageProgressEvent event = new Event.OnReceiveMessageProgressEvent();
            LocationMessage locationMessage = (LocationMessage) message.getContent();
            event.setMessage(message);
            event.setProgress(100);
            getContext().getEventBus().post(event);

            try {
                URL url = new URL(uri.toString());
                final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setReadTimeout(5000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    final File file = new File(getDataPath(getContext()) + message.getMessageId() + ".tmp");
                    InputStream is = conn.getInputStream();
                    OutputStream os = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    is.close();
                    os.close();

                    locationMessage.setImgUri(Uri.fromFile(file));
                    RongIM.getInstance().sendMessage(message, null, null, new IRongCallback.ISendMessageCallback() {
                        @Override
                        public void onAttached(Message message) {

                        }

                        @Override
                        public void onSuccess(Message message) {

                        }

                        @Override
                        public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                        }
                    });

                } else {
                    message.setSentStatus(Message.SentStatus.FAILED);
                    getContext().getEventBus().post(event);
                }
            } catch (Exception e) {
                RLog.e(TAG, "DownloadRunnable get thumbnail file fail.");
                e.printStackTrace();
                message.setSentStatus(Message.SentStatus.FAILED);
                getContext().getEventBus().post(event);
            }
        }
    }

    private String getDataPath(Context context) {
        String path;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            path = Environment.getExternalStorageDirectory().getPath() + "/" +context.getPackageName() + "/img_cache";
        else
            path = context.getFilesDir().getPath() + "/" +context.getPackageName() + "/img_cache";
        if (!path.endsWith("/"))
            path = path + "/";
        File file = new File(path);
        if(!file.exists())
            file.mkdirs();
        return path;
    }
}
