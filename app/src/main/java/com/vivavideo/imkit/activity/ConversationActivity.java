package com.vivavideo.imkit.activity;

import com.sea_monster.exception.BaseException;
import com.sea_monster.network.AbstractHttpRequest;
import com.vivavideo.imkit.DemoApi;
import com.vivavideo.imkit.DemoContext;
import com.vivavideo.imkit.eventbus.EventBus;
import com.vivavideo.imkit.widget.InputView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.RongIM;
import com.vivavideo.imkit.database.UserInfos;
import com.vivavideo.imkit.fragment.ConversationFragment;
import com.vivavideo.imkit.fragment.UriFragment;
import com.vivavideo.imkit.model.RongEvent;
import com.vivavideo.imkit.provider.InputProvider;
import com.vivavideo.imkit.provider.TextInputProvider;
import com.vivavideo.imkit.utils.Constants;
import com.vivavideo.imkit.widget.LoadingDialog;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.TypingMessage.TypingStatus;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

//import io.rong.imlib.TypingMessage.TypingStatus;

/**
 * Created by Bob on 15/11/3.
 * 会话页面
 * 1，设置 ActionBar title
 * 2，加载会话页面
 * 3，push 和 通知 判断
 */
public class ConversationActivity extends BaseApiActivity implements RongIMClient.RealTimeLocationListener, Handler.Callback {

    private String TAG = ConversationActivity.class.getSimpleName();
    /**
     * 对方id
     */
    private String mTargetId;
    /**
     * 刚刚创建完讨论组后获得讨论组的targetIds
     */
    private String mTargetIds;
    /**
     * 会话类型
     */
    private Conversation.ConversationType mConversationType;
    /**
     * title
     */
    private String title;
    /**
     * 是否在讨论组内，如果不在讨论组内，则进入不到讨论组设置页面
     */
    private boolean isDiscussion = false;
    private boolean isFromPush = false;

    private RelativeLayout mRealTimeBar;//real-time bar
    private RealTimeLocationConstant.RealTimeLocationStatus currentLocationStatus;
    private LoadingDialog mDialog;

    private final String TextTypingTitle = "对方正在输入...";
    private final String VoiceTypingTitle = "对方正在讲话...";

    private Handler mHandler;

    public static final int SET_TEXT_TYPING_TITLE = 1;
    public static final int SET_VOICE_TYPING_TITLE = 2;
    public static final int SET_TARGETID_TITLE = 0;

    private ConversationFragment fragment;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "------------onCreate--------");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation);

        mDialog = new LoadingDialog(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);

        Intent intent = getIntent();

        if (intent == null || intent.getData() == null)
            return;

        mTargetId = intent.getData().getQueryParameter("targetId");
        //10000 为 Demo Server 加好友的 id，若 targetId 为 10000，则为加好友消息，默认跳转到 NewFriendListActivity
        // Demo 逻辑
        if (mTargetId != null && mTargetId.equals("10000")) {
//            startActivity(new Intent(ConversationActivity.this, NewFriendListActivity.class));
            return;
        }
        //intent.getData().getLastPathSegment();//获得当前会话类型
        mConversationType = Conversation.ConversationType.valueOf(intent.getData()
                .getLastPathSegment().toUpperCase(Locale.getDefault()));

        title = intent.getData().getQueryParameter("title");

        mTargetIds = intent.getData().getQueryParameter("targetIds");

        setActionBarTitle(mConversationType, mTargetId);

        //讨论组 @ 消息
//        checkTextInputEditTextChanged();

        isPushMessage(intent);

        //地理位置共享，若不是用地理位置共享，可忽略
//        setRealTime();


        if ("ConversationActivity".equals(this.getClass().getSimpleName()))
            EventBus.getDefault().register(this);

