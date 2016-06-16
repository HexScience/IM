package com.vivavideo.imkit.provider;

import com.vivavideo.imkit.InputView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.model.ConversationKey;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;

import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.imlib.model.PublicServiceMenuItem;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.message.PublicServiceCommandMessage;

/**
 * Created by weiqinxiao on 15/6/26.
 */
public class PublicServiceMenuInputProvider extends InputProvider.MainInputProvider implements View.OnTouchListener {
    Context mContext;
    Conversation conversation;

    public PublicServiceMenuInputProvider(RongContext context) {
        super(context);
        mContext = context;
    }

    @Override
    public Drawable obtainSwitchDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_voice);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, InputView inputView) {

        conversation = getCurrentConversation();
        ConversationKey key = ConversationKey.obtain(conversation.getTargetId(), conversation.getConversationType());
        PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(key.getKey());

        if(info == null)
            return parent;

        PublicServiceMenu menu = info.getMenu();
        ArrayList<PublicServiceMenuItem> items = menu.getMenuItems();
        if(items == null || items.size() ==0)
            return parent;

        parent.removeAllViews();
        for (final PublicServiceMenuItem item : items) {
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.rc_item_public_service_input_menu, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            layout.setLayoutParams(lp);

            TextView title = (TextView) layout.findViewById(R.id.rc_title);
            title.setText(item.getName());
            ImageView iv = (ImageView) layout.findViewById(R.id.rc_icon);
            if(item.getSubMenuItems().size() > 0)
                iv.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ic_trangle));

            layout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (item.getSubMenuItems() == null || item.getSubMenuItems().size() == 0) {
                        onMenuItemSelect(item);
                    } else {
                        PopupMenu custom = new PopupMenu(mContext, item.getSubMenuItems());
                        custom.showAtLocation(v);
                    }
                }
            });
            parent.addView(layout);
        }
        return parent;
    }

    @Override
    public void onActive(Context context) {

    }

    @Override
    public void onInactive(Context context) {

    }

    private void onMenuItemSelect(PublicServiceMenuItem item) {
        if(item.getType().equals(PublicServiceMenu.PublicServiceMenuItemType.View)) {
//            Intent intent = new Intent(mContext, RongWebviewActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("url", item.getUrl());
//            mContext.startActivity(intent);
        }

        PublicServiceCommandMessage msg = PublicServiceCommandMessage.obtain(item);
        RongIMClient.getInstance().sendMessage(conversation.getConversationType(),
                conversation.getTargetId(),
                msg, null, null, new IRongCallback.ISendMessageCallback() {
                    @Override
                    public void onAttached(Message message) {

                    }

                    @Override
                    public void onSuccess(Message message) {

                    }

                    @Override
                    public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                    }
                });
    }

    private class PopupMenu {
        PopupWindow popupWindow;
        View container;
        ArrayList<PublicServiceMenuItem> list;

        public PopupMenu(Context context, ArrayList<PublicServiceMenuItem> list) {
            this.list = list;
            container = LayoutInflater.from(context).inflate(R.layout.rc_item_public_service_input_menus, null);
            LinearLayout group = (LinearLayout) container.findViewById(R.id.rc_layout);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            container.setLayoutParams(lp);

            group.setClickable(true);
            group.setFocusable(true);

            setupMenu(group);

            popupWindow = new PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        public void showAtLocation(View parent) {
            popupWindow.setBackgroundDrawable(new ColorDrawable());
            container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            int[] location = new int[2];
            int w = container.getMeasuredWidth();
            parent.getLocationOnScreen(location);
            int x = location[0] + (parent.getWidth() - w) / 2;
            int y = parent.getHeight() + 10;

            popupWindow.showAtLocation(parent, Gravity.LEFT | Gravity.BOTTOM, x, y);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.update();
        }

        public void dismiss() {
            popupWindow.dismiss();
        }

        void setupMenu(LinearLayout group){
            group.removeAllViews();
            for (int i = 0; i < list.size(); i++) {
                LinearLayout layoutItem = (LinearLayout) ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.rc_item_public_service_input_menu_item, null);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                container.setLayoutParams(lp);
                layoutItem.setFocusable(true);
                layoutItem.setTag(list.get(i));

                TextView tv = (TextView) layoutItem.findViewById(R.id.rc_menu_item_text);
                View pop_item_line = layoutItem.findViewById(R.id.rc_menu_line);
                if ((i + 1) == list.size()) {
                    pop_item_line.setVisibility(View.GONE);
                }
                tv.setText(list.get(i).getName());
                layoutItem.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PublicServiceMenuItem item = (PublicServiceMenuItem) v.getTag();
                        onMenuItemSelect(item);
                        dismiss();
                    }
                });
                group.addView(layoutItem);
            }
            group.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSwitch(final Context context) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return false;
    }
}
