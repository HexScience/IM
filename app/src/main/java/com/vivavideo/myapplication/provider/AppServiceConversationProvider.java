package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.model.ConversationKey;
import com.vivavideo.myapplication.model.ConversationProviderTag;
import com.vivavideo.myapplication.model.UIConversation;

import android.net.Uri;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;

@ConversationProviderTag(conversationType = "app_public_service", portraitPosition = 1)
public class AppServiceConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation>{

    @Override
    public String getTitle(String id) {
        String name;
        ConversationKey mKey = ConversationKey.obtain(id, Conversation.ConversationType.APP_PUBLIC_SERVICE);
        PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());

        if (info != null) {
            name = info.getName();
        } else {
            name = "";
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        Uri uri;
        ConversationKey mKey = ConversationKey.obtain(id, Conversation.ConversationType.APP_PUBLIC_SERVICE);
        PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());

        if (info != null) {
            uri = info.getPortraitUri();
        } else {
            uri = null;
        }
        return uri;
    }
}
