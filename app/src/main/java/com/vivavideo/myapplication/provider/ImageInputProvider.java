package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.SendImageManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import java.util.ArrayList;

import io.rong.imlib.model.Conversation;

public class ImageInputProvider extends InputProvider.ExtendProvider {
    private final static String TAG = "ImageInputProvider";

    public ImageInputProvider(RongContext context) {
        super(context);
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_picture);
    }

    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.rc_plugins_image);
    }

    @Override
    public void onPluginClick(View view) {
        Intent intent = new Intent();
        //TODO
//        intent.setClass(view.getContext(), PictureSelectorActivity.class);
        startActivityForResult(intent, 23);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            return;

        boolean sendOrigin = data.getBooleanExtra("sendOrigin", false);
        ArrayList<Uri> list = data.getParcelableArrayListExtra("android.intent.extra.RETURN_RESULT");
        Conversation conversation = getCurrentConversation();
        SendImageManager.getInstance().sendImages(conversation.getConversationType(), conversation.getTargetId(), list, sendOrigin);
    }
}