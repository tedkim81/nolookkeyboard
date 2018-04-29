package com.teuskim.nlkeyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterCustomActivity extends Activity implements View.OnClickListener {
	
	public static final int LEFT_TOP = 0;
	public static final int MID_TOP = 1;
	public static final int RIGHT_TOP = 2;
	public static final int LEFT_MID = 3;
	public static final int RIGHT_MID = 4;
	public static final int LEFT_BOT = 5;
	public static final int MID_BOT = 6;
	public static final int RIGHT_BOT = 7;
	public static Map<Integer, Integer> sTitleMap = new HashMap<Integer, Integer>();
	static{
		sTitleMap.put(LEFT_TOP, R.string.txt_keymap_left_top);
		sTitleMap.put(MID_TOP, R.string.txt_keymap_mid_top);
		sTitleMap.put(RIGHT_TOP, R.string.txt_keymap_right_top);
		sTitleMap.put(LEFT_MID, R.string.txt_keymap_left_mid);
		sTitleMap.put(RIGHT_MID, R.string.txt_keymap_right_mid);
		sTitleMap.put(LEFT_BOT, R.string.txt_keymap_left_bottom);
		sTitleMap.put(MID_BOT, R.string.txt_keymap_mid_bottom);
		sTitleMap.put(RIGHT_BOT, R.string.txt_keymap_right_bottom);
	}
	
	private NLKeyboardDb mDb;
	private EditText mNameCustom;
	private RegisterCustomKeyboardView mKeyboardView;
	private Map<Integer, List<String>> mKeysetMap;
	private LinearLayout mKeymapListView;
	private Button mBtnRecommend;
	private Button mBtnReset;
	private LayoutInflater mInflater;
	private int mCustomKeysetId;
	
	public static final String ACTION_SAVE_COMPLETE = "action_save_complete";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_custom);
		findViews();
		
		mDb = NLKeyboardDb.getInstance(getApplicationContext());
		mKeysetMap = new HashMap<Integer, List<String>>();
		mInflater = LayoutInflater.from(getApplicationContext());
		
		mCustomKeysetId = getIntent().getIntExtra("customKeySetId", 0);
		if(mCustomKeysetId > 0){
			String name = mDb.getCustomKeySetName(mCustomKeysetId);
			mNameCustom.setText(name);
			
			List<NLKeyboardDb.CustomKeysetData> cksdList = mDb.getCustomKeySetDataList(mCustomKeysetId);
			for(NLKeyboardDb.CustomKeysetData cksd : cksdList){
				List<String> dataList;
				if(mKeysetMap.containsKey(cksd.mKeysetNumber) == false){
					dataList = new ArrayList<String>();
					mKeysetMap.put(cksd.mKeysetNumber, dataList);
				}
				else{
					dataList = mKeysetMap.get(cksd.mKeysetNumber);
				}
				
				dataList.add(cksd.mData);
			}
			
			refreshList();
		}
	}
	
	protected void findViews(){
		mNameCustom = (EditText) findViewById(R.id.name_custom);
		mKeyboardView = (RegisterCustomKeyboardView) findViewById(R.id.keyboard_view);
		mKeymapListView = (LinearLayout) findViewById(R.id.keymap_list);
		mBtnRecommend = (Button) findViewById(R.id.btn_custom_recommend);
		mBtnReset = (Button) findViewById(R.id.btn_custom_reset);
		
		mKeyboardView.setOnClickListener(this);
		findViewById(R.id.btn_save).setOnClickListener(this);
		findViewById(R.id.btn_close).setOnClickListener(this);
		mBtnRecommend.setOnClickListener(this);
		mBtnReset.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.keyset_left_top:
			dialogKeySetInput(LEFT_TOP);
			break;
		case R.id.keyset_mid_top:
			dialogKeySetInput(MID_TOP);
			break;
		case R.id.keyset_right_top:
			dialogKeySetInput(RIGHT_TOP);
			break;
		case R.id.keyset_left_mid:
			dialogKeySetInput(LEFT_MID);
			break;
		case R.id.keyset_right_mid:
			dialogKeySetInput(RIGHT_MID);
			break;
		case R.id.keyset_left_bot:
			dialogKeySetInput(LEFT_BOT);
			break;
		case R.id.keyset_mid_bot:
			dialogKeySetInput(MID_BOT);
			break;
		case R.id.keyset_right_bot:
			dialogKeySetInput(RIGHT_BOT);
			break;
		case R.id.btn_save:
			save();
			break;
		case R.id.btn_close:
			finish();
			break;
		case R.id.btn_custom_recommend:
			dialogRecommendedCustom();
			break;
		case R.id.btn_custom_reset:
			reset();
			break;
		}
	}

	private void dialogKeySetInput(final int keysetNumber){
		LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		View layout = inflater.inflate(R.layout.register_keyset_dialog, null);
		final EditText edittext = (EditText) layout.findViewById(R.id.input_keyset);
		
		new AlertDialog.Builder(this)
			.setTitle(sTitleMap.get(keysetNumber))
			.setView(layout)
			.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					String data = edittext.getText().toString();
					if(data != null && data.length() > 0){
						List<String> list;
						if(mKeysetMap.containsKey(keysetNumber) == false){
							list = new ArrayList<String>();
							mKeysetMap.put(keysetNumber, list);
						}
						else{
							list = mKeysetMap.get(keysetNumber);
						}
						list.add(data);
						
						refreshList();
					}
				}
			})
			.setNeutralButton(R.string.btn_special, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialogKeySetInputSpecial(keysetNumber);
				}
				
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.create().show();
	}
	
	private void dialogKeySetInputSpecial(final int keysetNumber){
		CharSequence[] cs = new CharSequence[]{getString(R.string.txt_backspacekey)
												,getString(R.string.txt_spacebar)
												,getString(R.string.txt_enterkey)};
		new AlertDialog.Builder(this)
			.setTitle(sTitleMap.get(keysetNumber))
			.setItems(cs, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String data = "";
					switch(which){
					case 0:
						data = "\b";
						break;
					case 1:
						data = " ";
						break;
					case 2:
						data = "\n";
						break;
					}
					
					List<String> list;
					if(mKeysetMap.containsKey(keysetNumber) == false){
						list = new ArrayList<String>();
						mKeysetMap.put(keysetNumber, list);
					}
					else{
						list = mKeysetMap.get(keysetNumber);
					}
					list.add(data);
					dialog.dismiss();
					refreshList();
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.create().show();
	}
	
	private void save(){
		String name = mNameCustom.getText().toString();
		if(name == null || name.length() == 0){
			Toast.makeText(getApplicationContext(), R.string.toast_input_title, Toast.LENGTH_SHORT).show();
			return;
		}
		
		SaveTask task = new SaveTask();
		task.execute(name);
		
		finish();
	}
	
	private class SaveTask extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			String name = params[0];
			int show = NLKeyboardDb.CustomKeyset.SHOW_NONE;
			int direction = mDb.getDirection();
			if(mCustomKeysetId > 0){
				show = mDb.getCustomKeySetShow(mCustomKeysetId, direction);
				if(mDb.deleteCustomKeyset(mCustomKeysetId) == false){
					mDb.deleteCustomKeyset(mCustomKeysetId);
				}
			}		
			return mDb.insertCustomKeyset(name, show, direction
					, mKeysetMap.get(LEFT_TOP), mKeysetMap.get(MID_TOP), mKeysetMap.get(RIGHT_TOP)
					, mKeysetMap.get(LEFT_MID), mKeysetMap.get(RIGHT_MID)
					, mKeysetMap.get(LEFT_BOT), mKeysetMap.get(MID_BOT), mKeysetMap.get(RIGHT_BOT));
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result)
				Toast.makeText(getApplicationContext(), R.string.toast_save_ok, Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), R.string.toast_save_fail, Toast.LENGTH_SHORT).show();
			sendBroadcast(new Intent(ACTION_SAVE_COMPLETE));
		}
		
	}
	
	private void refreshList(){
		mKeymapListView.removeAllViews();
		
		refreshList(LEFT_TOP);
		refreshList(MID_TOP);
		refreshList(RIGHT_TOP);
		refreshList(LEFT_MID);
		refreshList(RIGHT_MID);
		refreshList(LEFT_BOT);
		refreshList(MID_BOT);
		refreshList(RIGHT_BOT);
		
		if(mKeysetMap.size() > 0)
			mBtnReset.setVisibility(View.VISIBLE);
		else
			mBtnReset.setVisibility(View.GONE);
	}
	
	private void refreshList(final int keysetNumber){
		List<String> list = mKeysetMap.get(keysetNumber);
		
		fillKeyset(keysetNumber, list);
		
		View titleView = mInflater.inflate(R.layout.register_custom_title_item, null);
		TextView titleTextView = (TextView) titleView.findViewById(R.id.custom_title);
		titleTextView.setText(sTitleMap.get(keysetNumber));
		mKeymapListView.addView(titleView);
		
		if(list != null && list.size() > 0){
			for(int i=0; i<list.size(); i++){
				String data = list.get(i);
				final int j = i;
				View v = mInflater.inflate(R.layout.register_custom_item, null);
				TextView tv = (TextView) v.findViewById(R.id.custom_data);
				if("\b".equals(data))
					tv.setText(R.string.txt_backspacekey);
				else if(" ".equals(data))
					tv.setText(R.string.txt_spacebar);
				else if("\n".equals(data))
					tv.setText(R.string.txt_enterkey);
				else
					tv.setText(data);
				Button btn = (Button) v.findViewById(R.id.btn_delete);
				btn.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						List<String> list = mKeysetMap.get(keysetNumber);
						list.remove(j);
						refreshList();
					}
				});
				mKeymapListView.addView(v);
			}
		}
		
		mInflater.inflate(R.layout.register_custom_divider, mKeymapListView);
	}
	
	private void fillKeyset(int keysetNumber, List<String> keys){
		String key1 = "";
		String key2 = "";
		String key3 = "";
		String key4 = "";
		if(keys != null){
			if(keys.size() > 0)
				key1 = keys.get(0).substring(0, 1);
			if(keys.size() > 1)
				key2 = keys.get(1).substring(0, 1);
			if(keys.size() > 2)
				key3 = keys.get(2).substring(0, 1);
			if(keys.size() > 3)
				key4 = keys.get(3).substring(0, 1);
		}
		
		switch(keysetNumber){
		case LEFT_TOP:
			mKeyboardView.fillKeys(R.id.keyset_left_top, key1, key2, key3, key4);
			break;
		case MID_TOP:
			mKeyboardView.fillKeys(R.id.keyset_mid_top, key1, key2, key3, key4);
			break;
		case RIGHT_TOP:
			mKeyboardView.fillKeys(R.id.keyset_right_top, key1, key2, key3, key4);
			break;
		case LEFT_MID:
			mKeyboardView.fillKeys(R.id.keyset_left_mid, key1, key2, key3, key4);
			break;
		case RIGHT_MID:
			mKeyboardView.fillKeys(R.id.keyset_right_mid, key1, key2, key3, key4);
			break;
		case LEFT_BOT:
			mKeyboardView.fillKeys(R.id.keyset_left_bot, key1, key2, key3, key4);
			break;
		case MID_BOT:
			mKeyboardView.fillKeys(R.id.keyset_mid_bot, key1, key2, key3, key4);
			break;
		case RIGHT_BOT:
			mKeyboardView.fillKeys(R.id.keyset_right_bot, key1, key2, key3, key4);
			break;
		}
	}
	
	private void dialogRecommendedCustom(){
		new AlertDialog.Builder(this)
			.setTitle(R.string.btn_custom_recommend)
			.setItems(R.array.custom_names, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Resources res = getResources();
					String[] names = res.getStringArray(R.array.custom_names);
					mNameCustom.setText(names[which]);
					
					String[] strs = null;
					switch(which){
					case 0:
						strs = res.getStringArray(R.array.ex1_symbols);
						break;
					case 1:
						strs = res.getStringArray(R.array.ex2_emoticons);
						break;
					case 2:
						strs = res.getStringArray(R.array.ex3_hangul_2nd);
						break;
					}
					if(strs != null && strs.length > 0){
						mKeysetMap.clear();
						for(int i=0; i<strs.length; i++){
							List<String> dataList;
							int keysetNumber = 7 - (i / 4);
							if(mKeysetMap.containsKey(keysetNumber) == false){
								dataList = new ArrayList<String>();
								mKeysetMap.put(keysetNumber, dataList);
							}
							else{
								dataList = mKeysetMap.get(keysetNumber);
							}
							
							dataList.add(strs[i]);
						}
					}
					refreshList();
				}
			})
			.create().show();
	}
	
	private void reset(){
		mNameCustom.setText(null);
		mKeysetMap.clear();
		refreshList();
	}

}
