package com.teuskim.nlkeyboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.view.inputmethod.EditorInfo;

public class NLKeyboardDb {
	
	private static final String DATABASE_NAME = "nolookbrd.db";
	
	private static final int DATABASE_VERSION = 2;
	private static NLKeyboardDb sInstance;
	private Context mContext;
	
	private SQLiteDatabase mDb;
	
	private static final int KEYBOARD_TYPE_ENGLISH = 0;
	private static final int KEYBOARD_TYPE_HANGUL = 1;
	private static final int KEYBOARD_TYPE_NUMBERS = 2;
	private static final int KEYBOARD_TYPE_SYMBOLS = 3;
	
	public static final int WEIGHT_SUM = 100;
	public static final int WEIGHT_INIT_NEXT = 50;
	public static final int WEIGHT_INIT_TOTAL = 10;
	public static final int WEIGHT_INIT_N = 10;
	public static final int WEIGHT_INIT_XXX = 30;
	public static final int AVAILABLE_PERIOD_INIT = 60;
	
	private NLKeyboardDb(Context context){
		mContext = context;
	}
	
	public static NLKeyboardDb getInstance(Context context){
		
		if(sInstance != null){
			return sInstance;
		}
		
		sInstance = new NLKeyboardDb(context);
		if( sInstance.open(context) ){
			return sInstance;
		}else{
			return null;
		}
		
	}
	
	private boolean open(Context context){
		DbOpenHelper dbHelper;
    	dbHelper = new DbOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    	mDb = dbHelper.getWritableDatabase();
    	return (mDb == null) ? false : true;
	}
	
	public void close(){
		mDb.close();
	}
	
	public String getKeyboardName(int type){
		switch(type){
		case KEYBOARD_TYPE_ENGLISH:
			return mContext.getString(R.string.txt_english);
		case KEYBOARD_TYPE_HANGUL:
			return mContext.getString(R.string.txt_hangul);
		case KEYBOARD_TYPE_NUMBERS:
			return mContext.getString(R.string.txt_numbers);
		case KEYBOARD_TYPE_SYMBOLS:
			return mContext.getString(R.string.txt_symbols);
		}
		return "Unknown";
	}
	
	/**
	 * 4방향인지 8방향인지 구하기
	 * @return
	 */
	public int getDirection(){
		int direction = 8;
		try{
			Cursor cursor = mDb.query(MyInfo.TABLE_NAME
							, new String[]{MyInfo.DIRECTION}
							, null, null, null, null, null);
			
			if(cursor.moveToFirst()){
				direction = cursor.getInt(0);
			}
			cursor.close();
		}catch(Exception e){}
		
		return direction;
	}
	
	/**
	 * 4방향인지 8방향인지 업데이트 하기
	 * @param direction
	 * @return
	 */
	public boolean updateDirection(int direction){
		ContentValues values = new ContentValues();
		values.put(MyInfo.DIRECTION, direction);
		
		return (mDb.update(MyInfo.TABLE_NAME, values, null, null) > 0);
	}
	
	/**
	 * 최근단어 저장/입력 기능을 사용할지 여부 구하기
	 * @return
	 */
	public boolean useHistory(){
		boolean useHistory = false;
		try{
			Cursor cursor = mDb.query(MyInfo.TABLE_NAME
							, new String[]{MyInfo.USE_HISTORY}
							, null, null, null, null, null);
			
			if(cursor.moveToFirst() && cursor.getInt(0) == 1){
				useHistory = true;
			}
			cursor.close();
		}catch(Exception e){}
		
		return useHistory;
	}
	
	/**
	 * 최근단어 저장/입력 기능 사용여부 업데이트 하기
	 * @param useHistory
	 * @return
	 */
	public boolean updateUseHistory(boolean useHistory){
		ContentValues values = new ContentValues();
		values.put(MyInfo.USE_HISTORY, (useHistory ? 1 : 0));
		
		return (mDb.update(MyInfo.TABLE_NAME, values, null, null) > 0);
	}
	
	/**
	 * 키보드셋 리스트 구하기
	 * @param side
	 * @return
	 */
	public List<KeySet> getKeySetList(int side){
		List<KeySet> result = new ArrayList<KeySet>();
		try{
			Cursor cursor = mDb.query(KeySet.TABLE_NAME
							, new String[]{KeySet._ID, KeySet.TYPE, KeySet.SHOW_YN, KeySet.SIDE}
							, KeySet.SIDE+"="+side
							, null, null, null, null);
			
			if(cursor.moveToFirst()){
				do{
					KeySet keyset = new KeySet();
					keyset.mId = cursor.getInt(0);
					keyset.mType = cursor.getInt(1);
					keyset.mShowYN = cursor.getString(2);
					keyset.mDirection = cursor.getInt(3);
					result.add(keyset);
				}while(cursor.moveToNext());			
			}
			cursor.close();
		}catch(Exception e){}
		
		return result;
	}
	
	/**
	 * 키보드셋 노출여부 변경
	 * @param id
	 * @param isChecked
	 * @return
	 */
	public boolean updateKeySetChecked(int id, boolean isChecked){
		ContentValues values = new ContentValues();
		values.put(KeySet.SHOW_YN, (isChecked ? "Y" : "N"));
		
		return (mDb.update(KeySet.TABLE_NAME, values, KeySet._ID+"="+id, null) > 0);
	}
	
	/**
	 * 사용자 정의 키보드셋 리스트 구하기
	 * @return
	 */
	public List<CustomKeyset> getCustomKeySetList(){
		List<CustomKeyset> result = new ArrayList<CustomKeyset>();
		try{
			Cursor cursor = mDb.query(CustomKeyset.TABLE_NAME
							, new String[]{CustomKeyset._ID, CustomKeyset.NAME, CustomKeyset.SHOW_4, CustomKeyset.SHOW_8}
							, null, null, null, null, null);
			
			if(cursor.moveToFirst()){
				do{
					CustomKeyset keyset = new CustomKeyset();
					keyset.mId = cursor.getInt(0);
					keyset.mName = cursor.getString(1);
					keyset.mShow4 = cursor.getInt(2);
					keyset.mShow8 = cursor.getInt(3);
					result.add(keyset);
				}while(cursor.moveToNext());			
			}
			cursor.close();
		}catch(Exception e){}
		
		return result;
	}
	
