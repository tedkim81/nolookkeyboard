package com.teuskim.nlkeyboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teuskim.nlkeyboard.NLKeyboard.KeySet;
import com.teuskim.nlkeyboard.NLKeyboardDb.CustomKeysetData;

/**
 * 키보드 서비스 ( 컨트롤러 )
 * 1. 키보드 모델 및 키보드 뷰를 생성한다.
 * 2. 키보드뷰에 리스너를 달고, 콜백 받을 메소드들을 구현한다.
 * 3. 선택받은 글자를 출력하거나, 키보드를 변경하는 메소드를 구현한다.
 * 
 * 참고 ( 생명주기 )
 * 1. 키보드가 생성될 때, onCreate, onStartInput 호출
 * 2. 키보드가 노출될 때, onCreateInputView, onCreateCandidatesView, onStartInputView 호출
 * 3. 가끔 onStartInput 부터가 다시 호출되는데 왜그런지 모르겠다.
 * 4. 백키로 키보드 없애면 onFinishInputView 호출
 * 5. 입력영역을 눌러서 키보드를 다시 노출하면 onStartInputView 호출
 * 6. 다른 키보드를 선택하면 onDestroy 호출
 * 7. 다시 키보드 선택하면 1번부터.
 * 8. 현재화면에서 벗어나면 onFinishInputView, onStartInput 호출
 * 9. 현재화면 들어오면 onStartInput 호출
 */
public class NLKeyboardService extends InputMethodService implements OnClickListener {
	
	private static final String TAG = "NLKeyboardService";
	
	public static final int KEY_DELETE = android.inputmethodservice.Keyboard.KEYCODE_DELETE;
	private static final int KEY_SHIFT = android.inputmethodservice.Keyboard.KEYCODE_SHIFT;
	private static final int KEY_ENTER = -3;
	
	private static final int IDX_LEFT_TOP = 0;
	private static final int IDX_MID_TOP = 1;
	private static final int IDX_RIGHT_TOP = 2;
	private static final int IDX_LEFT_MID = 3;
	private static final int IDX_RIGHT_MID = 4;
	private static final int IDX_LEFT_BOT = 5;
	private static final int IDX_MID_BOT = 6;
	private static final int IDX_RIGHT_BOT = 7;
	
	private static final int TYPE_CHAR = 1;
	private static final int TYPE_SPACE = 2;
	private static final int TYPE_ETC = 3;
	private static final double AFFINITY_WEIGHT = 50;
	
	private static final int LAST_INPUT_SPACE = 1;
	private static final int LAST_INPUT_BACKSPACE = 2;
	private static final int LAST_INPUT_ETC = 0;
	
	private View mInputView;
	private NLKeyboard mEnglishKeyboard;
	private NLKeyboard mHangulKeyboard;
	private NLKeyboard mHangulShiftKeyboard;
	private NLKeyboard mNumbersKeyboard;
	private NLKeyboard mSymbolsKeyboard;
	private NLKeyboardView mKeyboardViewLeft;
	private NLKeyboardView mKeyboardViewRight;
	private LinearLayout mWordListLayout;
	private Button mBtnSpace;
	
	private StringBuilder mComposing = new StringBuilder();
	
	private KeyboardActionListener mLeftKeyboardListener = new KeyboardActionListener();
	private KeyboardActionListener mRightKeyboardListener = new KeyboardActionListener();
	private List<NLKeyboard> mKeyboardListForAll = new ArrayList<NLKeyboard>();
	private List<NLKeyboard> mKeyboardListForLeft = new ArrayList<NLKeyboard>();
	private List<NLKeyboard> mKeyboardListForRight = new ArrayList<NLKeyboard>();
	
	private HangulHandler mHangulHandler;
	private NLKeyboardDb mDb;
	
	private NLKeyboardView mLastKeyboardView;
	private Map<Integer, String> mCustomStringMap;
	private Map<Integer, TextView> mCustomTextViewMap;
	
	private View mCustomKeymapLeft;
	private View mCustomKeymapRight;
	
	private WordListTask mWordListTask;
	private String mInputString;
	private int mLastInput;
	
	private boolean mLockChangeKeyboard = false;
	private Handler mHandler = new Handler();
	private Runnable mHangulInputRunnable = new Runnable() {
		
		@Override
		public void run() {
			try{
				if(mLastKeyboardView != null){
					mHangulHandler.handle(mLastKeyboardView.getSelectedKeyInt());
					
					if(mIsShiftRemain == false && mLastKeyboardView.getKeyboard() == mHangulShiftKeyboard){
						if(mLastKeyboardView.getDirection() == 4){
							mKeyboardViewLeft.setKeyboard(mHangulKeyboard);
							mKeyboardViewRight.setKeyboard(mHangulKeyboard);
						}
						else{
							mLastKeyboardView.setKeyboard(mHangulKeyboard);
						}
					}
					showWordList();
					changeSpacebarText();
				}
			}catch(Exception e){}
		}
	};
	
	private NLPreference mPref;
	private boolean mLastInputIsHangul = false;
	private int mKeyCodeEnter = KeyEvent.KEYCODE_ENTER;
	private boolean mIsShiftRemain = true;
	private int mCurrTypeTextVariation;
	private boolean mIsInserting = false;
	
	
	@Override
	public void onCreate() {
		// 객체 생성시 호출. 변경되지 않는 멤버 변수 초기화
		Log.d(TAG, "onCreate!!");
		super.onCreate();		
		
		mDb = NLKeyboardDb.getInstance(getApplicationContext());
		mPref = new NLPreference(getApplicationContext());
	}

