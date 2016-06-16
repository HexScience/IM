package com.vivavideo.imkit.userInfoCache;

import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;

public interface IRongCacheListener {
    void onUserInfoUpdated(UserInfo info);

    void onGroupUpdated(Group group);

    void onDiscussionUpdated(Discussion discussion);

    void onPublicServiceProfileUpdated(PublicServiceProfile profile);

    UserInfo getUserInfo(String id);

}
