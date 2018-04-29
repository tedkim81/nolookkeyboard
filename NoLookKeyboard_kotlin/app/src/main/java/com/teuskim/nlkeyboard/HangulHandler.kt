package com.teuskim.nlkeyboard

import java.util.HashMap

import android.view.KeyEvent

/**
 * 한글입력 클래스
 */
class HangulHandler(private val mService: NLKeyboardService) {

    private var mCurrState: State? = null
    private val mState0 = State0()
    private val mState1 = State1()
    private val mState2 = State2()
    private val mState3 = State3()
    private val mState4 = State4()

    private var mChosung: Int = 0
    private var mJungsung: Int = 0
    private var mJongsung: Int = 0

    private var mCurrKey: Int = 0
    val composing = StringBuilder()
    private val mHangulString = StringBuilder()  // 입력된 전체 한글 문자열

    private var mUseDupChosung = true  // 중복입력으로 곁자음 완성기능 사용여부
    private val mKeyIntMap: MutableMap<String, Int>
    var isInit = true
        private set

    val hangulString: String
        get() = mHangulString.toString()

    val composingLength: Int
        get() = composing.length

    /**
     * 현재 입력받은 키가 자음인지 여부
     */
    private val isJaum: Boolean
        get() = if (mCurrKey < JAUM_SIZE) true else false

    init {
        mCurrState = mState0

        // xml파일 파싱하는 것과 중복되는 작업. 일단 빨리하기 위해서 아래와 같이 한다.
        mKeyIntMap = HashMap()
        mKeyIntMap["ㄱ"] = 0
        mKeyIntMap["ㄲ"] = 1
        mKeyIntMap["ㄴ"] = 2
        mKeyIntMap["ㄷ"] = 3
        mKeyIntMap["ㄸ"] = 4
        mKeyIntMap["ㄹ"] = 5
        mKeyIntMap["ㅁ"] = 6
        mKeyIntMap["ㅂ"] = 7
        mKeyIntMap["ㅃ"] = 8
        mKeyIntMap["ㅅ"] = 9
        mKeyIntMap["ㅆ"] = 10
        mKeyIntMap["ㅇ"] = 11
        mKeyIntMap["ㅈ"] = 12
        mKeyIntMap["ㅉ"] = 13
        mKeyIntMap["ㅊ"] = 14
        mKeyIntMap["ㅋ"] = 15
        mKeyIntMap["ㅌ"] = 16
        mKeyIntMap["ㅍ"] = 17
        mKeyIntMap["ㅎ"] = 18
        mKeyIntMap["ㅏ"] = 19
        mKeyIntMap["ㅐ"] = 20
        mKeyIntMap["ㅑ"] = 21
        mKeyIntMap["ㅒ"] = 22
        mKeyIntMap["ㅓ"] = 23
        mKeyIntMap["ㅔ"] = 24
        mKeyIntMap["ㅕ"] = 25
        mKeyIntMap["ㅖ"] = 26
        mKeyIntMap["ㅗ"] = 27
        mKeyIntMap["ㅛ"] = 28
        mKeyIntMap["ㅜ"] = 29
        mKeyIntMap["ㅠ"] = 30
        mKeyIntMap["ㅡ"] = 31
        mKeyIntMap["ㅣ"] = 32

        mKeyIntMap["ㅘ"] = 33
        mKeyIntMap["ㅙ"] = 34
        mKeyIntMap["ㅚ"] = 35
        mKeyIntMap["ㅝ"] = 36
        mKeyIntMap["ㅞ"] = 37
        mKeyIntMap["ㅟ"] = 38
        mKeyIntMap["ㅢ"] = 39
    }

    private fun init() {
        mCurrState = mState0
        mChosung = 0
        mJungsung = 0
        mJongsung = 0
        isInit = true
    }

    fun initHangulData() {
        mHangulString.setLength(0)
        composing.setLength(0)
        init()
    }

    fun onFinishInputView() {
        if (composing.length > 0)
            mHangulString.append(composing.toString())
    }

    fun setUseDupChosung(useDupChosung: Boolean) {
        mUseDupChosung = useDupChosung
    }

