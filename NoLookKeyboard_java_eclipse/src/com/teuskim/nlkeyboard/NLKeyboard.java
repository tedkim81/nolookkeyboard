package com.teuskim.nlkeyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

/**
 * 키보드 모델 클래스
 * 1. xml 로부터 키 데이터를 만들어 저장하고,
 * 2. 키 데이터를 작은원세트의 리스트로 반환하는 메소드를 만든다.
 * 
 * @author kim5724
 *
 */
public class NLKeyboard {
	
	// Keyboard XML Tags
	private static final String TAG_KEYBOARD = "Keyboard";
	private static final String TAG_KEYSET = "KeySet";
	private static final String TAG_KEY = "Key";
	
	private Context mContext;
	private int mXmlLayoutResId = 0;
	private List<KeySet> mKeySetList;
	private boolean mIsShift = false;
	private List<Map<Integer, String>> mKeyStringList;
	private boolean mIsCustom = false;

	public NLKeyboard(Context context, int xmlLayoutResId){
		// 필요한 멤버 변수 초기화하고, xml 로딩한다.
		mContext = context;
		mXmlLayoutResId = xmlLayoutResId;		
		loadKeyboard();
	}
	
	public NLKeyboard(Context context, List<Map<Integer, String>> keyStringList){
		mContext = context;
		mKeyStringList = keyStringList;
		loadKeyboard();
		setIsCustom(true);
	}
	
	public void loadKeyboard(){
		if(mXmlLayoutResId > 0){
			loadKeyboardByXml();
		}
		else{
			loadKeyboardByList();
		}
	}
	
	private void loadKeyboardByXml() {
		// xml 파싱하여 키 데이터를 만들어서 저장한다.
		KeySet keySet = null;
		boolean inKeySet = false;
		boolean inKey = false;
		mKeySetList = new ArrayList<KeySet>();
		XmlResourceParser parser = mContext.getResources().getXml(mXmlLayoutResId);
		
		try{
			int event;
			while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT){
				if (event == XmlResourceParser.START_TAG) {
					String tag = parser.getName();
					if(TAG_KEYBOARD.equals(tag)){
						
					}
					else if(TAG_KEYSET.equals(tag)){
						inKeySet = true;
						keySet = new KeySet();
					}
					else if(TAG_KEY.equals(tag)){
						inKey = true;
						
						AttributeSet as = Xml.asAttributeSet(parser);
						int key = as.getAttributeIntValue(0, 0);
						String keyLabel = as.getAttributeValue(1);
						if(mIsShift){
							key = Character.toUpperCase(key);
							keyLabel = ""+Character.toUpperCase(keyLabel.charAt(0));  // TODO:어거지스럽다..
						}						
						int iconResId = as.getAttributeResourceValue(2, 0);
						keySet.setKey(key, keyLabel, iconResId);
					}
				}
				else if(event == XmlResourceParser.END_TAG){
					if(inKeySet){
						inKeySet = false;
						mKeySetList.add(keySet);
					}
					else if(inKey){
						inKey = false;
					}
				}
			}
		}catch(Exception e){
//			Log.e("NLKeyboard", "keyboard parse error", e);
		}		
	}
	
	private void loadKeyboardByList(){
		mKeySetList = new ArrayList<KeySet>();
		for(Map<Integer, String> keysetMap : mKeyStringList){
			KeySet keyset = new KeySet();
			TreeSet<Integer> set = new TreeSet<Integer>(keysetMap.keySet());
			Iterator<Integer> it = set.iterator();
			while(it.hasNext()){
				int key = it.next();
				String data = keysetMap.get(key);
				int iconResId = 0;
				if("\b".equals(data))
					iconResId = R.drawable.key_backspace;
				else if(" ".equals(data))
					iconResId = R.drawable.key_space;
				else if("\n".equals(data))
					iconResId = R.drawable.key_enter;
				keyset.setKey(key, data.substring(0, 1), iconResId);
			}
			mKeySetList.add(keyset);
		}
	}
	
	public List<KeySet> getKeySets(){
		// 키 데이터를 리턴한다.
		return mKeySetList;
	}
	
	public void setIsShift(boolean isShift){
		mIsShift = isShift;
	}
	
	public boolean isShift(){
		return mIsShift;
	}
	
	public void setIsCustom(boolean isCustom){
		mIsCustom = isCustom;
	}
	
	public boolean isCustom(){
		return mIsCustom;
	}
	
	public List<Map<Integer, String>> getKeyStringList(){
		return mKeyStringList;
	}
	
	/**
	 * 작은원세트 객체
	 * 1. 3~4개 정도의 글자 집합이다.
	 */
	public static class KeySet {
		
		private List<Integer> mKeyList;
		private Map<Integer, String> mKeyLabelMap;
		private Map<Integer, Integer> mKeyIconMap;
		
		public KeySet(){
			mKeyList = new ArrayList<Integer>();
			mKeyLabelMap = new HashMap<Integer, String>();
			mKeyIconMap = new HashMap<Integer, Integer>();
		}
		
		public void setKey(int key, String keyLabel, int iconResId){
			mKeyList.add(key);
			mKeyLabelMap.put(key, keyLabel);
			if(iconResId > 0){
				mKeyIconMap.put(key, iconResId);
			}
		}
		
		public int getKey(int location){
			return mKeyList.get(location);
		}
		
		public int getKeyIcon(int key){
			if(mKeyIconMap.containsKey(key))
				return mKeyIconMap.get(key);
			return 0;
		}
		
		public List<Integer> getKeyList(){
			return mKeyList;
		}
		
		public Map<Integer, String> getKeyLabelMap(){
			return mKeyLabelMap;
		}
		
		public int size(){
			return mKeyList.size();
		}
	}

}
