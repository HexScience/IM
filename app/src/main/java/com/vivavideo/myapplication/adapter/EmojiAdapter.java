package com.vivavideo.myapplication.adapter;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.model.Emoji;
import com.vivavideo.myapplication.utils.AndroidEmoji;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class EmojiAdapter extends android.widget.BaseAdapter {
    private static final int EMOJI_PER_PAGE = 23;
    private Context mContext;
    private int mStartIndex;

    public EmojiAdapter(Context context, int startIndex) {
        this.mContext = context;
        this.mStartIndex = startIndex;
    }

    @Override
    public int getCount() {
        int count = AndroidEmoji.getEmojiList().size() - mStartIndex + 1;
        count = Math.min(count, EMOJI_PER_PAGE + 1);
        return count;
    }

    @Override
    public Object getItem(int position) {
        Emoji e = AndroidEmoji.getEmojiList().get(position + mStartIndex);
        return e.getRes();
    }

    @Override
    public long getItemId(int position) {
        return mStartIndex + position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(mContext).inflate(R.layout.rc_emoji_item, null);
        ImageView emoji = (ImageView)convertView.findViewById(R.id.rc_emoji_item);
        int count = AndroidEmoji.getEmojiList().size();
        int index = position + mStartIndex;
        if(position == EMOJI_PER_PAGE || index == count) {
            emoji.setImageResource(R.drawable.rc_ic_delete);
        } else if(index < count) {
            Emoji e = AndroidEmoji.getEmojiList().get(index);
            emoji.setImageResource(e.getRes());
        }
        return convertView;
    }
}