    /**
     * 자음/모음 구분하여 현재 상태의 자음출력/모음출력 메소드 호출
     */
    fun handle(key: Int) {
        isInit = false
        mCurrKey = key

        when (mCurrKey) {
            CUSTOM_SPACE -> handleSpecialString(" ")

            CUSTOM_ENTER -> {
                commit()
                mService.handleEnter()
                mHangulString.append("\n")
            }

            CUSTOM_BACKSPACE -> mCurrState!!.back()

            else -> if (isJaum) {
                mCurrState!!.jaum()
            } else {
                mCurrState!!.moum()
            }
        }
    }

    fun handleSpace() {
        if (isInit == false) {
            init()
        } else {
            handleSpecialString(" ")
        }
    }

    fun handleEnter() {
        commit()
        mHangulString.append("\n")
    }

    fun handleBackspace() {
        val isDeleted = mCurrState!!.back()

        val length = mHangulString.length
        if (length > 0 && isDeleted) {
            mHangulString.delete(length - 1, length)
        }

    }

    fun commit() {
        if (composing.length > 0)
            commitText(composing.toString())
        composing.setLength(0)
        init()
    }

    private fun handleSpecialString(str: String) {
        composing.append(str)
        commit()
    }

    fun getKeyIntFromJaso(str: String): Int? {
        return if (mKeyIntMap.containsKey(str)) {
            mKeyIntMap[str]
        } else -1
    }

    fun getKeyIntFromKeyCode(keyCode: Int): Int? {
        return getKeyIntFromJaso(keyCode.toChar() + "")
    }

    private fun setComposingText(text: String) {
        mService.setComposingText(text, 1)
    }

    private fun commitText(text: String?) {
        if (text != null) {
            mService.commitHangul(text, 1)
            mHangulString.append(text)
        }
    }

    fun separateJaso(word: String): String {
        val sb = StringBuilder()
        for (i in 0 until word.length) {
            val ch = word[i]
            if (ch.toInt() >= HANGUL_START_INDEX) {  // 조합형 한글이면 분리하여 반환한다.
                val index = ch.toInt() - HANGUL_START_INDEX
                val chosungIndex = index / (21 * 28)
                val jungsungIndex = index % (21 * 28) / 28
                val jongsungIndex = index % 28

                sb.append((JASO_START_INDEX + State.sJasoArr[chosungIndex]).toChar())
                sb.append((JASO_START_INDEX + JAUM_FULL_SIZE + jungsungIndex).toChar())
                if (jongsungIndex > 0)
                    sb.append((JASO_START_INDEX + State.sJasoArr[jongToCho(jongsungIndex)]).toChar())
            } else
                sb.append(ch)
        }
        return sb.toString()
    }


    /**
     * 각각의 상태들은 자음 및 모음을 입력받았을때의 행동을 구현해야 한다.
     */
    private abstract class State {

        abstract fun jaum()
        abstract fun moum()
        abstract fun back(): Boolean

        companion object {
            // 곁자음/곁모음 포함한 총 51개의 자소 중 키보드에 들어간 33개의 자소에 대한 인덱스
            var sJasoArr = intArrayOf(0, 1, 3, 6, 7, 8, 16, 17, 18, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 42, 43, 47, 48, 50, 39, 40, 41, 44, 45, 46, 49)//  ㄱㄲ ㄴ ㄷㄸ ㄹ  ㅁ   ㅂ   ㅃ   ㅅ  ㅆ   ㅇ  ㅈ   ㅉ   ㅊ   ㅋ  ㅌ   ㅍ   ㅎ
            //   ㅏ   ㅐ   ㅑ   ㅒ   ㅓ  ㅔ   ㅕ   ㅖ   ㅗ  ㅛ   ㅜ   ㅠ   ㅡ  ㅣ
            //   ㅘ   ㅙ   ㅚ   ㅝ  ㅞ   ㅟ   ㅢ

            // 초성에 들어갈 수 있는 자음들의 인덱스. 초성가능자음은 모두 키보드에 표현되므로 인덱스값이 그대로 초성이 된다.
            //public static int[] sChosungArr = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
            // ㄱㄲ ㄴ ㄷ ㄸ ㄹㅁ ㅂㅃ ㅅ  ㅆ   ㅇ   ㅈ   ㅉ  ㅊ   ㅋ   ㅌ   ㅍ  ㅎ

            // 중성에 들어갈 수 있는 모음들의 인덱스
            var sJungsungArr = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 17, 18, 20, 9, 10, 11, 14, 15, 16, 19)
            //  ㅏㅐ ㅑ ㅒㅓ ㅔ ㅕ ㅖ ㅗ ㅛ   ㅜ   ㅠ   ㅡ  ㅣ        ㅘ  ㅙ  ㅚ   ㅝ   ㅞ   ㅟ   ㅢ

