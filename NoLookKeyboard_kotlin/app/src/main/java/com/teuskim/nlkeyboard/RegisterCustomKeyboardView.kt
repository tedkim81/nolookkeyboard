package com.teuskim.nlkeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout

class RegisterCustomKeyboardView : RelativeLayout {

    private var mLeftTopView: RegisterCustomKeySetView? = null
    private var mMidTopView: RegisterCustomKeySetView? = null
    private var mRightTopView: RegisterCustomKeySetView? = null
    private var mLeftMidView: RegisterCustomKeySetView? = null
    private var mRightMidView: RegisterCustomKeySetView? = null
    private var mLeftBotView: RegisterCustomKeySetView? = null
    private var mMidBotView: RegisterCustomKeySetView? = null
    private var mRightBotView: RegisterCustomKeySetView? = null

    constructor(context: Context, attrs: AttributeSet,
                defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        // xml layout 으로 뷰를 생성하고, 멤버 변수들을 초기화 한다.
        LayoutInflater.from(context).inflate(R.layout.register_custom_keyboard, this)

        mLeftTopView = findViewById<RegisterCustomKeySetView>(R.id.keyset_left_top)
        mMidTopView = findViewById<RegisterCustomKeySetView>(R.id.keyset_mid_top)
        mRightTopView = findViewById<RegisterCustomKeySetView>(R.id.keyset_right_top)
        mLeftMidView = findViewById<RegisterCustomKeySetView>(R.id.keyset_left_mid)
        mRightMidView = findViewById<RegisterCustomKeySetView>(R.id.keyset_right_mid)
        mLeftBotView = findViewById<RegisterCustomKeySetView>(R.id.keyset_left_bot)
        mMidBotView = findViewById<RegisterCustomKeySetView>(R.id.keyset_mid_bot)
        mRightBotView = findViewById<RegisterCustomKeySetView>(R.id.keyset_right_bot)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        mLeftTopView!!.setOnClickListener(l)
        mMidTopView!!.setOnClickListener(l)
        mRightTopView!!.setOnClickListener(l)
        mLeftMidView!!.setOnClickListener(l)
        mRightMidView!!.setOnClickListener(l)
        mLeftBotView!!.setOnClickListener(l)
        mMidBotView!!.setOnClickListener(l)
        mRightBotView!!.setOnClickListener(l)
    }

    fun fillKeys(viewId: Int, key1: String, key2: String, key3: String, key4: String) {
        val view = getViewById(viewId)
        view!!.fillKeys(key1, key2, key3, key4)
    }

    private fun getViewById(viewId: Int): RegisterCustomKeySetView? {
        when (viewId) {
            R.id.keyset_left_top -> return mLeftTopView
            R.id.keyset_mid_top -> return mMidTopView
            R.id.keyset_right_top -> return mRightTopView
            R.id.keyset_left_mid -> return mLeftMidView
            R.id.keyset_right_mid -> return mRightMidView
            R.id.keyset_left_bot -> return mLeftBotView
            R.id.keyset_mid_bot -> return mMidBotView
            else -> return mRightBotView
        }
    }

    /*
	private int getStartNumberById(int viewId){
		switch(viewId){
		case R.id.keyset_left_top:
			return 1;
		case R.id.keyset_mid_top:
			return 5;
		case R.id.keyset_right_top:
			return 9;
		case R.id.keyset_left_mid:
			return 13;
		case R.id.keyset_right_mid:
			return 17;
		case R.id.keyset_left_bot:
			return 21;
		case R.id.keyset_mid_bot:
			return 25;
		default:
			return 29;
		}
	}
	*/

}
