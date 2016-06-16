package com.vivavideo.imkit.provider;

import com.vivavideo.imkit.widget.InputView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.RongIM;
import com.vivavideo.imkit.fragment.MessageInputFragment;
import com.vivavideo.imkit.model.Event;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.message.VoiceMessage;

public class VoiceInputProvider extends InputProvider.MainInputProvider implements View.OnTouchListener, Handler.Callback {
    private final static String TAG = "VoiceInputProvider";
    Button mVoiceBtn;
    PopupWindow mPopWindow;
    LayoutInflater mInflater;
    float lastTouchY;
    float mOffsetLimit;
    private AudioManager mAudioManager;
    private Uri mCurrentRecUri;
    long mVoiceLength;
    int mMaxDuration = 60;

    private ImageView mIcon;
    private TextView mText, mMessage;
    private Handler mHandler;
    private int mStatus = MSG_NORMAL;

    private static final int MSG_NORMAL = 0;
    private static final int MSG_SEC = 1;
    private static final int MSG_REC = 6;
    private static final int MSG_CANCEL = 2;
    private static final int MSG_SHORT = 7;
    private static final int MSG_SAMPLING = 3;
    private static final int MSG_COMPLETE = 5;
    private static final int MSG_READY = 4;
    private MediaRecorder mMediaRecorder;
    private boolean isSend = false;

    @Override
    public void onAttached(MessageInputFragment fragment, InputView inputView) {
        super.onAttached(fragment, inputView);
        mHandler = new Handler(fragment.getActivity().getMainLooper(), this);
        mAudioManager = (AudioManager) fragment.getActivity().getSystemService(Context.AUDIO_SERVICE);
        mOffsetLimit = 70 * fragment.getActivity().getResources().getDisplayMetrics().density;

    }

    @Override
    public void onSwitch(Context context) {

    }

    @Override
    public void onDetached() {
        mHandler = null;
        mAudioManager = null;
        super.onDetached();
    }

    public VoiceInputProvider(RongContext context) {
        super(context);
    }