	/**
	 * 사용자 정의 키보드셋 이름 구하기
	 * @param id
	 * @return
	 */
	public String getCustomKeySetName(int id){
		String name = null;
		try{
			Cursor cursor = mDb.query(CustomKeyset.TABLE_NAME
							, new String[]{CustomKeyset.NAME}
							, CustomKeyset._ID+"="+id
							, null, null, null, null);
			if(cursor.moveToFirst()){
				name = cursor.getString(0);
			}
			cursor.close();
		}catch(Exception e){}
		
		return name;
	}
	
	/**
	 * 사용자 정의 키보드셋 노출여부 구하기
	 * @param id
	 * @param direction
	 * @return
	 */
	public int getCustomKeySetShow(int id, int direction){
		int show = CustomKeyset.SHOW_NONE;
		try{
			String column;
			if(direction == 4)
				column = CustomKeyset.SHOW_4;
			else
				column = CustomKeyset.SHOW_8;
			Cursor cursor = mDb.query(CustomKeyset.TABLE_NAME
							, new String[]{column}
							, CustomKeyset._ID+"="+id
							, null, null, null, null);
			if(cursor.moveToFirst()){
				show = cursor.getInt(0);
			}
			cursor.close();
		}catch(Exception e){}
		
		return show;
	}
	
	/**
	 * 사용자 정의 키보드셋 노출여부 업데이트
	 * @param id
	 * @param show
	 * @return
	 */
	public boolean updateCustomKeySetShow(int id, int show, int direction){
		ContentValues values = new ContentValues();
		if(direction == 4)
			values.put(CustomKeyset.SHOW_4, show);
		else
			values.put(CustomKeyset.SHOW_8, show);
		
		return (mDb.update(CustomKeyset.TABLE_NAME, values, CustomKeyset._ID+"="+id, null) > 0);
	}
	
	/**
	 * 사용자 정의 키보드셋의 저장된 값 구하기
	 * @param id
	 * @return
	 */
	public List<CustomKeysetData> getCustomKeySetDataList(int id){
		List<CustomKeysetData> result = new ArrayList<CustomKeysetData>();
		try{
			Cursor cursor = mDb.query(CustomKeysetData.TABLE_NAME
							, new String[]{CustomKeysetData._ID, CustomKeysetData.CUSTOM_KEYSET_ID, CustomKeysetData.KEYSET_NUMBER, CustomKeysetData.DATA}
							, CustomKeysetData.CUSTOM_KEYSET_ID+"=?"
							, new String[]{""+id}
							, null, null, CustomKeysetData._ID+" asc");
			
			if(cursor.moveToFirst()){
				do{
					CustomKeysetData keysetData = new CustomKeysetData();
					keysetData.mId = cursor.getInt(0);
					keysetData.mCustomKeysetId = cursor.getInt(1);
					keysetData.mKeysetNumber = cursor.getInt(2);
					keysetData.mData = cursor.getString(3);
					result.add(keysetData);
				}while(cursor.moveToNext());			
			}
			cursor.close();
		}catch(Exception e){}
		
		return result;
	}
	
