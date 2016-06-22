package com.vivavideo.imkit.activity;

import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongIM;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import io.rong.imlib.model.Conversation;

public class MainActivity extends Activity {


    private TextView chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        chat = (TextView) findViewById(R.id.chat);
        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RongIM.getInstance().startConversation(getApplicationContext(), Conversation.ConversationType.PRIVATE, "136336", "MMMc");
            }
        });
//        UIConversation uiconversation = (UIConversation) parent.getAdapter().getItem(position);
//        Conversation.ConversationType type = uiconversation.getConversationType();
//        if (RongContext.getInstance().getConversationGatherState(type.getName())) {
//            RongIM.getInstance().startSubConversationList(getActivity(), type);
//        } else {
//            if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
//                boolean isDefault = RongContext.getInstance().getConversationListBehaviorListener().onConversationClick(getActivity(), view, uiconversation);
//                if (isDefault)
//                    return;
//            }
//            uiconversation.setUnReadMessageCount(0);

//        }

    }
}