            // 종성에 들어갈 수 있는 자음들의 인덱스. ㄱ이 1이다. -1인 경우 종성에 들어갈 수 없으니 다음 음절 초성으로 분리한다.
            var sJongsungArr = intArrayOf(1, 2, 4, 7, -1, 8, 16, 17, -1, 19, 20, 21, 22, -1, 23, 24, 25, 26, 27)
            //  ㄱㄲ ㄴ ㄷ  ㄸ ㄹ  ㅁ   ㅂ  ㅃ   ㅅ   ㅆ   ㅇ   ㅈ   ㅉ  ㅊ   ㅋ   ㅌ   ㅍ  ㅎ

            // 곁자소 매핑
            var sDupJasoMap: MutableMap<Int, Int>

            // 초성 변경 매핑
            var sChangeChosungMap: MutableMap<Int, Int>

            // 초성 곁자음 매핑
            var sDupChosungMap: MutableMap<Int, Int>

            // 중성에 곁모음 매핑
            var sDupJungsungMap: MutableMap<Int, Int>

            // 종성에 곁자음 매핑
            var sDupJongsungMap: MutableMap<Int, Int>

            // 앞글자 종성과 뒷글자 초성이 곁자음이 되는 경우 매핑
            var sPostDupChosungMap: MutableMap<Int, Int>

            // 곁종성과 곁초성 매핑
            var sDupJongToDupChoMap: MutableMap<Int, Int>


