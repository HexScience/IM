package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.RongIM;
import com.vivavideo.myapplication.model.LinkTextView;
import com.vivavideo.myapplication.model.ProviderTag;
import com.vivavideo.myapplication.model.UIMessage;
import com.vivavideo.myapplication.userInfoCache.RongUserInfoManager;
import com.vivavideo.myapplication.utils.AndroidEmoji;
import com.vivavideo.myapplication.widget.ArraysDialogFragment;
import com.vivavideo.myapplication.widget.ILinkClickListener;
import com.vivavideo.myapplication.widget.LinkTextViewMovementMethod;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.TextMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
@ProviderTag(messageContent = TextMessage.class)
public class TextMessageItemProvider extends IContainerItemProvider.MessageProvider<TextMessage> {

    class ViewHolder {
        LinkTextView message;
        boolean longClick;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_text_message, null);

        ViewHolder holder = new ViewHolder();
        holder.message = (LinkTextView) view.findViewById(android.R.id.text1);
        view.setTag(holder);
        return view;
    }

    @Override
    public Spannable getContentSummary(TextMessage data) {
        if(data == null)
            return null;
        
        String content = data.getContent();
        if (content != null) {
            if(content.length() > 100) {
                content = content.substring(0, 100);
            }
            return new SpannableString(AndroidEmoji.ensure(content));
        }
        return null;
    }

    @Override
    public void onItemClick(View view, int position, TextMessage content, UIMessage message) {

    }

    @Override
    public void onItemLongClick(final View view, int position, final TextMessage content, final UIMessage message) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.longClick = true;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text instanceof Spannable)
                Selection.removeSelection((Spannable) text);
        }

        String name = null;
        if (message.getConversationType().getName().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE.getName()) ||
                message.getConversationType().getName().equals(Conversation.ConversationType.PUBLIC_SERVICE.getName())) {
            if (message.getUserInfo() != null) {
                name = message.getUserInfo().getName();
            } else {
                Conversation.PublicServiceType publicServiceType = Conversation.PublicServiceType.setValue(message.getConversationType().getValue());
                PublicServiceProfile info = RongUserInfoManager.getInstance().getPublicServiceProfile(publicServiceType, message.getTargetId());

                if (info != null)
                    name = info.getName();
            }
        } else {
            if (message.getSenderUserId() != null) {
                UserInfo userInfo = message.getUserInfo();
                if(userInfo == null || userInfo.getName() == null)
                    userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());

                if (userInfo != null)
                    name = userInfo.getName();
            }
        }
        String[] items;

        items = new String[]{view.getContext().getResources().getString(R.string.rc_dialog_item_message_copy), view.getContext().getResources().getString(R.string.rc_dialog_item_message_delete)};

        ArraysDialogFragment.newInstance(name, items).setArraysDialogItemListener(new ArraysDialogFragment.OnArraysDialogItemListener() {
            @Override
            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    @SuppressWarnings("deprecation")
                    ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(((TextMessage) content).getContent());
                } else if (which == 1) {
                    RongIM.getInstance().deleteMessages(new int[]{message.getMessageId()}, null);
                }

            }
        }).show(((FragmentActivity) view.getContext()).getSupportFragmentManager());
    }

    @Override
    public void bindView(final View v, int position, TextMessage content, final UIMessage data) {
        ViewHolder holder = (ViewHolder) v.getTag();

        if (data.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_right);
        } else {
            holder.message.setBackgroundResource(R.drawable.rc_ic_bubble_left);
        }

        final TextView textView = holder.message;
        if(data.getTextMessageContent() != null) {
            int len = data.getTextMessageContent().length();
            if (v.getHandler() != null && len > 500) {
                v.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(data.getTextMessageContent());
                    }
                }, 50);
            } else {
                textView.setText(data.getTextMessageContent());
            }
        }

        holder.message.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                RongIM.ConversationBehaviorListener listener = RongContext.getInstance().getConversationBehaviorListener();
                if (listener != null)
                    return listener.onMessageLinkClick(v.getContext(), link);
                else
                    return false;
            }
        }));
    }
}
