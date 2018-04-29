package com.teuskim.nlkeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RegisterCustomKeySetView extends LinearLayout {
	
	private TextView mKeyView1;
	private TextView mKeyView2;
	private TextView mKeyView3;
	private TextView mKeyView4;

	public RegisterCustomKeySetView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RegisterCustomKeySetView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context){
		LayoutInflater.from(context).inflate(R.layout.register_custom_keyset, this);
		
		mKeyView1 = (TextView) findViewById(R.id.key_1);
		mKeyView2 = (TextView) findViewById(R.id.key_2);
		mKeyView3 = (TextView) findViewById(R.id.key_3);
		mKeyView4 = (TextView) findViewById(R.id.key_4);
	}
	
	public void fillKeys(String key1, String key2, String key3, String key4){
		fillKey(mKeyView1, key1);
		fillKey(mKeyView2, key2);
		fillKey(mKeyView3, key3);
		fillKey(mKeyView4, key4);
	}
	
	private void fillKey(TextView keyView, String key){
		keyView.setText("");
		keyView.setBackgroundDrawable(null);
		if("\b".equals(key))
			keyView.setBackgroundResource(R.drawable.key_backspace);
		else if(" ".equals(key))
			keyView.setBackgroundResource(R.drawable.key_space);
		else if("\n".equals(key))
			keyView.setBackgroundResource(R.drawable.key_enter);
		else if("\t".equals(key))
			keyView.setBackgroundResource(R.drawable.key_enter);
		else
			keyView.setText(key);
	}
	
}
