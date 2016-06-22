package com.vivavideo.imkit.userInfoCache;

import com.vivavideo.imkit.RongIM;
import com.vivavideo.imkit.common.RongConst;
import com.vivavideo.imkit.utils.StringUtils;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashSet;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;

public class RongUserInfoManager {
    private static RongDatabaseDao mRongDatabaseDao;
    private RongUserCache<String, UserInfo> mUserInfoCache;
    private RongUserCache<String, RongConversationInfo> mGroupCache;
    private RongUserCache<String, RongConversationInfo> mDiscussionCache;
    private RongUserCache<String, PublicServiceProfile> mPublicServiceProfileCache;
    private final HashSet<String> mUserQuerySet;
    private final HashSet<String> mGroupUserQuerySet;
    private final HashSet<String> mGroupQuerySet;
    private final HashSet<String> mDiscussionQuerySet;
    private static IRongCacheListener mCacheListener;
    private boolean mIsCacheUserInfo = true;
    private boolean mIsCacheGroupInfo = true;
    private boolean mIsCacheGroupUserInfo = true;
    private Handler mWorkHandler;
    private HandlerThread mWorkThread;
    private static String mAppKey;

    private static class SingletonHolder {
        static RongUserInfoManager sInstance = new RongUserInfoManager();
    }

