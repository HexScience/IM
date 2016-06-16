package com.vivavideo.myapplication.provider;

import com.vivavideo.myapplication.InputView;
import com.vivavideo.myapplication.R;
import com.vivavideo.myapplication.RongContext;
import com.vivavideo.myapplication.RongEmojiPager;
import com.vivavideo.myapplication.fragment.MessageInputFragment;
import com.vivavideo.myapplication.model.Draft;
import com.vivavideo.myapplication.utils.AndroidEmoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.TextMessage;

public class TextInputProvider extends InputProvider.MainInputProvider implements TextWatcher, View.OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener {
    private final static String TAG = "TextInputProvider";
    EditText mEdit;
    ImageView mSmile;
    Button mButton;
    volatile InputView mInputView;
    RongEmojiPager mEmojiPager;
    TextWatcher mExtraTextWatcher;

    public TextInputProvider(RongContext context) {
        super(context);
        RLog.d(TAG, "TextInputProvider");
    }

    @Override
    public void onAttached(MessageInputFragment fragment, InputView view) {
        RLog.d(TAG, "onAttached");
        super.onAttached(fragment, view);
    }

    @Override
    public void onDetached() {
        RLog.d(TAG, "Detached");

        if (mEdit != null && !TextUtils.isEmpty(mEdit.getText())) {
            String text = mEdit.getText().toString();
            RongContext.getInstance().executorBackground(new SaveDraftRunnable(getCurrentConversation(), text));
        } else {
            RongContext.getInstance().executorBackground(new CleanDraftRunnable(getCurrentConversation()));
        }

        mEmojiPager = null;

        super.onDetached();
    }

