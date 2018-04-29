package com.teuskim.nlkeyboard;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private TextView mTxtAboutButton;
	private Button mBtnChangeSettings;
	private EditText mInputTest;
	private InputMethodManager mImm;
	private int mCurrState;
	
	private static final int CURR_STATE_UNCHECKED = 1;
	private static final int CURR_STATE_UNSELECTED = 2;
	private static final int CURR_STATE_USE = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		findViews();
		
		mImm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
	}
	
	protected void findViews(){
		mTxtAboutButton = (TextView) findViewById(R.id.txt_about_button);
		mBtnChangeSettings = (Button) findViewById(R.id.btn_change_settings);
		mInputTest = (EditText) findViewById(R.id.input_test);
		
		mBtnChangeSettings.setOnClickListener(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		makeSettingButton();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if(hasFocus){
			makeSettingButton();
		}
	}
	
	private void makeSettingButton(){
		String packageName = "com.teuskim.nlkeyboard";
		List<InputMethodInfo> infos = mImm.getEnabledInputMethodList();
		boolean enabled = false;
		for(InputMethodInfo info : infos){
			if(packageName.equals(info.getPackageName())){
				enabled = true;
				break;
			}
		}
		if(enabled){
			String strIM = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
			if(strIM != null && strIM.contains(packageName)){
				mTxtAboutButton.setText("");
				mTxtAboutButton.setVisibility(View.GONE);
				mBtnChangeSettings.setText(R.string.title_go_settings);
				mCurrState = CURR_STATE_USE;
				mInputTest.setVisibility(View.VISIBLE);
			}
			else{
				mTxtAboutButton.setText(R.string.txt_change_keyboard);
				mTxtAboutButton.setVisibility(View.VISIBLE);
				mBtnChangeSettings.setText(R.string.title_change_keyboard);
				mCurrState = CURR_STATE_UNSELECTED;
				mInputTest.setVisibility(View.GONE);
			}
		}
		else{
			mTxtAboutButton.setText(R.string.txt_go_android_settings);
			mTxtAboutButton.setVisibility(View.VISIBLE);
			mBtnChangeSettings.setText(R.string.title_go_android_settings);
			mCurrState = CURR_STATE_UNCHECKED;
			mInputTest.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_change_settings:
			actionButton();
			break;
		}
	}
	
	private void actionButton(){
		Intent i;
		switch(mCurrState){
		case CURR_STATE_UNCHECKED:
			i = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
			startActivity(i);
			break;
		case CURR_STATE_UNSELECTED:
			if (mImm != null) {
				mImm.showInputMethodPicker();
            }
			break;
		case CURR_STATE_USE:
			i = new Intent(getApplicationContext(), SettingActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;
		}
	}
}
