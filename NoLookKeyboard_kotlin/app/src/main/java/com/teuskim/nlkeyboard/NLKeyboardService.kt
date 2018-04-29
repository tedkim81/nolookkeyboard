package com.teuskim.nlkeyboard

import android.content.Context
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.HashMap
import java.util.TreeSet

import android.content.Intent
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.AsyncTask
import android.os.Handler
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.teuskim.nlkeyboard.NLKeyboard.KeySet
import com.teuskim.nlkeyboard.NLKeyboardDb.CustomKeysetData

/**
 * 키보드 서비스 ( 컨트롤러 )
 * 1. 키보드 모델 및 키보드 뷰를 생성한다.
 * 2. 키보드뷰에 리스너를 달고, 콜백 받을 메소드들을 구현한다.
 * 3. 선택받은 글자를 출력하거나, 키보드를 변경하는 메소드를 구현한다.
 *
 * 참고 ( 생명주기 )
 * 1. 키보드가 생성될 때, onCreate, onStartInput 호출
 * 2. 키보드가 노출될 때, onCreateInputView, onCreateCandidatesView, onStartInputView 호출
 * 3. 가끔 onStartInput 부터가 다시 호출되는데 왜그런지 모르겠다.
 * 4. 백키로 키보드 없애면 onFinishInputView 호출
 * 5. 입력영역을 눌러서 키보드를 다시 노출하면 onStartInputView 호출
 * 6. 다른 키보드를 선택하면 onDestroy 호출
 * 7. 다시 키보드 선택하면 1번부터.
 * 8. 현재화면에서 벗어나면 onFinishInputView, onStartInput 호출
 * 9. 현재화면 들어오면 onStartInput 호출
 */
class NLKeyboardService : InputMethodService(), OnClickListener {

    private var mInputView: View? = null
    private var mEnglishKeyboard: NLKeyboard? = null
    private var mHangulKeyboard: NLKeyboard? = null
    private var mHangulShiftKeyboard: NLKeyboard? = null
    private var mNumbersKeyboard: NLKeyboard? = null
    private var mSymbolsKeyboard: NLKeyboard? = null
    private var mKeyboardViewLeft: NLKeyboardView? = null
    private var mKeyboardViewRight: NLKeyboardView? = null
    private var mWordListLayout: LinearLayout? = null
    private var mBtnSpace: Button? = null

    private val mComposing = StringBuilder()

    private val mLeftKeyboardListener = KeyboardActionListener()
    private val mRightKeyboardListener = KeyboardActionListener()
    private val mKeyboardListForAll = ArrayList<NLKeyboard>()
    private val mKeyboardListForLeft = ArrayList<NLKeyboard>()
    private val mKeyboardListForRight = ArrayList<NLKeyboard>()

    private var mHangulHandler: HangulHandler? = null
    private var mDb: NLKeyboardDb? = null

    private var mLastKeyboardView: NLKeyboardView? = null
    private var mCustomStringMap: MutableMap<Int, String>? = null
    private var mCustomTextViewMap: MutableMap<Int, TextView>? = null

    private var mCustomKeymapLeft: View? = null
    private var mCustomKeymapRight: View? = null

    private var mWordListTask: WordListTask? = null
    private var mInputString: String? = null
    private var mLastInput: Int = 0

    private var mLockChangeKeyboard = false
    private val mHandler = Handler()
    private val mHangulInputRunnable = Runnable {
        try {
            if (mLastKeyboardView != null) {
                mHangulHandler!!.handle(mLastKeyboardView!!.selectedKeyInt)

                if (mIsShiftRemain == false && mLastKeyboardView!!.keyboard === mHangulShiftKeyboard) {
                    if (mLastKeyboardView!!.direction == 4) {
                        mKeyboardViewLeft!!.keyboard = mHangulKeyboard
                        mKeyboardViewRight!!.keyboard = mHangulKeyboard
                    } else {
                        mLastKeyboardView!!.keyboard = mHangulKeyboard
                    }
                }
                showWordList()
                changeSpacebarText()
            }
        } catch (e: Exception) {
        }
    }

    private var mPref: NLPreference? = null
    private var mLastInputIsHangul = false
    private var mKeyCodeEnter = KeyEvent.KEYCODE_ENTER
    private var mIsShiftRemain = true
    private var mCurrTypeTextVariation: Int = 0
    private var mIsInserting = false

    private val isHangulInput: Boolean
        get() = mHangulHandler!!.composingLength > 0

    private val allText: String
        get() {
            val ic = currentInputConnection
            return ic.getTextBeforeCursor(100, 0) as String + ic.getTextAfterCursor(100, 0) as String
        }

    private val currentWord: String
        get() = if (isHangulInput) {
            mHangulHandler!!.composing.toString()
        } else {
            mComposing.toString()
        }

    private val lastChar: Char
        get() = if (mInputString != null && mInputString!!.length > 0) {
            mInputString!![mInputString!!.length - 1]
        } else ' '

    private val inputType: Int
        get() {
            val info = currentInputEditorInfo
            return info.inputType and EditorInfo.TYPE_MASK_VARIATION
        }

    private val mWordListRunnable = WordListRunnable()


