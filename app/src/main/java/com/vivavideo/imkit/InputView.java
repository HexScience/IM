package com.vivavideo.imkit;

import com.vivavideo.imkit.R;
import com.vivavideo.imkit.provider.InputProvider;
import com.vivavideo.imkit.provider.VoiceInputProvider;
import com.vivavideo.imkit.widget.RongPluginPager;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.CustomServiceMode;

/**
 * Created by DragonJ on 15/2/9.
 */
public class InputView extends LinearLayout {
    private final static String TAG = "InputView";

    volatile InputProvider.MainInputProvider mMainProvider;
    volatile InputProvider.MainInputProvider mSlaveProvider;
    volatile InputProvider.MainInputProvider mMenuProvider;


    List<InputProvider> mProviderList;

    enum Style {
        SCE(0x123),
        ECS(0x321),
        CES(0x231),
        CSE(0x213),
        SC(0x120),
        CS(0x021),
        EC(0x320),
        CE(0x023),
        C(0x020);

        private int value = 0;

        Style(int value) {
            this.value = value;
        }
    }

    int mStyle;

    public enum Event {
        ACTION, INACTION, DESTROY;
    }

    RelativeLayout mInputLayout;
    LinearLayout mSwitcherLayout, mCustomMenuLayout;
    ImageView mMenuSwitcher1, mMenuSwitcher2;
    LinearLayout mInputMenuLayout, mInputMenuSwitchLayout;
    FrameLayout mCustomLayout;
    FrameLayout mWidgetLayout;
    FrameLayout mExtendLayout;
    FrameLayout mToggleLayout;
    ImageView mIcon1, mIcon2;
    LinearLayout mPluginsLayout;
    View mView;