    private RongUserInfoManager() {
        mUserInfoCache = new RongUserCache<>(RongConst.Cache.USER_CACHE_MAX_COUNT);
        mGroupCache = new RongUserCache<>(RongConst.Cache.GROUP_CACHE_MAX_COUNT);
        mDiscussionCache = new RongUserCache<>(RongConst.Cache.DISCUSSION_CACHE_MAX_COUNT);
        mPublicServiceProfileCache = new RongUserCache<>(RongConst.Cache.PUBLIC_ACCOUNT_CACHE_MAX_COUNT);
        mUserQuerySet = new HashSet<>();
        mGroupQuerySet = new HashSet<>();
        mGroupUserQuerySet = new HashSet<>();
        mDiscussionQuerySet = new HashSet<>();
        mWorkThread = new HandlerThread("RongUserInfoManager");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper());
    }

    public void setIsCacheUserInfo(boolean mIsCacheUserInfo) {
        this.mIsCacheUserInfo = mIsCacheUserInfo;
    }

    public void setIsCacheGroupInfo(boolean mIsCacheGroupInfo) {
        this.mIsCacheGroupInfo = mIsCacheGroupInfo;
    }

    public void setIsCacheGroupUserInfo(boolean mIsCacheGroupUserInfo) {
        this.mIsCacheGroupUserInfo = mIsCacheGroupUserInfo;
    }

    public static RongUserInfoManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public static void init(String appKey, IRongCacheListener listener) {
        mAppKey = appKey;
        mCacheListener = listener;
        if (mRongDatabaseDao == null) {
            mRongDatabaseDao = new RongDatabaseDao();
        }
    }

    public static void connectDB(Context context, String currentUserId) {
        if (mRongDatabaseDao != null) {
            mRongDatabaseDao.connectDB(context, mAppKey, currentUserId);
        }
    }

    private UserInfo putUserInfoInCache(UserInfo info) {
        return mUserInfoCache.put(info.getUserId(), info);
    }

    private void insertUserInfoInDB(UserInfo info) {
        if (mRongDatabaseDao != null) {
            mRongDatabaseDao.insertUserInfo(info);
        }
    }

    private void putUserInfoInDB(UserInfo info) {
        if (mRongDatabaseDao != null) {
            mRongDatabaseDao.putUserInfo(info);
        }
    }

    public UserInfo getUserInfo(final String id) {
        if (id == null) {
            return null;
        }
        UserInfo info = null;

        if (mIsCacheUserInfo) {
            info = mUserInfoCache.get(id);
        }
        if (info == null) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mUserQuerySet) {
                        if (mUserQuerySet.contains(id)) {
                            return;
                        }
                        mUserQuerySet.add(id);
                    }
                    UserInfo userInfo = null;

                    if (mRongDatabaseDao != null) {
                        userInfo = mRongDatabaseDao.getUserInfo(id);
                    }
                    if (userInfo == null) {
                        if (mCacheListener != null) {
                            userInfo = mCacheListener.getUserInfo(id);
                        }
                        if (userInfo != null) {
                            insertUserInfoInDB(userInfo);

                        }
                    }
                    if (userInfo != null) {
                        if (mIsCacheUserInfo) {
                            putUserInfoInCache(userInfo);
                        }
                        if (mCacheListener != null) {
                            mCacheListener.onUserInfoUpdated(userInfo);
                        }
                    }
                    mUserQuerySet.remove(id);
                }
            });
        }
        return info;
    }

    public Discussion getDiscussionInfo(final String id) {
        if (id == null) {
            return null;
        }
        Discussion discussionInfo = null;
        RongConversationInfo info = mDiscussionCache.get(id);
        if (info == null) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mDiscussionQuerySet) {
                        if (mDiscussionQuerySet.contains(id)) {
                            return;
                        }
                        mDiscussionQuerySet.add(id);
                    }
                    Discussion discussion = null;

                    if (mRongDatabaseDao != null) {
                        discussion = mRongDatabaseDao.getDiscussionInfo(id);
                    }
                    if (discussion != null) {
                        RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue()+"", discussion.getId(), discussion.getName(), null);
                        mDiscussionCache.put(id, conversationInfo);
                        if (mCacheListener != null) {
                            mCacheListener.onDiscussionUpdated(discussion);
                        }
                        mDiscussionQuerySet.remove(id);
                    } else {
                        RongIM.getInstance().getDiscussion(id, new RongIMClient.ResultCallback<Discussion>() {
                            @Override
                            public void onSuccess(Discussion discussion) {
                                if (discussion != null) {
                                    if (mRongDatabaseDao != null) {
                                        mRongDatabaseDao.insertDiscussionInfo(discussion);
                                    }
                                    RongConversationInfo conversationInfo = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue() + "", discussion.getId(), discussion.getName(), null);
                                    mDiscussionCache.put(id, conversationInfo);
                                    if (mCacheListener != null) {
                                        mCacheListener.onDiscussionUpdated(discussion);
                                    }
                                }
                                mDiscussionQuerySet.remove(id);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                mDiscussionQuerySet.remove(id);
                            }
                        });
                    }
                }
            });
        } else {
            discussionInfo = new Discussion(info.getId(), info.getName());
        }
        return discussionInfo;
    }

    public PublicServiceProfile getPublicServiceProfile(final Conversation.PublicServiceType type, final String id) {
        if (type == null || id == null) {
            return null;
        }
        final String key = StringUtils.getKey(type.getValue() + "", id);

        PublicServiceProfile info = mPublicServiceProfileCache.get(key);

        if (info == null) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    RongIM.getInstance().getPublicServiceProfile(type, id, new RongIMClient.ResultCallback<PublicServiceProfile>() {
                        @Override
                        public void onSuccess(PublicServiceProfile result) {
                            if (result != null) {
                                mPublicServiceProfileCache.put(key, result);
                                if (mCacheListener != null) {
                                    mCacheListener.onPublicServiceProfileUpdated(result);
                                }
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                        }
                    });
                }
            });
        }
        return info;
    }

    public void setUserInfo(final UserInfo info) {
        if (mIsCacheUserInfo) {
            final UserInfo oldInfo = putUserInfoInCache(info);
            if ((oldInfo == null)
                    || (oldInfo.getName() != null && info.getName() != null && !oldInfo.getName().equals(info.getName()))
                    || (oldInfo.getPortraitUri() != null && info.getPortraitUri() != null && !oldInfo.getPortraitUri().toString().equals(info.getPortraitUri().toString()))) {
                mWorkHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        putUserInfoInDB(info);
                        if (mCacheListener != null) {
                            mCacheListener.onUserInfoUpdated(info);
                        }
                    }
                });
            }
        } else {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    UserInfo oldInfo = null;
                    if (mRongDatabaseDao != null) {
                        oldInfo = mRongDatabaseDao.getUserInfo(info.getUserId());
                    }
                    if ((oldInfo == null)
                            || (oldInfo.getName() != null && info.getName() != null && !oldInfo.getName().equals(info.getName()))
                            || (oldInfo.getPortraitUri() != null && info.getPortraitUri() != null && !oldInfo.getPortraitUri().toString().equals(info.getPortraitUri().toString()))) {
                        putUserInfoInDB(info);
                        if (mCacheListener != null) {
                            mCacheListener.onUserInfoUpdated(info);
                        }
                    }
                }
            });
        }
    }

    public void setGroupInfo(final Group group) {
        if (mIsCacheGroupInfo) {
            final RongConversationInfo info = new RongConversationInfo (Conversation.ConversationType.GROUP.getValue()+"", group.getId(), group.getName(), group.getPortraitUri());
            final RongConversationInfo oldInfo = mGroupCache.put(info.getId(), info);
            if ((oldInfo == null)
                    || (oldInfo.getName() != null && info.getName() != null && !oldInfo.getName().equals(info.getName()))
                    || (oldInfo.getUri() != null && info.getUri() != null && !oldInfo.getUri().toString().equals(info.getUri().toString()))) {
                mWorkHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mRongDatabaseDao != null) {
                            mRongDatabaseDao.putGroupInfo(group);
                        }
                        if (mCacheListener != null) {
                            mCacheListener.onGroupUpdated(group);
                        }
                    }
                });
            }
        } else {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    Group oldGroup = null;
                    if (mRongDatabaseDao != null) {
                        oldGroup = mRongDatabaseDao.getGroupInfo(group.getId());
                    }
                    if ((oldGroup == null)
                            || (oldGroup.getName() != null && group.getName() != null && !oldGroup.getName().equals(group.getName()))
                            || (oldGroup.getPortraitUri() != null && group.getPortraitUri() != null && !oldGroup.getPortraitUri().toString().equals(group.getPortraitUri().toString()))) {
                        if (mRongDatabaseDao != null) {
                            mRongDatabaseDao.putGroupInfo(group);
                        }
                        if (mCacheListener != null) {
                            mCacheListener.onGroupUpdated(group);
                        }
                    }
                }
            });
        }
    }

    public void setDiscussionInfo(final Discussion discussion) {
        RongConversationInfo info = new RongConversationInfo(Conversation.ConversationType.DISCUSSION.getValue()+"", discussion.getId(), discussion.getName(), null);
        RongConversationInfo oldInfo = mDiscussionCache.put(info.getId(), info);
        if ((oldInfo == null)
                || (oldInfo.getName() != null && info.getName() != null && !oldInfo.getName().equals(info.getName()))) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRongDatabaseDao != null) {
                        mRongDatabaseDao.putDiscussionInfo(discussion);
                    }
                    if (mCacheListener != null) {
                        mCacheListener.onDiscussionUpdated(discussion);
                    }
                }
            });
        }
    }

    public void setPublicServiceProfile(final PublicServiceProfile profile) {
        String key = StringUtils.getKey(profile.getConversationType().getValue() + "", profile.getTargetId());
        PublicServiceProfile oldInfo = mPublicServiceProfileCache.put(key, profile);

        if ((oldInfo == null)
                || (oldInfo.getName() != null && profile.getName() != null && !oldInfo.getName().equals(profile.getName()))
                || (oldInfo.getPortraitUri() != null && profile.getPortraitUri() != null && !oldInfo.getPortraitUri().toString().equals(profile.getPortraitUri().toString()))) {
            mWorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCacheListener != null) {
                        mCacheListener.onPublicServiceProfileUpdated(profile);
                    }
                }
            });
        }
    }
}