	/**
	 * 사용자 정의 키보드셋 저장하기
	 * @param name
	 * @param showYN
	 * @param side
	 * @param leftTop
	 * @param midTop
	 * @param rightTop
	 * @param leftMid
	 * @param rightMid
	 * @param leftBot
	 * @param midBot
	 * @param rightBot
	 * @return
	 */
	public boolean insertCustomKeyset(String name, int show, int direction
			, List<String> leftTop, List<String> midTop, List<String> rightTop
			, List<String> leftMid, List<String> rightMid
			, List<String> leftBot, List<String> midBot, List<String> rightBot){
		
		if(name == null || name.length() == 0)
			return false;
		
		if((leftTop == null || leftTop.size() == 0)
				&& (midTop == null || midTop.size() == 0)
				&& (rightTop == null || rightTop.size() == 0)
				&& (leftMid == null || leftMid.size() == 0)
				&& (rightMid == null || rightMid.size() == 0)
				&& (leftBot == null || leftBot.size() == 0)
				&& (midBot == null || midBot.size() == 0)
				&& (rightBot == null || rightBot.size() == 0)){
			return false;
		}
		
		ContentValues values = new ContentValues();
		values.put(CustomKeyset.NAME, name);
		if(direction == 4)
			values.put(CustomKeyset.SHOW_4, show);
		else
			values.put(CustomKeyset.SHOW_8, show);		
		
		long rowID = mDb.insert(CustomKeyset.TABLE_NAME, null, values);
		if(rowID <= 0){
			throw new SQLException("Failed to insert row into " + CustomKeyset.TABLE_NAME);
		}
		
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_TOP, leftTop);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_MID_TOP, midTop);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_TOP, rightTop);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_MID, leftMid);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_MID, rightMid);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_BOT, leftBot);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_MID_BOT, midBot);
		insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_BOT, rightBot);
	
		return true;
	}
	
	private boolean insertCustomKeysetData(long id, int keysetNumber, List<String> datas){
		if(datas != null && datas.size() > 0){
			for(String data : datas){
				ContentValues values = new ContentValues();
				values.put(CustomKeysetData.CUSTOM_KEYSET_ID, id);
				values.put(CustomKeysetData.KEYSET_NUMBER, keysetNumber);
				values.put(CustomKeysetData.DATA, data);
				long rowID = mDb.insert(CustomKeysetData.TABLE_NAME, null, values);
				if(rowID <= 0){
					throw new SQLException("Failed to insert row into " + CustomKeysetData.TABLE_NAME);
				}
			}		
			return true;	
		}
		return false;
	}
	
	/**
	 * 사용자정의 키보드셋 삭제하기
	 * @param id
	 * @return
	 */
	public boolean deleteCustomKeyset(long id){
		int deleteCnt = mDb.delete(CustomKeyset.TABLE_NAME, CustomKeyset._ID+"="+id, null);
		mDb.delete(CustomKeysetData.TABLE_NAME, CustomKeysetData.CUSTOM_KEYSET_ID+"="+id, null);
		return (deleteCnt > 0);
	}
	
	/**
	 * 입력 히스토리 추가 또는 업데이트하기
	 * @param keyboard
	 * @param word
	 * @param data
	 * @return
	 */
	public boolean insertORupdateHistory(int keyboard, String word, String data){
		int historyId = getHistoryId(word);
		if(historyId > 0){
			int useCnt = getHistoryUseCnt(historyId) + 1;
			ContentValues values = new ContentValues();
			values.put(History.USE_CNT, useCnt);
			mDb.update(History.TABLE_NAME
					, values
					, History._ID+"=?"
					, new String[]{""+historyId});
		}
		else{
			ContentValues values = new ContentValues();
			values.put(History.KEYBOARD, keyboard);
			values.put(History.WORD, word);
			values.put(History.DATA, data);
			mDb.insert(History.TABLE_NAME, null, values);
		}
		return true;
	}
	
	private int getHistoryUseCnt(int historyId){
		int useCnt = 0;
		try{
			Cursor cursor = mDb.query(History.TABLE_NAME
								, new String[]{History.USE_CNT}
								, History._ID+"=?"
								, new String[]{""+historyId}
								, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				useCnt = cursor.getInt(0);
			}
			cursor.close();
		}catch(Exception e){}
		
		return useCnt;
	}
	
	/**
	 * 입력단어로 아이디 가져오기
	 * @param word
	 * @return
	 */
	public int getHistoryId(String word){
		int historyId = 0;
		try{
			Cursor cursor = mDb.query(History.TABLE_NAME
								, new String[]{History._ID}
								, History.WORD+"=?"
								, new String[]{word}
								, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				historyId = cursor.getInt(0);
			}
			cursor.close();
		}catch(Exception e){}
		
		return historyId;
	}
	
	/**
	 * 현재까지 입력된 값으로 like검색한 결과 구하기
	 * @param preData
	 * @param keyboard
	 * @return
	 */
	public Cursor getHistoryList(String preData, int keyboard){
		Cursor cursor = mDb.query(History.TABLE_NAME
								, new String[]{History.WORD, History._ID, History.USE_CNT}
								, History.DATA+" like ? and "+History.KEYBOARD+"=?"
								, new String[]{preData+"%", ""+keyboard}
								, null, null
								, History.USE_CNT+" desc");
		return cursor;
	}
	
	/**
	 * 저장된 최근단어 총갯수 구하기
	 * @return
	 */
	public int getHistoryCount(){
		int count = 0;
		try{
			Cursor cursor = mDb.rawQuery("select count(*) from "+History.TABLE_NAME, null);
			if(cursor.moveToFirst())
				count = cursor.getInt(0);
			cursor.close();
		}catch(Exception e){}
		
		return count;
	}
	
	/**
	 * 전체 히스토리 삭제
	 * @return
	 */
	public boolean deleteHistory(){
		return (mDb.delete(History.TABLE_NAME, null, null) > 0);
	}
	
	/**
	 * 입력단어로 word 객체 가져오기
	 * @param word
	 * @return
	 */
	public Word getWord(String word){
		Word wordObj = null;
		try{
			Cursor cursor = mDb.query(Word.TABLE_NAME
								, new String[]{Word.WORD, Word.COMPOSITION, Word.USE_CNT_TOTAL, Word.USE_CNT_0, Word.USE_CNT_4
											, Word.USE_CNT_8, Word.USE_CNT_12, Word.USE_CNT_16, Word.USE_CNT_20
											, Word.USE_CNT_NORMAL, Word.USE_CNT_EMAIL_ADDRESS, Word.USE_CNT_EMAIL_SUBJECT
											, Word.USE_CNT_URI, Word.USE_CNT_PERSON_NAME, Word.USE_CNT_POSTAL_ADDRESS
											, Word.USE_CNT_NUMBER, Word.UPD_DT, Word.CRT_DT, Word.WORD_ID}
								, Word.WORD+"=?"
								, new String[]{word}
								, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				wordObj = new Word();
				wordObj.mWord = cursor.getString(0);
				wordObj.mComposition = cursor.getString(1);		wordObj.mUseCntTotal = cursor.getInt(2);
				wordObj.mUseCnt0 = cursor.getInt(3);	wordObj.mUseCnt4 = cursor.getInt(4);
				wordObj.mUseCnt8 = cursor.getInt(5);	wordObj.mUseCnt12 = cursor.getInt(6);
				wordObj.mUseCnt16 = cursor.getInt(7);	wordObj.mUseCnt20 = cursor.getInt(8);
				wordObj.mUseCntNormal = cursor.getInt(9);
				wordObj.mUseCntEmailAddress = cursor.getInt(10);		wordObj.mUseCntEmailSubject = cursor.getInt(11);
				wordObj.mUseCntUri = cursor.getInt(12);		wordObj.mUseCntPersonName = cursor.getInt(13);
				wordObj.mUseCntPostalAddress = cursor.getInt(14);		wordObj.mUseCntNumber = cursor.getInt(15);
				wordObj.mUpdDt = cursor.getString(16);
				wordObj.mCrtDt = cursor.getString(17);		wordObj.mWordId = cursor.getInt(18);
			}
			cursor.close();
		}catch(Exception e){}
		
		return wordObj;
	}
	
	/**
	 * 입력 단어 추가 / 갱신
	 * @param word
	 * @param composition
	 * @param typeTextVariation
	 * @return
	 */
	public boolean insertOrUpdateWord(String word, String composition, int typeTextVariation, String columnUseCntXxx, int hour, String columnUseCntN){
		
		Word wordObj = getWord(word);
		long now = new Date().getTime();
		
		if (wordObj != null && wordObj.mWord != null){
			int useCntTotal = wordObj.mUseCntTotal + 1;
			int useCntN = wordObj.mUseCnt0;
			if(hour >= 4 && hour < 8)
				useCntN = wordObj.mUseCnt4;
			else if(hour >= 8 && hour < 12)
				useCntN = wordObj.mUseCnt8;
			else if(hour >= 12 && hour < 16)
				useCntN = wordObj.mUseCnt12;
			else if(hour >= 16 && hour < 20)
				useCntN = wordObj.mUseCnt16;
			else if(hour >= 20)
				useCntN = wordObj.mUseCnt20;
			useCntN++;
			
			int useCntXxx = wordObj.mUseCntNormal;
			switch (typeTextVariation){
			case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS: useCntXxx = wordObj.mUseCntEmailAddress; break;
			case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT: useCntXxx = wordObj.mUseCntEmailSubject; break;
			case EditorInfo.TYPE_TEXT_VARIATION_URI: useCntXxx = wordObj.mUseCntUri; break;
			case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME: useCntXxx = wordObj.mUseCntPersonName; break;
			case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS: useCntXxx = wordObj.mUseCntPostalAddress; break;
			case EditorInfo.TYPE_CLASS_NUMBER: useCntXxx = wordObj.mUseCntNumber; break;
			}
			useCntXxx++;
			
			ContentValues values = new ContentValues();
			values.put(Word.USE_CNT_TOTAL, useCntTotal);
			values.put(columnUseCntN, useCntN);
			values.put(columnUseCntXxx, useCntXxx);
			return mDb.update(Word.TABLE_NAME
							, values
							, Word.WORD+"=?"
							, new String[]{""+wordObj.mWord}) > 0;
		}
		else{
			ContentValues values = new ContentValues();
			values.put(Word.WORD, word);
			values.put(Word.COMPOSITION, composition);
			values.put(Word.USE_CNT_TOTAL, 1);
			values.put(columnUseCntN, 1);
			values.put(columnUseCntXxx, 1);
			values.put(Word.UPD_DT, now);
			values.put(Word.CRT_DT, now);
			return mDb.insert(Word.TABLE_NAME, null, values) > 0;
		}
	}
	
	public String getColumnUseCntN(int hour){
		String columnUseCntN = Word.USE_CNT_0;
		if(hour >= 4 && hour < 8)
			columnUseCntN = Word.USE_CNT_4;
		else if(hour >= 8 && hour < 12)
			columnUseCntN = Word.USE_CNT_8;
		else if(hour >= 12 && hour < 16)
			columnUseCntN = Word.USE_CNT_12;
		else if(hour >= 16 && hour < 20)
			columnUseCntN = Word.USE_CNT_16;
		else if(hour >= 20)
			columnUseCntN = Word.USE_CNT_20;
		
		return columnUseCntN;
	}
	
	public String getColumnUseCntXXX(int typeTextVariation){
		String columnUseCntXxx = Word.USE_CNT_NORMAL;  // 0
		switch (typeTextVariation){
		case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS: columnUseCntXxx = Word.USE_CNT_EMAIL_ADDRESS; break;  // 32
		case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT: columnUseCntXxx = Word.USE_CNT_EMAIL_SUBJECT; break;  // 48
		case EditorInfo.TYPE_TEXT_VARIATION_URI: columnUseCntXxx = Word.USE_CNT_URI; break;  // 16
		case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME: columnUseCntXxx = Word.USE_CNT_PERSON_NAME; break;  // 96
		case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS: columnUseCntXxx = Word.USE_CNT_POSTAL_ADDRESS; break;  // 112
		case EditorInfo.TYPE_CLASS_NUMBER: columnUseCntXxx = Word.USE_CNT_NUMBER; break;  // 2
		}
		
		return columnUseCntXxx;
	}
	
	/**
	 * 다음 단어 NextWordGroup 객체 가져오기
	 */
	public NextWordGroup getNextWordGroup(String word, String nextWord){
		NextWordGroup nextWordGroup = null;
		try{
			Cursor cursor = mDb.query(NextWordGroup.TABLE_NAME
								, new String[]{NextWordGroup.WORD, NextWordGroup.NEXT_WORD, NextWordGroup.USE_CNT}
								, NextWordGroup.WORD+"=? and "+NextWordGroup.NEXT_WORD+"=?"
								, new String[]{word, nextWord}
								, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				nextWordGroup = new NextWordGroup();
				nextWordGroup.mWord = cursor.getString(0);
				nextWordGroup.mNextWord = cursor.getString(1);
				nextWordGroup.mUseCnt = cursor.getInt(2);
			}
			cursor.close();
		}catch(Exception e){}
		
		return nextWordGroup;
	}
	
	/**
	 * 다음 단어 정보 추가 / 갱신
	 */
	public boolean insertOrUpdateNextWordGroup(String word, String nextWord){
		NextWordGroup nextWordGroup = getNextWordGroup(word, nextWord);
		if(nextWordGroup != null && nextWordGroup.mNextWord != null){
			ContentValues values = new ContentValues();
			values.put(NextWordGroup.USE_CNT, nextWordGroup.mUseCnt+1);
			return mDb.update(NextWordGroup.TABLE_NAME
							, values
							, NextWordGroup.WORD+"=? and "+NextWordGroup.NEXT_WORD+"=?"
							, new String[]{word, nextWord}) > 0;
		}
		else{
			ContentValues values = new ContentValues();
			values.put(NextWordGroup.WORD, word);
			values.put(NextWordGroup.NEXT_WORD, nextWord);
			values.put(NextWordGroup.USE_CNT, 1);
			return mDb.insert(NextWordGroup.TABLE_NAME, null, values) > 0;
		}
	}
	
	/**
	 * 다음단어 목록 구하기
	 */
	public List<RecommendWord> getNextWordList(String prevWord, String composition){
		Word word = getWord(prevWord);
		List<RecommendWord> wordList = new ArrayList<RecommendWord>();
		if (word != null && word.mWord != null){
			try {
				String sql = "select b." + NextWordGroup.NEXT_WORD+",b." + NextWordGroup.USE_CNT
						+ " from " + Word.TABLE_NAME + " a," + NextWordGroup.TABLE_NAME + " b"
						+ " where a." + Word.WORD + "=b." + NextWordGroup.WORD
						+ " and b." + NextWordGroup.WORD + "=? and a." + Word.COMPOSITION + " like ?"
						+ " order by b." + NextWordGroup.USE_CNT + " desc, a." + Word.UPD_DT + " desc"
						+ " limit 100";
				sql = "select tb1.*, tb2."+Word.WORD_ID+", tb2."+Word.UPD_DT
					+ " from ( " + sql + " ) tb1, "+Word.TABLE_NAME+" tb2"
					+ " where tb1."+NextWordGroup.NEXT_WORD+"=tb2."+Word.WORD;
				if(composition == null) composition = "";
				Cursor cursor = mDb.rawQuery(sql, new String[]{word.mWord, composition+"_%"});
				
				if(cursor.getCount() > 0 && cursor.moveToFirst()){
					int sum = 0;
					do{
						RecommendWord rw = new RecommendWord();
						rw.mWord = cursor.getString(0);
						rw.mUseCntNext = cursor.getInt(1);
						rw.mWordId = cursor.getInt(2);
						rw.mUpdDt = cursor.getString(3);
						wordList.add(rw);
						
						sum += rw.mUseCntNext;
					}while(cursor.moveToNext());
					
					for(RecommendWord rw : wordList){
						rw.mUseCntNextSum = sum;
					}
				}
				cursor.close();
			}catch(Exception e){}
		}
		return wordList;
	}
	
	/**
	 * 입력영역속성에 따른 단어 목록 구하기
	 */
	public List<RecommendWord> getWordListByAttr(String composition, int typeTextVariation){
		List<RecommendWord> wordList = new ArrayList<RecommendWord>();
		try{
			String columnUseCntXxx = Word.USE_CNT_NORMAL;
			switch (typeTextVariation){
			case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS: columnUseCntXxx = Word.USE_CNT_EMAIL_ADDRESS; break;
			case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT: columnUseCntXxx = Word.USE_CNT_EMAIL_SUBJECT; break;
			case EditorInfo.TYPE_TEXT_VARIATION_URI: columnUseCntXxx = Word.USE_CNT_URI; break;
			case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME: columnUseCntXxx = Word.USE_CNT_PERSON_NAME; break;
			case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS: columnUseCntXxx = Word.USE_CNT_POSTAL_ADDRESS; break;
			case EditorInfo.TYPE_CLASS_NUMBER: columnUseCntXxx = Word.USE_CNT_NUMBER; break;
			}
			
			if(composition == null) composition = "";
			Cursor cursor = mDb.query(Word.TABLE_NAME
					, new String[]{Word.WORD, columnUseCntXxx, Word.WORD_ID, Word.UPD_DT}
					, Word.COMPOSITION+" like ?"
					, new String[]{ composition+"_%" }
					, null, null
					, columnUseCntXxx+" desc, "+Word.UPD_DT+" desc"
					, "100");
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				int sum = 0;
				do{
					RecommendWord rw = new RecommendWord();
					rw.mWord = cursor.getString(0);
					rw.mUseCntXxx = cursor.getInt(1);
					rw.mWordId = cursor.getInt(2);
					rw.mUpdDt = cursor.getString(3);
					wordList.add(rw);
					
					sum += rw.mUseCntXxx;
				}while(cursor.moveToNext());
				
				for(RecommendWord rw : wordList){
					rw.mUseCntXxxSum = sum;
				}
			}
			cursor.close();
		}catch(Exception e){}
		
		return wordList;
	}
	
	/**
	 * 전체입력횟수에 따른 단어목록 구하기
	 */
	public List<RecommendWord> getWordListByUseCnt(String composition){
		List<RecommendWord> wordList = new ArrayList<RecommendWord>();
		try{
			if(composition == null) composition = "";
			Cursor cursor = mDb.query(Word.TABLE_NAME
					, new String[]{Word.WORD, Word.USE_CNT_TOTAL, Word.WORD_ID, Word.UPD_DT}
					, Word.COMPOSITION+" like ?"
					, new String[]{ composition+"_%" }
					, null, null
					, Word.USE_CNT_TOTAL+" desc, "+Word.UPD_DT+" desc"
					, "100");
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				int sum = 0;
				do{
					RecommendWord rw = new RecommendWord();
					rw.mWord = cursor.getString(0);
					rw.mUseCntTotal = cursor.getInt(1);
					rw.mWordId = cursor.getInt(2);
					rw.mUpdDt = cursor.getString(3);
					wordList.add(rw);
					
					sum += rw.mUseCntTotal;
				}while(cursor.moveToNext());
				
				for(RecommendWord rw : wordList){
					rw.mUseCntTotalSum = sum;
				}
			}
			cursor.close();
		}catch(Exception e){}
		
		return wordList;
	}
	
	/**
	 * 시간대별 입력횟수에 따른 단어목록 구하기
	 */
	public List<RecommendWord> getWordListByUseTime(String composition){
		List<RecommendWord> wordList = new ArrayList<RecommendWord>();
		try{
			String columnUseCntN = Word.USE_CNT_0;
			Calendar cal = Calendar.getInstance();
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			if(hour >= 4 && hour < 8)
				columnUseCntN = Word.USE_CNT_4;
			else if(hour >= 8 && hour < 12)
				columnUseCntN = Word.USE_CNT_8;
			else if(hour >= 12 && hour < 16)
				columnUseCntN = Word.USE_CNT_12;
			else if(hour >= 16 && hour < 20)
				columnUseCntN = Word.USE_CNT_16;
			else if(hour >= 20)
				columnUseCntN = Word.USE_CNT_20;
			
			if(composition == null) composition = "";
			Cursor cursor = mDb.query(Word.TABLE_NAME
					, new String[]{Word.WORD, columnUseCntN, Word.WORD_ID, Word.UPD_DT}
					, Word.COMPOSITION+" like ?"
					, new String[]{ composition+"_%" }
					, null, null
					, columnUseCntN+" desc, "+Word.UPD_DT+" desc"
					, "100");
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				int sum = 0;
				do{
					RecommendWord rw = new RecommendWord();
					rw.mWord = cursor.getString(0);
					rw.mUseCntN = cursor.getInt(1);
					rw.mWordId = cursor.getInt(2);
					rw.mUpdDt = cursor.getString(3);
					wordList.add(rw);
					
					sum += rw.mUseCntN;
				}while(cursor.moveToNext());
				
				for(RecommendWord rw : wordList){
					rw.mUseCntNSum = sum;
				}
			}
			cursor.close();
		}catch(Exception e){}
		
		return wordList;
	}
	
	/**
	 * 내정보 가져오기
	 * @return
	 */
	public MyInfo getMyInfo(){
		MyInfo myInfo = new MyInfo();
		try{
			Cursor cursor = mDb.query(MyInfo.TABLE_NAME
							, new String[]{MyInfo.DIRECTION, MyInfo.USE_HISTORY, MyInfo.USE_CNT_TOTAL_WEIGHT
										, MyInfo.USE_CNT_N_WEIGHT, MyInfo.USE_CNT_XXX_WEIGHT, MyInfo.AVAILABLE_PERIOD}
							, null, null, null, null, null);
			
			if(cursor.moveToFirst()){
				myInfo.mDirection = cursor.getInt(0);
				myInfo.mUseHistory = cursor.getInt(1);
				myInfo.mUseCntTotalWeight = cursor.getInt(2)>0 ? cursor.getInt(2) : WEIGHT_INIT_TOTAL;
				myInfo.mUseCntNWeight = cursor.getInt(3)>0 ? cursor.getInt(3) : WEIGHT_INIT_TOTAL;
				myInfo.mUseCntXxxWeight = cursor.getInt(4)>0 ? cursor.getInt(4) : WEIGHT_INIT_TOTAL;
				myInfo.mAvailablePeriod = cursor.getInt(5);
			}
			cursor.close();
		}catch(Exception e){}
		
		return myInfo;
	}
	
	/**
	 * 갱신되는 가중치값 구하기
	 * 0 이 리턴되면 가중치를 갱신하지 않는다.
	 */
	public double getUpdatedWeight(double w, String columnUseCnt){
		int useCntSum = 0;
		int useCntCnt = 1;
		double useCntAvg = 0;
		double standardDeviation = 0;
		try{
			Cursor cursor = mDb.rawQuery("select sum("+columnUseCnt+"), count("+columnUseCnt+") from "+Word.TABLE_NAME, null);
			if(cursor.moveToFirst()){
				useCntSum = cursor.getInt(0);
				useCntCnt = cursor.getInt(1);
			}
			cursor.close();
			
			if(useCntSum < 100)
				return 0;
			
			useCntAvg = (double)useCntSum / useCntCnt;
			
			cursor = mDb.query(Word.TABLE_NAME
					, new String[]{columnUseCnt}
					, null, null, null, null
					, columnUseCnt+" desc"
					, "100");
			if(cursor.moveToFirst()){
				double temp;
				do{
					temp = cursor.getInt(0) - useCntAvg;
					standardDeviation += temp*temp;
				}while(cursor.moveToNext());
				standardDeviation = Math.sqrt(standardDeviation / cursor.getCount());
				
				w = w * (1 + standardDeviation);
			}
		}catch(Exception e){}
		
		return w;
	}
	
	public boolean updateWeight(int weightTotal, int weightN, int weightXXX){
		try{
			ContentValues values = new ContentValues();
			values.put(MyInfo.USE_CNT_TOTAL_WEIGHT, weightTotal);
			values.put(MyInfo.USE_CNT_N_WEIGHT, weightN);
			values.put(MyInfo.USE_CNT_XXX_WEIGHT, weightXXX);
			return mDb.update(MyInfo.TABLE_NAME
							, values
							, null, null) > 0;
		}catch(Exception e){}
		return false;
	}
	
	/**
	 * TODO: For Test
	 * word 목록 모두 가져오기
	 */
	public List<Word> getWordList(){
		List<Word> wordList = new ArrayList<Word>();
		try{
			Cursor cursor = mDb.query(Word.TABLE_NAME
					, new String[]{Word.WORD, Word.COMPOSITION, Word.USE_CNT_TOTAL, Word.USE_CNT_0, Word.USE_CNT_4
						, Word.USE_CNT_8, Word.USE_CNT_12, Word.USE_CNT_16, Word.USE_CNT_20
						, Word.USE_CNT_NORMAL, Word.USE_CNT_EMAIL_ADDRESS, Word.USE_CNT_EMAIL_SUBJECT, Word.USE_CNT_URI
						, Word.USE_CNT_POSTAL_ADDRESS, Word.USE_CNT_PERSON_NAME, Word.USE_CNT_NUMBER
						, Word.UPD_DT, Word.CRT_DT, Word.WORD_ID}
					, null, null, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				do{
					Word word = new Word();
					word.mWord = cursor.getString(0);	word.mComposition = cursor.getString(1);
					word.mUseCntTotal = cursor.getInt(2);	word.mUseCnt0 = cursor.getInt(3);
					word.mUseCnt4 = cursor.getInt(4);	word.mUseCnt8 = cursor.getInt(5);
					word.mUseCnt12 = cursor.getInt(6);	word.mUseCnt16 = cursor.getInt(7);
					word.mUseCnt20 = cursor.getInt(8);
					word.mUseCntNormal = cursor.getInt(9);	word.mUseCntEmailAddress = cursor.getInt(10);
					word.mUseCntEmailSubject = cursor.getInt(11);	word.mUseCntUri = cursor.getInt(12);
					word.mUseCntPostalAddress = cursor.getInt(13);	word.mUseCntPersonName = cursor.getInt(14);
					word.mUseCntNumber = cursor.getInt(15);
					word.mUpdDt = cursor.getString(16);		word.mCrtDt = cursor.getString(17);
					word.mWordId = cursor.getInt(18);
					wordList.add(word);
				}while(cursor.moveToNext());
			}
			cursor.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return wordList;
	}
	
	/**
	 * TODO: For Test
	 * next_word_group 모두 가져오기
	 */
	public List<NextWordGroup> getNextWordGroupList(){
		List<NextWordGroup> nwgList = new ArrayList<NextWordGroup>();
		try{
			Cursor cursor = mDb.query(NextWordGroup.TABLE_NAME
					, new String[]{NextWordGroup.WORD, NextWordGroup.NEXT_WORD, NextWordGroup.USE_CNT}
					, null, null, null, null, null);
			
			if(cursor.getCount() > 0 && cursor.moveToFirst()){
				do{
					NextWordGroup nwg = new NextWordGroup();
					nwg.mWord = cursor.getString(0);
					nwg.mNextWord = cursor.getString(1);
					nwg.mUseCnt = cursor.getInt(2);
					nwgList.add(nwg);
				}while(cursor.moveToNext());
			}
			cursor.close();
		}catch(Exception e){}
		
		return nwgList;
	}
	
	
	/**
	 * db open helper 
	 */
	public static class DbOpenHelper extends SQLiteOpenHelper{
		
		public DbOpenHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		public DbOpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db){
			
			MyInfo.onCreate(db);
			KeySet.onCreate(db);
			CustomKeyset.onCreate(db);
			CustomKeysetData.onCreate(db);
			Word.onCreate(db);
			NextWordGroup.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// 버전 업그레이드 할때 필요한 동작은 여기에 추가.
			if(oldVersion == 1 && newVersion == 2){
				updateVer1to2(db);
			}
		}
		
		private void updateVer1to2(SQLiteDatabase db){
			// 단어예측 기능 추가에 따라 myinfo에 컬럼 추가 및 기존 단어히스토리 내용은 삭제
			try{
				Word.onCreate(db);
				NextWordGroup.onCreate(db);
				db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_TOTAL_WEIGHT + " INT DEFAULT "+WEIGHT_INIT_TOTAL);
				db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_N_WEIGHT + " INT DEFAULT "+WEIGHT_INIT_N);
				db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_XXX_WEIGHT + " INT DEFAULT "+WEIGHT_INIT_XXX);
				db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.AVAILABLE_PERIOD + " INT DEFAULT "+AVAILABLE_PERIOD_INIT);
			}catch(Exception e){}
		}
	}
	
	
	/**
	 * MyInfo 테이블 구조.
	 */
	public static class MyInfo{
		
		public static final String TABLE_NAME = "myinfo";
		public static final String _ID = "_id";
		public static final String DIRECTION = "direction";
		public static final String USE_HISTORY = "use_history";  // 1:use, 0:don't
		public static final String USE_CNT_TOTAL_WEIGHT = "use_cnt_total_weight";
		public static final String USE_CNT_N_WEIGHT = "use_cnt_n_weight";
		public static final String USE_CNT_XXX_WEIGHT = "use_cnt_xxx_weight";
		public static final String AVAILABLE_PERIOD = "available_period";
		
		public int mDirection;
		public int mUseHistory;
		public int mUseCntTotalWeight;
		public int mUseCntNWeight;
		public int mUseCntXxxWeight;
		public int mAvailablePeriod;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ _ID + " INTEGER primary key autoincrement, "
			+ DIRECTION + " INTEGER, "
			+ USE_HISTORY + " INTEGER, "
			+ USE_CNT_TOTAL_WEIGHT + " INTEGER, "
			+ USE_CNT_N_WEIGHT + " INTEGER, "
			+ USE_CNT_XXX_WEIGHT + " INTEGER, "
			+ AVAILABLE_PERIOD + " INTEGER"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
			
			ContentValues values = new ContentValues();
			values.put(DIRECTION, 8);
			values.put(USE_HISTORY, 0);
			values.put(USE_CNT_TOTAL_WEIGHT, WEIGHT_INIT_TOTAL);
			values.put(USE_CNT_N_WEIGHT, WEIGHT_INIT_N);
			values.put(USE_CNT_XXX_WEIGHT, WEIGHT_INIT_XXX);
			values.put(AVAILABLE_PERIOD, 60);
			db.insert(TABLE_NAME, null, values);
		}
	}
	
	/**
	 * KeySet 테이블 구조.
	 */
	public static class KeySet{
		
		public static final String TABLE_NAME = "keyset";
		public static final String _ID = "_id";
		public static final String TYPE = "type";
		public static final String SHOW_YN = "show_yn";
		public static final String SIDE = "side";  // 0:전체(4방향일때), 1:왼쪽, 2:오른쪽
		
		public static final int SIDE_ALL = 0;
		public static final int SIDE_LEFT = 1;
		public static final int SIDE_RIGHT = 2;
		
		public int mId;
		public int mType;
		public String mShowYN;
		public int mDirection;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ _ID + " INTEGER primary key autoincrement, "
			+ TYPE + " INTEGER,"
			+ SHOW_YN + " TEXT,"
			+ SIDE + " INTEGER"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
			
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_ALL));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_ALL));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_ALL));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_ALL));
			
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_LEFT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_LEFT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_LEFT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_LEFT));
			
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_RIGHT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_RIGHT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_RIGHT));
			db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_RIGHT));
		}
		
		private static ContentValues getContentValues(int type, String showYN, int side){
			ContentValues values = new ContentValues();
			values.put(TYPE, type);
			values.put(SHOW_YN, showYN);
			values.put(SIDE, side);
			return values;
		}
	}
	
	/**
	 * CustomKeyset 테이블 구조.
	 */
	public static class CustomKeyset{
		
		public static final String TABLE_NAME = "customkeyset";
		public static final String _ID = "_id";
		public static final String NAME = "name";
		public static final String SHOW_4 = "show_4";
		public static final String SHOW_8 = "show_8";
		
		// 4방향일때 *10 해서 들어가고, 8방향일때는 *1 해서 들어간다.
		public static final int SHOW_NONE = 0;
		public static final int SHOW_ALL = 1;
		public static final int SHOW_LEFT = 2;
		public static final int SHOW_RIGHT = 3;
		
		public int mId;
		public String mName;
		public int mShow4;
		public int mShow8;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ _ID + " INTEGER primary key autoincrement, "
			+ NAME + " TEXT, "
			+ SHOW_4 + " INTEGER, "
			+ SHOW_8 + " INTEGER "
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
		}
	}
	
	/**
	 * CustomKeysetData 테이블 구조.
	 */
	public static class CustomKeysetData{
		
		public static final String TABLE_NAME = "customkeysetdata";
		public static final String _ID = "_id";
		public static final String CUSTOM_KEYSET_ID = "custom_keyset_id";
		public static final String KEYSET_NUMBER = "keyset_number";
		public static final String DATA = "data";
		
		public static final int KEYSET_NUMBER_LEFT_TOP = 0;
		public static final int KEYSET_NUMBER_MID_TOP = 1;
		public static final int KEYSET_NUMBER_RIGHT_TOP = 2;
		public static final int KEYSET_NUMBER_LEFT_MID = 3;
		public static final int KEYSET_NUMBER_RIGHT_MID = 4;
		public static final int KEYSET_NUMBER_LEFT_BOT = 5;
		public static final int KEYSET_NUMBER_MID_BOT = 6;
		public static final int KEYSET_NUMBER_RIGHT_BOT = 7;
		
		public int mId;
		public int mCustomKeysetId;
		public int mKeysetNumber;
		public String mData;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ _ID + " INTEGER primary key autoincrement, "
			+ CUSTOM_KEYSET_ID + " INTEGER,"
			+ KEYSET_NUMBER + " INTEGER,"
			+ DATA + " TEXT"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
		}
	}
	
	/**
	 * History 테이블 구조.
	 */
	public static class History {
		
		public static final String TABLE_NAME = "history";
		public static final String _ID = "_id";
		public static final String KEYBOARD = "keyboard";  // 키보드종류
		public static final String WORD = "word";  
		public static final String DATA = "data";  // 검색시 태그의 역할 ( 코드번호를 컴마로 구분 )
		public static final String USE_CNT = "use_cnt";
		
		public static final int KEYBOARD_HANGUL = 1;
		public static final int KEYBOARD_ETC = 2;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ _ID + " INTEGER primary key autoincrement, "
			+ KEYBOARD + " INTEGER,"
			+ WORD + " TEXT,"
			+ DATA + " TEXT,"
			+ USE_CNT + " INTEGER"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
		}
	}
	
	/**
	 * word 테이블 구조
	 */
	public static class Word {
		
		public static final String TABLE_NAME = "word";
		public static final String WORD = "word";
		public static final String COMPOSITION = "composition";
		public static final String USE_CNT_TOTAL = "use_cnt_total";
		public static final String USE_CNT_0 = "use_cnt_0";
		public static final String USE_CNT_4 = "use_cnt_4";
		public static final String USE_CNT_8 = "use_cnt_8";
		public static final String USE_CNT_12 = "use_cnt_12";
		public static final String USE_CNT_16 = "use_cnt_16";
		public static final String USE_CNT_20 = "use_cnt_20";
		public static final String USE_CNT_NORMAL = "use_cnt_normal";
		public static final String USE_CNT_EMAIL_ADDRESS = "use_cnt_address";
		public static final String USE_CNT_EMAIL_SUBJECT = "use_cnt_subject";
		public static final String USE_CNT_URI = "use_cnt_uri";
		public static final String USE_CNT_PERSON_NAME = "use_cnt_person_name";
		public static final String USE_CNT_POSTAL_ADDRESS = "use_cnt_postal_address";
		public static final String USE_CNT_NUMBER = "use_cnt_number";
		public static final String UPD_DT = "upd_dt";
		public static final String CRT_DT = "crt_dt";
		public static final String WORD_ID = "word_id";
		
		public String mWord, mComposition, mUpdDt, mCrtDt;
		public int mUseCntTotal, mUseCnt0, mUseCnt4, mUseCnt8, mUseCnt12, mUseCnt16, mUseCnt20;
		public int mUseCntNormal, mUseCntEmailAddress, mUseCntEmailSubject, mUseCntUri, mUseCntPersonName;
		public int mUseCntPostalAddress, mUseCntNumber, mWordId;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ WORD_ID + " INTEGER primary key autoincrement, "
			+ WORD + " TEXT,"
			+ COMPOSITION + " TEXT,"
			+ USE_CNT_TOTAL + " INTEGER,"
			+ USE_CNT_0 + " INTEGER,"
			+ USE_CNT_4 + " INTEGER,"
			+ USE_CNT_8 + " INTEGER,"
			+ USE_CNT_12 + " INTEGER,"
			+ USE_CNT_16 + " INTEGER,"
			+ USE_CNT_20 + " INTEGER,"
			+ USE_CNT_NORMAL + " INTEGER,"
			+ USE_CNT_EMAIL_ADDRESS + " INTEGER,"
			+ USE_CNT_EMAIL_SUBJECT + " INTEGER,"
			+ USE_CNT_URI + " INTEGER,"
			+ USE_CNT_PERSON_NAME + " INTEGER,"
			+ USE_CNT_POSTAL_ADDRESS + " INTEGER,"
			+ USE_CNT_NUMBER + " INTEGER,"
			+ UPD_DT + " TEXT,"
			+ CRT_DT + " TEXT"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
		}
	}
	
	/**
	 * next_word_group 테이블 구조
	 */
	public static class NextWordGroup {
		
		public static final String TABLE_NAME = "next_word_group";
		public static final String WORD = "word";
		public static final String NEXT_WORD = "next_word";
		public static final String USE_CNT = "use_cnt";
		
		public int mUseCnt;
		public String mWord, mNextWord;
		
		public static final String CREATE = 
			"CREATE TABLE " + TABLE_NAME +"( "
			+ WORD + " TEXT,"
			+ NEXT_WORD + " TEXT,"
			+ USE_CNT + " INTEGER"
			+ ");";
		
		public static void onCreate(SQLiteDatabase db){
			db.execSQL(CREATE);
		}
	}
	
}
