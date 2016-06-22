package com.vivavideo.imkit;

import com.vivavideo.imkit.cache.RongCache;
import com.vivavideo.imkit.cache.RongCacheWrap;
import com.vivavideo.imkit.common.RongConst;
import com.vivavideo.imkit.eventbus.EventBus;
import com.vivavideo.imkit.imageloader.cache.disc.impl.ext.LruDiskCache;
import com.vivavideo.imkit.imageloader.cache.disc.naming.Md5FileNameGenerator;
import com.vivavideo.imkit.imageloader.cache.memory.impl.LruMemoryCache;
import com.vivavideo.imkit.imageloader.core.DisplayImageOptions;
import com.vivavideo.imkit.imageloader.core.ImageLoader;
import com.vivavideo.imkit.imageloader.core.ImageLoaderConfiguration;
import com.vivavideo.imkit.imageloader.core.download.BaseImageDownloader;
import com.vivavideo.imkit.imageloader.utils.StorageUtils;
import com.vivavideo.imkit.model.ConversationInfo;
import com.vivavideo.imkit.model.ConversationKey;
import com.vivavideo.imkit.model.ConversationProviderTag;
import com.vivavideo.imkit.model.Event;
import com.vivavideo.imkit.model.ProviderTag;
import com.vivavideo.imkit.notification.MessageCounter;
import com.vivavideo.imkit.notification.MessageSounder;
import com.vivavideo.imkit.provider.AppServiceConversationProvider;
import com.vivavideo.imkit.provider.EvaluateTextMessageItemProvider;
import com.vivavideo.imkit.provider.IContainerItemProvider;
import com.vivavideo.imkit.provider.ImageInputProvider;
import com.vivavideo.imkit.provider.InputProvider;
import com.vivavideo.imkit.provider.LocationInputProvider;
import com.vivavideo.imkit.provider.PrivateConversationProvider;
import com.vivavideo.imkit.provider.PublicServiceConversationProvider;
import com.vivavideo.imkit.provider.PublicServiceMenuInputProvider;
import com.vivavideo.imkit.provider.TextInputProvider;
import com.vivavideo.imkit.provider.VoiceInputProvider;
import com.vivavideo.imkit.userInfoCache.RongUserInfoManager;
import com.vivavideo.imkit.utils.AndroidEmoji;
import com.vivavideo.imkit.utils.RongAuthImageDownloader;
import com.vivavideo.imkit.utils.StringUtils;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.rong.common.RLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;

/**
 * Created by DragonJ on 14-7-29.
 */
public class RongContext extends ContextWrapper {
    private final static String TAG = "RongContext";
    private static RongContext sContext;
    private EventBus mBus;

    private ExecutorService executorService;
    private String mAppKey;
    private RongIM.ConversationBehaviorListener mConversationBehaviorListener;// 会话页面
    private RongIM.ConversationListBehaviorListener mConversationListBehaviorListener;// 会话列表页面
    private RongIM.PublicServiceBehaviorListener mPublicServiceBehaviorListener;//公众号界面
    private RongIM.OnSelectMemberListener mMemberSelectListener;
    private RongIM.OnSendMessageListener mOnSendMessageListener;//发送消息监听
    private static RongIM.RequestPermissionsListener mRequestPermissionsListener; //Android 6.0以上系统时，请求权限监听器

    private RongIM.UserInfoProvider mUserInfoProvider;

    private Map<Class<? extends MessageContent>, IContainerItemProvider.MessageProvider> mTemplateMap;
    private IContainerItemProvider.MessageProvider mDefaultTemplate;
    private Map<Class<? extends MessageContent>, ProviderTag> mProviderMap;
    private Map<String, IContainerItemProvider.ConversationProvider> mConversationProviderMap;
    private Map<String, ConversationProviderTag> mConversationTagMap;
    private Map<String, Boolean> mConversationTypeStateMap;

    private RongCache<String, Conversation.ConversationNotificationStatus> mNotificationCache;

    private InputProvider.MainInputProvider mPrimaryProvider;
    private InputProvider.MainInputProvider mSecondaryProvider;
    private InputProvider.MainInputProvider mMenuProvider;