	@Override
	public void onInitializeInterface() {
		// 뭔가 설정이 변경될 때마다 호출된다. 여기에 UI관련 멤버변수를 모두 셋팅한다.
		Log.d(TAG, "onInitializeInterface!!");
		super.onInitializeInterface();
		
		mEnglishKeyboard = new NLKeyboard(this, R.xml.keyboard_english);
		mHangulKeyboard = new NLKeyboard(this, R.xml.keyboard_hangul);
		mHangulShiftKeyboard = new NLKeyboard(this, R.xml.keyboard_hangul_shift);
		mHangulHandler = new HangulHandler(this);
		mNumbersKeyboard = new NLKeyboard(this, R.xml.keyboard_numbers);
		mSymbolsKeyboard = new NLKeyboard(this, R.xml.keyboard_symbols);
		mCustomStringMap = new HashMap<Integer, String>();
		mCustomTextViewMap = new HashMap<Integer, TextView>();
	}

	@Override
	public View onCreateInputView() {
		// 키보드뷰를 최초 출력할 때와 설정이 변경될 때마다 호출된다. 키보드뷰를 생성하여 리턴한다.
		Log.d(TAG, "onCreateInputView!!");
		
		mInputView = getLayoutInflater().inflate(R.layout.input, null);
		mKeyboardViewLeft = (NLKeyboardView) mInputView.findViewById(R.id.keyboard_left);
		mKeyboardViewLeft.setOnKeyboardActionListener(mLeftKeyboardListener);
		mKeyboardViewLeft.setKeyboard(mEnglishKeyboard);
		mKeyboardViewRight = (NLKeyboardView) mInputView.findViewById(R.id.keyboard_right);
		mKeyboardViewRight.setOnKeyboardActionListener(mRightKeyboardListener);
		mKeyboardViewRight.setKeyboard(mEnglishKeyboard);
		mCustomKeymapLeft = mInputView.findViewById(R.id.custom_keymap_left);
		mCustomKeymapRight = mInputView.findViewById(R.id.custom_keymap_right);
		mWordListLayout = (LinearLayout) mInputView.findViewById(R.id.history_list_layout);
		
		mKeyboardViewLeft.setOppositeKeyboardView(mKeyboardViewRight);
		mKeyboardViewRight.setOppositeKeyboardView(mKeyboardViewLeft);
		
		Button btnRepeatLeft = (Button) mInputView.findViewById(R.id.btn_repeat_left);
		Button btnRepeatRight = (Button) mInputView.findViewById(R.id.btn_repeat_right);
		Button btnSetting = (Button) mInputView.findViewById(R.id.btn_setting);
		mBtnSpace = (Button) mInputView.findViewById(R.id.btn_space);
		Button btnBackspace = (Button) mInputView.findViewById(R.id.btn_backspace);
		btnSetting.setOnClickListener(this);
		
		RepeatListener listener = new RepeatListener(this);
		mBtnSpace.setOnTouchListener(listener);
		btnBackspace.setOnTouchListener(listener);
		btnRepeatLeft.setOnTouchListener(listener);
		btnRepeatRight.setOnTouchListener(listener);
		
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		int windowSize = Math.min(dm.widthPixels, dm.heightPixels) / 2;
		int twoInch = dm.densityDpi * 2;
		int layoutSize = Math.min(windowSize, twoInch);
		int centerSize = layoutSize / 3;
		int keysetSize = layoutSize / 4 + 4;
		mKeyboardViewLeft.setSize(layoutSize, centerSize, keysetSize);
		mKeyboardViewRight.setSize(layoutSize, centerSize, keysetSize);
		
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				int width = mKeyboardViewLeft.getWidth();
				if(width > 100){
					ViewGroup.LayoutParams params1 = mCustomKeymapLeft.getLayoutParams();
					params1.width = (int)(width * 0.9);
					mCustomKeymapLeft.setLayoutParams(params1);
					
					ViewGroup.LayoutParams params2 = mCustomKeymapRight.getLayoutParams();
					params2.width = (int)(width * 0.9);
					mCustomKeymapRight.setLayoutParams(params2);
				}
			}
		}, 500);
		
		return mInputView;
	}
	
	private class RepeatListener implements View.OnTouchListener{

		private boolean mIsDown = false;
		private View mView;
		private OnClickListener mListener;
		
		public RepeatListener(OnClickListener listener){
			mListener = listener;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mView = v;
			
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mIsDown = true;
				mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						delayedRepeat();
					}
				}, 300);
				break;
			case MotionEvent.ACTION_UP:
				mIsDown = false;
				mHandler.removeCallbacksAndMessages(null);
				mHandler.post(mRepeatRunnable);
				break;
			}
			
			return false;
		}
		
		private void delayedRepeat(){
			if(mIsDown == true){
				mHandler.postDelayed(mRepeatRunnable, 50);
			}
		}
		
		private Runnable mRepeatRunnable = new Runnable() {
			
			@Override
			public void run() {
				mListener.onClick(mView);
				delayedRepeat();
			}
		};
		
	}
	
	private void handleRepeat(){
		switch(mLastInput){
		case LAST_INPUT_SPACE:
			handleSpace();
			break;
		case LAST_INPUT_BACKSPACE:
			handleBackspace();
			break;
		default:
			handleInput(mLastKeyboardView);
		}
	}
	
	private void goSetting(){
		Intent i = new Intent(getApplicationContext(), SettingActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	@Override
	public View onCreateCandidatesView() {
		// 자동완성 뷰를 최초 출력할 때와 설정이 변경될 때마다 호출된다. 자동완성뷰를 생성하여 리턴한다.
		Log.d(TAG, "onCreateCandidatesView!!");
		return super.onCreateCandidatesView();
	}

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		// 키보드뷰 셋팅이 완료된 후 호출된다. 키보드 객체를 셋팅한다.
		Log.d(TAG, "onStartInputView!!");
		super.onStartInputView(info, restarting);
		
		// 키보드가 보여지는 동안 입력되는 내용을 저장하기 위해
		mComposing.setLength(0);
		mHangulHandler.initHangulData();
		mWordListLayout.removeAllViews();
		
		int direction = mDb.getDirection();
		mKeyboardViewLeft.setDirectionInfo(direction, true);
		mKeyboardViewRight.setDirectionInfo(direction, false);
		
		List<NLKeyboardDb.KeySet> listAll = mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_ALL);
		fillKeyboardList(mKeyboardListForAll, listAll);
		List<NLKeyboardDb.KeySet> listLeft = mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_LEFT);
		fillKeyboardList(mKeyboardListForLeft, listLeft);
		List<NLKeyboardDb.KeySet> listRight = mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_RIGHT);
		fillKeyboardList(mKeyboardListForRight, listRight);
		
		mCustomStringMap.clear();
		List<NLKeyboardDb.CustomKeyset> customKeysetList = mDb.getCustomKeySetList();
		for(NLKeyboardDb.CustomKeyset ck : customKeysetList){
			List<Map<Integer, String>> keyStringList = new ArrayList<Map<Integer,String>>();
			if((ck.mShow4 != NLKeyboardDb.CustomKeyset.SHOW_NONE)
					|| ck.mShow8 != NLKeyboardDb.CustomKeyset.SHOW_NONE){
				
				List<CustomKeysetData> ckdList = mDb.getCustomKeySetDataList(ck.mId);
				for(int i=0; i<8; i++){
					keyStringList.add(new HashMap<Integer, String>());
				}
				for(CustomKeysetData ckd : ckdList){
					Map<Integer, String> map = keyStringList.get(ckd.mKeysetNumber);
					map.put(ckd.mId, ckd.mData);
					mCustomStringMap.put(ckd.mId, ckd.mData);
				}
			}
			
			if(ck.mShow4 == NLKeyboardDb.CustomKeyset.SHOW_ALL){
				mKeyboardListForAll.add(new NLKeyboard(this, keyStringList));
			}
			if(ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_LEFT || ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_ALL){
				mKeyboardListForLeft.add(new NLKeyboard(this, keyStringList));
			}
			if(ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_RIGHT || ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_ALL){
				mKeyboardListForRight.add(new NLKeyboard(this, keyStringList));
			}
		}
		
		// 최근단어 영역에 초기 메세지 셋팅
		showWordList();
		
		setKeyboardByEditorInfo(info);
		
		// 초성 중복 입력시 쌍자음 되도록 하는 기능
		mHangulHandler.setUseDupChosung(mPref.useDupChosung());
		
		// 진동기능 사용여부
		NLKeyboardView.setIsVibrate(mPref.isVibrate());
		
		// shift키 유지 기능
		mIsShiftRemain = mPref.isShiftRemain();
		
		// wordInsert 할때만 사용
		mCurrTypeTextVariation = info.inputType&EditorInfo.TYPE_MASK_VARIATION;
	}
	
	private void fillKeyboardList(List<NLKeyboard> keyboardList, List<NLKeyboardDb.KeySet> keysetList){
		keyboardList.clear();
		String english = getString(R.string.txt_english);
		String hangul = getString(R.string.txt_hangul);
		String numbers = getString(R.string.txt_numbers);
		String symbols = getString(R.string.txt_symbols);
		for(NLKeyboardDb.KeySet ks : keysetList){
			if("Y".equals(ks.mShowYN)){
				String keyboardName = mDb.getKeyboardName(ks.mType);
				if(keyboardName.equals(english))
					keyboardList.add(mEnglishKeyboard);
				else if(keyboardName.equals(hangul))
					keyboardList.add(mHangulKeyboard);
				else if(keyboardName.equals(numbers))
					keyboardList.add(mNumbersKeyboard);
				else if(keyboardName.equals(symbols))
					keyboardList.add(mSymbolsKeyboard);
			}			
		}
	}

	@Override
	public void onFinishInputView(boolean finishingInput) {
		// 키보드뷰가 사라지면 호출된다.
		Log.d(TAG, "onFinishInputView!!");
		
		// 입력된 내용을 저장한다.
		wordInsert(mInputString);
		
		super.onFinishInputView(finishingInput);
		
		mHangulHandler.onFinishInputView();
	}
	
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		// 키 입력전 호출되며 가장 중요한 부분이다. 각종 멤버변수를 셋팅하는 등의 입력 준비 단계를 마무리 짓는다.
		Log.d(TAG, "onStartInput!!");
		super.onStartInput(attribute, restarting);
		mComposing.setLength(0);
		
		switch (attribute.imeOptions & (EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
		case EditorInfo.IME_ACTION_NEXT:
			mKeyCodeEnter = KeyEvent.KEYCODE_TAB;
			break;
		default:
			mKeyCodeEnter = KeyEvent.KEYCODE_ENTER;
		}
	}
	
	private void setKeyboardByEditorInfo(EditorInfo attribute){
		switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS){
		case EditorInfo.TYPE_CLASS_NUMBER:
		case EditorInfo.TYPE_CLASS_DATETIME:
		case EditorInfo.TYPE_CLASS_PHONE:
			mLockChangeKeyboard = true;
			
			if(mKeyboardViewLeft != null)
				mKeyboardViewLeft.setKeyboard(mNumbersKeyboard);
			if(mKeyboardViewRight != null)
				mKeyboardViewRight.setKeyboard(mNumbersKeyboard);
			break;
		default:
			mLockChangeKeyboard = false;
			
			if(mDb.getDirection() == 4){
				if(mKeyboardListForAll != null && mKeyboardListForAll.size() > 0){
					int keyboardPos = mPref.getKeyboardPosAll();
					mKeyboardViewLeft.setKeyboard(getKeyboardFromList(mKeyboardListForAll,keyboardPos));
					mKeyboardViewRight.setKeyboard(getKeyboardFromList(mKeyboardListForAll,keyboardPos));
				}
			}
			else{
				if(mKeyboardListForLeft != null && mKeyboardListForLeft.size() > 0){
					mKeyboardViewLeft.setKeyboard(getKeyboardFromList(mKeyboardListForLeft,mPref.getKeyboardPosLeft()));
				}
				if(mKeyboardListForRight != null && mKeyboardListForRight.size() > 0){
					mKeyboardViewRight.setKeyboard(getKeyboardFromList(mKeyboardListForRight,mPref.getKeyboardPosRight()));
				}
			}
		}
	}
	
	private NLKeyboard getKeyboardFromList(List<NLKeyboard> list, int pos){
		if(list.size()-1 < pos)
			return list.get(0);
		return list.get(pos);
	}

	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		
