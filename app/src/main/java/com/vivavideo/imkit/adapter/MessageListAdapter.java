package com.vivavideo.imkit.adapter;

import com.vivavideo.imkit.eventbus.EventBus;
import com.vivavideo.imkit.widget.ProviderContainerView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.model.ConversationKey;
import com.vivavideo.imkit.model.Event;
import com.vivavideo.imkit.model.ProviderTag;
import com.vivavideo.imkit.model.UIMessage;
import com.vivavideo.imkit.provider.IContainerItemProvider;
import com.vivavideo.imkit.userInfoCache.RongUserInfoManager;
import com.vivavideo.imkit.utils.RongDateUtils;
import com.vivavideo.imkit.widget.AsyncImageView;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

import io.rong.common.RLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.TextMessage;

public class MessageListAdapter extends BaseAdapter<UIMessage> {
    LayoutInflater mInflater;
    Context mContext;
    Drawable mDefaultDrawable;
    OnItemHandlerListener mOnItemHandlerListener;
    View subView;
    boolean evaForRobot = false;
    boolean robotMode = true;

    private boolean timeGone = false;

    class ViewHolder {
        AsyncImageView leftIconView;
        AsyncImageView rightIconView;
        TextView nameView;
        ProviderContainerView contentView;
        ProgressBar progressBar;
        ImageView warning;
        ImageView readReceipt;
        ViewGroup layout;
        TextView time;
        TextView sentStatus;
    }