    private RongIM.LocationProvider mLocationProvider;
    private MessageCounter mCounterLogic;

    private List<String> mCurrentConversationList;

    private Map<Conversation.ConversationType, List<InputProvider.ExtendProvider>> mExtendProvider;

    VoiceInputProvider mVoiceInputProvider;
    ImageInputProvider mImageInputProvider;
    LocationInputProvider mLocationInputProvider;
    InputProvider.ExtendProvider mVoIPInputProvider;

    Handler mHandler;

    private UserInfo mCurrentUserInfo;

    private boolean isUserInfoAttached;

    private boolean isShowUnreadMessageState;
    private boolean isShowNewMessageState;
    private EvaluateTextMessageItemProvider evaluateTextMessageItemProvider;

    static public void init(Context context) {

        if (sContext == null) {
            sContext = new RongContext(context);
            sContext.initRegister();
        }
    }


    public static RongContext getInstance() {
        return sContext;
    }

    protected RongContext(Context base) {
        super(base);

        mBus = EventBus.getDefault();
        mHandler = new Handler(getMainLooper());

        mTemplateMap = new HashMap<Class<? extends MessageContent>, IContainerItemProvider.MessageProvider>();

        mProviderMap = new HashMap<Class<? extends MessageContent>, ProviderTag>();

        mConversationProviderMap = new HashMap<String, IContainerItemProvider.ConversationProvider>();

        mConversationTagMap = new HashMap<String, ConversationProviderTag>();

        mConversationTypeStateMap = new HashMap<String, Boolean>();

        mCounterLogic = new MessageCounter(this);

        mCurrentConversationList = new ArrayList<String>();

        mExtendProvider = new HashMap<Conversation.ConversationType, List<InputProvider.ExtendProvider>>();
        initCache();

        //TODO
        executorService = Executors.newSingleThreadExecutor();

        AndroidEmoji.init(getApplicationContext());

        RongNotificationManager.getInstance().init(this);

        MessageSounder.init(getApplicationContext());

        ImageLoader.getInstance().init(getDefaultConfig(getApplicationContext()));
    }

    private ImageLoaderConfiguration getDefaultConfig(Context context) {
        int MAX_CACHE_MEMORY_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8);
        File cacheDir = StorageUtils.getOwnCacheDirectory(context, context.getPackageName() + "/cache/image/");

