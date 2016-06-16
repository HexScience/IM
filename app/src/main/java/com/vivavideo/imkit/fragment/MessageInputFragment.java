package com.vivavideo.imkit.fragment;

import com.vivavideo.imkit.EventBus;
import com.vivavideo.imkit.InputView;
import com.vivavideo.imkit.R;
import com.vivavideo.imkit.RongContext;
import com.vivavideo.imkit.RongIM;
import com.vivavideo.imkit.model.ConversationKey;
import com.vivavideo.imkit.model.Event;
import com.vivavideo.imkit.provider.InputProvider;
import com.vivavideo.imkit.userInfoCache.RongUserInfoManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.rong.common.RLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.CustomServiceMode;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.imlib.model.PublicServiceProfile;

/**
 * Created by DragonJ on 14/10/23.
 */
public class MessageInputFragment extends UriFragment implements View.OnClickListener {
    private final static String TAG = "MessageInputFragment";

    private final static String IS_SHOW_EXTEND_INPUTS = "isShowExtendInputs";

    Conversation mConversation;
    InputView mInput;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_messageinput, container, false);
        mInput = (InputView) view.findViewById(R.id.rc_input);
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (RongContext.getInstance().getPrimaryInputProvider() == null) {
            throw new RuntimeException("MainInputProvider must not be null.");
        }

        if (getUri() != null) {

            String isShowExtendInputs = getUri().getQueryParameter(IS_SHOW_EXTEND_INPUTS);

            if (isShowExtendInputs != null && ("true".equals(isShowExtendInputs) || "1".equals(isShowExtendInputs))) {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setExtendInputsVisibility(View.VISIBLE);
                    }
                }, 500);

            } else {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setExtendInputsVisibility(View.GONE);
                    }
                }, 500);
            }
        }
    }

    public void setOnInfoButtonClick(InputView.OnInfoButtonClick onInfoButtonClick) {
        mInput.setOnInfoButtonClickListener(onInfoButtonClick);
    }

    public void setInputBoardListener(InputView.IInputBoardListener inputBoardListener) {
        mInput.setInputBoardListener(inputBoardListener);
    }

    private void setCurrentConversation(final Conversation conversation) {

        RongContext.getInstance().getPrimaryInputProvider().setCurrentConversation(conversation);

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().setCurrentConversation(conversation);
        }

        if (RongContext.getInstance().getMenuInputProvider() != null) {
            RongContext.getInstance().getMenuInputProvider().setCurrentConversation(conversation);
        }

        for (InputProvider provider : RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType())) {
            provider.setCurrentConversation(conversation);
        }

        mInput.setExtendProvider(RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()), mConversation.getConversationType());

        for (InputProvider provider : RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType())) {
            provider.onAttached(this, mInput);
        }

        if (conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) ||
                conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)) {

            ConversationKey key = ConversationKey.obtain(conversation.getTargetId(), conversation.getConversationType());
            PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(key.getKey());
            if(info == null) {
                Conversation.PublicServiceType type = Conversation.PublicServiceType.setValue(conversation.getConversationType().getValue());
                RongIM.getInstance().getPublicServiceProfile(type, conversation.getTargetId(), new RongIMClient.ResultCallback<PublicServiceProfile>() {
                    @Override
                    public void onSuccess(PublicServiceProfile publicServiceProfile) {
                        RongUserInfoManager.getInstance().setPublicServiceProfile(publicServiceProfile);
                        PublicServiceMenu menu = publicServiceProfile.getMenu();
                        if (menu != null && menu.getMenuItems() != null && menu.getMenuItems().size() > 0) {
                            mInput.setInputProviderEx(RongContext.getInstance().getPrimaryInputProvider(),
                                    RongContext.getInstance().getSecondaryInputProvider(),
                                    RongContext.getInstance().getMenuInputProvider());
                        } else {
                            mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                                    RongContext.getInstance().getSecondaryInputProvider());
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                                RongContext.getInstance().getSecondaryInputProvider());
                    }
                });
            } else {
                PublicServiceMenu menu = info.getMenu();
                if (menu != null && menu.getMenuItems() != null && menu.getMenuItems().size() > 0) {
                    mInput.setInputProviderEx(RongContext.getInstance().getPrimaryInputProvider(),
                            RongContext.getInstance().getSecondaryInputProvider(),
                            RongContext.getInstance().getMenuInputProvider());
                } else {
                    mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                            RongContext.getInstance().getSecondaryInputProvider());
                }
            }
        } else if(conversation.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)){
            mInput.setInputProviderForCS(RongContext.getInstance().getPrimaryInputProvider(),
                    RongContext.getInstance().getSecondaryInputProvider());
        } else{
            mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                    RongContext.getInstance().getSecondaryInputProvider());
        }

        RongContext.getInstance().getPrimaryInputProvider().onAttached(this, mInput);

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().onAttached(this, mInput);
        }
    }

    public void setInputProviderType(CustomServiceMode type) {
        switch (type) {
            case CUSTOM_SERVICE_MODE_ROBOT:
                mInput.setOnlyRobotInputType();
                break;
            case CUSTOM_SERVICE_MODE_ROBOT_FIRST:
                mInput.setPriorRobotInputType();
                break;
            case CUSTOM_SERVICE_MODE_HUMAN_FIRST:
            case CUSTOM_SERVICE_MODE_HUMAN:
                mInput.setOnlyAdminInputType();
                break;
            default:
                break;
        }
    }

    public void setOnRobotSwitcherListener(View.OnClickListener listener) {
        mInput.setOnSwitcherListener(listener);
    }

    @Override
    protected void initFragment(Uri uri) {
        String typeStr = uri.getLastPathSegment().toUpperCase();
        Conversation.ConversationType type = Conversation.ConversationType.valueOf(typeStr);

        String targetId = uri.getQueryParameter("targetId");

        String title = uri.getQueryParameter("title");

        if (type == null)
            return;

        mConversation = Conversation.obtain(type, targetId, title);

        if (mConversation != null) {
            setCurrentConversation(mConversation);
        }

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onDestroyView() {
        RLog.d(TAG, "onDestroyView the primary input provider is:" + RongContext.getInstance().getPrimaryInputProvider());

        RongContext.getInstance().getPrimaryInputProvider().onDetached();

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().onDetached();
        }

        EventBus.getDefault().unregister(this);

        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }


    private DispatchResultFragment getDispatchFragment(Fragment fragment) {
        if (fragment instanceof DispatchResultFragment)
            return (DispatchResultFragment) fragment;

        if (fragment.getParentFragment() == null)
            throw new RuntimeException(fragment.getClass().getName() + " must has a parent fragment instance of DispatchFragment.");

        return getDispatchFragment(fragment.getParentFragment());
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        return false;
    }

    public void startActivityFromProvider(InputProvider provider, Intent intent, int requestCode) {
        if (requestCode == -1) {
            startActivityForResult(intent, -1);
            return;
        }
        if ((requestCode & 0xffffff80) != 0) {
            throw new IllegalArgumentException("Can only use lower 7 bits for requestCode");
        }

        getDispatchFragment(this).startActivityForResult(this, intent, ((provider.getIndex() + 1) << 7) + (requestCode & 0x7f));

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        int index = requestCode >> 7;
        if (index != 0) {
            index--;
            if (index > RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()).size() + 1) {
                RLog.w(TAG, "onActivityResult Activity result provider index out of range: 0x"
                        + Integer.toHexString(requestCode));
                return;
            }

            if (index == 0) {
                RongContext.getInstance().getPrimaryInputProvider().onActivityResult(requestCode & 0x7f, resultCode, data);
            } else if (index == 1) {
                RongContext.getInstance().getSecondaryInputProvider().onActivityResult(requestCode & 0x7f, resultCode, data);
            } else {
                RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()).get(index - 2).onActivityResult(requestCode & 0x7f, resultCode, data);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    public void onEventMainThread(Event.InputViewEvent event) {

        if (event.isVisibility()) {
            mInput.setExtendInputsVisibility(View.VISIBLE);
        } else {
            mInput.setExtendInputsVisibility(View.GONE);
        }
    }
}
