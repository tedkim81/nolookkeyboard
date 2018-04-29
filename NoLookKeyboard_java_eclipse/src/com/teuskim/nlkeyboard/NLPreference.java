package com.teuskim.nlkeyboard;

import android.content.Context;
import android.content.SharedPreferences;

public class NLPreference {
	
	private static final String KEY_DID_READ_GUIDE = "didreadguide";
	private static final String KEY_KEYBOARD_POS_LEFT = "keyboardposleft";
	private static final String KEY_KEYBOARD_POS_RIGHT = "keyboardposright";
	private static final String KEY_KEYBOARD_POS_ALL = "keyboardposall";
	private static final String KEY_VIBRATE = "vibrate";
	private static final String KEY_DUPCHOSUNG = "dupchosung";
	private static final String KEY_SHIFT_REMAIN = "shiftremain";
	
	private SharedPreferences mPref;
	private SharedPreferences.Editor mEditor;
	
	public NLPreference(Context context){
		mPref = context.getSharedPreferences("nlpref", 0);
		mEditor = mPref.edit();
	}
	
	public boolean didReadGuide(){
		return mPref.getBoolean(KEY_DID_READ_GUIDE, false);
	}
	
	public void setDidReadGuide(boolean didReadGuide){
		mEditor.putBoolean(KEY_DID_READ_GUIDE, didReadGuide);
		mEditor.commit();
	}
	
	public int getKeyboardPosLeft(){
		return mPref.getInt(KEY_KEYBOARD_POS_LEFT, 0);
	}
	
	public void setKeyboardPosLeft(int keyboardPos){
		mEditor.putInt(KEY_KEYBOARD_POS_LEFT, keyboardPos);
		mEditor.commit();
	}
	
	public int getKeyboardPosRight(){
		return mPref.getInt(KEY_KEYBOARD_POS_RIGHT, 0);
	}
	
	public void setKeyboardPosRight(int keyboardPos){
		mEditor.putInt(KEY_KEYBOARD_POS_RIGHT, keyboardPos);
		mEditor.commit();
	}
	
	public int getKeyboardPosAll(){
		return mPref.getInt(KEY_KEYBOARD_POS_ALL, 0);
	}
	
	public void setKeyboardPosAll(int keyboardPos){
		mEditor.putInt(KEY_KEYBOARD_POS_ALL, keyboardPos);
		mEditor.commit();
	}
	
	public boolean isVibrate(){
		return mPref.getBoolean(KEY_VIBRATE, true);
	}
	
	public void setIsVibrate(boolean isVibrate){
		mEditor.putBoolean(KEY_VIBRATE, isVibrate);
		mEditor.commit();
	}
	
	public boolean useDupChosung(){
		return mPref.getBoolean(KEY_DUPCHOSUNG, true);
	}
	
	public void setUseDupChosung(boolean useDupChosung){
		mEditor.putBoolean(KEY_DUPCHOSUNG, useDupChosung);
		mEditor.commit();
	}
	
	public boolean isShiftRemain(){
		return mPref.getBoolean(KEY_SHIFT_REMAIN, true);
	}
	
	public void setIsShiftRemain(boolean isShiftRemain){
		mEditor.putBoolean(KEY_SHIFT_REMAIN, isShiftRemain);
		mEditor.commit();
	}
	
}
