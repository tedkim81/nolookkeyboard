package com.teuskim.nlkeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

public class RegisterCustomKeyboardView extends RelativeLayout {
	
	private RegisterCustomKeySetView mLeftTopView;
	private RegisterCustomKeySetView mMidTopView;
	private RegisterCustomKeySetView mRightTopView;
	private RegisterCustomKeySetView mLeftMidView;
	private RegisterCustomKeySetView mRightMidView;
	private RegisterCustomKeySetView mLeftBotView;
	private RegisterCustomKeySetView mMidBotView;
	private RegisterCustomKeySetView mRightBotView;
	
	public RegisterCustomKeyboardView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public RegisterCustomKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RegisterCustomKeyboardView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context){
		// xml layout 으로 뷰를 생성하고, 멤버 변수들을 초기화 한다.
		LayoutInflater.from(context).inflate(R.layout.register_custom_keyboard, this);
		
		mLeftTopView = (RegisterCustomKeySetView) findViewById(R.id.keyset_left_top);
		mMidTopView = (RegisterCustomKeySetView) findViewById(R.id.keyset_mid_top);
		mRightTopView = (RegisterCustomKeySetView) findViewById(R.id.keyset_right_top);
		mLeftMidView = (RegisterCustomKeySetView) findViewById(R.id.keyset_left_mid);
		mRightMidView = (RegisterCustomKeySetView) findViewById(R.id.keyset_right_mid);
		mLeftBotView = (RegisterCustomKeySetView) findViewById(R.id.keyset_left_bot);
		mMidBotView = (RegisterCustomKeySetView) findViewById(R.id.keyset_mid_bot);
		mRightBotView = (RegisterCustomKeySetView) findViewById(R.id.keyset_right_bot);
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		mLeftTopView.setOnClickListener(l);
		mMidTopView.setOnClickListener(l);
		mRightTopView.setOnClickListener(l);
		mLeftMidView.setOnClickListener(l);
		mRightMidView.setOnClickListener(l);
		mLeftBotView.setOnClickListener(l);
		mMidBotView.setOnClickListener(l);
		mRightBotView.setOnClickListener(l);
	}
	
	public void fillKeys(int viewId, String key1, String key2, String key3, String key4){
		RegisterCustomKeySetView view = getViewById(viewId);
		view.fillKeys(key1, key2, key3, key4);
	}
	
	private RegisterCustomKeySetView getViewById(int viewId){
		switch(viewId){
		case R.id.keyset_left_top:
			return mLeftTopView;
		case R.id.keyset_mid_top:
			return mMidTopView;
		case R.id.keyset_right_top:
			return mRightTopView;
		case R.id.keyset_left_mid:
			return mLeftMidView;
		case R.id.keyset_right_mid:
			return mRightMidView;
		case R.id.keyset_left_bot:
			return mLeftBotView;
		case R.id.keyset_mid_bot:
			return mMidBotView;
		default:
			return mRightBotView;
		}
	}
	
	/*
	private int getStartNumberById(int viewId){
		switch(viewId){
		case R.id.keyset_left_top:
			return 1;
		case R.id.keyset_mid_top:
			return 5;
		case R.id.keyset_right_top:
			return 9;
		case R.id.keyset_left_mid:
			return 13;
		case R.id.keyset_right_mid:
			return 17;
		case R.id.keyset_left_bot:
			return 21;
		case R.id.keyset_mid_bot:
			return 25;
		default:
			return 29;
		}
	}
	*/

}
