package com.teuskim.nlkeyboard;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.view.KeyEvent;

/**
 * 한글입력 클래스
 */
public class HangulHandler {

	private static final int JASO_START_INDEX = 0x3131;
	public static final int HANGUL_START_INDEX = 0xAC00;
	private static final int JAUM_SIZE = 19;
	private static final int JAUM_FULL_SIZE = 30;  // 곁자음 포함한 수
	
	public static final int CUSTOM_SPACE = -2;
	public static final int CUSTOM_ENTER = -3;
	public static final int CUSTOM_BACKSPACE = NLKeyboardService.KEY_DELETE;
	
	private State mCurrState;
	private final State0 mState0 = new State0();
	private final State1 mState1 = new State1();
	private final State2 mState2 = new State2();
	private final State3 mState3 = new State3();
	private final State4 mState4 = new State4();
	
	private int mChosung;
	private int mJungsung;
	private int mJongsung;
	
	private int mCurrKey;
	private NLKeyboardService mService;
	private StringBuilder mComposing = new StringBuilder();
	private StringBuilder mHangulString = new StringBuilder();  // 입력된 전체 한글 문자열
	
	private boolean mUseDupChosung = true;  // 중복입력으로 곁자음 완성기능 사용여부
	private Map<String, Integer> mKeyIntMap;
	private boolean mIsInit = true;
	
	public HangulHandler(NLKeyboardService service){
		mCurrState = mState0;
		mService = service;
		
		// xml파일 파싱하는 것과 중복되는 작업. 일단 빨리하기 위해서 아래와 같이 한다.
		mKeyIntMap = new HashMap<String, Integer>();
		mKeyIntMap.put("ㄱ", 0);
		mKeyIntMap.put("ㄲ", 1);
		mKeyIntMap.put("ㄴ", 2);
		mKeyIntMap.put("ㄷ", 3);
		mKeyIntMap.put("ㄸ", 4);
		mKeyIntMap.put("ㄹ", 5);
		mKeyIntMap.put("ㅁ", 6);
		mKeyIntMap.put("ㅂ", 7);
		mKeyIntMap.put("ㅃ", 8);
		mKeyIntMap.put("ㅅ", 9);
		mKeyIntMap.put("ㅆ", 10);
		mKeyIntMap.put("ㅇ", 11);
		mKeyIntMap.put("ㅈ", 12);
		mKeyIntMap.put("ㅉ", 13);
		mKeyIntMap.put("ㅊ", 14);
		mKeyIntMap.put("ㅋ", 15);
		mKeyIntMap.put("ㅌ", 16);
		mKeyIntMap.put("ㅍ", 17);
		mKeyIntMap.put("ㅎ", 18);
		mKeyIntMap.put("ㅏ", 19);
		mKeyIntMap.put("ㅐ", 20);
		mKeyIntMap.put("ㅑ", 21);
		mKeyIntMap.put("ㅒ", 22);
		mKeyIntMap.put("ㅓ", 23);
		mKeyIntMap.put("ㅔ", 24);
		mKeyIntMap.put("ㅕ", 25);
		mKeyIntMap.put("ㅖ", 26);
		mKeyIntMap.put("ㅗ", 27);
		mKeyIntMap.put("ㅛ", 28);
		mKeyIntMap.put("ㅜ", 29);
		mKeyIntMap.put("ㅠ", 30);
		mKeyIntMap.put("ㅡ", 31);
		mKeyIntMap.put("ㅣ", 32);
		
		mKeyIntMap.put("ㅘ", 33);
		mKeyIntMap.put("ㅙ", 34);
		mKeyIntMap.put("ㅚ", 35);
		mKeyIntMap.put("ㅝ", 36);
		mKeyIntMap.put("ㅞ", 37);
		mKeyIntMap.put("ㅟ", 38);
		mKeyIntMap.put("ㅢ", 39);
	}
	
	private void init(){
		mCurrState = mState0;
		mChosung = 0;
		mJungsung = 0;
		mJongsung = 0;
		mIsInit = true;
	}
	
	public void initHangulData(){
		mHangulString.setLength(0);
		mComposing.setLength(0);
		init();
	}
	
	public String getHangulString(){
		return mHangulString.toString();
	}
	
	public void onFinishInputView(){
		if(mComposing.length() > 0)
			mHangulString.append(mComposing.toString());
	}
	
