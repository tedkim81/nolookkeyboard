package com.teuskim.nlkeyboard

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.teuskim.nlkeyboard.NLKeyboard.KeySet

class KeySetView : LinearLayout {

    private var mKeyView1: TextView? = null
    private var mKeyView2: TextView? = null
    private var mKeyView3: TextView? = null
    private var mKeyView4: TextView? = null
    private var mKeyViewIcon1: ImageView? = null
    private var mKeyViewIcon2: ImageView? = null
    private var mKeyViewIcon3: ImageView? = null
    private var mKeyViewIcon4: ImageView? = null

    private var mSize = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.keyset, this)

        mKeyView1 = findViewById<View>(R.id.key_1) as TextView
        mKeyView2 = findViewById<View>(R.id.key_2) as TextView
        mKeyView3 = findViewById<View>(R.id.key_3) as TextView
        mKeyView4 = findViewById<View>(R.id.key_4) as TextView
        mKeyViewIcon1 = findViewById<View>(R.id.key_1_icon) as ImageView
        mKeyViewIcon2 = findViewById<View>(R.id.key_2_icon) as ImageView
        mKeyViewIcon3 = findViewById<View>(R.id.key_3_icon) as ImageView
        mKeyViewIcon4 = findViewById<View>(R.id.key_4_icon) as ImageView
    }

    fun setKeySet(keySet: KeySet) {
        val keyList = keySet.keyList
        val keyLabelMap = keySet.keyLabelMap
        mSize = keyLabelMap.size
        setOneKey(keySet, keyList, keyLabelMap, 0, mKeyView1, mKeyViewIcon1)
        setOneKey(keySet, keyList, keyLabelMap, 1, mKeyView2, mKeyViewIcon2)
        setOneKey(keySet, keyList, keyLabelMap, 2, mKeyView3, mKeyViewIcon3)
        setOneKey(keySet, keyList, keyLabelMap, 3, mKeyView4, mKeyViewIcon4)
    }

    fun setSelected(position: Int) {
        resetSelected()

        setBackgroundResource(R.drawable.bg_keyset_selected)

        when (position) {
            0 -> {
                mKeyView1!!.setBackgroundResource(R.drawable.bg_selected_key)
                mKeyViewIcon1!!.setBackgroundResource(R.drawable.bg_selected_key)
            }
            1 -> {
                mKeyView2!!.setBackgroundResource(R.drawable.bg_selected_key)
                mKeyViewIcon2!!.setBackgroundResource(R.drawable.bg_selected_key)
            }
            2 -> {
                mKeyView3!!.setBackgroundResource(R.drawable.bg_selected_key)
                mKeyViewIcon3!!.setBackgroundResource(R.drawable.bg_selected_key)
            }
            3 -> {
                mKeyView4!!.setBackgroundResource(R.drawable.bg_selected_key)
                mKeyViewIcon4!!.setBackgroundResource(R.drawable.bg_selected_key)
            }
        }
    }

    fun resetSelected() {
        setBackgroundResource(R.drawable.bg_keyset)

        mKeyView1!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyViewIcon1!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyView2!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyViewIcon2!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyView3!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyViewIcon3!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyView4!!.setBackgroundColor(Color.TRANSPARENT)
        mKeyViewIcon4!!.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setOneKey(keySet: KeySet, keyList: List<Int>, keyLabelMap: Map<Int, String>, position: Int, keyView: TextView?, keyViewIcon: ImageView?) {
        if (mSize > position) {
            val key = keyList[position]
            val keyLabel = keyLabelMap[key]
            val icon = keySet.getKeyIcon(key)
            if (icon > 0) {
                keyViewIcon!!.setImageResource(icon)
                keyView!!.visibility = View.GONE
                keyViewIcon.visibility = View.VISIBLE
            } else {
                keyView!!.text = keyLabel
                keyView.visibility = View.VISIBLE
                keyViewIcon!!.visibility = View.GONE
            }
        } else {
            keyView!!.visibility = View.GONE
            keyViewIcon!!.visibility = View.GONE
        }
    }

}