    @Override
    public Drawable obtainSwitchDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_keyboard);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, InputView inputView) {
        RLog.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.rc_wi_txt_provider, parent);
        mEdit = (EditText) view.findViewById(android.R.id.edit);
        mSmile = (ImageView) view.findViewById(android.R.id.icon);

        if (inputView.getToggleLayout().getVisibility() == View.VISIBLE) {
            mButton = (Button) inflater.inflate(R.layout.rc_wi_text_btn, inputView.getToggleLayout(), false);
            inputView.getToggleLayout().addView(mButton);
        }

        if (inputView.getToggleLayout().getVisibility() != View.VISIBLE || mButton == null)
            mButton = (Button) view.findViewById(android.R.id.button1);

        mEdit.addTextChangedListener(this);
        mEdit.setOnFocusChangeListener(this);
        mSmile.setOnClickListener(this);
        mEdit.setOnClickListener(this);
        mEdit.setOnLongClickListener(this);
        mInputView = inputView;
        mButton.setOnClickListener(this);

        RongContext.getInstance().executorBackground(new DraftRenderRunnable(getCurrentConversation()));

        return view;
    }

    @Override
    public void setCurrentConversation(Conversation conversation) {
        super.setCurrentConversation(conversation);
        RongContext.getInstance().executorBackground(new DraftRenderRunnable(conversation));
    }

    class DraftRenderRunnable implements Runnable {
        Conversation conversation;

        DraftRenderRunnable(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().getTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), new RongIMClient.ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    if(!TextUtils.isEmpty(s) && mEdit != null) {
                        mEdit.setText(s);
                        mEdit.setSelection(s.length());
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    class SaveDraftRunnable implements Runnable {
        String content;
        Conversation conversation;

        SaveDraftRunnable(Conversation conversation, String content) {
            this.conversation = conversation;
            this.content = content;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().saveTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), content, new RongIMClient.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    int type = conversation.getConversationType().getValue();
                    String targetId = conversation.getTargetId();
                    Draft draft = new Draft(targetId, type, content, null);
                    getContext().getEventBus().post(draft);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    class CleanDraftRunnable implements Runnable {
        Conversation conversation;

        CleanDraftRunnable(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().clearTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), new RongIMClient.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    int type = conversation.getConversationType().getValue();
                    String targetId = conversation.getTargetId();
                    Draft draft = new Draft(targetId, type, null, null);
                    getContext().getEventBus().post(draft);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (mSmile.equals(v)) {
            if (mEmojiPager == null) {
                mEmojiPager = new RongEmojiPager(mInputView.getExtendLayout());
                mEmojiPager.setOnEmojiClickListener(new RongEmojiPager.OnEmojiClickListener() {
                    @Override
                    public void onEmojiClick(String key) {
                        if (key.equals("/DEL")) {
                            mEdit.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        } else {
                            int start = mEdit.getSelectionStart();
                            mEdit.getText().insert(start, key);
                        }
                    }
                });

                if(mEdit != null){
                    mEdit.requestFocus();
                }
                mInputView.onEmojiProviderActive(getContext());
                mInputView.setExtendLayoutVisibility(View.VISIBLE);
            } else if (mInputView.getExtendLayout().getVisibility() == View.GONE) {
                mInputView.onEmojiProviderActive(getContext());
                mInputView.setExtendLayoutVisibility(View.VISIBLE);
            } else {
                mInputView.onProviderInactive(getContext());
            }
        } else if (v.equals(mButton)) {
            if (TextUtils.isEmpty(mEdit.getText().toString().trim())) {
                mEdit.getText().clear();
                mEdit.setText("");
                return;
            }

            publish(TextMessage.obtain(mEdit.getText().toString()));
            mEdit.getText().clear();
            mEdit.setText("");
        } else if (mEdit.equals(v)) {
            mInputView.onProviderActive(getContext());
        }
    }


    @Override
    public boolean onLongClick(View v) {
        if (mInputView != null && mInputView.getExtendLayout().getVisibility() == View.VISIBLE) {
            mInputView.onProviderInactive(getContext());
            mInputView.setExtendLayoutVisibility(View.GONE);
        }
        return false;
    }

    @Override
    public void onActive(Context context) {
        if (mEdit == null)
            return;

        mEdit.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEdit, 0);

    }

    @Override
    public void onInactive(Context context) {

        if (mEdit == null)
            return;

//        mEdit.clearFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
    }

    @Override
    public void onSwitch(Context context) {
        mButton.setVisibility(View.GONE);
        onInactive(context);
        if (mEdit != null && !TextUtils.isEmpty(mEdit.getText())) {
            RongContext.getInstance().executorBackground(new SaveDraftRunnable(getCurrentConversation(), mEdit.getText().toString()));
        } else {
            RongContext.getInstance().executorBackground(new CleanDraftRunnable(getCurrentConversation()));
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (mInputView != null && hasFocus)
            mInputView.setExtendInputsVisibility(View.GONE);
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if(mExtraTextWatcher != null)
        mExtraTextWatcher.beforeTextChanged(s, start, count, after);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(mExtraTextWatcher != null)

            mExtraTextWatcher.onTextChanged(s,start,before,count);

        if (mButton != null) {
            if (TextUtils.isEmpty(s)) {
                mButton.setVisibility(View.GONE);
            } else {
                if(mInputView.getToggleLayout().getVisibility() == View.VISIBLE) {
                    View view = mInputView.getToggleLayout().findViewById(android.R.id.button1);
                    if (view == null) {
                        if(mButton != null)
                            mButton.setVisibility(View.GONE);
                        mButton = (Button) LayoutInflater.from(getContext()).inflate(R.layout.rc_wi_text_btn, mInputView.getToggleLayout(), false);
                        mInputView.getToggleLayout().addView(mButton);
                        mButton.setOnClickListener(this);
                    }
                    mButton.setVisibility(View.VISIBLE);
                } else {
                    mButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {
        //isShowMessageTyping 是否显示输入状态
        if (s.toString().length() > 0) {
            boolean isShowMessageTyping = RongIMClient.getInstance().getTypingStatus();
            if (isShowMessageTyping == true) {
                MessageTag tag = TextMessage.class.getAnnotation(MessageTag.class);
                onTypingMessage(tag.value());
            }
        }

        if (AndroidEmoji.isEmoji(s.toString())) {
            int start = mEdit.getSelectionStart();
            int end = mEdit.getSelectionEnd();
            mEdit.removeTextChangedListener(this);
            mEdit.setText(AndroidEmoji.ensure(s.toString()));
            mEdit.addTextChangedListener(this);
            mEdit.setSelection(start, end);
        }
        if(mExtraTextWatcher != null)
            mExtraTextWatcher.afterTextChanged(s);

        RLog.d(TAG, "afterTextChanged " + s.toString());
    }

    /**
     * 设置输入框
     *
     * @param content
     */
    public void setEditTextContent(CharSequence content) {

        if (mEdit != null && content != null) {
            mEdit.setText(content);
            mEdit.setSelection(content.length());
        }
    }

    public void setEditTextChangedListener(TextWatcher listener) {
        mExtraTextWatcher = listener;
    }
}