package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.RongIM;
import com.vivavideo.myapplication.model.ConversationKey;
import com.vivavideo.myapplication.model.ConversationProviderTag;
import com.vivavideo.myapplication.model.ProviderTag;
import com.vivavideo.myapplication.model.UIConversation;
import com.vivavideo.myapplication.userInfoCache.RongUserInfoManager;
import com.vivavideo.myapplication.utils.AndroidEmoji;
import com.vivavideo.myapplication.utils.RongDateUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

@ConversationProviderTag(conversationType = "private", portraitPosition = 1)
public class PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation> {

    class ViewHolder {
        TextView title;
        TextView time;
        TextView content;
        ImageView notificationBlockImage;
        ImageView readStatus;
    }

    public View newView(Context context, ViewGroup viewGroup) {
        View result = LayoutInflater.from(context).inflate(R.layout.rc_item_base_conversation, null);

        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) result.findViewById(R.id.rc_conversation_title);
        holder.time = (TextView) result.findViewById(R.id.rc_conversation_time);
        holder.content = (TextView) result.findViewById(R.id.rc_conversation_content);
        holder.notificationBlockImage = (ImageView) result.findViewById(R.id.rc_conversation_msg_block);
        holder.readStatus = (ImageView) result.findViewById(R.id.rc_conversation_status);
        result.setTag(holder);

        return result;
    }

    public void bindView(View view, int position, UIConversation data) {
        ViewHolder holder = (ViewHolder) view.getTag();
        ProviderTag tag = null;
        if (data == null) {
            holder.title.setText(null);
            holder.time.setText(null);
            holder.content.setText(null);
        } else {
            //设置会话标题
            holder.title.setText(data.getUIConversationTitle());
            //设置会话时间
            String time = RongDateUtils.getConversationListFormatDate(new Date(data.getUIConversationTime()));
            holder.time.setText(time);

            //设置内容
            if (!TextUtils.isEmpty(data.getDraft())) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                SpannableString string = new SpannableString(view.getContext().getString(R.string.rc_message_content_draft));

                string.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_draft_color)), 0, string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(string).append(" ")
                        .append(data.getDraft());

                AndroidEmoji.ensure(builder);

                holder.content.setText(builder);
                holder.readStatus.setVisibility(View.GONE);

            } else {
                //设置已读
                //readRec 是否显示已读回执
                boolean readRec = RongIMClient.getInstance().getReadReceipt();
                if (readRec == true) {
                    if (data.getSentStatus() == Message.SentStatus.READ && data.getConversationType().getName().equals(Conversation.ConversationType.PRIVATE.getName())
                            && data.getConversationSenderId().equals(RongIM.getInstance().getCurrentUserId())) {
                        holder.readStatus.setVisibility(View.VISIBLE);
                    } else {
                        holder.readStatus.setVisibility(View.GONE);
                    }
                }
                holder.content.setText(data.getConversationContent());
            }

            if (RongContext.getInstance() != null && data.getMessageContent() != null)
                tag = RongContext.getInstance().getMessageProviderTag(data.getMessageContent().getClass());

            if (data.getSentStatus() != null && (data.getSentStatus() == Message.SentStatus.FAILED
                    || data.getSentStatus() == Message.SentStatus.SENDING) && tag != null && tag.showWarning() == true
                    && data.getConversationSenderId() != null && data.getConversationSenderId().equals(RongIM.getInstance().getCurrentUserId())) {
                int width = (int) view.getContext().getResources().getDimension(R.dimen.rc_message_send_status_image_size);
                Drawable drawable = null;
                if (data.getSentStatus() == Message.SentStatus.FAILED && TextUtils.isEmpty(data.getDraft()))
                    drawable = view.getContext().getResources().getDrawable(R.drawable.rc_conversation_list_msg_send_failure);
                else if (data.getSentStatus() == Message.SentStatus.SENDING && TextUtils.isEmpty(data.getDraft()))
                    drawable = view.getContext().getResources().getDrawable(R.drawable.rc_conversation_list_msg_sending);
                if (drawable != null) {
                    drawable.setBounds(0, 0, width, width);
                    holder.content.setCompoundDrawablePadding(10);
                    holder.content.setCompoundDrawables(drawable, null, null, null);
                }
            } else {
                holder.content.setCompoundDrawables(null, null, null, null);
            }

            ConversationKey key = ConversationKey.obtain(data.getConversationTargetId(), data.getConversationType());
            Conversation.ConversationNotificationStatus status = RongContext.getInstance().getConversationNotifyStatusFromCache(key);
            if (status != null && status.equals(Conversation.ConversationNotificationStatus.DO_NOT_DISTURB))
                holder.notificationBlockImage.setVisibility(View.VISIBLE);
            else
                holder.notificationBlockImage.setVisibility(View.GONE);
        }
    }


    public Spannable getSummary(UIConversation data) {
        return null;
    }

    public String getTitle(String userId) {
        String name;
        if (RongUserInfoManager.getInstance().getUserInfo(userId) == null) {
            name = userId;
        } else {
            name = RongUserInfoManager.getInstance().getUserInfo(userId).getName();
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        Uri uri;
        if (RongUserInfoManager.getInstance().getUserInfo(id) == null) {
            uri = null;
        } else {
            uri = RongUserInfoManager.getInstance().getUserInfo(id).getPortraitUri();
        }
        return uri;
    }

}
