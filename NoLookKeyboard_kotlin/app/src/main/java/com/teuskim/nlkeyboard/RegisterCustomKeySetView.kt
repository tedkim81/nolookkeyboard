package com.teuskim.nlkeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class RegisterCustomKeySetView : LinearLayout {

    private var mKeyView1: TextView? = null
    private var mKeyView2: TextView? = null
    private var mKeyView3: TextView? = null
    private var mKeyView4: TextView? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.register_custom_keyset, this)

        mKeyView1 = findViewById<TextView>(R.id.key_1)
        mKeyView2 = findViewById<TextView>(R.id.key_2)
        mKeyView3 = findViewById<TextView>(R.id.key_3)
        mKeyView4 = findViewById<TextView>(R.id.key_4)
    }

    fun fillKeys(key1: String, key2: String, key3: String, key4: String) {
        fillKey(mKeyView1, key1)
        fillKey(mKeyView2, key2)
        fillKey(mKeyView3, key3)
        fillKey(mKeyView4, key4)
    }

    private fun fillKey(keyView: TextView?, key: String) {
        keyView!!.text = ""
        keyView.setBackgroundDrawable(null)
        if ("\b" == key)
            keyView.setBackgroundResource(R.drawable.key_backspace)
        else if (" " == key)
            keyView.setBackgroundResource(R.drawable.key_space)
        else if ("\n" == key)
            keyView.setBackgroundResource(R.drawable.key_enter)
        else if ("\t" == key)
            keyView.setBackgroundResource(R.drawable.key_enter)
        else
            keyView.text = key
    }

}
