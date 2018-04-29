package com.teuskim.nlkeyboard

import android.app.Activity
import android.os.Bundle
import android.view.View

class GuideActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guide)
        val pref = NLPreference(applicationContext)
        pref.setDidReadGuide(true)

        findViewById<View>(R.id.btn_close).setOnClickListener { finish() }
    }

}