	public void setUseDupChosung(boolean useDupChosung){
		mUseDupChosung = useDupChosung;
	}
	
	public int getComposingLength(){
		return mComposing.length();
	}
	
	public StringBuilder getComposing(){
		return mComposing;
	}
	
	/**
	 * 자음/모음 구분하여 현재 상태의 자음출력/모음출력 메소드 호출
	 */
	public void handle(int key){
		mIsInit = false;
		mCurrKey = key;
		
		switch(mCurrKey){
		case CUSTOM_SPACE:
			handleSpecialString(" ");
			break;
			
		case CUSTOM_ENTER:
			commit();
			mService.handleEnter();
			mHangulString.append("\n");
			break;
			
		case CUSTOM_BACKSPACE:
			mCurrState.back();
			break;
			
		default:
			if(isJaum()){
				mCurrState.jaum();
			}
			else{
				mCurrState.moum();
			}
		}
	}
	
	public void handleSpace(){
		if(mIsInit == false){
			init();
		}
		else{
			handleSpecialString(" ");
		}
	}
	
	public void handleEnter(){
		commit();
		mHangulString.append("\n");
	}
	
	public void handleBackspace(){
		boolean isDeleted = mCurrState.back();
		
		int length = mHangulString.length();
		if(length > 0 && isDeleted){
			mHangulString.delete(length-1, length);
		}
		
	}
	
	public void commit(){
		if(mComposing.length() > 0)
			commitText(mComposing.toString());
		mComposing.setLength(0);
		init();
	}
	
	private void handleSpecialString(String str){
		mComposing.append(str);		
		commit();
	}
	
	public int getKeyIntFromJaso(String str){
		if(mKeyIntMap.containsKey(str)){
			return mKeyIntMap.get(str);
		}
		return -1;
	}
	
	public int getKeyIntFromKeyCode(int keyCode){
		return getKeyIntFromJaso((char)keyCode+"");
	}
	
	public boolean isInit(){
		return mIsInit;
	}
	
	/**
	 * 현재 입력받은 키가 자음인지 여부
	 */
	private boolean isJaum(){
		if(mCurrKey < JAUM_SIZE)
			return true;
		return false;
	}
	
	private void setComposingText(String text){
		mService.setComposingText(text, 1);
	}
	
	private void commitText(String text){
		mService.commitHangul(text, 1);
		if(text != null)
			mHangulString.append(text);
	}
	