    override fun onCreate() {
        // 객체 생성시 호출. 변경되지 않는 멤버 변수 초기화
        Log.d(TAG, "onCreate!!")
        super.onCreate()

        mDb = NLKeyboardDb.getInstance(applicationContext)
        mPref = NLPreference(applicationContext)
    }

    override fun onInitializeInterface() {
        // 뭔가 설정이 변경될 때마다 호출된다. 여기에 UI관련 멤버변수를 모두 셋팅한다.
        Log.d(TAG, "onInitializeInterface!!")
        super.onInitializeInterface()

        mEnglishKeyboard = NLKeyboard(this, R.xml.keyboard_english)
        mHangulKeyboard = NLKeyboard(this, R.xml.keyboard_hangul)
        mHangulShiftKeyboard = NLKeyboard(this, R.xml.keyboard_hangul_shift)
        mHangulHandler = HangulHandler(this)
        mNumbersKeyboard = NLKeyboard(this, R.xml.keyboard_numbers)
        mSymbolsKeyboard = NLKeyboard(this, R.xml.keyboard_symbols)
        mCustomStringMap = HashMap()
        mCustomTextViewMap = HashMap()
    }

    override fun onCreateInputView(): View {
        // 키보드뷰를 최초 출력할 때와 설정이 변경될 때마다 호출된다. 키보드뷰를 생성하여 리턴한다.
        Log.d(TAG, "onCreateInputView!!")

        mInputView = layoutInflater.inflate(R.layout.input, null)
        mKeyboardViewLeft = mInputView!!.findViewById<View>(R.id.keyboard_left) as NLKeyboardView
        mKeyboardViewLeft!!.setOnKeyboardActionListener(mLeftKeyboardListener)
        mKeyboardViewLeft!!.keyboard = mEnglishKeyboard
        mKeyboardViewRight = mInputView!!.findViewById<View>(R.id.keyboard_right) as NLKeyboardView
        mKeyboardViewRight!!.setOnKeyboardActionListener(mRightKeyboardListener)
        mKeyboardViewRight!!.keyboard = mEnglishKeyboard
        mCustomKeymapLeft = mInputView!!.findViewById(R.id.custom_keymap_left)
        mCustomKeymapRight = mInputView!!.findViewById(R.id.custom_keymap_right)
        mWordListLayout = mInputView!!.findViewById<View>(R.id.history_list_layout) as LinearLayout

        mKeyboardViewLeft!!.setOppositeKeyboardView(mKeyboardViewRight!!)
        mKeyboardViewRight!!.setOppositeKeyboardView(mKeyboardViewLeft!!)

        val btnRepeatLeft = mInputView!!.findViewById<View>(R.id.btn_repeat_left) as Button
        val btnRepeatRight = mInputView!!.findViewById<View>(R.id.btn_repeat_right) as Button
        val btnSetting = mInputView!!.findViewById<View>(R.id.btn_setting) as Button
        mBtnSpace = mInputView!!.findViewById<View>(R.id.btn_space) as Button
        val btnBackspace = mInputView!!.findViewById<View>(R.id.btn_backspace) as Button
        btnSetting.setOnClickListener(this)

        val listener = RepeatListener(this)
        mBtnSpace!!.setOnTouchListener(listener)
        btnBackspace.setOnTouchListener(listener)
        btnRepeatLeft.setOnTouchListener(listener)
        btnRepeatRight.setOnTouchListener(listener)

        val dm = DisplayMetrics()
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(dm)
        val windowSize = Math.min(dm.widthPixels, dm.heightPixels) / 2
        val twoInch = dm.densityDpi * 2
        val layoutSize = Math.min(windowSize, twoInch)
        val centerSize = layoutSize / 3
        val keysetSize = layoutSize / 4 + 4
        mKeyboardViewLeft!!.setSize(layoutSize, centerSize, keysetSize)
        mKeyboardViewRight!!.setSize(layoutSize, centerSize, keysetSize)

        mHandler.postDelayed({
            val width = mKeyboardViewLeft!!.width
            if (width > 100) {
                val params1 = mCustomKeymapLeft!!.layoutParams
                params1.width = (width * 0.9).toInt()
                mCustomKeymapLeft!!.layoutParams = params1

                val params2 = mCustomKeymapRight!!.layoutParams
                params2.width = (width * 0.9).toInt()
                mCustomKeymapRight!!.layoutParams = params2
            }
        }, 500)

        return mInputView as View
    }

    private inner class RepeatListener(private val mListener: OnClickListener) : View.OnTouchListener {

        private var mIsDown = false
        private var mView: View? = null

        private val mRepeatRunnable = Runnable {
            mListener.onClick(mView)
            delayedRepeat()
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            mView = v

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mIsDown = true
                    mHandler.postDelayed({ delayedRepeat() }, 300)
                }
                MotionEvent.ACTION_UP -> {
                    mIsDown = false
                    mHandler.removeCallbacksAndMessages(null)
                    mHandler.post(mRepeatRunnable)
                }
            }