//		Log.d(TAG, "oldStart:"+oldSelStart+", oldEnd:"+oldSelEnd+", newStart:"+newSelStart+", newEnd:"+newSelEnd+", candStart:"+candidatesStart+", candEnd:"+candidatesEnd);
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
		
		if ((mComposing.length() > 0 || mHangulHandler.getComposingLength() > 0)
				&& (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)){
			
//			Log.d(TAG, "onUpdateSelection! init data!");
			mComposing.setLength(0);
			mHangulHandler.initHangulData();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
	}

	@Override
	public void onFinishInput() {
		// 키 입력이 완료되면 호출된다. 각종 상태를 리셋한다.
		Log.d(TAG, "onFinishInput!!");
		super.onFinishInput();
		mComposing.setLength(0);
	}

	@Override
	public void onDestroy() {
		// 객체가 종료될 때 호출된다. 메모리릭 방지를 위한 종료처리 등을 한다.
		Log.d(TAG, "onDestroy!!");
		super.onDestroy();
	}

	public void keyDownUp(int keyEventCode) {
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}
	
	@Override
	public void onClick(View v) {
		boolean isTextChanged = false;
		
		switch(v.getId()){
		case R.id.btn_space:
			handleSpace();
			isTextChanged = true;
			break;
		case R.id.btn_backspace:
			handleBackspace();
			isTextChanged = true;
			break;
		case R.id.btn_repeat_left:
		case R.id.btn_repeat_right:
			handleRepeat();
			isTextChanged = true;
			break;
		case R.id.btn_setting:
			goSetting();
			break;
		}
		
		if(isTextChanged){
			showWordList();
			changeSpacebarText();
		}
	}

	protected void setComposingText(CharSequence text, int pos){
		getCurrentInputConnection().setComposingText(text, pos);
		
		showWordList();
	}
	
	private void commitText(CharSequence text, int pos){
		getCurrentInputConnection().commitText(text, pos);
		
		showWordList();
	}
	
	protected void commitHangul(CharSequence text, int pos){
		getCurrentInputConnection().commitText(text, pos);
		
		showWordList();
	}
	
	private void handleInput(NLKeyboardView keyboardView){
		if(keyboardView == null)
			return;
		
		// 반복키 사용을 위해 저장한다.
		mLastKeyboardView = keyboardView;
		
		// 스와잎이 끝나면 호출된다. 글자를 출력한다.
		NLKeyboard currentKeyboard = keyboardView.getKeyboard();
		if(currentKeyboard == mHangulKeyboard || currentKeyboard == mHangulShiftKeyboard){
			handleHangul(keyboardView, currentKeyboard);
			mLastInputIsHangul = true;
		}
		else if(currentKeyboard.isCustom() == true){
			handleCustom(keyboardView);
			
			showWordList();
			changeSpacebarText();
		}
		else{
			handleCommon(keyboardView, currentKeyboard);
			mLastInputIsHangul = false;
			
			showWordList();
			changeSpacebarText();
		}
		
		mLastInput = LAST_INPUT_ETC;
	}
	
	private void changeSpacebarText(){
		if(mHangulHandler.isInit() == false){
			mBtnSpace.setText(R.string.btn_commit);
		}
		else{
			mBtnSpace.setText(R.string.btn_space);
		}
	}
	
	private void handleCommon(NLKeyboardView keyboardView, NLKeyboard currentKeyboard){
		if(keyboardView.getSelectedKeyInt() == KEY_DELETE){
			handleDelete(keyboardView);
		}
		else if(keyboardView.getSelectedKeyInt() == KEY_SHIFT){
			handleShift(keyboardView, currentKeyboard);			
		}
		else if(keyboardView.getSelectedKeyInt() == KEY_ENTER){
			handleEnter();
		}
		else{
			handleCharacter(keyboardView);
			mHangulHandler.initHangulData();
		}
	}
	
	public void handleDelete(NLKeyboardView keyboardView){
		if(mLastInputIsHangul){
			mHandler.post(mHangulInputRunnable);
		}
		else{
			final int length = mComposing.length();
			if(length > 1){
				mComposing.delete(length - 1, length);
				setComposingText(mComposing, 1);
			}
			else if(length > 0){
				mComposing.setLength(0);
				commitText("", 0);
			}
			else{
				keyDownUp(KeyEvent.KEYCODE_DEL);
			}
			
			mHangulHandler.initHangulData();
		}
	}
	
	public void handleEnter(){
		keyDownUp(mKeyCodeEnter);
	}
	
	private void handleShift(NLKeyboardView keyboardView, NLKeyboard currentKeyboard){
		int direction = keyboardView.getDirection();
		if(currentKeyboard == mHangulKeyboard){
			if(direction == 4){
				mKeyboardViewLeft.setKeyboard(mHangulShiftKeyboard);
				mKeyboardViewRight.setKeyboard(mHangulShiftKeyboard);
			}
			else{
				keyboardView.setKeyboard(mHangulShiftKeyboard);
			}
		}
		else if(currentKeyboard == mHangulShiftKeyboard){
			if(direction == 4){
				mKeyboardViewLeft.setKeyboard(mHangulKeyboard);
				mKeyboardViewRight.setKeyboard(mHangulKeyboard);
			}
			else{
				keyboardView.setKeyboard(mHangulKeyboard);
			}
		}
		else{
			currentKeyboard.setIsShift(!currentKeyboard.isShift());
			currentKeyboard.loadKeyboard();
			if(direction == 4){
				mKeyboardViewLeft.setKeyboard(currentKeyboard);
				mKeyboardViewRight.setKeyboard(currentKeyboard);
			}
			else{
				keyboardView.setKeyboard(currentKeyboard);
			}
		}
	}
	
	private void handleCharacter(NLKeyboardView keyboardView){
		mHangulHandler.commit();
		
		String selectedStr = String.valueOf((char)keyboardView.getSelectedKeyInt());
		mComposing.append(selectedStr);
		if(Character.isLetterOrDigit(keyboardView.getSelectedKeyInt())){
			setComposingText(mComposing, 1);
		}
		else{
			commitText(mComposing, mComposing.length());
			mComposing.setLength(0);
		}
		
		if(mIsShiftRemain == false && keyboardView.getKeyboard() == mEnglishKeyboard && keyboardView.getKeyboard().isShift())
			handleShift(keyboardView, keyboardView.getKeyboard());
	}
	
	private void handleCustom(NLKeyboardView keyboardView){
		String selectedStr = mCustomStringMap.get(keyboardView.getSelectedKeyInt());
		if("\b".equals(selectedStr)){
			if(mLastInputIsHangul){
				mHangulHandler.handle(HangulHandler.CUSTOM_BACKSPACE);
			}
			else{
				handleDelete(keyboardView);
			}
		}			
		else if("\n".equals(selectedStr)){
			handleEnter();
		}
		else{
			handleSelectedString(keyboardView, selectedStr);
		}			
	}
	
	private void handleSelectedString(NLKeyboardView keyboardView, String str){
		int keyInt = mHangulHandler.getKeyIntFromJaso(str);
		if(keyInt >= 0 && keyboardView != null){
			if(mComposing.length() > 0){
				commitText(mComposing, 1);
				mComposing.setLength(0);
			}
			mHangulHandler.handle(keyInt);
			
			mLastInputIsHangul = true;
		}
		else{
			mHangulHandler.commit();
			
			mComposing.append(str);
			commitText(mComposing, 1);
			mComposing.setLength(0);
			
			mLastInputIsHangul = false;
		}
	}
	
	private void handleHangul(NLKeyboardView keyboardView, NLKeyboard currentKeyboard){
		if(keyboardView.getSelectedKeyInt() == KEY_SHIFT){
			handleShift(keyboardView, currentKeyboard);
		}
		else{
			if(mComposing.length() > 0){
				commitText(mComposing, 1);
				mComposing.setLength(0);
			}
			mHandler.post(mHangulInputRunnable);
		}
	}
	
	private void handleBackspace(){
		if(isHangulInput()){
			mHangulHandler.handleBackspace();
		}
		else{
			final int length = mComposing.length();
			if(length > 1){
				mComposing.delete(length - 1, length);
				getCurrentInputConnection().setComposingText(mComposing, 1);
			}
			else if(length > 0){
				mComposing.setLength(0);
				getCurrentInputConnection().commitText("", 0);
			}
			else{
				keyDownUp(KeyEvent.KEYCODE_DEL);
			}
		}
		mLastInput = LAST_INPUT_BACKSPACE;
	}
	
	private void handleSpace(){
		if(isHangulInput()){
			mHangulHandler.handleSpace();
		}
		else{
			mHangulHandler.commit();
			mComposing.append(' ');
			commitText(mComposing, mComposing.length());
			mComposing.setLength(0);
		}
		mLastInput = LAST_INPUT_SPACE;
	}
	
	private boolean isHangulInput(){
		return mHangulHandler.getComposingLength() > 0;
	}
	
	private class KeyboardActionListener implements NLKeyboardView.OnKeyboardActionListener {

		@Override
		public void onTouchDown(NLKeyboardView keyboardView) {
			NLKeyboard keyboard = keyboardView.getKeyboard();
			if(keyboard.isCustom() == true){
				if(keyboardView.isLeft() == true)
					fillCustomKeymapView(mCustomKeymapLeft, keyboard.getKeyStringList());
				else
					fillCustomKeymapView(mCustomKeymapRight, keyboard.getKeyStringList());
			}
		}
		
		private void fillCustomKeymapView(View keymapView, List<Map<Integer, String>> keyStringList){
			if(keymapView != null){
				fillCustomKeymapText(keyStringList.get(IDX_LEFT_TOP), keymapView, R.id.keymap_left_top, R.drawable.icon_keymap_left_top);
				fillCustomKeymapText(keyStringList.get(IDX_MID_TOP), keymapView, R.id.keymap_mid_top, R.drawable.icon_keymap_mid_top);
				fillCustomKeymapText(keyStringList.get(IDX_RIGHT_TOP), keymapView, R.id.keymap_right_top, R.drawable.icon_keymap_right_top);
				fillCustomKeymapText(keyStringList.get(IDX_LEFT_MID), keymapView, R.id.keymap_left_mid, R.drawable.icon_keymap_left_mid);
				fillCustomKeymapText(keyStringList.get(IDX_RIGHT_MID), keymapView, R.id.keymap_right_mid, R.drawable.icon_keymap_right_mid);
				fillCustomKeymapText(keyStringList.get(IDX_LEFT_BOT), keymapView, R.id.keymap_left_bot, R.drawable.icon_keymap_left_bottom);
				fillCustomKeymapText(keyStringList.get(IDX_MID_BOT), keymapView, R.id.keymap_mid_bot, R.drawable.icon_keymap_mid_bottom);
				fillCustomKeymapText(keyStringList.get(IDX_RIGHT_BOT), keymapView, R.id.keymap_right_bot, R.drawable.icon_keymap_right_bottom);
				
				keymapView.setVisibility(View.VISIBLE);
			}
		}
		
		private void fillCustomKeymapText(Map<Integer, String> map, View keymapView, int keymapResId, int iconResId){
			if(map.size() > 0){
				LinearLayout keymapPart = (LinearLayout) keymapView.findViewById(keymapResId);
				keymapPart.setVisibility(View.VISIBLE);
				
				ImageView iconView = (ImageView) keymapPart.findViewById(R.id.icon_keymap);
				iconView.setImageResource(iconResId);
				
				TreeSet<Integer> set = new TreeSet<Integer>(map.keySet());
				Iterator<Integer> it = set.iterator();
				int i=1;
				while(it.hasNext()){
					TextView keymapText = (TextView) keymapPart.getChildAt(i++);
					keymapText.setTextColor(Color.BLACK);
					int idx = it.next();
					String customString = mCustomStringMap.get(idx);
					if("\b".equals(customString))
						keymapText.setText("backspace");
					else if(" ".equals(customString))
						keymapText.setText("spacebar");
					else if("\n".equals(customString))
						keymapText.setText("enter");
					else
						keymapText.setText(customString);
					mCustomTextViewMap.put(idx, keymapText);
				}
			}
		}

		@Override
		public void onTouchUp(NLKeyboardView keyboardView) {
			if(mCustomKeymapLeft != null)
				mCustomKeymapLeft.setVisibility(View.GONE);
			if(mCustomKeymapRight != null)
				mCustomKeymapRight.setVisibility(View.GONE);
		}

		public void onClick(NLKeyboardView keyboardView) {
			// 숫자 입력과 같이 입력영역의 타입이 정해져있을 경우 키보드 교체를 못하도록 한다.
			if(mLockChangeKeyboard)
				return;
			
			// 입력영역을 클릭하면 호출된다. 키보드를 교체한다.
			NLKeyboard currentKeyboard = keyboardView.getKeyboard();
			int direction = mDb.getDirection();
			
			try{
				if(direction == 4){
					int idx = (mKeyboardListForAll.indexOf(currentKeyboard) + 1) % mKeyboardListForAll.size();
					mKeyboardViewLeft.setKeyboard(mKeyboardListForAll.get(idx));
					mKeyboardViewRight.setKeyboard(mKeyboardListForAll.get(idx));
					
					mPref.setKeyboardPosAll(idx);
				}
				else{
					if(keyboardView.getId() == R.id.keyboard_left){
						int idx = (mKeyboardListForLeft.indexOf(currentKeyboard) + 1) % mKeyboardListForLeft.size();
						mKeyboardViewLeft.setKeyboard(mKeyboardListForLeft.get(idx));
						
						mPref.setKeyboardPosLeft(idx);
					}
					else{
						int idx = (mKeyboardListForRight.indexOf(currentKeyboard) + 1) % mKeyboardListForRight.size();
						mKeyboardViewRight.setKeyboard(mKeyboardListForRight.get(idx));
						
						mPref.setKeyboardPosRight(idx);
					}
				}
			}catch(Exception e){}
		}
		
		@Override
		public void onSwipe(NLKeyboardView keyboardView, KeySet keySet, int swipeCnt) {
			Iterator<Integer> it = mCustomTextViewMap.keySet().iterator();
			while(it.hasNext()){
				int idx = it.next();
				TextView textView = mCustomTextViewMap.get(idx);
				if(idx == keyboardView.getSelectedKeyInt())
					textView.setTextColor(Color.RED);
				else
					textView.setTextColor(Color.BLACK);
			}
		}

		@Override
		public void onSwipeFinish(NLKeyboardView keyboardView, int swipeCnt) {
			handleInput(keyboardView);
		}
		
	}
	
	private String getAllText(){
		InputConnection ic = getCurrentInputConnection();
		return (String)ic.getTextBeforeCursor(100, 0) + (String)ic.getTextAfterCursor(100, 0);
	}
	
	private String getCurrentWord(){
		if(isHangulInput()){
			return mHangulHandler.getComposing().toString();
		}
		else{
			return mComposing.toString();
		}
	}
	
	private char getLastChar(){
		if(mInputString != null && mInputString.length() > 0){
			return mInputString.charAt(mInputString.length()-1);
		}
		return ' ';
	}
	
	private int getInputType(){
		EditorInfo info = getCurrentInputEditorInfo();
		return info.inputType&EditorInfo.TYPE_MASK_VARIATION;
	}
	
	private void recordInputString(){
		if(getInputType() != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD){
			String inputString = getAllText();
			
			// 입력영역이 초기화가 되었다면 단어저장
			if(mInputString != null && mInputString.length() > 1
					&& (inputString == null || inputString.length() == 0)
					&& mIsInserting == false){
				wordInsert(mInputString);
			}
			
			mInputString = inputString;
		}
	}
	
	private class WordListTask extends AsyncTask<String, Integer, List<RecommendWord>>{
		
		@Override
		protected List<RecommendWord> doInBackground(String... params) {
			String text = getCurrentWord();
			if(text != null && getCharType(getLastChar()) != TYPE_SPACE)
				text = text.trim();
			else
				text = "";
			return recommendWords(text, mHangulHandler.separateJaso(text));
		}

		@Override
		protected void onPostExecute(List<RecommendWord> result) {
//			for(RecommendWord rw : result){
//				Log.e("AAAA", rw.mWord+" , "+rw.mPoint);
//			}
			
			LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
			mWordListLayout.removeAllViews();
			int cnt;
			if(result != null && (cnt = result.size()) > 0){
				int min = Math.min(cnt, 10);
				for(int i=0; i<min; i++){
					RecommendWord rw = result.get(i);
					final String word = rw.mWord;
					
					View v = inflater.inflate(R.layout.history_list_item, null);
					((TextView) v.findViewById(R.id.history_word)).setText(word);
					v.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							mComposing.setLength(0);
							commitHangul("", 1);
							mHangulHandler.initHangulData();
							
							if(getCharType(word.charAt(word.length()-1)) == TYPE_ETC)
								commitText(word, 1);
							else
								commitText(word+" ", 1);
							showWordList();
							
							changeSpacebarText();
						}
					});
					mWordListLayout.addView(v);
				}
			}
			else{
				// TODO: 예측 단어가 없을 경우 어떻게 할까? 기존에는 설정으로 보내는 알림 텍스트를 넣었으나, 다른게 좋을듯.
			}
		}
		
		private List<RecommendWord> recommendWords(String word, String composition){

			// 앞단어 이용해서 친밀도 기반 목록 구한다.
			List<RecommendWord> affinityWordList = getAffinityWordList(word, composition);
			
			// 입력영역속성 기반 목록을 구한다.
			List<RecommendWord> attrWordList = getAttrWordList(composition);
			
			// 입력횟수기반 목록을 구한다.
			List<RecommendWord> useCntWordList = getUseCntWordList(composition);
			
			// 입력시각기반 목록을 구한다.
			List<RecommendWord> useTimeWordList = getUseTimeWordList(composition);
			
			// 모든 목록을 병합하여 내림차순으로 10개의 항목을 갖는 목록을 만들어 리턴한다.
			return mergeWordList(affinityWordList, attrWordList, useCntWordList, useTimeWordList);
		}
		
		private List<RecommendWord> getAffinityWordList(String word, String composition){
			InputConnection ic = getCurrentInputConnection();
			String prevText = (String)ic.getTextBeforeCursor(50, 0);
			List<RecommendWord> resultList = new ArrayList<RecommendWord>();
			
			if(prevText.length() > 0){
				List<String> texts = splitText(prevText);
				if(texts.size() > 0)
					resultList = mDb.getNextWordList(texts.get(texts.size()-1), composition);
			}
			
			if(word != null && word.length() > 0 && getCharType(word.charAt(word.length()-1)) == TYPE_ETC){
				resultList.addAll(mDb.getNextWordList(word, ""));
			}
			
			return resultList;
		}
		
		private List<RecommendWord> getAttrWordList(String composition){
			EditorInfo info = getCurrentInputEditorInfo();
			int typeTextVariation = (info.inputType&EditorInfo.TYPE_MASK_VARIATION);
			return mDb.getWordListByAttr(composition, typeTextVariation);
		}
		
		private List<RecommendWord> getUseCntWordList(String composition){
			return mDb.getWordListByUseCnt(composition);
		}
		
		private List<RecommendWord> getUseTimeWordList(String composition){
			return mDb.getWordListByUseTime(composition);
		}
		
		private List<RecommendWord> mergeWordList(List<RecommendWord> affinityWordList, List<RecommendWord> attrWordList
				, List<RecommendWord> useCntWordList, List<RecommendWord> useTimeWordList){
			
//			for(RecommendWord rw : affinityWordList){
//				Log.e("AAAA", "affinityWordList : "+rw.mWordId+" , "+rw.mWord+" , "+rw.mUseCntNext+" , "+rw.mUseCntNextSum);
//			}
//			for(RecommendWord rw : attrWordList){
//				Log.e("AAAA", "attrWordList : "+rw.mWord+" , "+rw.mUseCntXxx+" , "+rw.mUseCntXxxSum);
//			}
//			for(RecommendWord rw : useCntWordList){
//				Log.e("AAAA", "useCntWordList : "+rw.mWord+" , "+rw.mUseCntTotal+" , "+rw.mUseCntTotalSum);
//			}
//			for(RecommendWord rw : useTimeWordList){
//				Log.e("AAAA", "useTimeWordList : "+rw.mWord+" , "+rw.mUseCntN+" , "+rw.mUseCntNSum);
//			}
			
			Map<Integer, RecommendWord> map = new HashMap<Integer, RecommendWord>();
			for(RecommendWord rw : affinityWordList){
				map.put(rw.mWordId, rw);
			}
			for(RecommendWord rw : attrWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntXxx = rw.mUseCntXxx;
					rwTmp.mUseCntXxxSum = rw.mUseCntXxxSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			for(RecommendWord rw : useCntWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntTotal = rw.mUseCntTotal;
					rwTmp.mUseCntTotalSum = rw.mUseCntTotalSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			for(RecommendWord rw : useTimeWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntN = rw.mUseCntN;
					rwTmp.mUseCntNSum = rw.mUseCntNSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			
			Iterator<RecommendWord> iter = map.values().iterator();
			NLKeyboardDb.MyInfo myInfo = mDb.getMyInfo();
			long availableTime = myInfo.mAvailablePeriod * 86400000L;
			List<RecommendWord> resultList = new ArrayList<RecommendWord>();
			while(iter.hasNext()){
				RecommendWord rw = iter.next();
				rw.generatePoint(AFFINITY_WEIGHT, myInfo.mUseCntXxxWeight, myInfo.mUseCntTotalWeight, myInfo.mUseCntNWeight, availableTime);
				resultList.add(rw);
			}
			Collections.sort(resultList);
			return resultList;
		}
	}
	
	private void wordInsert(String inputString){
		// 입력된 내용을 저장한다.
		mIsInserting = true;
		WordInsertTask task = new WordInsertTask();
		task.execute(inputString);
	}
	
	private int getCharType(char ch){
		if((ch >= 97 && ch <= 122) || (ch >= 65 && ch <= 90) || (ch >= 0x3131 && ch <= 0xd7a3))
			return TYPE_CHAR;
		else if (ch == 9 || ch == 10 || ch == 13 || ch == 32)
			return TYPE_SPACE;
		else
			return TYPE_ETC;
	}
	
	private List<String> splitText(String inputText){
		List<String> wordList = new ArrayList<String>();
		if(inputText == null)
			return wordList;
		
		StringBuilder sb = new StringBuilder();
		int currType = 0;
		for(int i=0; i<inputText.length(); i++){
			char ch = inputText.charAt(i);
			int type = getCharType(ch);
			
			if(currType != TYPE_CHAR && type == TYPE_CHAR && sb.length() > 0){
				wordList.add(sb.toString());
				sb.setLength(0);
			}
			if(type != TYPE_SPACE){
				sb.append(ch);
			}
			
			currType = type;
		}
		if(sb.length() > 0)
			wordList.add(sb.toString());
		
		return wordList;
	}
	
	private class WordInsertTask extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			String inputString = params[0];
			if(inputString == null || inputString.length() == 0)
				return false;
			
			String inputText = inputString.trim();
			if(inputText.length() > 0){
				// 단어분리 {{
				List<String> wordList = splitText(inputText);
				// }} 단어분리
				
				// 저장하기 {{
				String preWord = null;
				Calendar cal = Calendar.getInstance();
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				String columnUseCntXXX = mDb.getColumnUseCntXXX(mCurrTypeTextVariation);
				String columnUseCntN = mDb.getColumnUseCntN(hour);
				for(String word : wordList){
					String composition = mHangulHandler.separateJaso(word);
					mDb.insertOrUpdateWord(word, composition
										, mCurrTypeTextVariation, columnUseCntXXX
										, hour, columnUseCntN);
					
					if(preWord != null)
						mDb.insertOrUpdateNextWordGroup(preWord, word);
					preWord = word;
					
					Log.d(TAG, "insert word: "+word+" , composition: "+composition+" , typeTextVariation: "+mCurrTypeTextVariation);
				}
				// }} 저장하기
				
				// 분석 / 학습 {{
				// 가중치 갱신하기
				double weightTotal = mDb.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_TOTAL, NLKeyboardDb.Word.USE_CNT_TOTAL);
				double weightN = mDb.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_N, columnUseCntN);
				double weightXXX = mDb.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_XXX, columnUseCntXXX);
				if(weightTotal > 0 && weightN > 0 && weightXXX > 0){
					double ratio = (NLKeyboardDb.WEIGHT_SUM - NLKeyboardDb.WEIGHT_INIT_NEXT) / (weightTotal + weightN + weightXXX);
					weightTotal = weightTotal * ratio;
					weightN = weightN * ratio;
					weightXXX = weightXXX * ratio;
					mDb.updateWeight((int)weightTotal, (int)weightN, (int)weightXXX);
				}
				
				// 유효기간 갱신하기
				// TODO: 성능상의 문제가 있다. 나중에 하자.
				// }} 분석 / 학습
				
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if(result){
				mInputString = null;
			}
			mIsInserting = false;
		}
		
	}
	
	public void showWordList(){
		recordInputString();
		
		mHandler.removeCallbacks(mWordListRunnable);
		mHandler.postDelayed(mWordListRunnable, 50);
	}
	
	private WordListRunnable mWordListRunnable = new WordListRunnable();
	private class WordListRunnable implements Runnable {

		@Override
		public void run() {
			if(mWordListTask != null){
				mWordListTask.cancel(true);
			}
			mWordListTask = new WordListTask();
			mWordListTask.execute();
		}
		
	}
	
}