//        if (Build.VERSION.SDK_INT >= 23) {
//            RongIM.getInstance().setRequestPermissionListener(new RongIM.RequestPermissionsListener() {
//                @Override
//                public void onPermissionRequest(String[] permissions, final int requestCode) {
//                    for (final String permission : permissions) {
//                        if (shouldShowRequestPermissionRationale(permission)) {
//                            requestPermissions(new String[]{permission}, requestCode);
//                        } else {
//                            int isPermissionGranted = checkSelfPermission(permission);
//                            if (isPermissionGranted != PackageManager.PERMISSION_GRANTED) {
//                                new AlertDialog.Builder(ConversationActivity.this)
//                                        .setMessage("你需要在设置里打开以下权限:" + permission)
//                                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
//                                            @Override
//                                            public void onClick(DialogInterface dialog, int which) {
//                                                requestPermissions(new String[]{permission}, requestCode);
//                                            }
//                                        })
//                                        .setNegativeButton("取消", null)
//                                        .create().show();
//                            }
//                            return;
//                        }
//                    }
//                }
//            });
//        }


        mHandler = new Handler(this);

        RongIMClient.setTypingStatusListener(new RongIMClient.TypingStatusListener() {
            @Override
            public void onTypingStatusChanged(Conversation.ConversationType type, String targetId, Collection<TypingStatus> typingStatusSet) {
                //当输入状态的会话类型和targetID与当前会话一致时，才需要显示
                if (type.equals(mConversationType) && targetId.equals(mTargetId)) {
                    int count = typingStatusSet.size();
                    RLog.d(TAG, "onTypingStatusChanged, count = " + count);
                    //count表示当前会话中正在输入的用户数量，目前只支持单聊，所以判断大于0就可以给予显示了
                    if (count > 0) {
                        Iterator iterator = typingStatusSet.iterator();
                        TypingStatus status = (TypingStatus) iterator.next();
                        String objectName = status.getTypingContentType();

                        MessageTag textTag = TextMessage.class.getAnnotation(MessageTag.class);
                        MessageTag voiceTag = VoiceMessage.class.getAnnotation(MessageTag.class);
                        //匹配对方正在输入的是文本消息还是语音消息
                        if (objectName.equals(textTag.value())) {
                            mHandler.sendEmptyMessage(SET_TEXT_TYPING_TITLE);
                        } else if (objectName.equals(voiceTag.value())) {
                            mHandler.sendEmptyMessage(SET_VOICE_TYPING_TITLE);
                        }
                    } else {//当前会话没有用户正在输入，标题栏仍显示原来标题
                        mHandler.sendEmptyMessage(SET_TARGETID_TITLE);
                    }
                }
            }
        });

    }

    private String mEditText;

    private void checkTextInputEditTextChanged() {

        InputProvider.MainInputProvider provider = RongContext.getInstance().getPrimaryInputProvider();
        if (provider instanceof TextInputProvider) {
            TextInputProvider textInputProvider = (TextInputProvider) provider;
            textInputProvider.setEditTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    if (mConversationType.equals(Conversation.ConversationType.DISCUSSION)) {

                        if (s.length() > 0) {
                            String str = s.toString().substring(s.toString().length() - 1, s.toString().length());

                            if (str.equals("@")) {

//                                Intent intent = new Intent(ConversationActivity.this, NewTextMessageActivity.class);
//                                intent.putExtra("DEMO_REPLY_CONVERSATIONTYPE", mConversationType.toString());
//
//                                if (mTargetIds != null) {
//                                    UriFragment fragment = (UriFragment) getSupportFragmentManager().getFragments().get(0);
//                                    //得到讨论组的 targetId
//                                    mTargetId = fragment.getUri().getQueryParameter("targetId");
//                                }
//                                intent.putExtra("DEMO_REPLY_TARGETID", mTargetId);
//                                startActivityForResult(intent, 29);

                                mEditText = s.toString();
                            }
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    /**
     * 判断是否是 Push 消息，判断是否需要做 connect 操作
     */
    private void isPushMessage(Intent intent) {
        //push
        if (intent.getData().getScheme().equals("rong")
                && intent.getData().getQueryParameter("push") != null) {
            //通过intent.getData().getQueryParameter("push") 为true，判断是否是push消息
            if (intent.getData().getQueryParameter("push").equals("true")) {
                //只有收到系统消息和不落地 push 消息的时候，pushId 不为 null。而且这两种消息只能通过 server 来发送，客户端发送不了。
                String id = intent.getData().getQueryParameter("pushId");
//                RongIM.getInstance().getRongIMClient().recordNotificationEvent(id);

                if (mDialog != null && !mDialog.isShowing()) {
                    mDialog.show();
                }
                isFromPush = true;
                enterActivity();
            }
        } else {//通知过来
            //程序切到后台，收到消息后点击进入,会执行这里
            if (RongIMClient.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.DISCONNECTED)) {
                if (mDialog != null && !mDialog.isShowing())
                    mDialog.show();
                isFromPush = true;
                enterActivity();
            } else
                enterFragment(mConversationType, mTargetId);
        }
    }


    /**
     * 收到 push 消息后，选择进入哪个 Activity
     * 如果程序缓存未被清理，进入 MainActivity
     * 程序缓存被清理，进入 LoginActivity，重新获取token
     * <p/>
     * 作用：由于在 manifest 中 intent-filter 是配置在 ConversationActivity 下面，所以收到消息后点击notifacition 会跳转到
     * ConversationActivity。
     * 以跳到 MainActivity 为例：
     * 在 ConversationActivity 收到消息后，选择进入 MainActivity，这样就把 MainActivity 激活了，当你读完收到的消息点击 返回键 时，程序会退到
     * MainActivity 页面，而不是直接退回到 桌面。
     */
    private void enterActivity() {

        if (DemoContext.getInstance() == null)
            return;

        String token = DemoContext.getInstance().getSharedPreferences()
                .getString(Constants.APP_TOKEN, Constants.DEFAULT);

        if (token.equals(Constants.DEFAULT)) {

            startActivity(new Intent(ConversationActivity.this, LoginActivity.class));
            finish();
        } else {
            reconnect(token);
        }
    }

    private void reconnect(String token) {

        RongIM.connect(token, new RongIMClient.ConnectCallback() {
            @Override
            public void onTokenIncorrect() {

                Log.e(TAG, "---onTokenIncorrect--");
            }

            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "---onSuccess--" + s);

                enterFragment(mConversationType, mTargetId);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                Log.e(TAG, "---onError--" + e);
            }
        });

    }

    /**
     * 加载会话页面 ConversationFragment
     */
    private void enterFragment(Conversation.ConversationType mConversationType, String mTargetId) {

        Log.e(TAG, "------------enterFragment--------");
        fragment = new ConversationFragment();

        Uri uri = Uri.parse("rong://" + getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(mConversationType.getName().toLowerCase())
                .appendQueryParameter("targetId", mTargetId).build();

        fragment.setUri(uri);
        fragment.setInputBoardListener(new InputView.IInputBoardListener() {
            @Override
            public void onBoardExpanded(int height) {
                RLog.e(TAG, "onBoardExpanded h : " + height);
            }

            @Override
            public void onBoardCollapsed() {
                RLog.e(TAG, "onBoardCollapsed");
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //xxx 为你要加载的 id
        transaction.add(R.id.rong_content, fragment);
        transaction.commit();
    }

    /**
     * 设置会话页面 Title
     *
     * @param conversationType 会话类型
     * @param targetId         目标 Id
     */
    private void setActionBarTitle(Conversation.ConversationType conversationType, String targetId) {

        if (conversationType == null)
            return;

        if (conversationType.equals(Conversation.ConversationType.PRIVATE)) {
            setPrivateActionBar(targetId);
        }
//        else if (conversationType.equals(Conversation.ConversationType.GROUP)) {
//            setGroupActionBar(targetId);
//        } else if (conversationType.equals(Conversation.ConversationType.DISCUSSION)) {
//            setDiscussionActionBar(targetId, mTargetIds);
//        } else if (conversationType.equals(Conversation.ConversationType.CHATROOM)) {
//            getSupportActionBar().setTitle(title);
//        } else if (conversationType.equals(Conversation.ConversationType.SYSTEM)) {
//            getSupportActionBar().setTitle(R.string.de_actionbar_system);
//        } else if (conversationType.equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
//            setAppPublicServiceActionBar(targetId);
//        } else if (conversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE)) {
//            setPublicServiceActionBar(targetId);
//        } else if (conversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
//            getSupportActionBar().setTitle(R.string.main_customer);
//        } else {
//            getSupportActionBar().setTitle(R.string.de_actionbar_sub_defult);
//        }

    }

    /**
     * 设置私聊界面 ActionBar
     */
    private void setPrivateActionBar(String targetId) {

        if (DemoContext.getInstance() != null) {

            UserInfos userInfos = DemoContext.getInstance().getUserInfosById(targetId);

            if (userInfos == null) {
                getSupportActionBar().setTitle("");
            } else {
                getSupportActionBar().setTitle(userInfos.getUsername());
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.de_conversation_menu, menu);

        if (mConversationType == null)
            return true;

        if (mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
            menu.getItem(0).setVisible(false);
        } else if (mConversationType.equals(Conversation.ConversationType.DISCUSSION)
                && isDiscussion) {
            menu.getItem(0).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.icon:

                if (mConversationType == null)
                    return true;

                enterSettingActivity();
                break;
            case android.R.id.home:
                Log.e(TAG, "------------onOptionsItemSelected--------");
                if (!closeRealTimeLocation()) {

                    if (fragment != null && !fragment.onBackPressed()) {
                        if (isFromPush) {
                            isFromPush = false;
                            startActivity(new Intent(this, MainActivity.class));
                        }
                        finish();
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 根据 targetid 和 ConversationType 进入到设置页面
     */
    private void enterSettingActivity() {

        if (true) {
            Toast.makeText(getApplicationContext(), "未实现设置页面", Toast.LENGTH_LONG).show();
            return;
        }

        if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE
                || mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE) {

            RongIM.getInstance().startPublicServiceProfile(this, mConversationType, mTargetId);
        } else {
            //当你刚刚创建完讨论组以后获得的是 targetIds
            if (!TextUtils.isEmpty(mTargetIds)) {
                UriFragment fragment = (UriFragment) getSupportFragmentManager().getFragments().get(0);
                //得到讨论组的 targetId
                mTargetId = fragment.getUri().getQueryParameter("targetId");

                if (TextUtils.isEmpty(mTargetId)) {
//                    WinToast.toast(ConversationActivity.this, "讨论组尚未创建成功");
                }
            }

            Uri uri = Uri.parse("demo://" + getApplicationInfo().packageName).buildUpon()
                    .appendPath("conversationSetting")
                    .appendPath(mConversationType.getName())
                    .appendQueryParameter("targetId", mTargetId).build();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 29 && resultCode == Constants.MESSAGE_REPLY) {
            if (data != null && data.hasExtra("REPLY_NAME") && data.hasExtra("REPLY_ID")) {
                String id = data.getStringExtra("REPLY_ID");
                String name = data.getStringExtra("REPLY_NAME");
                TextInputProvider textInputProvider = (TextInputProvider) RongContext.getInstance().getPrimaryInputProvider();
                textInputProvider.setEditTextContent(mEditText + name + " ");

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    @Override
    public void onCallApiSuccess(AbstractHttpRequest request, Object obj) {
    }

//    /**
//     * @param obj
//     */
//    private void getMyGroupApiSuccess(Object obj) {
//
//        if (obj instanceof Groups) {
//            final Groups groups = (Groups) obj;
//
//            if (groups.getCode() == 200) {
//                List<Group> grouplist = new ArrayList<Group>();
//                if (groups.getResult() != null) {
//                    for (int i = 0; i < groups.getResult().size(); i++) {
//
//                        String id = groups.getResult().get(i).getId();
//                        String name = groups.getResult().get(i).getName();
//                        if (groups.getResult().get(i).getPortrait() != null) {
//                            Uri uri = Uri.parse(groups.getResult().get(i).getPortrait());
//                            grouplist.add(new Group(id, name, uri));
//                        } else {
//                            grouplist.add(new Group(id, name, null));
//                        }
//                    }
//                    HashMap<String, Group> groupM = new HashMap<String, Group>();
//                    for (int i = 0; i < grouplist.size(); i++) {
//                        groupM.put(groups.getResult().get(i).getId(), grouplist.get(i));
//                    }
//                    if (DemoContext.getInstance() != null)
//                        DemoContext.getInstance().setGroupMap(groupM);
//
//                    if (grouplist.size() > 0)
//                        RongIM.getInstance().syncGroup(grouplist, new RongIMClient.OperationCallback() {
//                            @Override
//                            public void onSuccess() {
//                                Log.e(TAG, "---syncGroup-onSuccess---");
//                            }
//
//                            @Override
//                            public void onError(RongIMClient.ErrorCode errorCode) {
//                                Log.e(TAG, "---syncGroup-onError---");
//                            }
//                        });
//
//                    if (mDialog != null)
//                        mDialog.dismiss();
//                }
//            }
//        }
//    }


    @Override
    public void onCallApiFailure(AbstractHttpRequest request, BaseException e) {
        Log.e(TAG, "---push--onCallApiFailure-");
        if (mDialog != null)
            mDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRealTimeLocationBar(null);
    }


/*－－－－－－－－－－－－－地理位置共享 start－－－－－－－－－*/

//    private void setRealTime() {
//
//        mRealTimeBar = (RelativeLayout) this.findViewById(R.id.layout);
//
//        mRealTimeBar.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                if (currentLocationStatus == null)
//                    currentLocationStatus = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
//
//                if (currentLocationStatus == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
//
//                    final AlterDialogFragment alterDialogFragment = AlterDialogFragment.newInstance("", "加入位置共享", "取消", "加入");
//                    alterDialogFragment.setOnAlterDialogBtnListener(new AlterDialogFragment.AlterDialogBtnListener() {
//
//                        @Override
//                        public void onDialogPositiveClick(AlterDialogFragment dialog) {
//                            RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
//
//                            if (status == null || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE) {
//                                startRealTimeLocation();
//                            } else {
//                                joinRealTimeLocation();
//                            }
//
//                        }
//
//                        @Override
//                        public void onDialogNegativeClick(AlterDialogFragment dialog) {
//                            alterDialogFragment.dismiss();
//                        }
//                    });
//                    alterDialogFragment.show(getSupportFragmentManager());
//
//                } else {
//                    Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
//                    intent.putExtra("conversationType", mConversationType.getValue());
//                    intent.putExtra("targetId", mTargetId);
//                    startActivity(intent);
//                }
//            }
//        });
//
//        if (!TextUtils.isEmpty(mTargetId) && mConversationType != null) {
//
//            RealTimeLocationConstant.RealTimeLocationErrorCode errorCode = RongIMClient.getInstance().getRealTimeLocation(mConversationType, mTargetId);
//            if (errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_SUCCESS || errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_IS_ON_GOING) {
//                RongIMClient.getInstance().addRealTimeLocationListener(mConversationType, mTargetId, this);//设置监听
//                currentLocationStatus = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
//
//                if (currentLocationStatus == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
//                    showRealTimeLocationBar(currentLocationStatus);
//                }
//            }
//        }
//
//
//    }

    //real-time location method beign

//    private void startRealTimeLocation() {
//        RongIMClient.getInstance().startRealTimeLocation(mConversationType, mTargetId);
//        Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
//        intent.putExtra("conversationType", mConversationType.getValue());
//        intent.putExtra("targetId", mTargetId);
//        startActivity(intent);
//    }
//
//    private void joinRealTimeLocation() {
//        RongIMClient.getInstance().joinRealTimeLocation(mConversationType, mTargetId);
//        Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
//        intent.putExtra("conversationType", mConversationType.getValue());
//        intent.putExtra("targetId", mTargetId);
//        startActivity(intent);
//    }

    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            closeRealTimeLocation();
        }
        if (fragment != null && !fragment.onBackPressed()) {
            if (isFromPush) {
                isFromPush = false;
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }

    }


    private boolean closeRealTimeLocation() {

        if (mConversationType == null || TextUtils.isEmpty(mTargetId))
            return false;

        if (mConversationType != null && !TextUtils.isEmpty(mTargetId)) {

            RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

            if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                return false;
            }
        }

//        final AlterDialogFragment alterDialogFragment = AlterDialogFragment.newInstance("提示", "退出当前页面将会终止实时位置共享,确定退出？", "否", "是");
//        alterDialogFragment.setOnAlterDialogBtnListener(new AlterDialogFragment.AlterDialogBtnListener() {
//            @Override
//            public void onDialogPositiveClick(AlterDialogFragment dialog) {
//                RongIMClient.getInstance().quitRealTimeLocation(mConversationType, mTargetId);
//                finish();
//            }
//
//            @Override
//            public void onDialogNegativeClick(AlterDialogFragment dialog) {
//                alterDialogFragment.dismiss();
//            }
//        });
//        alterDialogFragment.show(getSupportFragmentManager());

        return true;
    }


    private void showRealTimeLocationBar(RealTimeLocationConstant.RealTimeLocationStatus status) {

        if (status == null)
            status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

        final List<String> userIds = RongIMClient.getInstance().getRealTimeLocationParticipants(mConversationType, mTargetId);

        if (status != null && status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {

            if (userIds != null && userIds.get(0) != null && userIds.size() == 1) {

                DemoContext.getInstance().getDemoApi().getUserInfo(userIds.get(0), new DemoApi.GetUserInfoListener() {

                    @Override
                    public void onSuccess(final UserInfo userInfo) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                                textView.setText(userInfo.getName() + " 正在共享位置");
                            }
                        });

                    }

                    @Override
                    public void onError(String userId, BaseException e) {

                    }
                });
            } else {
                if (userIds != null && userIds.size() > 0) {
                    if (mRealTimeBar != null) {
                        TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                        textView.setText(userIds.size() + " 人正在共享位置");
                    }
                } else {
                    if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.VISIBLE) {
                        mRealTimeBar.setVisibility(View.GONE);
                    }
                }
            }

        } else if (status != null && status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING) {
            TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
            textView.setText("你正在共享位置");
        } else {

            if (mRealTimeBar != null && userIds != null) {
                TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                textView.setText(userIds.size() + " 人正在共享位置");
            }
        }

        if (userIds != null && userIds.size() > 0) {

            if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.GONE) {
                mRealTimeBar.setVisibility(View.VISIBLE);
            }
        } else {

            if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.VISIBLE) {
                mRealTimeBar.setVisibility(View.GONE);
            }
        }

    }

    public void onEventMainThread(RongEvent.RealTimeLocationMySelfJoinEvent event) {

        onParticipantsJoin(RongIM.getInstance().getCurrentUserId());
    }

    private void hideRealTimeBar() {
        if (mRealTimeBar != null) {
            mRealTimeBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if ("ConversationActivity".equals(this.getClass().getSimpleName()))
            EventBus.getDefault().unregister(this);
        RongIMClient.setTypingStatusListener(null);
        super.onDestroy();
    }


    @Override
    public void onStatusChange(final RealTimeLocationConstant.RealTimeLocationStatus status) {
        currentLocationStatus = status;

        EventBus.getDefault().post(status);

        if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE) {
            hideRealTimeBar();

            RealTimeLocationConstant.RealTimeLocationErrorCode errorCode = RongIMClient.getInstance().getRealTimeLocation(mConversationType, mTargetId);

            if (errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_SUCCESS) {
                RongIM.getInstance().insertMessage(mConversationType, mTargetId, RongIM.getInstance().getCurrentUserId(), InformationNotificationMessage.obtain("位置共享已结束"));
            }
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING) {//发自定义消息
            showRealTimeLocationBar(status);
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
            showRealTimeLocationBar(status);
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_CONNECTED) {
            showRealTimeLocationBar(status);
        }

    }


    @Override
    public void onReceiveLocation(double latitude, double longitude, String userId) {
        EventBus.getDefault().post(RongEvent.RealTimeLocationReceiveEvent.obtain(userId, latitude, longitude));
    }

    @Override
    public void onParticipantsJoin(String userId) {
        EventBus.getDefault().post(RongEvent.RealTimeLocationJoinEvent.obtain(userId));

        if (RongIMClient.getInstance().getCurrentUserId().equals(userId)) {
            showRealTimeLocationBar(null);
        }
    }

    @Override
    public void onParticipantsQuit(String userId) {
        EventBus.getDefault().post(RongEvent.RealTimeLocationQuitEvent.obtain(userId));
    }

    @Override
    public void onError(RealTimeLocationConstant.RealTimeLocationErrorCode errorCode) {
        Log.e(TAG, "onError:---" + errorCode);
    }

    /*－－－－－－－－－－－－－地理位置共享 end－－－－－－－－－*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            closeRealTimeLocation();
        }
        if (isFromPush) {
            isFromPush = false;
            startActivity(new Intent(this, MainActivity.class));
        }

        //finish();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        switch (msg.what) {
            case SET_TEXT_TYPING_TITLE:
                getSupportActionBar().setTitle(TextTypingTitle);
                break;
            case SET_VOICE_TYPING_TITLE:
                getSupportActionBar().setTitle(VoiceTypingTitle);
                break;
            case SET_TARGETID_TITLE:
                setActionBarTitle(mConversationType, mTargetId);
                break;
            default:
                break;
        }
        return true;
    }

}
