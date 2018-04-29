package com.teuskim.nlkeyboard;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingActivity extends Activity implements View.OnClickListener {
	
	private RadioGroup mGroupDirection;
	private RadioButton mBtnDirection4;
	private RadioButton mBtnDirection8;
	private LinearLayout mListViewLeft;
	private LinearLayout mListViewRight;
	private LinearLayout mListCustom;
	private TextView mListTitleLeft;
	private TextView mListTitleRight;
	private CheckBox mCheckboxVibrate;
	private CheckBox mCheckboxDupChosung;
	private CheckBox mCheckboxShift;
	
	private LayoutInflater mInflater;
	private NLKeyboardDb mDb;
	private NLPreference mPref;
	
	private BroadcastReceiver mSaveReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshCustomList();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		findViews();
		
		mDb = NLKeyboardDb.getInstance(getApplicationContext());
		mPref = new NLPreference(getApplicationContext());
		mInflater = LayoutInflater.from(getApplicationContext());
		
		refreshKeySetList();
		refreshCustomList();
		
		mCheckboxVibrate.setChecked(mPref.isVibrate());
		mCheckboxDupChosung.setChecked(mPref.useDupChosung());
		mCheckboxShift.setChecked(mPref.isShiftRemain());
	}
	
	private void findViews(){
		mGroupDirection = (RadioGroup) findViewById(R.id.group_direction);
		mBtnDirection4 = (RadioButton) findViewById(R.id.btn_direction_4);
		mBtnDirection8 = (RadioButton) findViewById(R.id.btn_direction_8);
		mListViewLeft = (LinearLayout) findViewById(R.id.list_left);
		mListViewRight = (LinearLayout) findViewById(R.id.list_right);
		mListCustom = (LinearLayout) findViewById(R.id.list_custom);
		mListTitleLeft = (TextView) findViewById(R.id.list_title_left);
		mListTitleRight = (TextView) findViewById(R.id.list_title_right);
		mCheckboxVibrate = (CheckBox) findViewById(R.id.checkbox_vibrate);
		mCheckboxDupChosung = (CheckBox) findViewById(R.id.checkbox_dupchosung);
		mCheckboxShift = (CheckBox) findViewById(R.id.checkbox_shift);
		
		RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {
			
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.btn_direction_4){
					mDb.updateDirection(4);
				}
				else{
					mDb.updateDirection(8);
				}
				refreshKeySetList();
				refreshCustomList();
			}
		};
		mGroupDirection.setOnCheckedChangeListener(listener);
		
		findViewById(R.id.btn_guide).setOnClickListener(this);
		findViewById(R.id.btn_close).setOnClickListener(this);
		findViewById(R.id.btn_add_custom).setOnClickListener(this);
		mCheckboxVibrate.setOnClickListener(this);
		mCheckboxDupChosung.setOnClickListener(this);
		mCheckboxShift.setOnClickListener(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(mSaveReceiver, new IntentFilter(RegisterCustomActivity.ACTION_SAVE_COMPLETE));
	}

	@Override
	protected void onStop() {
		unregisterReceiver(mSaveReceiver);
		super.onStop();
	}

	public void onClick(View v) {
		Intent i;
		switch(v.getId()){
		case R.id.btn_guide:
			i = new Intent(getApplicationContext(), GuideActivity.class);
			startActivity(i);
			break;
		case R.id.btn_close:
			finish();
			break;
		case R.id.btn_add_custom:
			i = new Intent(getApplicationContext(), RegisterCustomActivity.class);
			startActivityForResult(i, 0);
			break;
		case R.id.checkbox_vibrate:
			mPref.setIsVibrate(mCheckboxVibrate.isChecked());
			break;
		case R.id.checkbox_dupchosung:
			mPref.setUseDupChosung(mCheckboxDupChosung.isChecked());
			break;
		case R.id.checkbox_shift:
			mPref.setIsShiftRemain(mCheckboxShift.isChecked());
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// custom keyset 추가한 후 리프레쉬
		refreshCustomList();
	}

	private void refreshKeySetList(){
		int direction = mDb.getDirection();
		if(direction == 4){
			mBtnDirection4.setChecked(true);
			
			mListTitleLeft.setText(R.string.subtitle_keyboardset_all);
			mListTitleRight.setVisibility(View.GONE);
			mListViewRight.setVisibility(View.GONE);
			
			refreshKeySetList(mListViewLeft, mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_ALL));
		}			
		else{
			mBtnDirection8.setChecked(true);
			
			mListTitleLeft.setText(R.string.subtitle_keyboardset_left);
			mListTitleRight.setText(R.string.subtitle_keyboardset_right);
			mListTitleRight.setVisibility(View.VISIBLE);
			mListViewRight.setVisibility(View.VISIBLE);
			
			refreshKeySetList(mListViewLeft, mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_LEFT));
			refreshKeySetList(mListViewRight, mDb.getKeySetList(NLKeyboardDb.KeySet.SIDE_RIGHT));
		}
	}
	
	private void refreshKeySetList(LinearLayout listView, List<NLKeyboardDb.KeySet> list){
		listView.removeAllViews();
		
		for(final NLKeyboardDb.KeySet item : list){
			View v = mInflater.inflate(R.layout.setting_keyset_item, null);
			CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_keyset);
			checkbox.setText(mDb.getKeyboardName(item.mType));
			if("Y".equals(item.mShowYN))
				checkbox.setChecked(true);
			else
				checkbox.setChecked(false);
			
			checkbox.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					mDb.updateKeySetChecked(item.mId, ((CheckBox)v).isChecked());
					mPref.setKeyboardPosAll(0);
					mPref.setKeyboardPosLeft(0);
					mPref.setKeyboardPosRight(0);
				}
			});
			
			listView.addView(v);
		}		
	}
	
	private void refreshCustomList(){
		mListCustom.removeAllViews();
		final int direction = mDb.getDirection();
		List<NLKeyboardDb.CustomKeyset> list = mDb.getCustomKeySetList();
		
		for(final NLKeyboardDb.CustomKeyset item : list){
			View v = mInflater.inflate(R.layout.setting_custom_item, null);
			TextView name = (TextView) v.findViewById(R.id.custom_name);
			name.setText(item.mName);
			Button btnEdit = (Button) v.findViewById(R.id.btn_edit_custom);
			Button btnDelete = (Button) v.findViewById(R.id.btn_delete_custom);
			View.OnClickListener listener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					switch(v.getId()){
					case R.id.btn_edit_custom:
						Intent i = new Intent(getApplicationContext(), RegisterCustomActivity.class);
						i.putExtra("customKeySetId", item.mId);
						startActivityForResult(i, 0);
						break;
					case R.id.btn_delete_custom:
						dialogDeleteCustom(item.mId);
						break;
					}
				}
			};
			btnEdit.setOnClickListener(listener);
			btnDelete.setOnClickListener(listener);
			
			final CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_custom);
			final CheckBox checkboxRight = (CheckBox) v.findViewById(R.id.checkbox_custom_right);
			int show = mDb.getCustomKeySetShow(item.mId, direction);
			
			if(direction == 4){
				checkbox.setText(R.string.txt_left_right);
				checkboxRight.setVisibility(View.GONE);
				
				if(show == NLKeyboardDb.CustomKeyset.SHOW_NONE){
					checkbox.setChecked(false);
				}
				else{
					checkbox.setChecked(true);
				}
				checkbox.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(checkbox.isChecked()){
							mDb.updateCustomKeySetShow(item.mId, NLKeyboardDb.CustomKeyset.SHOW_ALL, direction);
						}
						else{
							mDb.updateCustomKeySetShow(item.mId, NLKeyboardDb.CustomKeyset.SHOW_NONE, direction);
						}
					}
				});
			}
			else{
				checkbox.setText(R.string.txt_left);
				checkboxRight.setText(R.string.txt_right);
				checkboxRight.setVisibility(View.VISIBLE);
				
				View.OnClickListener checkboxListener = new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						int show = NLKeyboardDb.CustomKeyset.SHOW_NONE;
						if(checkbox.isChecked() && checkboxRight.isChecked())
							show = NLKeyboardDb.CustomKeyset.SHOW_ALL;
						else if(checkbox.isChecked())
							show = NLKeyboardDb.CustomKeyset.SHOW_LEFT;
						else if(checkboxRight.isChecked())
							show = NLKeyboardDb.CustomKeyset.SHOW_RIGHT;
						
						mDb.updateCustomKeySetShow(item.mId, show, direction);
					}
				};
				
				if(show == NLKeyboardDb.CustomKeyset.SHOW_ALL || show == NLKeyboardDb.CustomKeyset.SHOW_LEFT){
					checkbox.setChecked(true);
				}
				else{
					checkbox.setChecked(false);
				}
				checkbox.setOnClickListener(checkboxListener);
				
				if(show == NLKeyboardDb.CustomKeyset.SHOW_ALL || show == NLKeyboardDb.CustomKeyset.SHOW_RIGHT){
					checkboxRight.setChecked(true);
				}
				else{
					checkboxRight.setChecked(false);
				}
				checkboxRight.setOnClickListener(checkboxListener);
			}
			
			mListCustom.addView(v);
		}
	}
	
	private void dialogDeleteCustom(final long itemId){
		new AlertDialog.Builder(this)
		.setMessage(R.string.alert_delete)
		.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDb.deleteCustomKeyset(itemId);
				refreshCustomList();
			}
		})
		.setNegativeButton(R.string.btn_cancel, null)
		.show();
	}
	
}