    int left, center, right;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        View view = inflate(context, R.layout.rc_wi_input, this);
        mView = view;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InputView);
        mStyle = a.getInt(R.styleable.InputView_RCStyle, 0x123);
        a.recycle();

        mProviderList = new ArrayList<>();

        mSwitcherLayout = (LinearLayout) view.findViewById(R.id.rc_switcher);
        mInputMenuSwitchLayout = (LinearLayout) view.findViewById(R.id.rc_menu_switch);
        mMenuSwitcher1 = (ImageView) view.findViewById(R.id.rc_switcher1);
        mMenuSwitcher2 = (ImageView) view.findViewById(R.id.rc_switcher2);
        mInputMenuLayout = (LinearLayout) view.findViewById(R.id.rc_input_menu);
        mInputLayout = (RelativeLayout) view.findViewById(android.R.id.input);
        mCustomLayout = (FrameLayout) view.findViewById(android.R.id.custom);
        mWidgetLayout = (FrameLayout) view.findViewById(android.R.id.widget_frame);
        mExtendLayout = (FrameLayout) view.findViewById(R.id.rc_ext);
        mToggleLayout = (FrameLayout) view.findViewById(android.R.id.toggle);

        mCustomMenuLayout = (LinearLayout) view.findViewById(R.id.rc_input_custom_menu);

        mIcon1 = (ImageView) view.findViewById(android.R.id.icon1);
        mIcon2 = (ImageView) view.findViewById(android.R.id.icon2);

        mPluginsLayout = (LinearLayout) view.findViewById(R.id.rc_plugins);

        left = (mStyle >> 8) % 16;
        center = (mStyle >> 4) % 16;
        right = (mStyle) % 16;

        mIcon2.setImageDrawable(getResources().getDrawable(R.drawable.rc_ic_extend));
        mIcon2.setOnClickListener(new ExtendClickListener());
    }

    class ExtendClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mPluginsLayout.getVisibility() == View.GONE || mExtendLayout.getVisibility() == View.VISIBLE) {
                onExtendProviderActive(v.getContext());
                mPluginsLayout.setVisibility(View.VISIBLE);
                if (mMainProvider instanceof VoiceInputProvider) {
                    setInputProvider(mSlaveProvider, mMainProvider);
                }
            } else if (mPluginsLayout.getVisibility() == View.VISIBLE) {
                onProviderInactive(v.getContext());
            }
        }
    }

    public interface OnInfoButtonClick {
        public void onClick(View view);
    }

    private OnInfoButtonClick onInfoButtonClick;
    public void setOnInfoButtonClickListener(OnInfoButtonClick clickListener) {
        this.onInfoButtonClick = clickListener;
    }

    public void setExtendInputsVisibility(int visibility) {
        onProviderInactive(this.getContext());
        setPluginsLayoutVisibility(visibility);
    }

    public void setPluginsLayoutVisibility(int visibility) {
        mPluginsLayout.setVisibility(visibility);
    }

    public void setExtendLayoutVisibility(int visibility) {
        mExtendLayout.setVisibility(visibility);
    }

    public void setWidgetLayoutVisibility(int visibility) {
        mWidgetLayout.setVisibility(visibility);
    }

    private final void changeMainProvider(View view, InputProvider.MainInputProvider main, InputProvider.MainInputProvider slave) {
        mMainProvider.onSwitch(view.getContext());

        mPluginsLayout.setVisibility(View.GONE);
        mExtendLayout.setVisibility(View.GONE);

        setInputProvider(mSlaveProvider, mMainProvider);
    }

    private void setSCE() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(leftParams);

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(rightParams);
        mToggleLayout.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }


    private void setECS() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(leftParams);

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCES() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mToggleLayout.setLayoutParams(centerParams);


        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(leftParams);
    }

    private void setCSE() {

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mSwitcherLayout.setLayoutParams(centerParams);


        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(leftParams);
    }

    private void setSC() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(leftParams);

        mToggleLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCS() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        mToggleLayout.setVisibility(View.GONE);


        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setEC() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(leftParams);

        mSwitcherLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCE() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setVisibility(View.VISIBLE);
        mToggleLayout.setLayoutParams(rightParams);

        mSwitcherLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private CustomServiceMode currentType;
    public void setOnlyRobotInputType() {
        currentType = CustomServiceMode.CUSTOM_SERVICE_MODE_ROBOT;
        setC();
    }

    public void setPriorRobotInputType() {
        if(currentType == null || currentType != CustomServiceMode.CUSTOM_SERVICE_MODE_ROBOT_FIRST) {
            currentType = CustomServiceMode.CUSTOM_SERVICE_MODE_ROBOT_FIRST;
            mIcon1.setImageResource(R.drawable.rc_ic_admin_selector);
            mIcon1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(switcherListener != null)
                        switcherListener.onClick(v);
                }
            });
            setSC();
        }
    }

    public void setOnlyAdminInputType() {
        currentType = CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN;
        if (mSlaveProvider != null) {
            mIcon1.setImageDrawable(mSlaveProvider.obtainSwitchDrawable(getContext()));
            mIcon1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeMainProvider(v, mSlaveProvider, mMainProvider);
                }
            });
        }
        setSCE();
    }

    private View.OnClickListener switcherListener;
    public void setOnSwitcherListener(View.OnClickListener listener) {
        switcherListener = listener;
    }

    private void setC() {
        mSwitcherLayout.setVisibility(View.GONE);
        mToggleLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

        mCustomLayout.setLayoutParams(centerParams);
    }


    public void setInputProvider(final InputProvider.MainInputProvider mainProvider, final InputProvider.MainInputProvider slaveProvider) {
        mMainProvider = mainProvider;
        mSlaveProvider = slaveProvider;

        if (mMenuProvider == null)
            mInputMenuSwitchLayout.setVisibility(View.GONE);

        mCustomLayout.removeAllViews();

        View leftView = null;
        View rightView = null;
        View centerView = null;


        switch (mStyle) {
            //SCE
            case (0x123):
                setSCE();
                break;
            //ECS
            case (0x321):
                setECS();
                break;
            //CES
            case (0x231):
                setCES();
                break;
            //CSE
            case (0x213):
                setCSE();
                break;
            //SC
            case (0x120):
                setSC();
                break;
            //CS
            case (0x021):
                setCS();
                break;
            //EC
            case (0x320):
                setEC();
                break;
            //CE
            case (0x023):
                setCE();
                break;
            //C
            case (0x020):
                setC();
                break;
        }

        if (mSlaveProvider != null) {
            mIcon1.setImageDrawable(mSlaveProvider.obtainSwitchDrawable(getContext()));
            mIcon1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentType != null && currentType.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_ROBOT_FIRST)) {
                        if(switcherListener != null)
                            switcherListener.onClick(v);
                    } else {
                        if (onInfoButtonClick != null)
                            onInfoButtonClick.onClick(v);
                        else
                            changeMainProvider(v, mSlaveProvider, mMainProvider);
                    }
                }
            });
        }

        mMainProvider.onCreateView(LayoutInflater.from(getContext()), mCustomLayout, this);
    }

    public void setInputProviderForCS(final InputProvider.MainInputProvider mainProvider, final InputProvider.MainInputProvider slaveProvider) {
        mMainProvider = mainProvider;
        mSlaveProvider = slaveProvider;

        if (mMenuProvider == null)
            mInputMenuSwitchLayout.setVisibility(View.GONE);

        mCustomLayout.removeAllViews();

        setPriorRobotInputType();

        if (mSlaveProvider != null) {
            mIcon1.setImageResource(R.drawable.rc_ic_admin_selector);
            mIcon1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentType != null && currentType.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_ROBOT_FIRST)) {
                        if(switcherListener != null)
                            switcherListener.onClick(v);
                    } else {
                        if (onInfoButtonClick != null)
                            onInfoButtonClick.onClick(v);
                        else
                            changeMainProvider(v, mSlaveProvider, mMainProvider);
                    }
                }
            });
        }

        mMainProvider.onCreateView(LayoutInflater.from(getContext()), mCustomLayout, this);
    }

    private Animation createPopupAnimIn(Context context) {
        AnimationSet animationSet = new AnimationSet(context, null);
        animationSet.setFillAfter(true);

        TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 150, 0);
        translateAnim.setDuration(300);
        animationSet.addAnimation(translateAnim);

        return animationSet;
    }

    private Animation createPopupAnimOut(Context context) {
        AnimationSet animationSet = new AnimationSet(context, null);
        animationSet.setFillAfter(true);

        TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 0, 150);
        translateAnim.setDuration(300);
        animationSet.addAnimation(translateAnim);

        return animationSet;
    }

    public void setInputProviderEx(final InputProvider.MainInputProvider mainProvider,
                                   final InputProvider.MainInputProvider slaveProvider,
                                   final InputProvider.MainInputProvider menuProvider) {
        mMenuProvider = menuProvider;
        setInputProvider(mainProvider, slaveProvider);

        if (menuProvider != null && mMenuSwitcher1 != null) {
            mInputMenuSwitchLayout.setVisibility(View.VISIBLE);
            menuProvider.onCreateView(LayoutInflater.from(getContext()), mCustomMenuLayout, this);
            mInputMenuSwitchLayout.setOnClickListener(new InputClickListener());
            mMenuSwitcher2.setOnClickListener(new InputMenuClickListener());

            mMainProvider.onSwitch(getContext());
            mPluginsLayout.setVisibility(View.GONE);
            mExtendLayout.setVisibility(View.GONE);
            mInputLayout.setVisibility(View.GONE);
            mInputMenuLayout.setVisibility(View.VISIBLE);
        }
    }

    class InputClickListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            RLog.d(TAG, "InputClickListener change to input menu");

            mMainProvider.onSwitch(v.getContext());
            mPluginsLayout.setVisibility(View.GONE);
            mExtendLayout.setVisibility(View.GONE);

            mInputLayout.startAnimation(createPopupAnimOut(v.getContext()));
            mInputMenuLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInputLayout.clearAnimation();
                    mInputLayout.setVisibility(View.GONE);

                    mInputMenuLayout.startAnimation(createPopupAnimIn(v.getContext()));
                    mInputMenuLayout.setVisibility(View.VISIBLE);
                }
            }, 300 + 10);
        }
    }

    class InputMenuClickListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            RLog.d(TAG, "InputMenuClickListener change to input");

            mInputMenuLayout.startAnimation(createPopupAnimOut(v.getContext()));

            mInputLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInputMenuLayout.clearAnimation();
                    mInputMenuLayout.setVisibility(View.GONE);

                    mInputLayout.startAnimation(createPopupAnimIn(v.getContext()));
                    mInputLayout.setVisibility(View.VISIBLE);
                }
            }, 300 + 10);
        }
    }

    public void setExtendProvider(List<InputProvider.ExtendProvider> providers, Conversation.ConversationType conversationType) {
        mProviderList.clear();
        for (InputProvider.ExtendProvider provider : providers) {
            mProviderList.add(provider);
        }
        int i = 1;
        for (InputProvider.ExtendProvider provider : providers) {
            provider.setIndex(++i);
        }

        new RongPluginPager(conversationType, mPluginsLayout);
    }


    public ViewGroup getExtendLayout() {
        return mExtendLayout;
    }

    public FrameLayout getToggleLayout() {
        return mToggleLayout;
    }
    public ImageView getIcon1() {
        return mIcon1;
    }

    public void onProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onActive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onActive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }

    public void onProviderInactive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.INACTION);
    }

    public void onExtendProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }

    public void onEmojiProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }

    public void setInputBoardListener(IInputBoardListener inputBoardListener) {
        this.inputBoardListener = inputBoardListener;
    }

    public interface IInputBoardListener {
        void onBoardExpanded(int height);
        void onBoardCollapsed();
    }

    boolean collapsed = true;
    int originalTop = 0;
    int originalBottom = 0;
    IInputBoardListener inputBoardListener;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(originalTop != 0) {
            if(originalTop > t) {
                if(originalBottom > b && inputBoardListener != null && collapsed) {
                    collapsed = false;
                    inputBoardListener.onBoardExpanded(originalBottom - t);
                } else if (collapsed && inputBoardListener != null) {
                    collapsed = false;
                    inputBoardListener.onBoardExpanded(b - t);
                }
            } else {
                if(!collapsed && inputBoardListener != null) {
                    collapsed = true;
                    inputBoardListener.onBoardCollapsed();
                }
            }
        }
        if(originalTop == 0) {
            originalTop = t;
            originalBottom = b;
        }
    }
}