    public MessageListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mDefaultDrawable = context.getResources().getDrawable(R.drawable.rc_ic_def_msg_portrait);
    }

    public void setOnItemHandlerListener(OnItemHandlerListener onItemHandlerListener) {
        this.mOnItemHandlerListener = onItemHandlerListener;
    }

    public interface OnItemHandlerListener {
        public void onWarningViewClick(int position, Message data, View v);
    }

    @Override
    public long getItemId(int position) {
        Message message = getItem(position);
        if (message == null)
            return -1;
        return message.getMessageId();
    }

    @Override
    protected View newView(final Context context, final int position, ViewGroup group) {
        View result = mInflater.inflate(R.layout.rc_item_message, null);

        final ViewHolder holder = new ViewHolder();
        holder.leftIconView = findViewById(result, R.id.rc_left);
        holder.rightIconView = findViewById(result, R.id.rc_right);
        holder.nameView = findViewById(result, R.id.rc_title);
        holder.contentView = findViewById(result, R.id.rc_content);
        holder.layout = findViewById(result, R.id.rc_layout);
        holder.progressBar = findViewById(result, R.id.rc_progress);
        holder.warning = findViewById(result, R.id.rc_warning);
        holder.readReceipt = findViewById(result, R.id.rc_read_receipt);
        holder.time = findViewById(result, R.id.rc_time);
        holder.sentStatus = findViewById(result, R.id.rc_sent_status);
        if (holder.time.getVisibility() == View.GONE) {
            timeGone = true;
        } else {
            timeGone = false;
        }

        result.setTag(holder);


        return result;
    }

    public void playNextAudioIfNeed(UIMessage data, int position) {
        IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
        if (provider != null && subView != null)
            provider.onItemClick(subView, position, data.getContent(), data);
    }

    private boolean getNeedEvaluate(UIMessage data){
        String extra = "";
        String robotEva = "";
        String sid = "";
        if(data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)){
            if(data.getContent() instanceof TextMessage) {
                extra = ((TextMessage) data.getContent()).getExtra();
                if(TextUtils.isEmpty(extra))
                    return false;
                try {
                    JSONObject jsonObj = new JSONObject(extra);
                    robotEva = jsonObj.optString("robotEva");
                    sid = jsonObj.optString("sid");
                } catch (JSONException e) {
                }
            }
            if(data.getMessageDirection() == Message.MessageDirection.RECEIVE
                    && data.getContent() instanceof TextMessage
                    && evaForRobot
                    && robotMode
                    && !TextUtils.isEmpty(robotEva)
                    && !TextUtils.isEmpty(sid)
                    && !data.getIsHistoryMessage()){
                return true;
            }
        }
        return false;
    }
    @Override
    protected void bindView(View v, final int position, final UIMessage data) {

        final ViewHolder holder = (ViewHolder) v.getTag();
        IContainerItemProvider provider = null;

        if(getNeedEvaluate(data)) {
            provider = RongContext.getInstance().getEvaluateProvider();
        }
        else if (RongContext.getInstance() != null && data != null && data.getContent() != null) {
            provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
            if (provider == null) {
                RLog.e("MessageListAdapter", data.getObjectName() + " message provider not found !");
                return;
            }
        } else {
            RLog.e("MessageListAdapter", "Message is null !");
            return;
        }

        View view = holder.contentView.inflate(provider);
        provider.bindView(view, position, data);

        subView = view;

        ProviderTag tag = RongContext.getInstance().getMessageProviderTag(data.getContent().getClass());
        if(tag == null) {
            RLog.e("MessageListAdapter", "Can not find ProviderTag for " + data.getObjectName());
            return;
        }

        if (tag.hide()) {
            holder.contentView.setVisibility(View.GONE);
            holder.time.setVisibility(View.GONE);
            holder.nameView.setVisibility(View.GONE);
            holder.leftIconView.setVisibility(View.GONE);
            holder.rightIconView.setVisibility(View.GONE);
        } else {
            holder.contentView.setVisibility(View.VISIBLE);
        }

        if (data.getMessageDirection() == Message.MessageDirection.SEND) {

            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.VISIBLE);
                holder.leftIconView.setVisibility(View.GONE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.RIGHT);
                holder.contentView.containerViewRight();
                holder.nameView.setGravity(Gravity.RIGHT);
            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            //readRec 是否显示已读回执
            boolean readRec = RongIMClient.getInstance().getReadReceipt();

            if (data.getSentStatus() == Message.SentStatus.SENDING) {
                if (tag.showProgress())
                    holder.progressBar.setVisibility(View.VISIBLE);
                else
                    holder.progressBar.setVisibility(View.GONE);

                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.FAILED) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.VISIBLE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.SENT) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (readRec && data.getSentStatus() == Message.SentStatus.READ) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                MessageContent content = data.getMessage().getContent();
                if (!(content instanceof InformationNotificationMessage)) {
                    holder.readReceipt.setVisibility(View.VISIBLE);
                } else {
                    holder.readReceipt.setVisibility(View.GONE);
                }
            } else {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            }

            if (data.getObjectName().equals("RC:VSTMsg")) {
                holder.readReceipt.setVisibility(View.GONE);
            }

            holder.nameView.setVisibility(View.GONE);

            holder.rightIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = null;
                        if (!TextUtils.isEmpty(data.getSenderUserId())) {
                            userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                            userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                        }
                        RongContext.getInstance().getConversationBehaviorListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo);
                    }
                }
            });

            holder.rightIconView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {

                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = null;
                        if (!TextUtils.isEmpty(data.getSenderUserId())) {
                            userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                            userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                        }
                        return RongContext.getInstance().getConversationBehaviorListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo);
                    }

                    return true;
                }
            });

            if (!tag.showWarning())
                holder.warning.setVisibility(View.GONE);

