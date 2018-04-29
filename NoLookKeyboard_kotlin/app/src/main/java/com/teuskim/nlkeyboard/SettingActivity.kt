package com.teuskim.nlkeyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

class SettingActivity : Activity(), View.OnClickListener {

    private var mGroupDirection: RadioGroup? = null
    private var mBtnDirection4: RadioButton? = null
    private var mBtnDirection8: RadioButton? = null
    private var mListViewLeft: LinearLayout? = null
    private var mListViewRight: LinearLayout? = null
    private var mListCustom: LinearLayout? = null
    private var mListTitleLeft: TextView? = null
    private var mListTitleRight: TextView? = null
    private var mCheckboxVibrate: CheckBox? = null
    private var mCheckboxDupChosung: CheckBox? = null
    private var mCheckboxShift: CheckBox? = null

    private var mInflater: LayoutInflater? = null
    private var mDb: NLKeyboardDb? = null
    private var mPref: NLPreference? = null

    private val mSaveReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            refreshCustomList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)
        findViews()

        mDb = NLKeyboardDb.getInstance(applicationContext)
        mPref = NLPreference(applicationContext)
        mInflater = LayoutInflater.from(applicationContext)

        refreshKeySetList()
        refreshCustomList()

        mCheckboxVibrate!!.isChecked = mPref!!.isVibrate
        mCheckboxDupChosung!!.isChecked = mPref!!.useDupChosung()
        mCheckboxShift!!.isChecked = mPref!!.isShiftRemain
    }

    private fun findViews() {
        mGroupDirection = findViewById<View>(R.id.group_direction) as RadioGroup
        mBtnDirection4 = findViewById<View>(R.id.btn_direction_4) as RadioButton
        mBtnDirection8 = findViewById<View>(R.id.btn_direction_8) as RadioButton
        mListViewLeft = findViewById<View>(R.id.list_left) as LinearLayout
        mListViewRight = findViewById<View>(R.id.list_right) as LinearLayout
        mListCustom = findViewById<View>(R.id.list_custom) as LinearLayout
        mListTitleLeft = findViewById<View>(R.id.list_title_left) as TextView
        mListTitleRight = findViewById<View>(R.id.list_title_right) as TextView
        mCheckboxVibrate = findViewById<View>(R.id.checkbox_vibrate) as CheckBox
        mCheckboxDupChosung = findViewById<View>(R.id.checkbox_dupchosung) as CheckBox
        mCheckboxShift = findViewById<View>(R.id.checkbox_shift) as CheckBox

        val listener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.btn_direction_4) {
                mDb!!.updateDirection(4)
            } else {
                mDb!!.updateDirection(8)
            }
            refreshKeySetList()
            refreshCustomList()
        }
        mGroupDirection!!.setOnCheckedChangeListener(listener)

        findViewById<View>(R.id.btn_guide).setOnClickListener(this)
        findViewById<View>(R.id.btn_close).setOnClickListener(this)
        findViewById<View>(R.id.btn_add_custom).setOnClickListener(this)
        mCheckboxVibrate!!.setOnClickListener(this)
        mCheckboxDupChosung!!.setOnClickListener(this)
        mCheckboxShift!!.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mSaveReceiver, IntentFilter(RegisterCustomActivity.ACTION_SAVE_COMPLETE))
    }

    override fun onStop() {
        unregisterReceiver(mSaveReceiver)
        super.onStop()
    }

    override fun onClick(v: View) {
        val i: Intent
        when (v.id) {
            R.id.btn_guide -> {
                i = Intent(applicationContext, GuideActivity::class.java)
                startActivity(i)
            }
            R.id.btn_close -> finish()
            R.id.btn_add_custom -> {
                i = Intent(applicationContext, RegisterCustomActivity::class.java)
                startActivityForResult(i, 0)
            }
            R.id.checkbox_vibrate -> mPref!!.isVibrate = mCheckboxVibrate!!.isChecked
            R.id.checkbox_dupchosung -> mPref!!.setUseDupChosung(mCheckboxDupChosung!!.isChecked)
            R.id.checkbox_shift -> mPref!!.isShiftRemain = mCheckboxShift!!.isChecked
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // custom keyset 추가한 후 리프레쉬
        refreshCustomList()
    }

    private fun refreshKeySetList() {
        val direction = mDb!!.direction
        if (direction == 4) {
            mBtnDirection4!!.isChecked = true

            mListTitleLeft!!.setText(R.string.subtitle_keyboardset_all)
            mListTitleRight!!.visibility = View.GONE
            mListViewRight!!.visibility = View.GONE

            refreshKeySetList(mListViewLeft, mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_ALL))
        } else {
            mBtnDirection8!!.isChecked = true

            mListTitleLeft!!.setText(R.string.subtitle_keyboardset_left)
            mListTitleRight!!.setText(R.string.subtitle_keyboardset_right)
            mListTitleRight!!.visibility = View.VISIBLE
            mListViewRight!!.visibility = View.VISIBLE

            refreshKeySetList(mListViewLeft, mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_LEFT))
            refreshKeySetList(mListViewRight, mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_RIGHT))
        }
    }

    private fun refreshKeySetList(listView: LinearLayout?, list: List<NLKeyboardDb.KeySet>) {
        listView!!.removeAllViews()

        for (item in list) {
            val v = mInflater!!.inflate(R.layout.setting_keyset_item, null)
            val checkbox = v.findViewById<View>(R.id.checkbox_keyset) as CheckBox
            checkbox.text = mDb!!.getKeyboardName(item.mType)
            if ("Y" == item.mShowYN)
                checkbox.isChecked = true
            else
                checkbox.isChecked = false

            checkbox.setOnClickListener { v ->
                mDb!!.updateKeySetChecked(item.mId, (v as CheckBox).isChecked)
                mPref!!.keyboardPosAll = 0
                mPref!!.keyboardPosLeft = 0
                mPref!!.keyboardPosRight = 0
            }

            listView.addView(v)
        }
    }

    private fun refreshCustomList() {
        mListCustom!!.removeAllViews()
        val direction = mDb!!.direction
        val list = mDb!!.customKeySetList

        for (item in list) {
            val v = mInflater!!.inflate(R.layout.setting_custom_item, null)
            val name = v.findViewById<View>(R.id.custom_name) as TextView
            name.text = item.mName
            val btnEdit = v.findViewById<View>(R.id.btn_edit_custom) as Button
            val btnDelete = v.findViewById<View>(R.id.btn_delete_custom) as Button
            val listener = View.OnClickListener { v ->
                when (v.id) {
                    R.id.btn_edit_custom -> {
                        val i = Intent(applicationContext, RegisterCustomActivity::class.java)
                        i.putExtra("customKeySetId", item.mId)
                        startActivityForResult(i, 0)
                    }
                    R.id.btn_delete_custom -> dialogDeleteCustom(item.mId.toLong())
                }
            }
            btnEdit.setOnClickListener(listener)
            btnDelete.setOnClickListener(listener)

            val checkbox = v.findViewById<View>(R.id.checkbox_custom) as CheckBox
            val checkboxRight = v.findViewById<View>(R.id.checkbox_custom_right) as CheckBox
            val show = mDb!!.getCustomKeySetShow(item.mId, direction)

            if (direction == 4) {
                checkbox.setText(R.string.txt_left_right)
                checkboxRight.visibility = View.GONE

                if (show == NLKeyboardDb.CustomKeyset.SHOW_NONE) {
                    checkbox.isChecked = false
                } else {
                    checkbox.isChecked = true
                }
                checkbox.setOnClickListener {
                    if (checkbox.isChecked) {
                        mDb!!.updateCustomKeySetShow(item.mId, NLKeyboardDb.CustomKeyset.SHOW_ALL, direction)
                    } else {
                        mDb!!.updateCustomKeySetShow(item.mId, NLKeyboardDb.CustomKeyset.SHOW_NONE, direction)
                    }
                }
            } else {
                checkbox.setText(R.string.txt_left)
                checkboxRight.setText(R.string.txt_right)
                checkboxRight.visibility = View.VISIBLE

                val checkboxListener = View.OnClickListener {
                    var show = NLKeyboardDb.CustomKeyset.SHOW_NONE
                    if (checkbox.isChecked && checkboxRight.isChecked)
                        show = NLKeyboardDb.CustomKeyset.SHOW_ALL
                    else if (checkbox.isChecked)
                        show = NLKeyboardDb.CustomKeyset.SHOW_LEFT
                    else if (checkboxRight.isChecked)
                        show = NLKeyboardDb.CustomKeyset.SHOW_RIGHT

                    mDb!!.updateCustomKeySetShow(item.mId, show, direction)
                }

                if (show == NLKeyboardDb.CustomKeyset.SHOW_ALL || show == NLKeyboardDb.CustomKeyset.SHOW_LEFT) {
                    checkbox.isChecked = true
                } else {
                    checkbox.isChecked = false
                }
                checkbox.setOnClickListener(checkboxListener)

                if (show == NLKeyboardDb.CustomKeyset.SHOW_ALL || show == NLKeyboardDb.CustomKeyset.SHOW_RIGHT) {
                    checkboxRight.isChecked = true
                } else {
                    checkboxRight.isChecked = false
                }
                checkboxRight.setOnClickListener(checkboxListener)
            }

            mListCustom!!.addView(v)
        }
    }

    private fun dialogDeleteCustom(itemId: Long) {
        AlertDialog.Builder(this)
                .setMessage(R.string.alert_delete)
                .setPositiveButton(R.string.btn_ok) { dialog, which ->
                    mDb!!.deleteCustomKeyset(itemId)
                    refreshCustomList()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

}
