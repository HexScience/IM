package com.vivavideo.imkit.widget;

import com.vivavideo.imkit.R;
import com.vivavideo.imkit.adapter.EmojiAdapter;
import com.vivavideo.imkit.model.Emoji;
import com.vivavideo.imkit.utils.AndroidEmoji;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;


public class RongEmojiPager {
    public static final int EMOJI_PER_PAGE = 23; // 最后一个是删除键

    private Context mContext;
    private ViewPager mViewPager;
    private LinearLayout mIndicator;
    private int mPageCount;
    private int mSelectedPage;

    public RongEmojiPager(ViewGroup viewGroup) {
        this.mContext = viewGroup.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.rc_input_pager_layout, viewGroup);
        this.mViewPager = (ViewPager) view.findViewById(R.id.rc_view_pager);
        this.mIndicator = (LinearLayout) view.findViewById(R.id.rc_indicator);

        mPageCount = (int) Math.ceil(AndroidEmoji.getEmojiList().size() / (float) EMOJI_PER_PAGE);
        mViewPager.setAdapter(new EmoticonViewPagerAdapter());
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onIndicatorChanged(mSelectedPage, position);
                mSelectedPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setCurrentItem(0, false);
        mViewPager.setOffscreenPageLimit(1);
        initIndicator(mPageCount, mIndicator);
        onIndicatorChanged(-1, 0);
    }

    private AdapterView.OnItemClickListener onItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String key = null;
                    int index = position + mSelectedPage * EMOJI_PER_PAGE;
                    if (position == EMOJI_PER_PAGE) {
                        key = "/DEL";
                    } else {
                        List<Emoji> emojis = AndroidEmoji.getEmojiList();
                        if (index >= emojis.size()) {
                            if (mSelectedPage == mPageCount - 1) {
                                key = "/DEL";
                            }
                        } else {
                            int code = emojis.get(index).getCode();
                            char[] chars = Character.toChars(code);
                            key = Character.toString(chars[0]);
                            for (int i = 1; i < chars.length; i++) {
                                key += Character.toString(chars[i]);
                            }
                        }
                    }
                    if(clickListener != null) {
                        clickListener.onEmojiClick(key);
                    }
                }
            };

    private OnEmojiClickListener clickListener;
    public void setOnEmojiClickListener(OnEmojiClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnEmojiClickListener {
        void onEmojiClick(String key);
    }

    private class EmoticonViewPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //emoji
            mIndicator.setVisibility(View.VISIBLE);
            GridView gridView = (GridView) LayoutInflater.from(container.getContext()).inflate(R.layout.rc_emoji_gridview, null);
            gridView.setOnItemClickListener(onItemClickListener);
            gridView.setAdapter(new EmojiAdapter(mContext, position * EMOJI_PER_PAGE));
            container.addView(gridView);
            return gridView;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mPageCount == 0 ? 1 : mPageCount;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View layout = (View) object;
            container.removeView(layout);
        }
    }

    private void initIndicator(int pages, LinearLayout indicator) {
        for(int i = 0; i < pages; i++) {
            ImageView imageView = new ImageView(mContext);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(16, 16);
            layoutParams.gravity = Gravity.CENTER;
            layoutParams.setMargins(0, 0, 20, 0);
            imageView.setLayoutParams(layoutParams);
            imageView.setImageResource(R.drawable.rc_indicator);
            indicator.addView(imageView);
        }
    }

    private void onIndicatorChanged(int pre, int cur) {
        int count = mIndicator.getChildCount();
        if(count > 0 && pre < count && cur < count) {
            if(pre >= 0) {
                ImageView preView = (ImageView) mIndicator.getChildAt(pre);
                preView.setImageResource(R.drawable.rc_indicator);
            }
            if(cur >= 0) {
                ImageView curView = (ImageView) mIndicator.getChildAt(cur);
                curView.setImageResource(R.drawable.rc_indicator_hover);
            }
        }
    }
}
