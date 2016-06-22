package com.vivavideo.imkit.widget;

import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.userInfoCache.IRongCacheListener;

import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;

public class RongUserCacheListener implements IRongCacheListener {
    @Override
    public void onUserInfoUpdated(UserInfo info) {
        if (info != null) {
            RongContext.getInstance().getEventBus().post(info);
        }
    }

    @Override
    public void onGroupUpdated(Group group) {
        if (group != null) {
            RongContext.getInstance().getEventBus().post(group);
        }
    }

    @Override
    public void onDiscussionUpdated(Discussion discussion) {
        if (discussion != null) {
            RongContext.getInstance().getEventBus().post(discussion);
        }
    }

    @Override
    public void onPublicServiceProfileUpdated(PublicServiceProfile profile) {
        if (profile != null) {
            RongContext.getInstance().getEventBus().post(profile);
        }
    }

    @Override
    public UserInfo getUserInfo(String id) {
        if (RongContext.getInstance().getUserInfoProvider() != null) {
            return RongContext.getInstance().getUserInfoProvider().getUserInfo(id);
        }
        return null;
    }
}