	public String separateJaso(String word){
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<word.length(); i++){
			char ch = word.charAt(i);
			if((int)ch >= HANGUL_START_INDEX){  // 조합형 한글이면 분리하여 반환한다.
				int index = (int)ch - HANGUL_START_INDEX;
				int chosungIndex = index / (21*28);
				int jungsungIndex = (index % (21*28)) / 28;
				int jongsungIndex = index % 28;
				
				sb.append((char)(JASO_START_INDEX+State.sJasoArr[chosungIndex]));
				sb.append((char)(JASO_START_INDEX+JAUM_FULL_SIZE+jungsungIndex));
				if(jongsungIndex > 0)
					sb.append((char)(JASO_START_INDEX+State.sJasoArr[jongToCho(jongsungIndex)]));
			}
			else
				sb.append(ch);
		}
		return sb.toString();
	}
	
	
	/**
	 * 각각의 상태들은 자음 및 모음을 입력받았을때의 행동을 구현해야 한다.
	 */
	private static abstract class State {
		// 곁자음/곁모음 포함한 총 51개의 자소 중 키보드에 들어간 33개의 자소에 대한 인덱스
		public static int[] sJasoArr = {0,1,3,6,7,8,16,17,18,20,21,22,23,24,25,26,27,28,29
									//  ㄱㄲ ㄴ ㄷㄸ ㄹ  ㅁ   ㅂ   ㅃ   ㅅ  ㅆ   ㅇ  ㅈ   ㅉ   ㅊ   ㅋ  ㅌ   ㅍ   ㅎ
										,30,31,32,33,34,35,36,37,38,42,43,47,48,50
									//   ㅏ   ㅐ   ㅑ   ㅒ   ㅓ  ㅔ   ㅕ   ㅖ   ㅗ  ㅛ   ㅜ   ㅠ   ㅡ  ㅣ
										,39,40,41,44,45,46,49};
									//   ㅘ   ㅙ   ㅚ   ㅝ  ㅞ   ㅟ   ㅢ
		
		// 초성에 들어갈 수 있는 자음들의 인덱스. 초성가능자음은 모두 키보드에 표현되므로 인덱스값이 그대로 초성이 된다.
		//public static int[] sChosungArr = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
										  // ㄱㄲ ㄴ ㄷ ㄸ ㄹㅁ ㅂㅃ ㅅ  ㅆ   ㅇ   ㅈ   ㅉ  ㅊ   ㅋ   ㅌ   ㅍ  ㅎ 
		
		// 중성에 들어갈 수 있는 모음들의 인덱스
		public static int[] sJungsungArr = {0,1,2,3,4,5,6,7,8,12,13,17,18,20,   9,10,11,14,15,16,19};
										//  ㅏㅐ ㅑ ㅒㅓ ㅔ ㅕ ㅖ ㅗ ㅛ   ㅜ   ㅠ   ㅡ  ㅣ        ㅘ  ㅙ  ㅚ   ㅝ   ㅞ   ㅟ   ㅢ
		
		// 종성에 들어갈 수 있는 자음들의 인덱스. ㄱ이 1이다. -1인 경우 종성에 들어갈 수 없으니 다음 음절 초성으로 분리한다.
		public static int[] sJongsungArr = {1,2,4,7,-1,8,16,17,-1,19,20,21,22,-1,23,24,25,26,27};
										//  ㄱㄲ ㄴ ㄷ  ㄸ ㄹ  ㅁ   ㅂ  ㅃ   ㅅ   ㅆ   ㅇ   ㅈ   ㅉ  ㅊ   ㅋ   ㅌ   ㅍ  ㅎ
		
		// 곁자소 매핑
		public static Map<Integer, Integer> sDupJasoMap;

		// 초성 변경 매핑
		public static Map<Integer, Integer> sChangeChosungMap;
		
		// 초성 곁자음 매핑
		public static Map<Integer, Integer> sDupChosungMap;
		
		// 중성에 곁모음 매핑
		public static Map<Integer, Integer> sDupJungsungMap;
		
		// 종성에 곁자음 매핑
		public static Map<Integer, Integer> sDupJongsungMap;
		
		// 앞글자 종성과 뒷글자 초성이 곁자음이 되는 경우 매핑
		public static Map<Integer, Integer> sPostDupChosungMap;
		
		// 곁종성과 곁초성 매핑
		public static Map<Integer, Integer> sDupJongToDupChoMap;
		
		
		static {
			// 초성 변경 맵핑
			sChangeChosungMap = new HashMap<Integer, Integer>();
			sChangeChosungMap.put(10000, 1);  // ㄱ -> ㄲ
			sChangeChosungMap.put(10100, 15);  // ㄲ -> ㅋ
			sChangeChosungMap.put(10303, 4);  // ㄷ -> ㄸ
			sChangeChosungMap.put(10403, 16);  // ㄸ -> ㅌ
			sChangeChosungMap.put(10707, 8);  // ㅂ -> ㅃ
			sChangeChosungMap.put(10807, 17);  // ㅃ -> ㅍ
			sChangeChosungMap.put(10909, 10);  // ㅅ -> ㅆ
			sChangeChosungMap.put(11212, 13);  // ㅈ -> ㅉ
			sChangeChosungMap.put(11312, 14);  // ㅉ -> ㅊ
			
			// 초성 변경 맵핑
			sDupChosungMap = new HashMap<Integer, Integer>();
			sDupChosungMap.put(0, 1);  // ㄱ -> ㄲ
			sDupChosungMap.put(3, 4);  // ㄷ -> ㄸ
			sDupChosungMap.put(7, 8);  // ㅂ -> ㅃ
			sDupChosungMap.put(9, 10);  // ㅅ -> ㅆ
			sDupChosungMap.put(12, 13);  // ㅈ -> ㅉ
			
			// key는 10000 + (첫자소 인덱스)*100 + (두번째자소 인덱스) 이고, value는 곁자소 인덱스
			sDupJasoMap = new HashMap<Integer, Integer>();
			sDupJasoMap.put(13050, 31);  // ㅏ + ㅣ = ㅐ
			sDupJasoMap.put(13030, 32);  // ㅏ + ㅏ = ㅑ
			sDupJasoMap.put(13250, 33);  // ㅑ + ㅣ = ㅒ
			sDupJasoMap.put(13131, 33);  // ㅐ + ㅐ = ㅒ
			sDupJasoMap.put(13450, 35);  // ㅓ + ㅣ = ㅔ
			sDupJasoMap.put(13434, 36);  // ㅓ + ㅓ = ㅕ
			sDupJasoMap.put(13650, 37);  // ㅕ + ㅣ = ㅖ
			sDupJasoMap.put(13535, 37);  // ㅔ + ㅔ = ㅖ
			sDupJasoMap.put(13830, 39);  // ㅗ + ㅏ = ㅘ
			sDupJasoMap.put(13838, 42);  // ㅗ + ㅗ = ㅛ
			sDupJasoMap.put(13831, 40);  // ㅗ + ㅐ = ㅙ
			sDupJasoMap.put(13950, 40);  // ㅘ + ㅣ = ㅙ
			sDupJasoMap.put(13850, 41);  // ㅗ + ㅣ = ㅚ
			sDupJasoMap.put(14334, 44);  // ㅜ + ㅓ = ㅝ
			sDupJasoMap.put(14343, 47);  // ㅜ + ㅜ = ㅠ
			sDupJasoMap.put(14335, 45);  // ㅜ + ㅔ = ㅞ
			sDupJasoMap.put(14450, 45);  // ㅝ + ㅣ = ㅞ
			sDupJasoMap.put(14350, 46);  // ㅜ + ㅣ = ㅟ
			sDupJasoMap.put(14850, 49);  // ㅡ + ㅣ = ㅢ
			
			sDupJungsungMap = new HashMap<Integer, Integer>();
			sDupJungsungMap.put(10020, 1);  // ㅏ + ㅣ = ㅐ
			sDupJungsungMap.put(10000, 2);  // ㅏ + ㅏ = ㅑ
			sDupJungsungMap.put(10220, 3);  // ㅑ + ㅣ = ㅒ
			sDupJungsungMap.put(10101, 3);  // ㅐ + ㅐ = ㅒ
			sDupJungsungMap.put(10420, 5);  // ㅓ + ㅣ = ㅔ
			sDupJungsungMap.put(10404, 6);  // ㅓ + ㅓ = ㅕ
			sDupJungsungMap.put(10620, 7);  // ㅕ + ㅣ = ㅖ
			sDupJungsungMap.put(10505, 7);  // ㅔ + ㅔ = ㅖ
			sDupJungsungMap.put(10800, 9);  // ㅗ + ㅏ = ㅘ
			sDupJungsungMap.put(10808, 12);  // ㅗ + ㅗ = ㅛ
			sDupJungsungMap.put(10801, 10);  // ㅗ + ㅐ = ㅙ
			sDupJungsungMap.put(10920, 10);  // ㅘ + ㅣ = ㅙ
			sDupJungsungMap.put(10820, 11);  // ㅗ + ㅣ = ㅚ
			sDupJungsungMap.put(11304, 14);  // ㅜ + ㅓ = ㅝ
			sDupJungsungMap.put(11313, 17);  // ㅜ + ㅜ = ㅠ
			sDupJungsungMap.put(11305, 15);  // ㅜ + ㅔ = ㅞ
			sDupJungsungMap.put(11420, 15);  // ㅝ + ㅣ = ㅞ
			sDupJungsungMap.put(11320, 16);  // ㅜ + ㅣ = ㅟ
			sDupJungsungMap.put(11820, 19);  // ㅡ + ㅣ = ㅢ
			
			sDupJongsungMap = new HashMap<Integer, Integer>();
			sDupJongsungMap.put(10101, 2);  // ㄱ + ㄱ = ㄲ
			sDupJongsungMap.put(10119, 3);  // ㄱ + ㅅ = ㄳ
			sDupJongsungMap.put(10422, 5);  // ㄴ + ㅈ = ㄵ
			sDupJongsungMap.put(10427, 6);  // ㄴ + ㅎ = ㄶ
			sDupJongsungMap.put(10801, 9);  // ㄹ + ㄱ = ㄺ
			sDupJongsungMap.put(10816, 10);  // ㄹ + ㅁ = ㄻ
			sDupJongsungMap.put(10817, 11);  // ㄹ + ㅂ = ㄼ
			sDupJongsungMap.put(10819, 12);  // ㄹ + ㅅ = ㄽ
			sDupJongsungMap.put(10825, 13);  // ㄹ + ㅌ = ㄾ
			sDupJongsungMap.put(10826, 14);  // ㄹ + ㅍ = ㄿ
			sDupJongsungMap.put(10827, 15);  // ㄹ + ㅎ = ㅀ
			sDupJongsungMap.put(11719, 18);  // ㅂ + ㅅ = ㅄ
			sDupJongsungMap.put(11919, 20);  // ㅅ + ㅅ = ㅆ
			
			sPostDupChosungMap = new HashMap<Integer, Integer>();
			sPostDupChosungMap.put(10100, 1);  // ㄱ + ㄱ = ㄲ
			sPostDupChosungMap.put(10703, 4);  // ㄷ + ㄷ = ㄸ
			sPostDupChosungMap.put(11707, 8);  // ㅂ + ㅂ = ㅃ
			sPostDupChosungMap.put(11909, 10);  // ㅅ + ㅅ = ㅆ
			sPostDupChosungMap.put(12212, 13);  // ㅈ + ㅈ = ㅉ
			
			sDupJongToDupChoMap = new HashMap<Integer, Integer>();
			sDupJongToDupChoMap.put(2, 1);  // ㄲ
			sDupJongToDupChoMap.put(20, 10);  // ㅆ
		}
		
		public abstract void jaum();
		public abstract void moum();
		public abstract boolean back();
	}
	
	private void composingAppend(String text){
		mComposing.append(text);
		setComposingText(mComposing.toString());
	}
	
	private void composingReplace(String text){
		mComposing.replace(mComposing.length()-1, mComposing.length(), text);
		setComposingText(mComposing.toString());
	}
	
	private int jongToCho(int jong){
		int choIdx = 0;
		for(int i=0; i<State.sJongsungArr.length; i++){
			if(jong == State.sJongsungArr[i]){
				choIdx = i;
				break;
			}
		}
		return choIdx;
	}

	/**
	 * state 0 : 초기상태
	 */
	private class State0 extends State {
		
		public void jaum() {
			mChosung = mCurrKey;
			composingAppend(String.valueOf((char)(JASO_START_INDEX+sJasoArr[mCurrKey])));
			mCurrState = mState1;
		}

		public void moum() {
			if(mCurrKey >= JAUM_SIZE){
				mJungsung = sJungsungArr[mCurrKey-JAUM_SIZE];
				composingAppend(String.valueOf((char)(JASO_START_INDEX+JAUM_FULL_SIZE+sJungsungArr[mCurrKey-JAUM_SIZE])));
				mCurrState = mState2;
			}
		}

		@Override
		public boolean back() {
			if(mComposing.length() > 0){
				mComposing.delete(mComposing.length()-1, mComposing.length());
				setComposingText(mComposing.toString());
			}
			else{
				mService.keyDownUp(KeyEvent.KEYCODE_DEL);
			}
			init();
			return true;
		}
	}
	
	/**
	 * state 1 : 자음
	 */
	private class State1 extends State {

		public void jaum() {
			int dupKey = 10000 + (mChosung*100) + mCurrKey;
			if(mUseDupChosung && sChangeChosungMap.containsKey(dupKey)){
				mChosung = sChangeChosungMap.get(dupKey);
				composingReplace(String.valueOf((char)(JASO_START_INDEX+sJasoArr[mChosung])));
			}
			else{
				mState0.jaum();
			}			
		}

		public void moum() {
			if(mCurrKey >= JAUM_SIZE){
				mJungsung = sJungsungArr[mCurrKey-JAUM_SIZE];
				composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX)));
				mCurrState = mState3;
			}
		}

		@Override
		public boolean back() {
			mComposing.delete(mComposing.length()-1, mComposing.length());
			setComposingText(mComposing.toString());
			init();
			return false;
		}
	}
	
	/**
	 * state 2 : 모음
	 */
	private class State2 extends State {

		public void jaum() {
			init();
			mState0.jaum();
		}

		public void moum() {
			if(mCurrKey >= JAUM_SIZE){
				int jung = sJungsungArr[mCurrKey-JAUM_SIZE];
				int dupKey = 10000 + (mJungsung*100) + jung;
				if(sDupJungsungMap.containsKey(dupKey)){
					mJungsung = sDupJungsungMap.get(dupKey);
					composingReplace(String.valueOf((char)(JASO_START_INDEX+JAUM_FULL_SIZE+mJungsung)));
				}
				else{
					mState0.moum();
				}
			}
		}

		@Override
		public boolean back() {
			mComposing.delete(mComposing.length()-1, mComposing.length());
			setComposingText(mComposing.toString());
			init();
			return false;
		}
	}
	
	/**
	 * state 3 : 자음+모음
	 */
	private class State3 extends State {
		
		public void jaum() {
			mJongsung = sJongsungArr[mCurrKey];
			if(mJongsung == -1){
				mState0.jaum();
			}
			else{
				composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + mJongsung + HANGUL_START_INDEX)));
				mCurrState = mState4;
			}
		}

		public void moum() {
			if(mCurrKey >= JAUM_SIZE){
				int jung = sJungsungArr[mCurrKey-JAUM_SIZE];
				int dupKey = 10000 + (mJungsung*100) + jung;
				if(sDupJungsungMap.containsKey(dupKey)){
					mJungsung = sDupJungsungMap.get(dupKey);
					composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX)));
				}
				else{
					mState0.moum();
				}
			}
		}

		@Override
		public boolean back() {
			composingReplace(String.valueOf((char)(JASO_START_INDEX+sJasoArr[mChosung])));
			mCurrState = mState1;
			return false;
		}
		
	}
	
	/**
	 * state 4 : 자음+모음+자음
	 */
	private class State4 extends State {
		
		private boolean mWillSplitJongsung = false;

		public void jaum() {
			int jong = sJongsungArr[mCurrKey];
			int dupKey = 10000 + (mJongsung*100) + jong;
			if(sDupJongsungMap.containsKey(dupKey)){
				mWillSplitJongsung = true;
				mJongsung = sDupJongsungMap.get(dupKey);
				composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + mJongsung + HANGUL_START_INDEX)));
			}
			else{
				mWillSplitJongsung = false;
				int postDupKey = 10000 + (mJongsung*100) + mCurrKey;
				if(sPostDupChosungMap.containsKey(postDupKey) && sDupChosungMap.containsKey(mCurrKey)){
					mJongsung = 0;
					composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX)));
					init();
					mCurrKey = sDupChosungMap.get(mCurrKey);
					mState0.jaum();
				}
				else{
					init();
					mState0.jaum();
				}
			}
		}

		public void moum() {
			int key = 0;
			if(sDupJongToDupChoMap.containsKey(mJongsung)){
				String str = String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX));
				composingReplace(str+" ");  // 공백은 마지막 글자 들어갈 자리
				
				mChosung = sDupJongToDupChoMap.get(mJongsung);
				mJongsung = 0;
				mState1.moum();
			}
			else if(mWillSplitJongsung && (key = dupJongToKey(mJongsung)) >= 10000){
				mJongsung = (key-10000) / 100;
				String str = String.valueOf((char)(mChosung*21*28 + mJungsung*28 + mJongsung + HANGUL_START_INDEX));
				composingReplace(str+" ");  // 공백은 마지막 글자 들어갈 자리
				
				mChosung = jongToCho((key-10000) % 100);
				mJongsung = 0;
				mState1.moum();
			}
			else{
				String str = String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX));
				composingReplace(str+" ");  // 공백은 마지막 글자 들어갈 자리
				
				mChosung = jongToCho(mJongsung);
				mJongsung = 0;
				mState1.moum();
			}
			mWillSplitJongsung = false;
		}
		
		@Override
		public boolean back() {
			composingReplace(String.valueOf((char)(mChosung*21*28 + mJungsung*28 + HANGUL_START_INDEX)));
			mCurrState = mState3;
			return false;
		}
		
		private int dupJongToKey(int dupJong){
			Iterator<Integer> iter = sDupJongsungMap.keySet().iterator();
			int result = 0;
			while(iter.hasNext()){
				int key = iter.next();
				if(sDupJongsungMap.get(key) == dupJong){
					result = key;
					break;
				}
			}
			return result;
		}
	}
	
}