        try {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration
                    .Builder(context)
                    .threadPoolSize(3) // 线程池内加载的数量
                    .threadPriority(Thread.NORM_PRIORITY - 2) // 降低线程的优先级，减小对UI主线程的影响
                    .denyCacheImageMultipleSizesInMemory()
                    .memoryCache(new LruMemoryCache(MAX_CACHE_MEMORY_SIZE))
                    .diskCache(new LruDiskCache(cacheDir, new Md5FileNameGenerator(), 0))
                    .imageDownloader(new RongAuthImageDownloader(this))
                    .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                    .imageDownloader(new BaseImageDownloader(context, 5 * 1000, 10 * 1000)) // connectTimeout (5 s), readTimeout (10 s)超时时间
                    .build();
//            io.rong.imageloader.utils.L.writeLogs(false);
            return config;

        } catch (IOException e) {

        }
        return null;
    }

    private void initRegister() {

        registerDefaultConversationGatherState();
        registerConversationTemplate(new PrivateConversationProvider());
//        registerConversationTemplate(new GroupConversationProvider());
//        registerConversationTemplate(new DiscussionConversationProvider());
//        registerConversationTemplate(new SystemConversationProvider());
//        registerConversationTemplate(new CustomerServiceConversationProvider());
        registerConversationTemplate(new AppServiceConversationProvider());
        registerConversationTemplate(new PublicServiceConversationProvider());

        mVoiceInputProvider = new VoiceInputProvider(sContext);
        mImageInputProvider = new ImageInputProvider(sContext);
        mLocationInputProvider = new LocationInputProvider(sContext);

        setPrimaryInputProvider(new TextInputProvider(sContext));
        setSecondaryInputProvider(mVoiceInputProvider);
        setMenuInputProvider(new PublicServiceMenuInputProvider(sContext));

        List<InputProvider.ExtendProvider> privateProvider = new ArrayList<InputProvider.ExtendProvider>();

        privateProvider.add(mImageInputProvider);
        privateProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> chatRoomProvider = new ArrayList<InputProvider.ExtendProvider>();
        chatRoomProvider.add(mImageInputProvider);
        chatRoomProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> groupProvider = new ArrayList<InputProvider.ExtendProvider>();
        groupProvider.add(mImageInputProvider);
        groupProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> customerProvider = new ArrayList<InputProvider.ExtendProvider>();
        customerProvider.add(mImageInputProvider);
        customerProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> discussionProvider = new ArrayList<InputProvider.ExtendProvider>();
        discussionProvider.add(mImageInputProvider);
        discussionProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> publicProvider = new ArrayList<InputProvider.ExtendProvider>();
        publicProvider.add(mImageInputProvider);
        publicProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> publicAppProvider = new ArrayList<InputProvider.ExtendProvider>();
        publicAppProvider.add(mImageInputProvider);
        publicAppProvider.add(mLocationInputProvider);

        List<InputProvider.ExtendProvider> systemProvider = new ArrayList<InputProvider.ExtendProvider>();
        systemProvider.add(mImageInputProvider);
        systemProvider.add(mLocationInputProvider);

        mExtendProvider.put(Conversation.ConversationType.PRIVATE, privateProvider);
        mExtendProvider.put(Conversation.ConversationType.CHATROOM, chatRoomProvider);
        mExtendProvider.put(Conversation.ConversationType.GROUP, groupProvider);
        mExtendProvider.put(Conversation.ConversationType.CUSTOMER_SERVICE, customerProvider);
        mExtendProvider.put(Conversation.ConversationType.DISCUSSION, discussionProvider);
        mExtendProvider.put(Conversation.ConversationType.APP_PUBLIC_SERVICE, publicAppProvider);
        mExtendProvider.put(Conversation.ConversationType.PUBLIC_SERVICE, publicProvider);
        mExtendProvider.put(Conversation.ConversationType.SYSTEM, systemProvider);
    }

    public VoiceInputProvider getVoiceInputProvider() {
        return mVoiceInputProvider;
    }

    public ImageInputProvider getImageInputProvider() {
        return mImageInputProvider;
    }

    public LocationInputProvider getLocationInputProvider() {
        return mLocationInputProvider;
    }

    public InputProvider.ExtendProvider getVoIPInputProvider() {
        return mVoIPInputProvider;
    }


    private void initCache() {
        mNotificationCache = new RongCacheWrap<String, Conversation.ConversationNotificationStatus>(this, RongConst.Cache.NOTIFICATION_CACHE_MAX_COUNT) {
            Vector<String> mRequests = new Vector<String>();
            Conversation.ConversationNotificationStatus notificationStatus = null;

            @Override
            public Conversation.ConversationNotificationStatus obtainValue(final String key) {

                if (TextUtils.isEmpty(key))
                    return null;

                synchronized (mRequests) {
                    if (mRequests.contains(key))
                        return null;
                    mRequests.add(key);
                }

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {

                        final ConversationKey conversationKey = ConversationKey.obtain(key);

                        if (conversationKey != null) {

                            RongIM.getInstance().getConversationNotificationStatus(conversationKey.getType(),
                                    conversationKey.getTargetId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {

                                        @Override
                                        public void onSuccess(Conversation.ConversationNotificationStatus status) {
                                            mRequests.remove(key);
                                            put(key, status);
                                            getContext().getEventBus().post(new Event.ConversationNotificationEvent(conversationKey.getTargetId(),
                                                    conversationKey.getType(), notificationStatus));
                                        }

                                        @Override
                                        public void onError(RongIMClient.ErrorCode errorCode) {
                                            mRequests.remove(key);
                                        }
                                    });
                        }
                    }
                });


                return notificationStatus;
            }
        };
    }

    public List<ConversationInfo> getCurrentConversationList() {
        ArrayList<ConversationInfo> infos = new ArrayList<>();
        int size = mCurrentConversationList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                ConversationKey key = ConversationKey.obtain(mCurrentConversationList.get(i));
                ConversationInfo info = ConversationInfo.obtain(key.getType(), key.getTargetId());
                infos.add(info);
            }
        }
        return infos;
    }

    public EventBus getEventBus() {
        return mBus;
    }

    public MessageCounter getMessageCounterLogic() {
        return mCounterLogic;
    }

    public void registerConversationTemplate(IContainerItemProvider.ConversationProvider provider) {
        ConversationProviderTag tag = provider.getClass().getAnnotation(ConversationProviderTag.class);
        if (tag == null)
            throw new RuntimeException("No ConversationProviderTag added with your provider!");
        mConversationProviderMap.put(tag.conversationType(), provider);
        mConversationTagMap.put(tag.conversationType(), tag);
    }

    public IContainerItemProvider.ConversationProvider getConversationTemplate(String conversationType) {
        return mConversationProviderMap.get(conversationType);
    }

    public ConversationProviderTag getConversationProviderTag(String conversationType) {
        if (!mConversationProviderMap.containsKey(conversationType)) {
            throw new RuntimeException("the conversation type hasn't been registered!");
        }
        return mConversationTagMap.get(conversationType);
    }

    public void registerDefaultConversationGatherState() {
        setConversationGatherState(Conversation.ConversationType.PRIVATE.getName(), false);
        setConversationGatherState(Conversation.ConversationType.GROUP.getName(), true);
        setConversationGatherState(Conversation.ConversationType.DISCUSSION.getName(), false);
        setConversationGatherState(Conversation.ConversationType.CHATROOM.getName(), false);
        setConversationGatherState(Conversation.ConversationType.CUSTOMER_SERVICE.getName(), false);
        setConversationGatherState(Conversation.ConversationType.SYSTEM.getName(), true);
        setConversationGatherState(Conversation.PublicServiceType.APP_PUBLIC_SERVICE.getName(), false);
        setConversationGatherState(Conversation.ConversationType.PUBLIC_SERVICE.getName(), false);

    }

    public void setConversationGatherState(String type, Boolean state) {
        if (type == null)
            throw new IllegalArgumentException("The name of the register conversation type can't be null");
        mConversationTypeStateMap.put(type, state);
    }

    public Boolean getConversationGatherState(String type) {
        if (mConversationTypeStateMap.containsKey(type) == false) {
            RLog.e(TAG, "getConversationGatherState, " + type + " ");
            return false;
        }
        return mConversationTypeStateMap.get(type);
    }


    public void registerMessageTemplate(IContainerItemProvider.MessageProvider provider) {
        ProviderTag tag = provider.getClass().getAnnotation(ProviderTag.class);
        if (tag == null)
            throw new RuntimeException("ProviderTag not def MessageContent type");
        mTemplateMap.put(tag.messageContent(), provider);
        mProviderMap.put(tag.messageContent(), tag);
    }

    public IContainerItemProvider.MessageProvider getMessageTemplate(Class<? extends MessageContent> type) {
        IContainerItemProvider.MessageProvider provider = mTemplateMap.get(type);
        return provider;
    }

    public ProviderTag getMessageProviderTag(Class<? extends MessageContent> type) {
        return mProviderMap.get(type);
    }

    public EvaluateTextMessageItemProvider getEvaluateProvider() {
        if (evaluateTextMessageItemProvider == null) {
            evaluateTextMessageItemProvider = new EvaluateTextMessageItemProvider();
        }
        return evaluateTextMessageItemProvider;
    }

    public void executorBackground(Runnable runnable) {
        if (runnable == null)
            return;

        executorService.execute(runnable);
    }


    public UserInfo getUserInfoFromCache(String userId) {
        if (userId != null) {
            return RongUserInfoManager.getInstance().getUserInfo(userId);
        } else {
            return null;
        }
    }

    public PublicServiceProfile getPublicServiceInfoFromCache(String messageKey) {
        String id = StringUtils.getArg1(messageKey);
        String arg2 = StringUtils.getArg2(messageKey);
        int iArg2 = Integer.parseInt(arg2);
        Conversation.PublicServiceType type = null;

        if (iArg2 == Conversation.PublicServiceType.PUBLIC_SERVICE.getValue()) {
            type = Conversation.PublicServiceType.PUBLIC_SERVICE;
        } else if (iArg2 == Conversation.PublicServiceType.APP_PUBLIC_SERVICE.getValue()) {
            type = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
        }
        return RongUserInfoManager.getInstance().getPublicServiceProfile(type, id);
    }

    public Conversation.ConversationNotificationStatus getConversationNotifyStatusFromCache(ConversationKey messageKey) {
        if (messageKey != null && messageKey.getKey() != null)
            return mNotificationCache.get(messageKey.getKey());
        else
            return null;
    }

    public void setConversationNotifyStatusToCache(ConversationKey conversationKey, Conversation.ConversationNotificationStatus status) {
        mNotificationCache.put(conversationKey.getKey(), status);
    }

    public RongIM.ConversationBehaviorListener getConversationBehaviorListener() {
        return mConversationBehaviorListener;
    }

    public void setConversationBehaviorListener(RongIM.ConversationBehaviorListener conversationBehaviorListener) {
        this.mConversationBehaviorListener = conversationBehaviorListener;
    }

    public RongIM.PublicServiceBehaviorListener getPublicServiceBehaviorListener() {
        return this.mPublicServiceBehaviorListener;
    }

    public void setPublicServiceBehaviorListener(RongIM.PublicServiceBehaviorListener publicServiceBehaviorListener) {
        this.mPublicServiceBehaviorListener = publicServiceBehaviorListener;
    }

    public void setOnMemberSelectListener(RongIM.OnSelectMemberListener listener) {
        this.mMemberSelectListener = listener;
    }

    public RongIM.OnSelectMemberListener getMemberSelectListener() {
        return mMemberSelectListener;
    }

    public RongIM.UserInfoProvider getUserInfoProvider() {
        return mUserInfoProvider;
    }

    public void setGetUserInfoProvider(RongIM.UserInfoProvider provider, boolean isCache) {
        this.mUserInfoProvider = provider;
        RongUserInfoManager.getInstance().setIsCacheUserInfo(isCache);
    }

    public void addInputExtentionProvider(Conversation.ConversationType conversationType, InputProvider.ExtendProvider[] providers) {
        if (providers == null || conversationType == null)
            return;
        if (mExtendProvider.containsKey(conversationType)) {
            for (InputProvider.ExtendProvider p : providers) {
                mExtendProvider.get(conversationType).add(p);
            }
        }
    }

    public void resetInputExtentionProvider(Conversation.ConversationType conversationType, InputProvider.ExtendProvider[] providers) {
        if (conversationType == null)
            return;
        if (mExtendProvider.containsKey(conversationType)) {
            mExtendProvider.get(conversationType).clear();
            if (providers == null)
                return;
            for (InputProvider.ExtendProvider p : providers) {
                mExtendProvider.get(conversationType).add(p);
            }
        }
    }


    public void setPrimaryInputProvider(InputProvider.MainInputProvider provider) {
        mPrimaryProvider = provider;
        mPrimaryProvider.setIndex(0);
    }

    public void setSecondaryInputProvider(InputProvider.MainInputProvider provider) {
        mSecondaryProvider = provider;
        mSecondaryProvider.setIndex(1);
    }

    public void setMenuInputProvider(InputProvider.MainInputProvider provider) {
        mMenuProvider = provider;
    }

    public InputProvider.MainInputProvider getSecondaryInputProvider() {
        return mSecondaryProvider;
    }

    public List<InputProvider.ExtendProvider> getRegisteredExtendProviderList(Conversation.ConversationType conversationType) {
        return mExtendProvider.get(conversationType);
    }

    public InputProvider.MainInputProvider getPrimaryInputProvider() {
        return mPrimaryProvider;
    }

    public InputProvider.MainInputProvider getMenuInputProvider() {
        return mMenuProvider;
    }

    public void registerConversationInfo(ConversationInfo info) {
        if (info != null) {
            ConversationKey key = ConversationKey.obtain(info.getTargetId(), info.getConversationType());
            if (key != null && !mCurrentConversationList.contains(key.getKey())) {
                mCurrentConversationList.add(key.getKey());
            }
        }
    }

    public void unregisterConversationInfo(ConversationInfo info) {
        if (info != null) {
            ConversationKey key = ConversationKey.obtain(info.getTargetId(), info.getConversationType());
            if (key != null && mCurrentConversationList.size() > 0) {
                mCurrentConversationList.remove(key.getKey());
            }
        }
    }


    public RongIM.LocationProvider getLocationProvider() {
        return mLocationProvider;
    }

    public void setLocationProvider(RongIM.LocationProvider locationProvider) {
        this.mLocationProvider = locationProvider;
    }

    public RongIM.OnSendMessageListener getOnSendMessageListener() {
        return mOnSendMessageListener;
    }

    public void setOnSendMessageListener(RongIM.OnSendMessageListener onSendMessageListener) {
        mOnSendMessageListener = onSendMessageListener;
    }

    /**
     * 设置当前用户信息。
     *
     * @param userInfo 当前用户信息。
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        mCurrentUserInfo = userInfo;

        if (userInfo != null && !TextUtils.isEmpty(userInfo.getUserId())) {
            RongUserInfoManager.getInstance().setUserInfo(userInfo);
        }
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息。
     */
    public UserInfo getCurrentUserInfo() {
        if (mCurrentUserInfo != null)
            return mCurrentUserInfo;

        return null;
    }

    /**
     * 获取保存的token信息。
     *
     * @return 当前用户的token信息。
     */
    public String getToken() {
        return getSharedPreferences("rc_token", Context.MODE_PRIVATE).getString("token_value", "");
    }

    /**
     * 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息？true:携带；false:不携带。
     */
    public void setUserInfoAttachedState(boolean state) {
        this.isUserInfoAttached = state;
    }

    /**
     * 获取当前用户关于消息体内是否携带用户信息的配置
     *
     * @return 是否携带用户信息
     */
    public boolean getUserInfoAttachedState() {
        return isUserInfoAttached;
    }


    public RongIM.ConversationListBehaviorListener getConversationListBehaviorListener() {
        return mConversationListBehaviorListener;
    }

    public void setConversationListBehaviorListener(RongIM.ConversationListBehaviorListener conversationListBehaviorListener) {
        mConversationListBehaviorListener = conversationListBehaviorListener;
    }

    public void setRequestPermissionListener(RongIM.RequestPermissionsListener listener) {
        mRequestPermissionsListener = listener;
    }

    public RongIM.RequestPermissionsListener getRequestPermissionListener() {
        return mRequestPermissionsListener;
    }

    public void saveAppKey(String appKey) {
        this.mAppKey = appKey;
    }

    public String getAppKey() {
        return mAppKey;
    }

    public void showUnreadMessageIcon(boolean state) {
        this.isShowUnreadMessageState = state;
    }

    public void showNewMessageIcon(boolean state) {
        this.isShowNewMessageState = state;
    }

    public boolean getUnreadMessageState() {
        return isShowUnreadMessageState;
    }

    public boolean getNewMessageState() {
        return isShowNewMessageState;
    }

    public String getGatheredConversationTitle(Conversation.ConversationType type) {
        String title = "";
        switch (type) {
            case PRIVATE:
                title = this.getString(R.string.rc_conversation_list_my_private_conversation);
                break;
//            case GROUP:
//                title = this.getString(R.string.rc_conversation_list_my_group);
//                break;
//            case DISCUSSION:
//                title = this.getString(R.string.rc_conversation_list_my_discussion);
//                break;
//            case CHATROOM:
//                title = this.getString(R.string.rc_conversation_list_my_chatroom);
//                break;
//            case CUSTOMER_SERVICE:
//                title = this.getString(R.string.rc_conversation_list_my_customer_service);
//                break;
//            case SYSTEM:
//                title = this.getString(R.string.rc_conversation_list_system_conversation);
//                break;
//            case APP_PUBLIC_SERVICE:
//                title = this.getString(R.string.rc_conversation_list_app_public_service);
//                break;
//            case PUBLIC_SERVICE:
//                title = this.getString(R.string.rc_conversation_list_public_service);
//                break;
//            default:
//                System.err.print("It's not the default conversation type!!");
//                break;
        }
        return title;
    }
}
