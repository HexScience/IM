package com.vivavideo.imkit.fragment;


import com.vivavideo.imkit.InputView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.RongIM;
import com.vivavideo.imkit.RongNotificationManager;
import com.vivavideo.imkit.SendImageManager;
import com.vivavideo.imkit.model.ConversationInfo;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imlib.CustomServiceConfig;
import io.rong.imlib.ICustomServiceListener;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.CSCustomServiceInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.CustomServiceMode;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.message.PublicServiceCommandMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
public class ConversationFragment extends DispatchResultFragment implements AbsListView.OnScrollListener {
    private static final String TAG = "ConversationFragment";

    MessageListFragment mListFragment;
    MessageInputFragment mInputFragment;

    Conversation.ConversationType mConversationType;
    String mTargetId;
    private CSCustomServiceInfo mCustomUserInfo;

    ConversationInfo mCurrentConversationInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_conversation, container, false);
        return view;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public void onResume() {
        RongNotificationManager.getInstance().onRemoveNotification();

        super.onResume();
    }

    private InputView.OnInfoButtonClick onInfoButtonClick;
    private InputView.IInputBoardListener inputBoardListener;

    public void setOnInfoButtonClick(InputView.OnInfoButtonClick onInfoButtonClick) {
        this.onInfoButtonClick = onInfoButtonClick;

        if (mInputFragment != null)
            mInputFragment.setOnInfoButtonClick(onInfoButtonClick);
    }

    public void setInputBoardListener(InputView.IInputBoardListener inputBoardListener) {
        this.inputBoardListener = inputBoardListener;
        if (mInputFragment != null)
            mInputFragment.setInputBoardListener(inputBoardListener);
    }

    @Override
    protected void initFragment(final Uri uri) {
        RLog.d(TAG, "initFragment : " + uri);
        if (uri != null) {
            String typeStr = uri.getLastPathSegment().toUpperCase();
            mConversationType = Conversation.ConversationType.valueOf(typeStr);
            mTargetId = uri.getQueryParameter("targetId");
            if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && getActivity() != null
                    && getActivity().getIntent() != null
                    && getActivity().getIntent().getData() != null) {
                mCustomUserInfo = getActivity().getIntent().getParcelableExtra("customServiceInfo");
            }

            mCurrentConversationInfo = ConversationInfo.obtain(mConversationType, mTargetId);
            RongContext.getInstance().registerConversationInfo(mCurrentConversationInfo);

            mListFragment = (MessageListFragment) getChildFragmentManager().findFragmentById(android.R.id.list);
            mInputFragment = (MessageInputFragment) getChildFragmentManager().findFragmentById(android.R.id.toggle);

            if (mListFragment == null)
                mListFragment = new MessageListFragment();

            if (mInputFragment == null)
                mInputFragment = new MessageInputFragment();

            if (mListFragment.getUri() == null)
                mListFragment.setUri(uri);

            if (mInputFragment.getUri() == null)
                mInputFragment.setUri(uri);

            mListFragment.setOnScrollListener(this);

            if (mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
                boolean createIfNotExist = getActivity() != null && getActivity().getIntent().getBooleanExtra("createIfNotExist", true);
                int pullCount = getResources().getInteger(R.integer.rc_chatroom_first_pull_message_count);
                if (createIfNotExist)
                    RongIMClient.getInstance().joinChatRoom(mTargetId, pullCount, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            RLog.i(TAG, "joinChatRoom onSuccess : " + mTargetId);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "joinChatRoom onError : " + errorCode);
                            csWarning(getString(R.string.rc_join_chatroom_failure), false, false);
                        }
                    });
                else
                    RongIMClient.getInstance().joinExistChatRoom(mTargetId, pullCount, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            RLog.i(TAG, "joinExistChatRoom onSuccess : " + mTargetId);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "joinExistChatRoom onError : " + errorCode);
                            csWarning(getString(R.string.rc_join_chatroom_failure), false, false);
                        }
                    });
            } else if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE ||
                    mConversationType == Conversation.ConversationType.PUBLIC_SERVICE) {
                PublicServiceCommandMessage msg = new PublicServiceCommandMessage();
                msg.setCommand(PublicServiceMenu.PublicServiceMenuItemType.Entry.getMessage());
                io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, msg);
                RongIMClient.getInstance().sendMessage(message, null, null, new IRongCallback.ISendMessageCallback() {
                    @Override
                    public void onAttached(io.rong.imlib.model.Message message) {

                    }

                    @Override
                    public void onSuccess(io.rong.imlib.model.Message message) {

                    }

                    @Override
                    public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode errorCode) {

                    }
                });
            } else if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
                enterTime = System.currentTimeMillis();
                mInputFragment.setOnRobotSwitcherListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RongIMClient.getInstance().switchToHumanMode(mTargetId);
                    }
                });
                RongIMClient.getInstance().startCustomService(mTargetId, customServiceListener, mCustomUserInfo);
            }
        }
    }

    private boolean robotType = true;
    private int source = 0;
    private boolean resolved = true;
    private boolean committing = false;
    private long enterTime;
    private boolean evaluate = true;

    ICustomServiceListener customServiceListener = new ICustomServiceListener() {
        @Override
        public void onSuccess(CustomServiceConfig config) {
            if(config.isBlack) {
                String msg = config.msg;
                csWarning(msg, false, false);
            }
            if(config.robotSessionNoEva){
                evaluate = false;
                if(mListFragment != null ){
                    mListFragment.setNeedEvaluateForRobot(true);
                }
            }
        }

        @Override
        public void onError(int code, String msg) {
            csWarning(msg, false, false);
        }

        @Override
        public void onModeChanged(CustomServiceMode mode) {
            mInputFragment.setInputProviderType(mode);
            if(mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN)
                    || mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN_FIRST)) {
                robotType = false;
                evaluate = true;
            } else if(mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_NO_SERVICE)) {
                evaluate = false;
            }
            if(mListFragment != null)
                mListFragment.setRobotMode(robotType);
        }

        @Override
        public void onQuit(String msg) {
            if(!committing)
                csWarning(msg, true, false);
        }

        @Override
        public void onPullEvaluation(String dialogId) {
            if(!committing)
                submitComment(true,dialogId);
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mInputFragment = (MessageInputFragment) getChildFragmentManager().findFragmentById(android.R.id.toggle);
        if(mInputFragment != null) {
            mInputFragment.setOnInfoButtonClick(this.onInfoButtonClick);
            mInputFragment.setInputBoardListener(this.inputBoardListener);
        }
    }

    @Override
    public void onDestroyView() {
        RongContext.getInstance().unregisterConversationInfo(mCurrentConversationInfo);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        RongContext.getInstance().getEventBus().unregister(this);

        if(mConversationType != null) {
            if (mConversationType.equals(Conversation.ConversationType.CHATROOM))
                SendImageManager.getInstance().cancelSendingImages(mConversationType, mTargetId);
                RongContext.getInstance().executorBackground(new Runnable() {
                    @Override
                    public void run() {
                        RongIM.getInstance().quitChatRoom(mTargetId, new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                            }
                        });
                    }
                });
            if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
                boolean needToQuit = true;
                try {
                    needToQuit = RongContext.getInstance().getResources().getBoolean(R.bool.rc_stop_custom_service_when_quit);
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
                if(needToQuit)
                    RongIMClient.getInstance().stopCustomService(mTargetId);
            }
        }

        super.onDestroy();
    }

    @Override
    public boolean onBackPressed() {
        return submitComment(false,"");
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    private void csWarning(String msg, final boolean evaluate,final boolean isPullEva) {
        if(getActivity() == null)
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.rc_cs_alert_warning);
        TextView tv = (TextView)window.findViewById(R.id.rc_cs_msg);
        tv.setText(msg);

        window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                if(evaluate) {
                    submitComment(isPullEva,"");
                } else {
                    FragmentManager fm = getChildFragmentManager();
                    if (fm.getBackStackEntryCount() > 0)
                        fm.popBackStack();
                    else
                        getActivity().finish();
                }
            }
        });
    }

    private boolean submitComment(boolean isPullEva, final String dialogId) {
        if(mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
            if(!evaluate) {
                return false;
            }
            long currentTime = System.currentTimeMillis();
            int interval = 60;
            try {
                interval = RongContext.getInstance().getResources().getInteger(R.integer.rc_custom_service_evaluation_interval);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
            if((currentTime - enterTime < interval * 1000) && !isPullEva){
                return false;
            }
            committing = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
            Window window = alertDialog.getWindow();
            if(robotType) {
                window.setContentView(R.layout.rc_cs_alert_robot_evaluation);
                final LinearLayout linearLayout = (LinearLayout)window.findViewById(R.id.rc_cs_yes_no);
                if(resolved) {
                    linearLayout.getChildAt(0).setSelected(true);
                    linearLayout.getChildAt(1).setSelected(false);
                } else {
                    linearLayout.getChildAt(0).setSelected(false);
                    linearLayout.getChildAt(1).setSelected(true);
                }
                for(int i = 0; i < linearLayout.getChildCount(); i++) {
                    View child = linearLayout.getChildAt(i);
                    child.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setSelected(true);
                            int index = linearLayout.indexOfChild(v);
                            if(index == 0) {
                                linearLayout.getChildAt(1).setSelected(false);
                                resolved = true;
                            } else {
                                resolved = false;
                                linearLayout.getChildAt(0).setSelected(false);
                            }
                        }
                    });
                }
            } else {
                window.setContentView(R.layout.rc_cs_alert_human_evaluation);
                final LinearLayout linearLayout = (LinearLayout)window.findViewById(R.id.rc_cs_stars);
                for(int i = 0; i < linearLayout.getChildCount(); i++) {
                    View child = linearLayout.getChildAt(i);
                    if(i < source) {
                        child.setSelected(true);
                    }
                    child.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int index = linearLayout.indexOfChild(v);
                            int count = linearLayout.getChildCount();
                            source = index + 1;
                            if(!v.isSelected()) {
                                while (index >= 0) {
                                    linearLayout.getChildAt(index).setSelected(true);
                                    index--;
                                }
                            } else {
                                index++;
                                while (index < count) {
                                    linearLayout.getChildAt(index).setSelected(false);
                                    index++;
                                }
                            }
                        }
                    });
                }
            }

            window.findViewById(R.id.rc_btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    committing = false;
                    alertDialog.dismiss();
                    FragmentManager fm = getChildFragmentManager();
                    if (fm.getBackStackEntryCount() > 0)
                        fm.popBackStack();
                    else
                        getActivity().finish();
                }
            });

            window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (robotType) {
                        RongIMClient.getInstance().evaluateCustomService(mTargetId, resolved, "");
                    } else {
                        if(source > 0)
                            RongIMClient.getInstance().evaluateCustomService(mTargetId, source, null,dialogId);
                    }
                    alertDialog.dismiss();
                    committing = false;
                    FragmentManager fm = getChildFragmentManager();
                    if (fm.getBackStackEntryCount() > 0)
                        fm.popBackStack();
                    else
                        getActivity().finish();
                }
            });

            return true;
        }
        return false;
    }
}
