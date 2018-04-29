package com.teuskim.nlkeyboard;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teuskim.nlkeyboard.NLKeyboard.KeySet;

public class KeySetView extends LinearLayout {
	
	private TextView mKeyView1;
	private TextView mKeyView2;
	private TextView mKeyView3;
	private TextView mKeyView4;
	private ImageView mKeyViewIcon1;
	private ImageView mKeyViewIcon2;
	private ImageView mKeyViewIcon3;
	private ImageView mKeyViewIcon4;
	
	private int mSize = 0;

	public KeySetView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public KeySetView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context){
		LayoutInflater.from(context).inflate(R.layout.keyset, this);
		
		mKeyView1 = (TextView) findViewById(R.id.key_1);
		mKeyView2 = (TextView) findViewById(R.id.key_2);
		mKeyView3 = (TextView) findViewById(R.id.key_3);
		mKeyView4 = (TextView) findViewById(R.id.key_4);
		mKeyViewIcon1 = (ImageView) findViewById(R.id.key_1_icon);
		mKeyViewIcon2 = (ImageView) findViewById(R.id.key_2_icon);
		mKeyViewIcon3 = (ImageView) findViewById(R.id.key_3_icon);
		mKeyViewIcon4 = (ImageView) findViewById(R.id.key_4_icon);
	}
	
	public void setKeySet(KeySet keySet){
		List<Integer> keyList = keySet.getKeyList();
		Map<Integer, String> keyLabelMap = keySet.getKeyLabelMap();
		mSize = keyLabelMap.size();
		setOneKey(keySet, keyList, keyLabelMap, 0, mKeyView1, mKeyViewIcon1);
		setOneKey(keySet, keyList, keyLabelMap, 1, mKeyView2, mKeyViewIcon2);
		setOneKey(keySet, keyList, keyLabelMap, 2, mKeyView3, mKeyViewIcon3);
		setOneKey(keySet, keyList, keyLabelMap, 3, mKeyView4, mKeyViewIcon4);
	}
	
	public void setSelected(int position){
		resetSelected();
		
		setBackgroundResource(R.drawable.bg_keyset_selected);
		
		switch(position){
		case 0:
			mKeyView1.setBackgroundResource(R.drawable.bg_selected_key);
			mKeyViewIcon1.setBackgroundResource(R.drawable.bg_selected_key);
			break;
		case 1:
			mKeyView2.setBackgroundResource(R.drawable.bg_selected_key);
			mKeyViewIcon2.setBackgroundResource(R.drawable.bg_selected_key);
			break;
		case 2:
			mKeyView3.setBackgroundResource(R.drawable.bg_selected_key);
			mKeyViewIcon3.setBackgroundResource(R.drawable.bg_selected_key);
			break;
		case 3:
			mKeyView4.setBackgroundResource(R.drawable.bg_selected_key);
			mKeyViewIcon4.setBackgroundResource(R.drawable.bg_selected_key);
			break;
		}
	}
	
	public void resetSelected(){
		setBackgroundResource(R.drawable.bg_keyset);
		
		mKeyView1.setBackgroundColor(Color.TRANSPARENT);
		mKeyViewIcon1.setBackgroundColor(Color.TRANSPARENT);
		mKeyView2.setBackgroundColor(Color.TRANSPARENT);
		mKeyViewIcon2.setBackgroundColor(Color.TRANSPARENT);
		mKeyView3.setBackgroundColor(Color.TRANSPARENT);
		mKeyViewIcon3.setBackgroundColor(Color.TRANSPARENT);
		mKeyView4.setBackgroundColor(Color.TRANSPARENT);
		mKeyViewIcon4.setBackgroundColor(Color.TRANSPARENT);
	}
	
	private void setOneKey(KeySet keySet, List<Integer> keyList, Map<Integer, String> keyLabelMap
			, int position, TextView keyView, ImageView keyViewIcon){
		if(mSize > position){
			int key = keyList.get(position);
			String keyLabel = keyLabelMap.get(key);
			int icon = keySet.getKeyIcon(key);
			if(icon > 0){
				keyViewIcon.setImageResource(icon);
				keyView.setVisibility(View.GONE);
				keyViewIcon.setVisibility(View.VISIBLE);
			}
			else{
				keyView.setText(keyLabel);
				keyView.setVisibility(View.VISIBLE);
				keyViewIcon.setVisibility(View.GONE);
			}			
		}
		else{
			keyView.setVisibility(View.GONE);
			keyViewIcon.setVisibility(View.GONE);
		}
	}

}
