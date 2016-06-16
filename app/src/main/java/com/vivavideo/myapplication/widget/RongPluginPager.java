package com.vivavideo.myapplication.widget;

import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.provider.InputProvider;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import io.rong.imlib.model.Conversation;

public class RongPluginPager {
    public static final int PLUGIN_PER_PAGE = 8;

    private ViewPager mViewPager;
    private LinearLayout mIndicator;
    private int mPageCount;
    private int mSelectedPage;
    private Conversation.ConversationType conversationType;

    public RongPluginPager(Conversation.ConversationType conversationType, ViewGroup viewGroup) {
        this.conversationType = conversationType;
        initView(viewGroup.getContext(), viewGroup);
        initData();
        initIndicator(mPageCount, mIndicator);
        mViewPager.setCurrentItem(0, false);
    }

    private void initView(Context context, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_input_pager_layout, viewGroup);
        mViewPager = (ViewPager)view.findViewById(R.id.rc_view_pager);
        mIndicator = (LinearLayout)view.findViewById(R.id.rc_indicator);
        mViewPager.setAdapter(new PluginViewPagerAdapter());
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
        mViewPager.setOffscreenPageLimit(1);
    }

    private void initData() {
        List<InputProvider.ExtendProvider> extendProviders;
        extendProviders = RongContext.getInstance().getRegisteredExtendProviderList(conversationType);
        mPageCount = (int) Math.ceil(extendProviders.size() / (float) PLUGIN_PER_PAGE);
        mViewPager.getAdapter().notifyDataSetChanged();
    }

    private void initIndicator(int pages, LinearLayout indicator) {
        for(int i = 0; i < pages; i++) {
            ImageView imageView = new ImageView(indicator.getContext());
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

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            List<InputProvider.ExtendProvider> extendProviders = RongContext.getInstance().getRegisteredExtendProviderList(conversationType);
            InputProvider.ExtendProvider provider = extendProviders.get(position + mSelectedPage * PLUGIN_PER_PAGE);
            provider.onPluginClick(view);
        }
    };

    private class PluginViewPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return mPageCount == 0 ? 1 : mPageCount;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            mIndicator.setVisibility(View.VISIBLE);
            GridView gridView = (GridView)LayoutInflater.from(container.getContext()).inflate(R.layout.rc_plugin_gridview, null);
            gridView.setAdapter(new PluginItemAdapter(position * PLUGIN_PER_PAGE));
            gridView.setOnItemClickListener(itemClickListener);
            container.addView(gridView);
            return gridView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View layout = (View) object;
            container.removeView(layout);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private class PluginItemAdapter extends BaseAdapter {
        int startIndex;
        List<InputProvider.ExtendProvider> extendProviders;

        public PluginItemAdapter(int startIndex) {
            this.startIndex = startIndex;
            extendProviders = RongContext.getInstance().getRegisteredExtendProviderList(conversationType);
        }

        @Override
        public int getCount() {
            int count = extendProviders.size() - startIndex;
            count = Math.min(count, PLUGIN_PER_PAGE);
            return count;
        }

        @Override
        public Object getItem(int position) {
            List<InputProvider.ExtendProvider> extendProviders;
            extendProviders = RongContext.getInstance().getRegisteredExtendProviderList(conversationType);
            return extendProviders.get(position + startIndex);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_wi_plugins, null);
            }
            ImageView imageView = (ImageView) convertView.findViewById(android.R.id.icon);
            TextView textView = (TextView) convertView.findViewById(android.R.id.title);
            if(startIndex + position < extendProviders.size()) {
                InputProvider.ExtendProvider provider = extendProviders.get(startIndex + position);
                imageView.setImageDrawable(provider.obtainPluginDrawable(parent.getContext()));
                textView.setText(provider.obtainPluginTitle(parent.getContext()));
            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return startIndex + position;
        }
    }
}