            init {
                // 초성 변경 맵핑
                sChangeChosungMap = HashMap()
                sChangeChosungMap[10000] = 1  // ㄱ -> ㄲ
                sChangeChosungMap[10100] = 15  // ㄲ -> ㅋ
                sChangeChosungMap[10303] = 4  // ㄷ -> ㄸ
                sChangeChosungMap[10403] = 16  // ㄸ -> ㅌ
                sChangeChosungMap[10707] = 8  // ㅂ -> ㅃ
                sChangeChosungMap[10807] = 17  // ㅃ -> ㅍ
                sChangeChosungMap[10909] = 10  // ㅅ -> ㅆ
                sChangeChosungMap[11212] = 13  // ㅈ -> ㅉ
                sChangeChosungMap[11312] = 14  // ㅉ -> ㅊ

                // 초성 변경 맵핑
                sDupChosungMap = HashMap()
                sDupChosungMap[0] = 1  // ㄱ -> ㄲ
                sDupChosungMap[3] = 4  // ㄷ -> ㄸ
                sDupChosungMap[7] = 8  // ㅂ -> ㅃ
                sDupChosungMap[9] = 10  // ㅅ -> ㅆ
                sDupChosungMap[12] = 13  // ㅈ -> ㅉ

                // key는 10000 + (첫자소 인덱스)*100 + (두번째자소 인덱스) 이고, value는 곁자소 인덱스
                sDupJasoMap = HashMap()
                sDupJasoMap[13050] = 31  // ㅏ + ㅣ = ㅐ
                sDupJasoMap[13030] = 32  // ㅏ + ㅏ = ㅑ
                sDupJasoMap[13250] = 33  // ㅑ + ㅣ = ㅒ
                sDupJasoMap[13131] = 33  // ㅐ + ㅐ = ㅒ
                sDupJasoMap[13450] = 35  // ㅓ + ㅣ = ㅔ
                sDupJasoMap[13434] = 36  // ㅓ + ㅓ = ㅕ
                sDupJasoMap[13650] = 37  // ㅕ + ㅣ = ㅖ
                sDupJasoMap[13535] = 37  // ㅔ + ㅔ = ㅖ
                sDupJasoMap[13830] = 39  // ㅗ + ㅏ = ㅘ
                sDupJasoMap[13838] = 42  // ㅗ + ㅗ = ㅛ
                sDupJasoMap[13831] = 40  // ㅗ + ㅐ = ㅙ
                sDupJasoMap[13950] = 40  // ㅘ + ㅣ = ㅙ
                sDupJasoMap[13850] = 41  // ㅗ + ㅣ = ㅚ
                sDupJasoMap[14334] = 44  // ㅜ + ㅓ = ㅝ
                sDupJasoMap[14343] = 47  // ㅜ + ㅜ = ㅠ
                sDupJasoMap[14335] = 45  // ㅜ + ㅔ = ㅞ
                sDupJasoMap[14450] = 45  // ㅝ + ㅣ = ㅞ
                sDupJasoMap[14350] = 46  // ㅜ + ㅣ = ㅟ
                sDupJasoMap[14850] = 49  // ㅡ + ㅣ = ㅢ

                sDupJungsungMap = HashMap()
                sDupJungsungMap[10020] = 1  // ㅏ + ㅣ = ㅐ
                sDupJungsungMap[10000] = 2  // ㅏ + ㅏ = ㅑ
                sDupJungsungMap[10220] = 3  // ㅑ + ㅣ = ㅒ
                sDupJungsungMap[10101] = 3  // ㅐ + ㅐ = ㅒ
                sDupJungsungMap[10420] = 5  // ㅓ + ㅣ = ㅔ
                sDupJungsungMap[10404] = 6  // ㅓ + ㅓ = ㅕ
                sDupJungsungMap[10620] = 7  // ㅕ + ㅣ = ㅖ
                sDupJungsungMap[10505] = 7  // ㅔ + ㅔ = ㅖ
                sDupJungsungMap[10800] = 9  // ㅗ + ㅏ = ㅘ
                sDupJungsungMap[10808] = 12  // ㅗ + ㅗ = ㅛ
                sDupJungsungMap[10801] = 10  // ㅗ + ㅐ = ㅙ
                sDupJungsungMap[10920] = 10  // ㅘ + ㅣ = ㅙ
                sDupJungsungMap[10820] = 11  // ㅗ + ㅣ = ㅚ
                sDupJungsungMap[11304] = 14  // ㅜ + ㅓ = ㅝ
                sDupJungsungMap[11313] = 17  // ㅜ + ㅜ = ㅠ
                sDupJungsungMap[11305] = 15  // ㅜ + ㅔ = ㅞ
                sDupJungsungMap[11420] = 15  // ㅝ + ㅣ = ㅞ
                sDupJungsungMap[11320] = 16  // ㅜ + ㅣ = ㅟ
                sDupJungsungMap[11820] = 19  // ㅡ + ㅣ = ㅢ

                sDupJongsungMap = HashMap()
                sDupJongsungMap[10101] = 2  // ㄱ + ㄱ = ㄲ
                sDupJongsungMap[10119] = 3  // ㄱ + ㅅ = ㄳ
                sDupJongsungMap[10422] = 5  // ㄴ + ㅈ = ㄵ
                sDupJongsungMap[10427] = 6  // ㄴ + ㅎ = ㄶ
                sDupJongsungMap[10801] = 9  // ㄹ + ㄱ = ㄺ
                sDupJongsungMap[10816] = 10  // ㄹ + ㅁ = ㄻ
                sDupJongsungMap[10817] = 11  // ㄹ + ㅂ = ㄼ
                sDupJongsungMap[10819] = 12  // ㄹ + ㅅ = ㄽ
                sDupJongsungMap[10825] = 13  // ㄹ + ㅌ = ㄾ
                sDupJongsungMap[10826] = 14  // ㄹ + ㅍ = ㄿ
                sDupJongsungMap[10827] = 15  // ㄹ + ㅎ = ㅀ
                sDupJongsungMap[11719] = 18  // ㅂ + ㅅ = ㅄ
                sDupJongsungMap[11919] = 20  // ㅅ + ㅅ = ㅆ

                sPostDupChosungMap = HashMap()
                sPostDupChosungMap[10100] = 1  // ㄱ + ㄱ = ㄲ
                sPostDupChosungMap[10703] = 4  // ㄷ + ㄷ = ㄸ
                sPostDupChosungMap[11707] = 8  // ㅂ + ㅂ = ㅃ
                sPostDupChosungMap[11909] = 10  // ㅅ + ㅅ = ㅆ
                sPostDupChosungMap[12212] = 13  // ㅈ + ㅈ = ㅉ

                sDupJongToDupChoMap = HashMap()
                sDupJongToDupChoMap[2] = 1  // ㄲ
                sDupJongToDupChoMap[20] = 10  // ㅆ
            }
        }
    }

    private fun composingAppend(text: String) {
        composing.append(text)
        setComposingText(composing.toString())
    }

    private fun composingReplace(text: String) {
        composing.replace(composing.length - 1, composing.length, text)
        setComposingText(composing.toString())
    }

    private fun jongToCho(jong: Int): Int {
        var choIdx = 0
        for (i in State.sJongsungArr.indices) {
            if (jong == State.sJongsungArr[i]) {
                choIdx = i
                break
            }
        }
        return choIdx
    }

    /**
     * state 0 : 초기상태
     */
    private inner class State0 : State() {

        override fun jaum() {
            mChosung = mCurrKey
            composingAppend((JASO_START_INDEX + HangulHandler.State.sJasoArr[mCurrKey]).toChar().toString())
            mCurrState = mState1
        }

        override fun moum() {
            if (mCurrKey >= JAUM_SIZE) {
                mJungsung = HangulHandler.State.sJungsungArr[mCurrKey - JAUM_SIZE]
                composingAppend((JASO_START_INDEX + JAUM_FULL_SIZE + HangulHandler.State.sJungsungArr[mCurrKey - JAUM_SIZE]).toChar().toString())
                mCurrState = mState2
            }
        }

        override fun back(): Boolean {
            if (composing.length > 0) {
                composing.delete(composing.length - 1, composing.length)
                setComposingText(composing.toString())
            } else {
                mService.keyDownUp(KeyEvent.KEYCODE_DEL)
            }
            init()
            return true
        }
    }

    /**
     * state 1 : 자음
     */
    private inner class State1 : State() {

        override fun jaum() {
            val dupKey = 10000 + mChosung * 100 + mCurrKey
            if (mUseDupChosung && HangulHandler.State.sChangeChosungMap.containsKey(dupKey)) {
                mChosung = HangulHandler.State.sChangeChosungMap[dupKey]!!
                composingReplace((JASO_START_INDEX + HangulHandler.State.sJasoArr[mChosung]).toChar().toString())
            } else {
                mState0.jaum()
            }
        }

        override fun moum() {
            if (mCurrKey >= JAUM_SIZE) {
                mJungsung = HangulHandler.State.sJungsungArr[mCurrKey - JAUM_SIZE]
                composingReplace((mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString())
                mCurrState = mState3
            }
        }

        override fun back(): Boolean {
            composing.delete(composing.length - 1, composing.length)
            setComposingText(composing.toString())
            init()
            return false
        }
    }

    /**
     * state 2 : 모음
     */
    private inner class State2 : State() {

        override fun jaum() {
            init()
            mState0.jaum()
        }

        override fun moum() {
            if (mCurrKey >= JAUM_SIZE) {
                val jung = HangulHandler.State.sJungsungArr[mCurrKey - JAUM_SIZE]
                val dupKey = 10000 + mJungsung * 100 + jung
                if (HangulHandler.State.sDupJungsungMap.containsKey(dupKey)) {
                    mJungsung = HangulHandler.State.sDupJungsungMap[dupKey]!!
                    composingReplace((JASO_START_INDEX + JAUM_FULL_SIZE + mJungsung).toChar().toString())
                } else {
                    mState0.moum()
                }
            }
        }

        override fun back(): Boolean {
            composing.delete(composing.length - 1, composing.length)
            setComposingText(composing.toString())
            init()
            return false
        }
    }

    /**
     * state 3 : 자음+모음
     */
    private inner class State3 : State() {

        override fun jaum() {
            mJongsung = HangulHandler.State.sJongsungArr[mCurrKey]
            if (mJongsung == -1) {
                mState0.jaum()
            } else {
                composingReplace((mChosung * 21 * 28 + mJungsung * 28 + mJongsung + HANGUL_START_INDEX).toChar().toString())
                mCurrState = mState4
            }
        }

        override fun moum() {
            if (mCurrKey >= JAUM_SIZE) {
                val jung = HangulHandler.State.sJungsungArr[mCurrKey - JAUM_SIZE]
                val dupKey = 10000 + mJungsung * 100 + jung
                if (HangulHandler.State.sDupJungsungMap.containsKey(dupKey)) {
                    mJungsung = HangulHandler.State.sDupJungsungMap[dupKey]!!
                    composingReplace((mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString())
                } else {
                    mState0.moum()
                }
            }
        }

        override fun back(): Boolean {
            composingReplace((JASO_START_INDEX + HangulHandler.State.sJasoArr[mChosung]).toChar().toString())
            mCurrState = mState1
            return false
        }

    }

    /**
     * state 4 : 자음+모음+자음
     */
    private inner class State4 : State() {

        private var mWillSplitJongsung = false

        override fun jaum() {
            val jong = HangulHandler.State.sJongsungArr[mCurrKey]
            val dupKey = 10000 + mJongsung * 100 + jong
            if (HangulHandler.State.sDupJongsungMap.containsKey(dupKey)) {
                mWillSplitJongsung = true
                mJongsung = HangulHandler.State.sDupJongsungMap[dupKey]!!
                composingReplace((mChosung * 21 * 28 + mJungsung * 28 + mJongsung + HANGUL_START_INDEX).toChar().toString())
            } else {
                mWillSplitJongsung = false
                val postDupKey = 10000 + mJongsung * 100 + mCurrKey
                if (HangulHandler.State.sPostDupChosungMap.containsKey(postDupKey) && HangulHandler.State.sDupChosungMap.containsKey(mCurrKey)) {
                    mJongsung = 0
                    composingReplace((mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString())
                    init()
                    mCurrKey = HangulHandler.State.sDupChosungMap[mCurrKey]!!
                    mState0.jaum()
                } else {
                    init()
                    mState0.jaum()
                }
            }
        }

        override fun moum() {
            if (HangulHandler.State.sDupJongToDupChoMap.containsKey(mJongsung)) {
                val str = (mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString()
                composingReplace(str + " ")  // 공백은 마지막 글자 들어갈 자리

                mChosung = HangulHandler.State.sDupJongToDupChoMap[mJongsung]!!
                mJongsung = 0
                mState1.moum()
            } else {
                val key = dupJongToKey(mJongsung)
                if (mWillSplitJongsung && key >= 10000) {
                    mJongsung = (key - 10000) / 100
                    val str = (mChosung * 21 * 28 + mJungsung * 28 + mJongsung + HANGUL_START_INDEX).toChar().toString()
                    composingReplace(str + " ")  // 공백은 마지막 글자 들어갈 자리

                    mChosung = jongToCho((key - 10000) % 100)
                    mJongsung = 0
                    mState1.moum()
                } else {
                    val str = (mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString()
                    composingReplace(str + " ")  // 공백은 마지막 글자 들어갈 자리

                    mChosung = jongToCho(mJongsung)
                    mJongsung = 0
                    mState1.moum()
                }
            }
            mWillSplitJongsung = false
        }

        override fun back(): Boolean {
            composingReplace((mChosung * 21 * 28 + mJungsung * 28 + HANGUL_START_INDEX).toChar().toString())
            mCurrState = mState3
            return false
        }

        private fun dupJongToKey(dupJong: Int): Int {
            val iter = HangulHandler.State.sDupJongsungMap.keys.iterator()
            var result = 0
            while (iter.hasNext()) {
                val key = iter.next()
                if (HangulHandler.State.sDupJongsungMap[key] == dupJong) {
                    result = key
                    break
                }
            }
            return result
        }
    }

    companion object {

        private val JASO_START_INDEX = 0x3131
        val HANGUL_START_INDEX = 0xAC00
        private val JAUM_SIZE = 19
        private val JAUM_FULL_SIZE = 30  // 곁자음 포함한 수

        val CUSTOM_SPACE = -2
        val CUSTOM_ENTER = -3
        val CUSTOM_BACKSPACE = NLKeyboardService.KEY_DELETE
    }

}
