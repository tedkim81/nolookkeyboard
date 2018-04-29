package com.teuskim.nlkeyboard

import java.util.ArrayList
import java.util.HashMap

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class RegisterCustomActivity : Activity(), View.OnClickListener {

    private var mDb: NLKeyboardDb? = null
    private var mNameCustom: EditText? = null
    private var mKeyboardView: RegisterCustomKeyboardView? = null
    private var mKeysetMap: MutableMap<Int, List<String>>? = null
    private var mKeymapListView: LinearLayout? = null
    private var mBtnRecommend: Button? = null
    private var mBtnReset: Button? = null
    private var mInflater: LayoutInflater? = null
    private var mCustomKeysetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_custom)
        findViews()

        mDb = NLKeyboardDb.getInstance(applicationContext)
        mKeysetMap = HashMap()
        mInflater = LayoutInflater.from(applicationContext)

        mCustomKeysetId = intent.getIntExtra("customKeySetId", 0)
        if (mCustomKeysetId > 0) {
            val name = mDb!!.getCustomKeySetName(mCustomKeysetId)
            mNameCustom!!.setText(name)

            val cksdList = mDb!!.getCustomKeySetDataList(mCustomKeysetId)
            for (cksd in cksdList) {
                val dataList: MutableList<String>
                if (mKeysetMap!!.containsKey(cksd.mKeysetNumber) == false) {
                    dataList = ArrayList()
                    mKeysetMap!![cksd.mKeysetNumber] = dataList
                } else {
                    dataList = mKeysetMap!![cksd.mKeysetNumber]!!.toMutableList()
                }

                cksd.mData?.let { dataList.add(it) }
            }

            refreshList()
        }
    }

    protected fun findViews() {
        mNameCustom = findViewById<View>(R.id.name_custom) as EditText
        mKeyboardView = findViewById<View>(R.id.keyboard_view) as RegisterCustomKeyboardView
        mKeymapListView = findViewById<View>(R.id.keymap_list) as LinearLayout
        mBtnRecommend = findViewById<View>(R.id.btn_custom_recommend) as Button
        mBtnReset = findViewById<View>(R.id.btn_custom_reset) as Button

        mKeyboardView!!.setOnClickListener(this)
        findViewById<View>(R.id.btn_save).setOnClickListener(this)
        findViewById<View>(R.id.btn_close).setOnClickListener(this)
        mBtnRecommend!!.setOnClickListener(this)
        mBtnReset!!.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.keyset_left_top -> dialogKeySetInput(LEFT_TOP)
            R.id.keyset_mid_top -> dialogKeySetInput(MID_TOP)
            R.id.keyset_right_top -> dialogKeySetInput(RIGHT_TOP)
            R.id.keyset_left_mid -> dialogKeySetInput(LEFT_MID)
            R.id.keyset_right_mid -> dialogKeySetInput(RIGHT_MID)
            R.id.keyset_left_bot -> dialogKeySetInput(LEFT_BOT)
            R.id.keyset_mid_bot -> dialogKeySetInput(MID_BOT)
            R.id.keyset_right_bot -> dialogKeySetInput(RIGHT_BOT)
            R.id.btn_save -> save()
            R.id.btn_close -> finish()
            R.id.btn_custom_recommend -> dialogRecommendedCustom()
            R.id.btn_custom_reset -> reset()
        }
    }

    private fun dialogKeySetInput(keysetNumber: Int) {
        val inflater = LayoutInflater.from(applicationContext)
        val layout = inflater.inflate(R.layout.register_keyset_dialog, null)
        val edittext = layout.findViewById<View>(R.id.input_keyset) as EditText

        AlertDialog.Builder(this)
                .setTitle(sTitleMap[keysetNumber]!!)
                .setView(layout)
                .setPositiveButton(R.string.btn_ok) { dialog, which ->
                    val data = edittext.text.toString()
                    if (data != null && data.length > 0) {
                        val list: MutableList<String>
                        if (mKeysetMap!!.containsKey(keysetNumber) == false) {
                            list = ArrayList()
                            mKeysetMap!![keysetNumber] = list
                        } else {
                            list = mKeysetMap!![keysetNumber]!!.toMutableList()
                        }
                        list.add(data)

                        refreshList()
                    }
                }
                .setNeutralButton(R.string.btn_special) { dialog, which -> dialogKeySetInputSpecial(keysetNumber) }
                .setNegativeButton(R.string.btn_cancel, null)
                .create().show()
    }

    private fun dialogKeySetInputSpecial(keysetNumber: Int) {
        val cs = arrayOf<CharSequence>(getString(R.string.txt_backspacekey), getString(R.string.txt_spacebar), getString(R.string.txt_enterkey))
        AlertDialog.Builder(this)
                .setTitle(sTitleMap[keysetNumber]!!)
                .setItems(cs) { dialog, which ->
                    var data = ""
                    when (which) {
                        0 -> data = "\b"
                        1 -> data = " "
                        2 -> data = "\n"
                    }

                    val list: MutableList<String>
                    if (mKeysetMap!!.containsKey(keysetNumber) == false) {
                        list = ArrayList()
                        mKeysetMap!![keysetNumber] = list
                    } else {
                        list = mKeysetMap!![keysetNumber]!!.toMutableList()
                    }
                    list.add(data)
                    dialog.dismiss()
                    refreshList()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .create().show()
    }

    private fun save() {
        val name = mNameCustom!!.text.toString()
        if (name == null || name.length == 0) {
            Toast.makeText(applicationContext, R.string.toast_input_title, Toast.LENGTH_SHORT).show()
            return
        }

        val task = SaveTask()
        task.execute(name)

        finish()
    }

    private inner class SaveTask : AsyncTask<String, Int, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val name = params[0]
            var show = NLKeyboardDb.CustomKeyset.SHOW_NONE
            val direction = mDb!!.direction
            if (mCustomKeysetId > 0) {
                show = mDb!!.getCustomKeySetShow(mCustomKeysetId, direction)
                if (mDb!!.deleteCustomKeyset(mCustomKeysetId.toLong()) == false) {
                    mDb!!.deleteCustomKeyset(mCustomKeysetId.toLong())
                }
            }
            return mDb!!.insertCustomKeyset(name, show, direction, mKeysetMap!![LEFT_TOP], mKeysetMap!![MID_TOP], mKeysetMap!![RIGHT_TOP], mKeysetMap!![LEFT_MID], mKeysetMap!![RIGHT_MID], mKeysetMap!![LEFT_BOT], mKeysetMap!![MID_BOT], mKeysetMap!![RIGHT_BOT])
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!)
                Toast.makeText(applicationContext, R.string.toast_save_ok, Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(applicationContext, R.string.toast_save_fail, Toast.LENGTH_SHORT).show()
            sendBroadcast(Intent(ACTION_SAVE_COMPLETE))
        }

    }

    private fun refreshList() {
        mKeymapListView!!.removeAllViews()

        refreshList(LEFT_TOP)
        refreshList(MID_TOP)
        refreshList(RIGHT_TOP)
        refreshList(LEFT_MID)
        refreshList(RIGHT_MID)
        refreshList(LEFT_BOT)
        refreshList(MID_BOT)
        refreshList(RIGHT_BOT)

        if (mKeysetMap!!.size > 0)
            mBtnReset!!.visibility = View.VISIBLE
        else
            mBtnReset!!.visibility = View.GONE
    }

    private fun refreshList(keysetNumber: Int) {
        val list = mKeysetMap!![keysetNumber]

        fillKeyset(keysetNumber, list)

        val titleView = mInflater!!.inflate(R.layout.register_custom_title_item, null)
        val titleTextView = titleView.findViewById<View>(R.id.custom_title) as TextView
        titleTextView.setText(sTitleMap[keysetNumber]!!)
        mKeymapListView!!.addView(titleView)

        if (list != null && list.size > 0) {
            for (i in list.indices) {
                val data = list[i]
                val v = mInflater!!.inflate(R.layout.register_custom_item, null)
                val tv = v.findViewById<View>(R.id.custom_data) as TextView
                if ("\b" == data)
                    tv.setText(R.string.txt_backspacekey)
                else if (" " == data)
                    tv.setText(R.string.txt_spacebar)
                else if ("\n" == data)
                    tv.setText(R.string.txt_enterkey)
                else
                    tv.text = data
                val btn = v.findViewById<View>(R.id.btn_delete) as Button
                btn.setOnClickListener {
                    mKeysetMap!![keysetNumber]!!.toMutableList().removeAt(i)
                    refreshList()
                }
                mKeymapListView!!.addView(v)
            }
        }

        mInflater!!.inflate(R.layout.register_custom_divider, mKeymapListView)
    }

    private fun fillKeyset(keysetNumber: Int, keys: List<String>?) {
        var key1 = ""
        var key2 = ""
        var key3 = ""
        var key4 = ""
        if (keys != null) {
            if (keys.size > 0)
                key1 = keys[0].substring(0, 1)
            if (keys.size > 1)
                key2 = keys[1].substring(0, 1)
            if (keys.size > 2)
                key3 = keys[2].substring(0, 1)
            if (keys.size > 3)
                key4 = keys[3].substring(0, 1)
        }

        when (keysetNumber) {
            LEFT_TOP -> mKeyboardView!!.fillKeys(R.id.keyset_left_top, key1, key2, key3, key4)
            MID_TOP -> mKeyboardView!!.fillKeys(R.id.keyset_mid_top, key1, key2, key3, key4)
            RIGHT_TOP -> mKeyboardView!!.fillKeys(R.id.keyset_right_top, key1, key2, key3, key4)
            LEFT_MID -> mKeyboardView!!.fillKeys(R.id.keyset_left_mid, key1, key2, key3, key4)
            RIGHT_MID -> mKeyboardView!!.fillKeys(R.id.keyset_right_mid, key1, key2, key3, key4)
            LEFT_BOT -> mKeyboardView!!.fillKeys(R.id.keyset_left_bot, key1, key2, key3, key4)
            MID_BOT -> mKeyboardView!!.fillKeys(R.id.keyset_mid_bot, key1, key2, key3, key4)
            RIGHT_BOT -> mKeyboardView!!.fillKeys(R.id.keyset_right_bot, key1, key2, key3, key4)
        }
    }

    private fun dialogRecommendedCustom() {
        AlertDialog.Builder(this)
                .setTitle(R.string.btn_custom_recommend)
                .setItems(R.array.custom_names) { dialog, which ->
                    val res = resources
                    val names = res.getStringArray(R.array.custom_names)
                    mNameCustom!!.setText(names[which])

                    var strs: Array<String>? = null
                    when (which) {
                        0 -> strs = res.getStringArray(R.array.ex1_symbols)
                        1 -> strs = res.getStringArray(R.array.ex2_emoticons)
                        2 -> strs = res.getStringArray(R.array.ex3_hangul_2nd)
                    }
                    if (strs != null && strs.size > 0) {
                        mKeysetMap!!.clear()
                        for (i in strs.indices) {
                            val dataList: MutableList<String>
                            val keysetNumber = 7 - i / 4
                            if (mKeysetMap!!.containsKey(keysetNumber) == false) {
                                dataList = ArrayList()
                                mKeysetMap!![keysetNumber] = dataList
                            } else {
                                dataList = mKeysetMap!![keysetNumber]!!.toMutableList()
                            }

                            dataList.add(strs[i])
                        }
                    }
                    refreshList()
                }
                .create().show()
    }

    private fun reset() {
        mNameCustom!!.text = null
        mKeysetMap!!.clear()
        refreshList()
    }

    companion object {

        val LEFT_TOP = 0
        val MID_TOP = 1
        val RIGHT_TOP = 2
        val LEFT_MID = 3
        val RIGHT_MID = 4
        val LEFT_BOT = 5
        val MID_BOT = 6
        val RIGHT_BOT = 7
        var sTitleMap: MutableMap<Int, Int> = HashMap()

        init {
            sTitleMap[LEFT_TOP] = R.string.txt_keymap_left_top
            sTitleMap[MID_TOP] = R.string.txt_keymap_mid_top
            sTitleMap[RIGHT_TOP] = R.string.txt_keymap_right_top
            sTitleMap[LEFT_MID] = R.string.txt_keymap_left_mid
            sTitleMap[RIGHT_MID] = R.string.txt_keymap_right_mid
            sTitleMap[LEFT_BOT] = R.string.txt_keymap_left_bottom
            sTitleMap[MID_BOT] = R.string.txt_keymap_mid_bottom
            sTitleMap[RIGHT_BOT] = R.string.txt_keymap_right_bottom
        }

        val ACTION_SAVE_COMPLETE = "action_save_complete"
    }

}