    @Override
    public Drawable obtainSwitchDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_voice);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, InputView inputView) {
        mInflater = inflater;
        View view = inflater.inflate(R.layout.rc_wi_vo_provider, parent);
        mVoiceBtn = (Button) view.findViewById(android.R.id.button1);
        mVoiceBtn.setOnTouchListener(this);
        return view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Class<?> classType = Context.class;
                Method method = classType.getMethod("checkSelfPermission", new Class[]{String.class});
                int hasPermission = (int) method.invoke(v.getContext(), new Object[]{Manifest.permission.RECORD_AUDIO});

                if (hasPermission != PackageManager.PERMISSION_GRANTED && RongContext.getInstance().getRequestPermissionListener() != null) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        RongContext.getInstance().getRequestPermissionListener().onPermissionRequest(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            onActive(v.getContext());
            lastTouchY = event.getY();
            mHandler.obtainMessage(MSG_READY, v.getRootView()).sendToTarget();
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (event.getEventTime() - event.getDownTime() < 1000)
                mStatus = MSG_SHORT;
            isSend = false;
            mHandler.obtainMessage(MSG_COMPLETE).sendToTarget();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (lastTouchY - event.getY() > mOffsetLimit) {
                mHandler.obtainMessage(MSG_CANCEL).sendToTarget();
            } else {
                mHandler.obtainMessage(MSG_REC).sendToTarget();

            }
        }
        return false;
    }

    @Override
    public void onActive(Context context) {
    }

    @Override
    public void onInactive(Context context) {

    }

    private int mCurrentDuration = -1;

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_READY:
                if (mPopWindow == null) {
                    View view = mInflater.inflate(R.layout.rc_wi_vo_popup, null);

                    mIcon = (ImageView) view.findViewById(android.R.id.icon);
                    mText = (TextView) view.findViewById(android.R.id.text1);
                    mMessage = (TextView) view.findViewById(android.R.id.message);
                    mPopWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                    View parent = (View) msg.obj;
                    mPopWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
                    mPopWindow.setFocusable(true);
                    mPopWindow.setOutsideTouchable(false);
                    mPopWindow.setTouchable(false);
                }

                //isShowMessageTyping 是否显示输入状态
                boolean isShowMessageTyping = RongIMClient.getInstance().getTypingStatus();
                if (isShowMessageTyping == true) {
                    MessageTag tag = VoiceMessage.class.getAnnotation(MessageTag.class);
                    onTypingMessage(tag.value());
                }

                startRec();
                mStatus = MSG_READY;
                mVoiceLength = SystemClock.elapsedRealtime();

                if (mMaxDuration <= 10 && mMaxDuration > 0) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_SEC, mMaxDuration, 0));
                } else {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SEC, 10, 0),
                            (mMaxDuration - 10) * 1000);
                }
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SAMPLING),
                        150);

                mMessage.setText(R.string.rc_voice_rec);
                mIcon.setImageResource(R.drawable.rc_ic_volume_1);
                mMessage.setBackgroundColor(Color.TRANSPARENT);

                break;
            case MSG_REC:

                if (mStatus == MSG_CANCEL) {
//                    mHandler.sendMessage(mHandler.obtainMessage(MSG_SAMPLING));
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SAMPLING), 150);
                }
                mStatus = MSG_REC;
                mMessage.setText(R.string.rc_voice_rec);
                mMessage.setBackgroundColor(Color.TRANSPARENT);
                break;
            case MSG_CANCEL:
                mMessage.setText(R.string.rc_voice_cancel);
                mText.setVisibility(View.GONE);
                mIcon.setVisibility(View.VISIBLE);
                mIcon.setImageResource(R.drawable.rc_ic_volume_cancel);
                mMessage.setBackgroundColor(RongContext.getInstance().getResources().getColor(R.color.rc_voice_cancle));
                mCurrentDuration = -1;
                mStatus = MSG_CANCEL;
                break;
            case MSG_SEC:
                if (msg.arg1 == mMaxDuration || mStatus == MSG_REC) {
                    mIcon.setVisibility(View.GONE);
                    mText.setText(msg.arg1 + "");
                    mCurrentDuration = msg.arg1;
                    mText.setVisibility(View.VISIBLE);
                }

                if (msg.arg1 > 0)
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(MSG_SEC, --msg.arg1, 0), 1000);

                else {
                    Message message = Message.obtain();
                    isSend = true;
                    message.what = MSG_COMPLETE;
                    message.obj = true;
                    mHandler.sendMessage(message);
                }
                break;
            case MSG_SAMPLING:
                if (mStatus == MSG_CANCEL || mStatus == MSG_SHORT)
                    break;

                if (mCurrentDuration <= 10 && mCurrentDuration > 0) {
                    mIcon.setVisibility(View.GONE);
                    break;
                }
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SAMPLING),
                        150);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    try {
                        TelephonyManager manager = (TelephonyManager) RongContext.getInstance()
                                .getSystemService(Context.TELEPHONY_SERVICE);

                        manager.listen(new PhoneStateListener() {
                            @Override
                            public void onCallStateChanged(int state, String incomingNumber) {
                                switch (state) {
                                    case TelephonyManager.CALL_STATE_IDLE:
                                        break;
                                    case TelephonyManager.CALL_STATE_RINGING:
                                        stopRec(false);
                                        break;
                                    case TelephonyManager.CALL_STATE_OFFHOOK:
                                        break;
                                }
                                super.onCallStateChanged(state, incomingNumber);
                            }
                        }, PhoneStateListener.LISTEN_CALL_STATE);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }

                int db = getCurrentVoiceDb();
                switch (db / 5) {
                    case 0:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_1);
                        break;
                    case 1:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_2);
                        break;
                    case 2:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_3);
                        break;
                    case 3:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_4);
                        break;
                    case 4:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_5);
                        break;
                    case 5:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_6);
                        break;
                    case 6:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_7);
                        break;
                    default:
                        mIcon.setImageResource(R.drawable.rc_ic_volume_8);
                        break;
                }
                break;
            case MSG_COMPLETE:
                mHandler.removeMessages(MSG_READY);
                mHandler.removeMessages(MSG_SEC);
                mHandler.removeMessages(MSG_CANCEL);
                mHandler.removeMessages(MSG_SAMPLING);
                mCurrentDuration = -1;

                if (mStatus == MSG_CANCEL) {
                    if (mPopWindow != null && mPopWindow.isShowing()) {
                        mPopWindow.dismiss();
                        mPopWindow = null;
                    }
                    if (isSend)
                        stopRec(true);
                    else
                        stopRec(false);
                } else if (mStatus == MSG_SHORT) {
                    mIcon.setImageResource(R.drawable.rc_ic_volume_wraning);
                    mMessage.setText(R.string.rc_voice_short);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mPopWindow != null && mPopWindow.isShowing()) {
                                {
                                    mPopWindow.dismiss();
                                    mPopWindow = null;
                                    stopRec(false);
                                }
                            }
                        }
                    }, 200);

                } else {
                    if (mPopWindow != null && mPopWindow.isShowing()) {
                        mPopWindow.dismiss();
                        mPopWindow = null;
                    }
                    stopRec(true);
                }
                break;
        }
        return false;
    }

    public class VoiceException extends RuntimeException {
        public VoiceException(Throwable e) {
            super(e);
        }
    }

    public void setMaxVoiceDuration(int duration) {
        if (duration < 5 || duration > 60)
            return;
        mMaxDuration = duration;
    }

    public int getMaxDuration() {
        return mMaxDuration;
    }

    public int getCurrentVoiceDb() {
        if (mMediaRecorder == null)
            return 0;
        return mMediaRecorder.getMaxAmplitude() / 600;
    }

    private void startRec() throws VoiceException {
        muteAudioFocus(RongContext.getInstance(), true);
        RongContext.getInstance().getEventBus().post(Event.VoiceInputOperationEvent.obtain(Event.VoiceInputOperationEvent.STATUS_INPUTING));
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        try {
            mMediaRecorder = new MediaRecorder();
            try {
                int bps = getContext().getResources().getInteger(R.integer.rc_audio_encoding_bit_rate);
                mMediaRecorder.setAudioSamplingRate(8000);
                mMediaRecorder.setAudioEncodingBitRate(bps);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
            mMediaRecorder.setAudioChannels(1);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mCurrentRecUri = Uri.fromFile(new File(mContext.getCacheDir(), System.currentTimeMillis() + "temp.voice"));
            mMediaRecorder.setOutputFile(mCurrentRecUri.getPath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (RuntimeException ex) {
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();
                mMediaRecorder.release();
            }
            mMediaRecorder = null;
            ex.printStackTrace();
        } catch (IOException e) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            e.printStackTrace();
        }

        mStatus = MSG_READY;
    }

    private void stopRec(boolean save) throws VoiceException {
        boolean isError = false;
        muteAudioFocus(RongContext.getInstance(), false);
        if (mMediaRecorder == null)
            return;

        RongContext.getInstance().getEventBus().post(Event.VoiceInputOperationEvent.obtain(Event.VoiceInputOperationEvent.STATUS_INPUT_COMPLETE));

        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        if (!save) {
            File file = new File(mCurrentRecUri.getPath());
            if (file.exists())
                file.delete();
            mCurrentRecUri = null;
        } else {
            int length = (int) ((SystemClock.elapsedRealtime() - mVoiceLength) / 1000 + 400);
            if (length == 400) {
                return;
            }

            File file = new File(mCurrentRecUri.getPath());
            if (!file.exists()) {
                return;
            }

            MediaPlayer player = new MediaPlayer();
            try {
                FileInputStream fis = new FileInputStream(file);
                player.setDataSource(fis.getFD());
                player.prepare();
            } catch (IllegalArgumentException e) {
                isError = true;
                e.printStackTrace();
            } catch (SecurityException e) {
                isError = true;
                e.printStackTrace();
            } catch (IllegalStateException e) {
                isError = true;
                e.printStackTrace();
            } catch (IOException e) {
                isError = true;
                e.printStackTrace();
            } finally {
                player.stop();
                player.release();
                player = null;
            }
            if (isError) {
                Toast.makeText(RongContext.getInstance(), RongContext.getInstance().getResources().getString(R.string.rc_voice_failure), Toast.LENGTH_SHORT).show();
                return;
            }

            if (mCurrentRecUri != null) {
                publish(VoiceMessage.obtain(mCurrentRecUri, (int) (SystemClock.elapsedRealtime() - mVoiceLength) / 1000), new RongIMClient.ResultCallback<io.rong.imlib.model.Message>() {
                    @Override
                    public void onSuccess(io.rong.imlib.model.Message message) {
                        io.rong.imlib.model.Message.ReceivedStatus status = message.getReceivedStatus();
                        status.setListened();
                        RongIM.getInstance().setMessageReceivedStatus(message.getMessageId(), status);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            }
        }

        mStatus = MSG_NORMAL;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private boolean muteAudioFocus(Context context, boolean bMute) {
        if (context == null) {
            RLog.d(TAG, "muteAudioFocus context is null.");
            return false;
        }
        if (Build.VERSION.SDK_INT < 8) {
            // 2.1以下的版本不支持下面的API：requestAudioFocus和abandonAudioFocus
            RLog.d(TAG, "muteAudioFocus Android 2.1 and below can not stop music");
            return false;
        }
        boolean bool = false;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (bMute) {
            int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = am.abandonAudioFocus(null);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        RLog.d(TAG, "muteAudioFocus pauseMusic bMute=" + bMute + " result=" + bool);
        return bool;
    }

}