            return false
        }

        private fun delayedRepeat() {
            if (mIsDown == true) {
                mHandler.postDelayed(mRepeatRunnable, 50)
            }
        }

    }

    private fun handleRepeat() {
        when (mLastInput) {
            LAST_INPUT_SPACE -> handleSpace()
            LAST_INPUT_BACKSPACE -> handleBackspace()
            else -> handleInput(mLastKeyboardView)
        }
    }

    private fun goSetting() {
        val i = Intent(applicationContext, SettingActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        // 키보드뷰 셋팅이 완료된 후 호출된다. 키보드 객체를 셋팅한다.
        Log.d(TAG, "onStartInputView!!")
        super.onStartInputView(info, restarting)

        // 키보드가 보여지는 동안 입력되는 내용을 저장하기 위해
        mComposing.setLength(0)
        mHangulHandler!!.initHangulData()
        mWordListLayout!!.removeAllViews()

        val direction = mDb!!.direction
        mKeyboardViewLeft!!.setDirectionInfo(direction, true)
        mKeyboardViewRight!!.setDirectionInfo(direction, false)

        val listAll = mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_ALL)
        fillKeyboardList(mKeyboardListForAll, listAll)
        val listLeft = mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_LEFT)
        fillKeyboardList(mKeyboardListForLeft, listLeft)
        val listRight = mDb!!.getKeySetList(NLKeyboardDb.KeySet.SIDE_RIGHT)
        fillKeyboardList(mKeyboardListForRight, listRight)

        mCustomStringMap!!.clear()
        val customKeysetList = mDb!!.customKeySetList
        for (ck in customKeysetList) {
            val keyStringList = ArrayList<Map<Int, String>>()
            if (ck.mShow4 != NLKeyboardDb.CustomKeyset.SHOW_NONE || ck.mShow8 != NLKeyboardDb.CustomKeyset.SHOW_NONE) {

                val ckdList = mDb!!.getCustomKeySetDataList(ck.mId)
                for (i in 0..7) {
                    keyStringList.add(HashMap<Int, String>())
                }
                for (ckd in ckdList) {
                    val map: HashMap<Int, String> = keyStringList[ckd.mKeysetNumber] as HashMap<Int, String>
                    map.put(ckd.mId, ckd.mData!!)
                    mCustomStringMap!!.put(ckd.mId, ckd.mData!!)
                }
            }

            if (ck.mShow4 == NLKeyboardDb.CustomKeyset.SHOW_ALL) {
                mKeyboardListForAll.add(NLKeyboard(this, keyStringList))
            }
            if (ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_LEFT || ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_ALL) {
                mKeyboardListForLeft.add(NLKeyboard(this, keyStringList))
            }
            if (ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_RIGHT || ck.mShow8 == NLKeyboardDb.CustomKeyset.SHOW_ALL) {
                mKeyboardListForRight.add(NLKeyboard(this, keyStringList))
            }
        }

        // 최근단어 영역에 초기 메세지 셋팅
        showWordList()

        setKeyboardByEditorInfo(info)

        // 초성 중복 입력시 쌍자음 되도록 하는 기능
        mHangulHandler!!.setUseDupChosung(mPref!!.useDupChosung())

        // 진동기능 사용여부
        NLKeyboardView.setIsVibrate(mPref!!.isVibrate)

        // shift키 유지 기능
        mIsShiftRemain = mPref!!.isShiftRemain

        // wordInsert 할때만 사용
        mCurrTypeTextVariation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
    }

    private fun fillKeyboardList(keyboardList: MutableList<NLKeyboard>, keysetList: List<NLKeyboardDb.KeySet>) {
        keyboardList.clear()
        val english = getString(R.string.txt_english)
        val hangul = getString(R.string.txt_hangul)
        val numbers = getString(R.string.txt_numbers)
        val symbols = getString(R.string.txt_symbols)
        for (ks in keysetList) {
            if ("Y" == ks.mShowYN) {
                val keyboardName = mDb!!.getKeyboardName(ks.mType)
                if (keyboardName == english)
                    keyboardList.add(mEnglishKeyboard!!)
                else if (keyboardName == hangul)
                    keyboardList.add(mHangulKeyboard!!)
                else if (keyboardName == numbers)
                    keyboardList.add(mNumbersKeyboard!!)
                else if (keyboardName == symbols)
                    keyboardList.add(mSymbolsKeyboard!!)
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        // 키보드뷰가 사라지면 호출된다.
        Log.d(TAG, "onFinishInputView!!")

        // 입력된 내용을 저장한다.
        wordInsert(mInputString)

        super.onFinishInputView(finishingInput)

        mHangulHandler!!.onFinishInputView()
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        // 키 입력전 호출되며 가장 중요한 부분이다. 각종 멤버변수를 셋팅하는 등의 입력 준비 단계를 마무리 짓는다.
        Log.d(TAG, "onStartInput!!")
        super.onStartInput(attribute, restarting)
        mComposing.setLength(0)

        when (attribute.imeOptions and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_NEXT -> mKeyCodeEnter = KeyEvent.KEYCODE_TAB
            else -> mKeyCodeEnter = KeyEvent.KEYCODE_ENTER
        }
    }

    private fun setKeyboardByEditorInfo(attribute: EditorInfo) {
        when (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME, EditorInfo.TYPE_CLASS_PHONE -> {
                mLockChangeKeyboard = true

                if (mKeyboardViewLeft != null)
                    mKeyboardViewLeft!!.keyboard = mNumbersKeyboard
                if (mKeyboardViewRight != null)
                    mKeyboardViewRight!!.keyboard = mNumbersKeyboard
            }
            else -> {
                mLockChangeKeyboard = false

                if (mDb!!.direction == 4) {
                    if (mKeyboardListForAll != null && mKeyboardListForAll.size > 0) {
                        val keyboardPos = mPref!!.keyboardPosAll
                        mKeyboardViewLeft!!.keyboard = getKeyboardFromList(mKeyboardListForAll, keyboardPos)
                        mKeyboardViewRight!!.keyboard = getKeyboardFromList(mKeyboardListForAll, keyboardPos)
                    }
                } else {
                    if (mKeyboardListForLeft != null && mKeyboardListForLeft.size > 0) {
                        mKeyboardViewLeft!!.keyboard = getKeyboardFromList(mKeyboardListForLeft, mPref!!.keyboardPosLeft)
                    }
                    if (mKeyboardListForRight != null && mKeyboardListForRight.size > 0) {
                        mKeyboardViewRight!!.keyboard = getKeyboardFromList(mKeyboardListForRight, mPref!!.keyboardPosRight)
                    }
                }
            }
        }
    }

    private fun getKeyboardFromList(list: List<NLKeyboard>, pos: Int): NLKeyboard {
        return if (list.size - 1 < pos) list[0] else list[pos]
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int,
                                   newSelStart: Int, newSelEnd: Int, candidatesStart: Int,
                                   candidatesEnd: Int) {

        //		Log.d(TAG, "oldStart:"+oldSelStart+", oldEnd:"+oldSelEnd+", newStart:"+newSelStart+", newEnd:"+newSelEnd+", candStart:"+candidatesStart+", candEnd:"+candidatesEnd);
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        if ((mComposing.length > 0 || mHangulHandler!!.composingLength > 0) && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {

            //			Log.d(TAG, "onUpdateSelection! init data!");
            mComposing.setLength(0)
            mHangulHandler!!.initHangulData()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    override fun onFinishInput() {
        // 키 입력이 완료되면 호출된다. 각종 상태를 리셋한다.
        Log.d(TAG, "onFinishInput!!")
        super.onFinishInput()
        mComposing.setLength(0)
    }

    override fun onDestroy() {
        // 객체가 종료될 때 호출된다. 메모리릭 방지를 위한 종료처리 등을 한다.
        Log.d(TAG, "onDestroy!!")
        super.onDestroy()
    }

    fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    override fun onClick(v: View) {
        var isTextChanged = false

        when (v.id) {
            R.id.btn_space -> {
                handleSpace()
                isTextChanged = true
            }
            R.id.btn_backspace -> {
                handleBackspace()
                isTextChanged = true
            }
            R.id.btn_repeat_left, R.id.btn_repeat_right -> {
                handleRepeat()
                isTextChanged = true
            }
            R.id.btn_setting -> goSetting()
        }

        if (isTextChanged) {
            showWordList()
            changeSpacebarText()
        }
    }

    fun setComposingText(text: CharSequence, pos: Int) {
        currentInputConnection.setComposingText(text, pos)

        showWordList()
    }

    private fun commitText(text: CharSequence?, pos: Int) {
        currentInputConnection.commitText(text, pos)

        showWordList()
    }

    fun commitHangul(text: CharSequence, pos: Int) {
        currentInputConnection.commitText(text, pos)

        showWordList()
    }

    private fun handleInput(keyboardView: NLKeyboardView?) {
        if (keyboardView == null)
            return

        // 반복키 사용을 위해 저장한다.
        mLastKeyboardView = keyboardView

        // 스와잎이 끝나면 호출된다. 글자를 출력한다.
        val currentKeyboard = keyboardView.keyboard
        if (currentKeyboard === mHangulKeyboard || currentKeyboard === mHangulShiftKeyboard) {
            handleHangul(keyboardView, currentKeyboard)
            mLastInputIsHangul = true
        } else if (currentKeyboard!!.isCustom == true) {
            handleCustom(keyboardView)

            showWordList()
            changeSpacebarText()
        } else {
            handleCommon(keyboardView, currentKeyboard)
            mLastInputIsHangul = false

            showWordList()
            changeSpacebarText()
        }

        mLastInput = LAST_INPUT_ETC
    }

    private fun changeSpacebarText() {
        if (mHangulHandler!!.isInit == false) {
            mBtnSpace!!.setText(R.string.btn_commit)
        } else {
            mBtnSpace!!.setText(R.string.btn_space)
        }
    }

    private fun handleCommon(keyboardView: NLKeyboardView, currentKeyboard: NLKeyboard) {
        if (keyboardView.selectedKeyInt == KEY_DELETE) {
            handleDelete(keyboardView)
        } else if (keyboardView.selectedKeyInt == KEY_SHIFT) {
            handleShift(keyboardView, currentKeyboard)
        } else if (keyboardView.selectedKeyInt == KEY_ENTER) {
            handleEnter()
        } else {
            handleCharacter(keyboardView)
            mHangulHandler!!.initHangulData()
        }
    }

    fun handleDelete(keyboardView: NLKeyboardView) {
        if (mLastInputIsHangul) {
            mHandler.post(mHangulInputRunnable)
        } else {
            val length = mComposing.length
            if (length > 1) {
                mComposing.delete(length - 1, length)
                setComposingText(mComposing, 1)
            } else if (length > 0) {
                mComposing.setLength(0)
                commitText("", 0)
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL)
            }

            mHangulHandler!!.initHangulData()
        }
    }

    fun handleEnter() {
        keyDownUp(mKeyCodeEnter)
    }

    private fun handleShift(keyboardView: NLKeyboardView, currentKeyboard: NLKeyboard?) {
        val direction = keyboardView.direction
        if (currentKeyboard === mHangulKeyboard) {
            if (direction == 4) {
                mKeyboardViewLeft!!.keyboard = mHangulShiftKeyboard
                mKeyboardViewRight!!.keyboard = mHangulShiftKeyboard
            } else {
                keyboardView.keyboard = mHangulShiftKeyboard
            }
        } else if (currentKeyboard === mHangulShiftKeyboard) {
            if (direction == 4) {
                mKeyboardViewLeft!!.keyboard = mHangulKeyboard
                mKeyboardViewRight!!.keyboard = mHangulKeyboard
            } else {
                keyboardView.keyboard = mHangulKeyboard
            }
        } else {
            currentKeyboard!!.isShift = !currentKeyboard.isShift
            currentKeyboard.loadKeyboard()
            if (direction == 4) {
                mKeyboardViewLeft!!.keyboard = currentKeyboard
                mKeyboardViewRight!!.keyboard = currentKeyboard
            } else {
                keyboardView.keyboard = currentKeyboard
            }
        }
    }

    private fun handleCharacter(keyboardView: NLKeyboardView) {
        mHangulHandler!!.commit()

        val selectedStr = keyboardView.selectedKeyInt.toChar().toString()
        mComposing.append(selectedStr)
        if (Character.isLetterOrDigit(keyboardView.selectedKeyInt)) {
            setComposingText(mComposing, 1)
        } else {
            commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
        }

        if (mIsShiftRemain == false && keyboardView.keyboard === mEnglishKeyboard && keyboardView.keyboard!!.isShift)
            handleShift(keyboardView, keyboardView.keyboard)
    }

    private fun handleCustom(keyboardView: NLKeyboardView) {
        val selectedStr = mCustomStringMap!![keyboardView.selectedKeyInt]
        if ("\b" == selectedStr) {
            if (mLastInputIsHangul) {
                mHangulHandler!!.handle(HangulHandler.CUSTOM_BACKSPACE)
            } else {
                handleDelete(keyboardView)
            }
        } else if ("\n" == selectedStr) {
            handleEnter()
        } else if (selectedStr != null) {
            handleSelectedString(keyboardView, selectedStr)
        }
    }

    private fun handleSelectedString(keyboardView: NLKeyboardView?, str: String) {
        val keyInt = mHangulHandler!!.getKeyIntFromJaso(str)!!
        if (keyInt >= 0 && keyboardView != null) {
            if (mComposing.length > 0) {
                commitText(mComposing, 1)
                mComposing.setLength(0)
            }
            mHangulHandler!!.handle(keyInt)

            mLastInputIsHangul = true
        } else {
            mHangulHandler!!.commit()

            mComposing.append(str)
            commitText(mComposing, 1)
            mComposing.setLength(0)

            mLastInputIsHangul = false
        }
    }

    private fun handleHangul(keyboardView: NLKeyboardView, currentKeyboard: NLKeyboard?) {
        if (keyboardView.selectedKeyInt == KEY_SHIFT) {
            handleShift(keyboardView, currentKeyboard)
        } else {
            if (mComposing.length > 0) {
                commitText(mComposing, 1)
                mComposing.setLength(0)
            }
            mHandler.post(mHangulInputRunnable)
        }
    }

    private fun handleBackspace() {
        if (isHangulInput) {
            mHangulHandler!!.handleBackspace()
        } else {
            val length = mComposing.length
            if (length > 1) {
                mComposing.delete(length - 1, length)
                currentInputConnection.setComposingText(mComposing, 1)
            } else if (length > 0) {
                mComposing.setLength(0)
                currentInputConnection.commitText("", 0)
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL)
            }
        }
        mLastInput = LAST_INPUT_BACKSPACE
    }

    private fun handleSpace() {
        if (isHangulInput) {
            mHangulHandler!!.handleSpace()
        } else {
            mHangulHandler!!.commit()
            mComposing.append(' ')
            commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
        }
        mLastInput = LAST_INPUT_SPACE
    }

    private inner class KeyboardActionListener : NLKeyboardView.OnKeyboardActionListener {

        override fun onTouchDown(keyboardView: NLKeyboardView) {
            val keyboard = keyboardView.keyboard
            if (keyboard!!.isCustom == true) {
                if (keyboardView.isLeft == true)
                    fillCustomKeymapView(mCustomKeymapLeft, keyboard.keyStringList!!)
                else
                    fillCustomKeymapView(mCustomKeymapRight, keyboard.keyStringList!!)
            }
        }

        private fun fillCustomKeymapView(keymapView: View?, keyStringList: List<Map<Int, String>>) {
            if (keymapView != null) {
                fillCustomKeymapText(keyStringList[IDX_LEFT_TOP], keymapView, R.id.keymap_left_top, R.drawable.icon_keymap_left_top)
                fillCustomKeymapText(keyStringList[IDX_MID_TOP], keymapView, R.id.keymap_mid_top, R.drawable.icon_keymap_mid_top)
                fillCustomKeymapText(keyStringList[IDX_RIGHT_TOP], keymapView, R.id.keymap_right_top, R.drawable.icon_keymap_right_top)
                fillCustomKeymapText(keyStringList[IDX_LEFT_MID], keymapView, R.id.keymap_left_mid, R.drawable.icon_keymap_left_mid)
                fillCustomKeymapText(keyStringList[IDX_RIGHT_MID], keymapView, R.id.keymap_right_mid, R.drawable.icon_keymap_right_mid)
                fillCustomKeymapText(keyStringList[IDX_LEFT_BOT], keymapView, R.id.keymap_left_bot, R.drawable.icon_keymap_left_bottom)
                fillCustomKeymapText(keyStringList[IDX_MID_BOT], keymapView, R.id.keymap_mid_bot, R.drawable.icon_keymap_mid_bottom)
                fillCustomKeymapText(keyStringList[IDX_RIGHT_BOT], keymapView, R.id.keymap_right_bot, R.drawable.icon_keymap_right_bottom)

                keymapView.visibility = View.VISIBLE
            }
        }

        private fun fillCustomKeymapText(map: Map<Int, String>, keymapView: View, keymapResId: Int, iconResId: Int) {
            if (map.size > 0) {
                val keymapPart = keymapView.findViewById<View>(keymapResId) as LinearLayout
                keymapPart.visibility = View.VISIBLE

                val iconView = keymapPart.findViewById<View>(R.id.icon_keymap) as ImageView
                iconView.setImageResource(iconResId)

                val set = TreeSet(map.keys)
                val it = set.iterator()
                var i = 1
                while (it.hasNext()) {
                    val keymapText = keymapPart.getChildAt(i++) as TextView
                    keymapText.setTextColor(Color.BLACK)
                    val idx = it.next()
                    val customString = mCustomStringMap!![idx]
                    if ("\b" == customString)
                        keymapText.text = "backspace"
                    else if (" " == customString)
                        keymapText.text = "spacebar"
                    else if ("\n" == customString)
                        keymapText.text = "enter"
                    else
                        keymapText.text = customString
                    mCustomTextViewMap!![idx] = keymapText
                }
            }
        }

        override fun onTouchUp(keyboardView: NLKeyboardView) {
            if (mCustomKeymapLeft != null)
                mCustomKeymapLeft!!.visibility = View.GONE
            if (mCustomKeymapRight != null)
                mCustomKeymapRight!!.visibility = View.GONE
        }

        override fun onClick(keyboardView: NLKeyboardView) {
            // 숫자 입력과 같이 입력영역의 타입이 정해져있을 경우 키보드 교체를 못하도록 한다.
            if (mLockChangeKeyboard)
                return

            // 입력영역을 클릭하면 호출된다. 키보드를 교체한다.
            val currentKeyboard = keyboardView.keyboard
            val direction = mDb!!.direction

            try {
                if (direction == 4) {
                    val idx = (mKeyboardListForAll.indexOf(currentKeyboard) + 1) % mKeyboardListForAll.size
                    mKeyboardViewLeft!!.keyboard = mKeyboardListForAll[idx]
                    mKeyboardViewRight!!.keyboard = mKeyboardListForAll[idx]

                    mPref!!.keyboardPosAll = idx
                } else {
                    if (keyboardView.id == R.id.keyboard_left) {
                        val idx = (mKeyboardListForLeft.indexOf(currentKeyboard) + 1) % mKeyboardListForLeft.size
                        mKeyboardViewLeft!!.keyboard = mKeyboardListForLeft[idx]

                        mPref!!.keyboardPosLeft = idx
                    } else {
                        val idx = (mKeyboardListForRight.indexOf(currentKeyboard) + 1) % mKeyboardListForRight.size
                        mKeyboardViewRight!!.keyboard = mKeyboardListForRight[idx]

                        mPref!!.keyboardPosRight = idx
                    }
                }
            } catch (e: Exception) {
            }

        }

        override fun onSwipe(keyboardView: NLKeyboardView, keySet: KeySet?, swipeCnt: Int) {
            val it = mCustomTextViewMap!!.keys.iterator()
            while (it.hasNext()) {
                val idx = it.next()
                val textView = mCustomTextViewMap!![idx]
                if (idx == keyboardView.selectedKeyInt)
                    textView!!.setTextColor(Color.RED)
                else
                    textView!!.setTextColor(Color.BLACK)
            }
        }

        override fun onSwipeFinish(keyboardView: NLKeyboardView, swipeCnt: Int) {
            handleInput(keyboardView)
        }

    }

    private fun recordInputString() {
        if (inputType != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) {
            val inputString = allText

            // 입력영역이 초기화가 되었다면 단어저장
            if (mInputString != null && mInputString!!.length > 1
                    && (inputString == null || inputString.length == 0)
                    && mIsInserting == false) {
                wordInsert(mInputString)
            }

            mInputString = inputString
        }
    }

    private inner class WordListTask : AsyncTask<String, Int, List<RecommendWord>>() {

        override fun doInBackground(vararg params: String): List<RecommendWord> {
            var text: String? = currentWord
            if (text != null && getCharType(lastChar) != TYPE_SPACE)
                text = text.trim { it <= ' ' }
            else
                text = ""
            return recommendWords(text, mHangulHandler!!.separateJaso(text))
        }

        override fun onPostExecute(result: List<RecommendWord>?) {
            val inflater = LayoutInflater.from(applicationContext)
            mWordListLayout!!.removeAllViews()
            if (result != null && result.size > 0) {
                val min = Math.min(result.size, 10)
                for (i in 0 until min) {
                    val rw = result[i]
                    val word = rw.mWord

                    val v = inflater.inflate(R.layout.history_list_item, null)
                    (v.findViewById<View>(R.id.history_word) as TextView).text = word
                    v.setOnClickListener {
                        mComposing.setLength(0)
                        commitHangul("", 1)
                        mHangulHandler!!.initHangulData()

                        if (getCharType(word!![word.length - 1]) == TYPE_ETC)
                            commitText(word, 1)
                        else
                            commitText(word + " ", 1)
                        showWordList()

                        changeSpacebarText()
                    }
                    mWordListLayout!!.addView(v)
                }
            } else {
                // TODO: 예측 단어가 없을 경우 어떻게 할까? 기존에는 설정으로 보내는 알림 텍스트를 넣었으나, 다른게 좋을듯.
            }
        }

        private fun recommendWords(word: String, composition: String): List<RecommendWord> {

            // 앞단어 이용해서 친밀도 기반 목록 구한다.
            val affinityWordList = getAffinityWordList(word, composition)

            // 입력영역속성 기반 목록을 구한다.
            val attrWordList = getAttrWordList(composition)

            // 입력횟수기반 목록을 구한다.
            val useCntWordList = getUseCntWordList(composition)

            // 입력시각기반 목록을 구한다.
            val useTimeWordList = getUseTimeWordList(composition)

            // 모든 목록을 병합하여 내림차순으로 10개의 항목을 갖는 목록을 만들어 리턴한다.
            return mergeWordList(affinityWordList, attrWordList, useCntWordList, useTimeWordList)
        }

        private fun getAffinityWordList(word: String?, composition: String): List<RecommendWord> {
            val ic = currentInputConnection
            val prevText = ic.getTextBeforeCursor(50, 0) as String
            var resultList: MutableList<RecommendWord> = ArrayList()

            if (prevText.length > 0) {
                val texts = splitText(prevText)
                if (texts.size > 0)
                    resultList = mDb!!.getNextWordList(texts[texts.size - 1], composition).toMutableList()
            }

            if (word != null && word.length > 0 && getCharType(word[word.length - 1]) == TYPE_ETC) {
                resultList.addAll(mDb!!.getNextWordList(word, ""))
            }

            return resultList
        }

        private fun getAttrWordList(composition: String): List<RecommendWord> {
            val info = currentInputEditorInfo
            val typeTextVariation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
            return mDb!!.getWordListByAttr(composition, typeTextVariation)
        }

        private fun getUseCntWordList(composition: String): List<RecommendWord> {
            return mDb!!.getWordListByUseCnt(composition)
        }

        private fun getUseTimeWordList(composition: String): List<RecommendWord> {
            return mDb!!.getWordListByUseTime(composition)
        }

        private fun mergeWordList(affinityWordList: List<RecommendWord>, attrWordList: List<RecommendWord>, useCntWordList: List<RecommendWord>, useTimeWordList: List<RecommendWord>): List<RecommendWord> {
            val map = HashMap<Int, RecommendWord>()
            for (rw in affinityWordList) {
                map[rw.mWordId] = rw
            }
            for (rw in attrWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]
                    rwTmp!!.mUseCntXxx = rw.mUseCntXxx
                    rwTmp!!.mUseCntXxxSum = rw.mUseCntXxxSum
                } else {
                    map[rw.mWordId] = rw
                }
            }
            for (rw in useCntWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]
                    rwTmp!!.mUseCntTotal = rw.mUseCntTotal
                    rwTmp!!.mUseCntTotalSum = rw.mUseCntTotalSum
                } else {
                    map[rw.mWordId] = rw
                }
            }
            for (rw in useTimeWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]
                    rwTmp!!.mUseCntN = rw.mUseCntN
                    rwTmp!!.mUseCntNSum = rw.mUseCntNSum
                } else {
                    map[rw.mWordId] = rw
                }
            }

            val iter = map.values.iterator()
            val myInfo = mDb!!.myInfo
            val availableTime = myInfo.mAvailablePeriod * 86400000L
            val resultList = ArrayList<RecommendWord>()
            while (iter.hasNext()) {
                val rw = iter.next()
                rw.generatePoint(AFFINITY_WEIGHT, myInfo.mUseCntXxxWeight.toDouble(), myInfo.mUseCntTotalWeight.toDouble(), myInfo.mUseCntNWeight.toDouble(), availableTime)
                resultList.add(rw)
            }
            Collections.sort(resultList)
            return resultList
        }
    }

    private fun wordInsert(inputString: String?) {
        // 입력된 내용을 저장한다.
        mIsInserting = true
        val task = WordInsertTask()
        task.execute(inputString)
    }

    private fun getCharType(ch: Char): Int {
        return if (ch.toInt() >= 97 && ch.toInt() <= 122 || ch.toInt() >= 65 && ch.toInt() <= 90 || ch.toInt() >= 0x3131 && ch.toInt() <= 0xd7a3)
            TYPE_CHAR
        else if (ch.toInt() == 9 || ch.toInt() == 10 || ch.toInt() == 13 || ch.toInt() == 32)
            TYPE_SPACE
        else
            TYPE_ETC
    }

    private fun splitText(inputText: String?): List<String> {
        val wordList = ArrayList<String>()
        if (inputText == null)
            return wordList

        val sb = StringBuilder()
        var currType = 0
        for (i in 0 until inputText.length) {
            val ch = inputText[i]
            val type = getCharType(ch)

            if (currType != TYPE_CHAR && type == TYPE_CHAR && sb.length > 0) {
                wordList.add(sb.toString())
                sb.setLength(0)
            }
            if (type != TYPE_SPACE) {
                sb.append(ch)
            }

            currType = type
        }
        if (sb.length > 0)
            wordList.add(sb.toString())

        return wordList
    }

    private inner class WordInsertTask : AsyncTask<String, Int, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val inputString = params[0]
            if (inputString == null || inputString.length == 0)
                return false

            val inputText = inputString.trim { it <= ' ' }
            if (inputText.length > 0) {
                // 단어분리 {{
                val wordList = splitText(inputText)
                // }} 단어분리

                // 저장하기 {{
                var preWord: String? = null
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val columnUseCntXXX = mDb!!.getColumnUseCntXXX(mCurrTypeTextVariation)
                val columnUseCntN = mDb!!.getColumnUseCntN(hour)
                for (word in wordList) {
                    val composition = mHangulHandler!!.separateJaso(word)
                    mDb!!.insertOrUpdateWord(word, composition, mCurrTypeTextVariation, columnUseCntXXX, hour, columnUseCntN)

                    if (preWord != null)
                        mDb!!.insertOrUpdateNextWordGroup(preWord, word)
                    preWord = word

                    Log.d(TAG, "insert word: $word , composition: $composition , typeTextVariation: $mCurrTypeTextVariation")
                }
                // }} 저장하기

                // 분석 / 학습 {{
                // 가중치 갱신하기
                var weightTotal = mDb!!.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_TOTAL.toDouble(), NLKeyboardDb.Word.USE_CNT_TOTAL)
                var weightN = mDb!!.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_N.toDouble(), columnUseCntN)
                var weightXXX = mDb!!.getUpdatedWeight(NLKeyboardDb.WEIGHT_INIT_XXX.toDouble(), columnUseCntXXX)
                if (weightTotal > 0 && weightN > 0 && weightXXX > 0) {
                    val ratio = (NLKeyboardDb.WEIGHT_SUM - NLKeyboardDb.WEIGHT_INIT_NEXT) / (weightTotal + weightN + weightXXX)
                    weightTotal = weightTotal * ratio
                    weightN = weightN * ratio
                    weightXXX = weightXXX * ratio
                    mDb!!.updateWeight(weightTotal.toInt(), weightN.toInt(), weightXXX.toInt())
                }

                // 유효기간 갱신하기
                // TODO: 성능상의 문제가 있다. 나중에 하자.
                // }} 분석 / 학습

                return true
            }
            return false
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)

            if (result!!) {
                mInputString = null
            }
            mIsInserting = false
        }

    }

    fun showWordList() {
        recordInputString()

        mHandler.removeCallbacks(mWordListRunnable)
        mHandler.postDelayed(mWordListRunnable, 50)
    }

    private inner class WordListRunnable : Runnable {

        override fun run() {
            if (mWordListTask != null) {
                mWordListTask!!.cancel(true)
            }
            mWordListTask = WordListTask()
            mWordListTask!!.execute()
        }

    }

    companion object {

        private val TAG = "NLKeyboardService"

        val KEY_DELETE = android.inputmethodservice.Keyboard.KEYCODE_DELETE
        private val KEY_SHIFT = android.inputmethodservice.Keyboard.KEYCODE_SHIFT
        private val KEY_ENTER = -3

        private val IDX_LEFT_TOP = 0
        private val IDX_MID_TOP = 1
        private val IDX_RIGHT_TOP = 2
        private val IDX_LEFT_MID = 3
        private val IDX_RIGHT_MID = 4
        private val IDX_LEFT_BOT = 5
        private val IDX_MID_BOT = 6
        private val IDX_RIGHT_BOT = 7

        private val TYPE_CHAR = 1
        private val TYPE_SPACE = 2
        private val TYPE_ETC = 3
        private val AFFINITY_WEIGHT = 50.0

        private val LAST_INPUT_SPACE = 1
        private val LAST_INPUT_BACKSPACE = 2
        private val LAST_INPUT_ETC = 0
    }

}
