package com.teuskim.nlkeyboard

import android.content.Context
import android.content.SharedPreferences

class NLPreference(context: Context) {

    private val mPref: SharedPreferences
    private val mEditor: SharedPreferences.Editor

    var keyboardPosLeft: Int
        get() = mPref.getInt(KEY_KEYBOARD_POS_LEFT, 0)
        set(keyboardPos) {
            mEditor.putInt(KEY_KEYBOARD_POS_LEFT, keyboardPos)
            mEditor.commit()
        }

    var keyboardPosRight: Int
        get() = mPref.getInt(KEY_KEYBOARD_POS_RIGHT, 0)
        set(keyboardPos) {
            mEditor.putInt(KEY_KEYBOARD_POS_RIGHT, keyboardPos)
            mEditor.commit()
        }

    var keyboardPosAll: Int
        get() = mPref.getInt(KEY_KEYBOARD_POS_ALL, 0)
        set(keyboardPos) {
            mEditor.putInt(KEY_KEYBOARD_POS_ALL, keyboardPos)
            mEditor.commit()
        }

    var isVibrate: Boolean
        get() = mPref.getBoolean(KEY_VIBRATE, true)
        set(isVibrate) {
            mEditor.putBoolean(KEY_VIBRATE, isVibrate)
            mEditor.commit()
        }

    var isShiftRemain: Boolean
        get() = mPref.getBoolean(KEY_SHIFT_REMAIN, true)
        set(isShiftRemain) {
            mEditor.putBoolean(KEY_SHIFT_REMAIN, isShiftRemain)
            mEditor.commit()
        }

    init {
        mPref = context.getSharedPreferences("nlpref", 0)
        mEditor = mPref.edit()
    }

    fun didReadGuide(): Boolean {
        return mPref.getBoolean(KEY_DID_READ_GUIDE, false)
    }

    fun setDidReadGuide(didReadGuide: Boolean) {
        mEditor.putBoolean(KEY_DID_READ_GUIDE, didReadGuide)
        mEditor.commit()
    }

    fun useDupChosung(): Boolean {
        return mPref.getBoolean(KEY_DUPCHOSUNG, true)
    }

    fun setUseDupChosung(useDupChosung: Boolean) {
        mEditor.putBoolean(KEY_DUPCHOSUNG, useDupChosung)
        mEditor.commit()
    }

    companion object {

        private val KEY_DID_READ_GUIDE = "didreadguide"
        private val KEY_KEYBOARD_POS_LEFT = "keyboardposleft"
        private val KEY_KEYBOARD_POS_RIGHT = "keyboardposright"
        private val KEY_KEYBOARD_POS_ALL = "keyboardposall"
        private val KEY_VIBRATE = "vibrate"
        private val KEY_DUPCHOSUNG = "dupchosung"
        private val KEY_SHIFT_REMAIN = "shiftremain"
    }

}