//            holder.sentStatus.setVisibility(View.VISIBLE);

        } else {
            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.GONE);
                holder.leftIconView.setVisibility(View.VISIBLE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.LEFT);
                holder.contentView.containerViewLeft();
                holder.nameView.setGravity(Gravity.LEFT);

            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            holder.progressBar.setVisibility(View.GONE);
            holder.warning.setVisibility(View.GONE);
            holder.readReceipt.setVisibility(View.GONE);

            holder.nameView.setVisibility(View.VISIBLE);

            if (data.getConversationType() == Conversation.ConversationType.PRIVATE
                    || !tag.showPortrait()
                    || data.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE
                    || data.getConversationType() == Conversation.ConversationType.APP_PUBLIC_SERVICE) {

                holder.nameView.setVisibility(View.GONE);
            } else {
                UserInfo userInfo;
                if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                        && data.getUserInfo() != null && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                    userInfo = data.getUserInfo();
                    holder.nameView.setText(userInfo.getName());
                } else {
                    userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                    if (userInfo == null)
                        holder.nameView.setText(data.getSenderUserId());
                    else
                        holder.nameView.setText(userInfo.getName());
                }
            }

            holder.leftIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = null;
                        if (!TextUtils.isEmpty(data.getSenderUserId())) {
                            userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                            userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                        }
                        RongContext.getInstance().getConversationBehaviorListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo);
                    }
                    EventBus.getDefault().post(Event.InputViewEvent.obtain(false));
                }
            });
        }

        holder.leftIconView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                    UserInfo userInfo = null;
                    if (!TextUtils.isEmpty(data.getSenderUserId())) {
                        userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());
                        userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                    }
                    return RongContext.getInstance().getConversationBehaviorListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo);
                }
                return false;
            }
        });


        if (holder.rightIconView.getVisibility() == View.VISIBLE) {
            UserInfo userInfo;
            Uri portrait;
            if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && data.getUserInfo() != null && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                userInfo = data.getUserInfo();
                portrait = userInfo.getPortraitUri();
                if (portrait != null) {
                    holder.rightIconView.setAvatar(portrait.toString(), 0);
                }
            } else if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {

                userInfo = data.getUserInfo();
                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                    if (portrait != null) {
                        holder.leftIconView.setAvatar(portrait.toString(), 0);
                    }
                } else {
                    PublicServiceProfile publicServiceProfile;

                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    portrait = publicServiceProfile.getPortraitUri();

                    if (portrait != null) {
                        holder.rightIconView.setAvatar(portrait.toString(), 0);
                    }
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());

                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    holder.rightIconView.setAvatar(userInfo.getPortraitUri().toString(), 0);
                }
            }
        } else if (holder.leftIconView.getVisibility() == View.VISIBLE) {
            UserInfo userInfo;
            Uri portrait;
            if (data.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    && data.getUserInfo() != null && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                userInfo = data.getUserInfo();
                portrait = userInfo.getPortraitUri();
                if (portrait != null) {
                    holder.leftIconView.setAvatar(portrait.toString(), 0);
                }
            } else if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {

                userInfo = data.getUserInfo();
                if (userInfo != null) {
                    portrait = userInfo.getPortraitUri();
                    if (portrait != null) {
                        holder.leftIconView.setAvatar(portrait.toString(), 0);
                    }
                } else {
                    PublicServiceProfile publicServiceProfile;
                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    if (publicServiceProfile != null && publicServiceProfile.getPortraitUri() != null) {
                        holder.leftIconView.setAvatar(publicServiceProfile.getPortraitUri().toString(), 0);
                    }
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                userInfo = RongUserInfoManager.getInstance().getUserInfo(data.getSenderUserId());

                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    holder.leftIconView.setAvatar(userInfo.getPortraitUri().toString(), 0);
                }
            }
        }

        if (view != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        if (RongContext.getInstance().getConversationBehaviorListener().onMessageClick(mContext, v, data)) {
                            return;
                        }
                    }

                    IContainerItemProvider.MessageProvider provider;//= RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    if(getNeedEvaluate(data))
                        provider = RongContext.getInstance().getEvaluateProvider();
                    else
                        provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    if(provider != null)
                        provider.onItemClick(v, position, data.getContent(), data);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null)
                        if (RongContext.getInstance().getConversationBehaviorListener().onMessageLongClick(mContext, v, data))
                            return true;

                    IContainerItemProvider.MessageProvider provider;//= RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    if(getNeedEvaluate(data))
                        provider = RongContext.getInstance().getEvaluateProvider();
                    else
                        provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    if(provider != null)
                        provider.onItemLongClick(v, position, data.getContent(), data);
                    return true;
                }
            });
        }

        holder.warning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemHandlerListener != null)
                    mOnItemHandlerListener.onWarningViewClick(position, data, v);
            }
        });

        if (tag.hide()) {
            holder.time.setVisibility(View.GONE);
            return;
        }

        if (!timeGone) {
            String time = RongDateUtils.getConversationFormatDate(new Date(data.getSentTime()));
            holder.time.setText(time);
            if (position == 0) {
                holder.time.setVisibility(View.VISIBLE);
            } else {
                Message pre = getItem(position - 1);

                if (data.getSentTime() - pre.getSentTime() > 60 * 1000) {
                    holder.time.setVisibility(View.VISIBLE);
                } else {
                    holder.time.setVisibility(View.GONE);
                }
            }
        }
    }

    private final void setGravity(View view, int gravity) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;
    }

    public void setEvaluateForRobot(boolean needEvaluate){
        evaForRobot = needEvaluate;
    }

    public void setRobotMode(boolean robotMode){
        this.robotMode = robotMode;
    }
}
