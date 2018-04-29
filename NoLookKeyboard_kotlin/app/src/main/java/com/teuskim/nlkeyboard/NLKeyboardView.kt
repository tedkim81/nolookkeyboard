package com.teuskim.nlkeyboard

import android.content.Context
import android.graphics.PointF
import android.os.Vibrator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout

import com.teuskim.nlkeyboard.NLKeyboard.KeySet

/**
 * 키보드 뷰 클래스
 * 1. xml layout 으로 뷰 생성한다.
 * 2. Keyboard 객체로부터 키 데이터를 받아와서 작은원세트들을 출력한다.
 * 3. 사용자 터치를 분석하여, 리스너의 적절한 콜백 메소드를 호출한다.
 *
 * @author kim5724
 */
class NLKeyboardView : RelativeLayout {

    var keyboardActionListener: OnKeyboardActionListener? = null
        private set
    // 키보드 멤버 변수 셋팅하고, 뷰를 갱신한다.
    var keyboard: NLKeyboard? = null
        set(keyboard) {
            field = keyboard
            mKeySetList = this.keyboard!!.keySets
            mLeftTopView!!.setKeySet(mKeySetList!![IDX_LEFT_TOP])
            mMidTopView!!.setKeySet(mKeySetList!![IDX_MID_TOP])
            mRightTopView!!.setKeySet(mKeySetList!![IDX_RIGHT_TOP])
            mLeftMidView!!.setKeySet(mKeySetList!![IDX_LEFT_MID])
            mRightMidView!!.setKeySet(mKeySetList!![IDX_RIGHT_MID])
            mLeftBotView!!.setKeySet(mKeySetList!![IDX_LEFT_BOT])
            mMidBotView!!.setKeySet(mKeySetList!![IDX_MID_BOT])
            mRightBotView!!.setKeySet(mKeySetList!![IDX_RIGHT_BOT])
        }
    var swipeTotalCnt: Int = 0  // 터치한 상태에서 swipe한 횟수
    var moveAngle: Double = 0.toDouble()  // swipe하는 방향의 각도
    val downPoint = PointF()  // 터치 다운 이벤트 받은 위치값
    val point = PointF()  // 터치 이벤트 받은 가장 최근 위치값
    private var mKeySetList: List<NLKeyboard.KeySet>? = null
    var keySet: KeySet? = null
    var direction: Int = 0
        private set
    var isLeft: Boolean = false
        private set  // 왼쪽이 X모양, 오른쪽이 +모양
    var selectedKeyInt: Int = 0

    private var mLeftTopView: KeySetView? = null
    private var mMidTopView: KeySetView? = null
    private var mRightTopView: KeySetView? = null
    private var mLeftMidView: KeySetView? = null
    private var mRightMidView: KeySetView? = null
    private var mLeftBotView: KeySetView? = null
    private var mMidBotView: KeySetView? = null
    private var mRightBotView: KeySetView? = null
    var currentKeySetView: KeySetView? = null
        set(keySetView) {
            if (currentKeySetView != null && currentKeySetView !== keySetView)
                currentKeySetView!!.resetSelected()
            field = keySetView
        }  // 현재 입력 진행중인 키셋뷰
    var keyboardCenterX: ImageView? = null
        private set
    var keyboardCenterPlus: ImageView? = null
        private set
    private var mKeyboardBgX: ImageView? = null
    private var mKeyboardBgPlus: ImageView? = null
    var currentKeyboardCenterImg: Int = 0
        private set
    var isCorner: Boolean = false
        private set  // 코너쪽의 방향 키셋 ( X 모양 )이 선택되었나?

    private var mOppositeKeyboardView: NLKeyboardView? = null

    private var mVibrator: Vibrator? = null

    private var mMyIndex = -1
    private var mOppositeIndex = -1

    val swipeCnt: Int
        get() {
            var swipeCnt = swipeTotalCnt % keySet!!.size()
            if (swipeCnt == 0)
                swipeCnt = keySet!!.size()
            return swipeCnt
        }

    interface OnKeyboardActionListener {
        /**
         * MotionEvent.ACTION_DOWN 발생시 호출
         */
        fun onTouchDown(keyboardView: NLKeyboardView)

        /**
         * MotionEvent.ACTION_UP 발생시 호출
         */
        fun onTouchUp(keyboardView: NLKeyboardView)

        /**
         * 스와잎이 발생하면 호출한다. 첫 스와잎에서 keySet 이 결정된다.
         */
        fun onSwipe(keyboardView: NLKeyboardView, keySet: NLKeyboard.KeySet?, swipeCnt: Int)

        /**
         * 스와잎이 끝나면 호출한다. 최종 글자가 결정된다.
         */
        fun onSwipeFinish(keyboardView: NLKeyboardView, swipeCnt: Int)

        /**
         * 스와잎 없이 클릭이 발생하면 호출한다. 키보드 객체를 변경한다.
         */
        fun onClick(keyboardView: NLKeyboardView)
    }

    fun vibrate() {
        if (sIsVibrate) {
            try {
                mVibrator!!.vibrate(30)
            } catch (e: Exception) {
            }

        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
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
        LayoutInflater.from(context).inflate(R.layout.keyboard, this)

        mLeftTopView = findViewById<View>(R.id.keyset_left_top) as KeySetView
        mMidTopView = findViewById<View>(R.id.keyset_mid_top) as KeySetView
        mRightTopView = findViewById<View>(R.id.keyset_right_top) as KeySetView
        mLeftMidView = findViewById<View>(R.id.keyset_left_mid) as KeySetView
        mRightMidView = findViewById<View>(R.id.keyset_right_mid) as KeySetView
        mLeftBotView = findViewById<View>(R.id.keyset_left_bot) as KeySetView
        mMidBotView = findViewById<View>(R.id.keyset_mid_bot) as KeySetView
        mRightBotView = findViewById<View>(R.id.keyset_right_bot) as KeySetView
        keyboardCenterX = findViewById<View>(R.id.keyboard_center_x) as ImageView
        keyboardCenterPlus = findViewById<View>(R.id.keyboard_center_plus) as ImageView
        mKeyboardBgX = findViewById<View>(R.id.keyboard_bg_x) as ImageView
        mKeyboardBgPlus = findViewById<View>(R.id.keyboard_bg_plus) as ImageView

        mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun setSize(layoutSize: Int, centerSize: Int, keysetSize: Int) {
        val lp = layoutParams
        lp.width = layoutSize
        lp.height = layoutSize
        layoutParams = lp

        val lp2 = keyboardCenterX!!.layoutParams
        lp2.width = centerSize
        lp2.height = centerSize
        keyboardCenterX!!.layoutParams = lp2

        val lp3 = keyboardCenterPlus!!.layoutParams
        lp3.width = centerSize
        lp3.height = centerSize
        keyboardCenterPlus!!.layoutParams = lp3

        setKeysetSize(mLeftTopView, keysetSize)
        setKeysetSize(mMidTopView, keysetSize)
        setKeysetSize(mRightTopView, keysetSize)
        setKeysetSize(mLeftMidView, keysetSize)
        setKeysetSize(mRightMidView, keysetSize)
        setKeysetSize(mLeftBotView, keysetSize)
        setKeysetSize(mMidBotView, keysetSize)
        setKeysetSize(mRightBotView, keysetSize)
    }

    private fun setKeysetSize(keysetView: KeySetView?, keysetSize: Int) {
        if (keysetView!!.childCount == 0)
            return

        val view = keysetView.getChildAt(0) ?: return

        val lp = view.layoutParams
        lp.width = keysetSize
        lp.height = keysetSize
        view.layoutParams = lp

        if (view is ViewGroup == false || (view as ViewGroup).childCount < 2)
            return

        val keysetHalf = keysetSize / 2 - 1
        val v1 = view.getChildAt(0)
        val lp1 = v1.layoutParams
        lp1.height = keysetHalf
        v1.layoutParams = lp1

        val v11 = (v1 as ViewGroup).getChildAt(0)
        val lp11 = v11.layoutParams
        lp11.width = keysetHalf
        v11.layoutParams = lp11

        val v12 = v1.getChildAt(1)
        val lp12 = v12.layoutParams
        lp12.width = keysetHalf
        v12.layoutParams = lp12

        val v2 = view.getChildAt(1)
        val lp2 = v1.getLayoutParams()
        lp2.height = keysetHalf
        v2.layoutParams = lp2

        val v21 = (v2 as ViewGroup).getChildAt(0)
        val lp21 = v21.layoutParams
        lp21.width = keysetHalf
        v21.layoutParams = lp21

        val v22 = v2.getChildAt(1)
        val lp22 = v22.layoutParams
        lp22.width = keysetHalf
        v22.layoutParams = lp22
    }

    fun setOppositeKeyboardView(keyboardView: NLKeyboardView) {
        mOppositeKeyboardView = keyboardView
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        // 리스너 멤버 변수 셋팅한다.
        keyboardActionListener = listener
    }

    fun setDirectionInfo(direction: Int, isLeft: Boolean) {
        this.direction = direction
        this.isLeft = isLeft

        if (this.direction == 8) {
            mLeftTopView!!.visibility = View.VISIBLE
            mMidTopView!!.visibility = View.VISIBLE
            mRightTopView!!.visibility = View.VISIBLE
            mRightMidView!!.visibility = View.VISIBLE
            mRightBotView!!.visibility = View.VISIBLE
            mMidBotView!!.visibility = View.VISIBLE
            mLeftBotView!!.visibility = View.VISIBLE
            mLeftMidView!!.visibility = View.VISIBLE

            keyboardCenterX!!.visibility = View.VISIBLE
            keyboardCenterPlus!!.visibility = View.VISIBLE
        } else if (this.direction == 4) {
            if (isLeft) {  // X모양
                mLeftTopView!!.visibility = View.VISIBLE
                mMidTopView!!.visibility = View.GONE
                mRightTopView!!.visibility = View.VISIBLE
                mRightMidView!!.visibility = View.GONE
                mRightBotView!!.visibility = View.VISIBLE
                mMidBotView!!.visibility = View.GONE
                mLeftBotView!!.visibility = View.VISIBLE
                mLeftMidView!!.visibility = View.GONE

                keyboardCenterX!!.visibility = View.VISIBLE
                keyboardCenterPlus!!.visibility = View.GONE
            } else {  // +모양
                mLeftTopView!!.visibility = View.GONE
                mMidTopView!!.visibility = View.VISIBLE
                mRightTopView!!.visibility = View.GONE
                mRightMidView!!.visibility = View.VISIBLE
                mRightBotView!!.visibility = View.GONE
                mMidBotView!!.visibility = View.VISIBLE
                mLeftBotView!!.visibility = View.GONE
                mLeftMidView!!.visibility = View.VISIBLE

                keyboardCenterX!!.visibility = View.GONE
                keyboardCenterPlus!!.visibility = View.VISIBLE
            }
        }
    }

    private fun showKeyboardArrowBg() {
        if (direction == 8) {
            mKeyboardBgX!!.visibility = View.VISIBLE
            mKeyboardBgPlus!!.visibility = View.VISIBLE
        } else if (direction == 4) {
            if (isLeft) {  // X 모양
                mKeyboardBgX!!.visibility = View.VISIBLE
                mKeyboardBgPlus!!.visibility = View.GONE
            } else {  // + 모양
                mKeyboardBgX!!.visibility = View.GONE
                mKeyboardBgPlus!!.visibility = View.VISIBLE
            }
        }
    }

    fun hideKeyboardArrowBg() {
        mKeyboardBgX!!.visibility = View.GONE
        mKeyboardBgPlus!!.visibility = View.GONE
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 터치 분석하여 상황에 맞는 콜백 메소드를 호출해 준다.
        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mMyIndex = getPointerIndex(action)
                mOppositeIndex = -1
                actionDown(this, event.getX(mMyIndex), event.getY(mMyIndex))
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (mMyIndex != -1) {
                mOppositeIndex = getPointerIndex(action)
                actionDown(mOppositeKeyboardView, event.getX(mOppositeIndex), event.getY(mOppositeIndex))
            } else {
                mMyIndex = getPointerIndex(action)
                actionDown(this, event.getX(mMyIndex), event.getY(mMyIndex))
            }

            MotionEvent.ACTION_MOVE -> {
                if (mMyIndex != -1) {
                    val x = event.getX(mMyIndex)
                    val y = event.getY(mMyIndex)
                    actionMove(this, x, y)
                }
                if (mOppositeIndex != -1) {
                    val x = event.getX(mOppositeIndex)
                    val y = event.getY(mOppositeIndex)
                    actionMove(mOppositeKeyboardView, x, y)
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> if (getPointerIndex(action) == mMyIndex) {
                actionUp(this, event.getX(mMyIndex), event.getY(mMyIndex))
                mMyIndex = -1
            } else {
                actionUp(mOppositeKeyboardView, event.getX(mOppositeIndex), event.getY(mOppositeIndex))
                mOppositeIndex = -1
            }
        }
        return true
    }

    private fun getPointerIndex(action: Int): Int {
        return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
    }

    fun actionDown(keyboardView: NLKeyboardView?, x: Float, y: Float) {
        // 변수값 초기화하고, 터치위치 저장
        keyboardView!!.swipeTotalCnt = 0
        keyboardView.moveAngle = (-1).toDouble()
        keyboardView.setDownPoint(x, y)
        keyboardView.setPoint(x, y)
        if (keyboardView.keyboardActionListener != null)
            keyboardView.keyboardActionListener!!.onTouchDown(keyboardView)
        keyboardView.showKeyboardArrowBg()
    }

    fun actionMove(keyboardView: NLKeyboardView?, x: Float, y: Float) {
        // 이전 터치위치와 비교하여 방향값 구하고, 처음이면 KeySet 저장, 방향값이 유효범위 밖이면 swipe cnt 증가
        val point = keyboardView!!.point
        val downPoint = keyboardView.downPoint
        val diffx = (x - point.x).toDouble()
        val diffy = (y - point.y).toDouble()
        val diffxFromDown = (x - downPoint.x).toDouble()
        val diffyFromDown = (y - downPoint.y).toDouble()

        var moveAngle = 0.0
        var moveAngleFromDown = 0.0
        if (keyboardView.swipeTotalCnt == 0 && (Math.abs(diffx) > keyboardView.width / 10 || Math.abs(diffy) > keyboardView.height / 10) || keyboardView.swipeTotalCnt > 0 && (Math.abs(diffx) > 20 || Math.abs(diffy) > 20)) {
            moveAngle = Math.toDegrees(Math.atan2(diffy, diffx))
            if (moveAngle < 0)
                moveAngle = moveAngle + 360
            keyboardView.setPoint(x, y)

            moveAngleFromDown = Math.toDegrees(Math.atan2(diffyFromDown, diffxFromDown))
            if (moveAngleFromDown < 0)
                moveAngleFromDown = moveAngleFromDown + 360
        } else {
            return
        }

        if (keyboardView.swipeTotalCnt == 0) {
            keyboardView.moveAngle = moveAngleFromDown
            keyboardView.keySet = keyboardView.getKeySetByDegree(keyboardView.moveAngle)
            if (keyboardView.keySet!!.size() > 0) {
                keyboardView.swipeTotalCnt = 1
                vibrate()
            } else {
                keyboardView.setDownPoint(x, y)
            }
        } else {
            var diffAngle = Math.abs(keyboardView.moveAngle - moveAngle)
            if (diffAngle > 180)
                diffAngle = 360 - diffAngle

            if (keyboardView.swipeTotalCnt == 1) {
                if (diffAngle > 90) {
                    keyboardView.moveAngle = moveAngle
                    keyboardView.swipeTotalCnt = keyboardView.swipeTotalCnt + 1

                    vibrate()
                } else if (diffAngle < 40) {
                    keyboardView.moveAngle = moveAngleFromDown
                    val keyset = keyboardView.getKeySetByDegree(keyboardView.moveAngle)

                    if (keyboardView.keySet !== keyset) {
                        keyboardView.keySet = keyset

                        vibrate()
                    }
                }
            } else {
                if (diffAngle > 60) {
                    keyboardView.moveAngle = moveAngle
                    keyboardView.swipeTotalCnt = keyboardView.swipeTotalCnt + 1

                    vibrate()
                }
            }
        }

        if (keyboardView.keySet != null && keyboardView.swipeTotalCnt > 0) {
            val swipeCnt = keyboardView.swipeCnt
            keyboardView.currentKeySetView!!.setSelected(swipeCnt - 1)
            if (keyboardView.isCorner)
                keyboardView.keyboardCenterX!!.setImageResource(keyboardView.currentKeyboardCenterImg)
            else
                keyboardView.keyboardCenterX!!.setImageResource(R.drawable.keyboard_center_x)

            if (keyboardView.isCorner)
                keyboardView.keyboardCenterPlus!!.setImageResource(R.drawable.keyboard_center_plus)
            else
                keyboardView.keyboardCenterPlus!!.setImageResource(keyboardView.currentKeyboardCenterImg)

            keyboardView.selectedKeyInt = keyboardView.keySet!!.getKey(swipeCnt - 1)
            keyboardView.keyboardActionListener!!.onSwipe(keyboardView, keyboardView.keySet, swipeCnt)
        }
    }

    fun actionUp(keyboardView: NLKeyboardView?, x: Float, y: Float) {
        // swipe cnt가 0보다 크면 swipe 이고, 그렇지 않으면 click으로 처리
        if (keyboardView!!.currentKeySetView != null) {
            keyboardView.currentKeySetView!!.resetSelected()
            keyboardView.keyboardCenterX!!.setImageResource(R.drawable.keyboard_center_x)
            keyboardView.keyboardCenterPlus!!.setImageResource(R.drawable.keyboard_center_plus)
        }
        if (keyboardView.keyboardActionListener != null) {
            val point = keyboardView.point
            val dx = (x - point.x).toDouble()
            val dy = (y - point.y).toDouble()
            if (keyboardView.swipeTotalCnt > 0) {  // swipe
                keyboardView.keyboardActionListener!!.onSwipeFinish(keyboardView, keyboardView.swipeCnt)
            } else if (Math.abs(dx) < 15 && Math.abs(dy) < 15) {  // click
                keyboardView.keyboardActionListener!!.onClick(keyboardView)
                vibrate()
            }
            keyboardView.keyboardActionListener!!.onTouchUp(keyboardView)
        }

        keyboardView.hideKeyboardArrowBg()
    }

    fun setDownPoint(x: Float, y: Float) {
        downPoint.x = x
        downPoint.y = y
    }

    fun setPoint(x: Float, y: Float) {
        point.x = x
        point.y = y
    }

    fun getKeySetByDegree(degree: Double): KeySet {
        var idx = 0
        val degreeNumber = getDegreeNumber(degree)
        if (direction == 4) {
            if (isLeft) {
                isCorner = true
                if (degreeNumber >= 0 && degreeNumber < 4) {
                    idx = IDX_RIGHT_BOT
                    currentKeySetView = mRightBotView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_right_bottom
                } else if (degreeNumber >= 4 && degreeNumber < 8) {
                    idx = IDX_LEFT_BOT
                    currentKeySetView = mLeftBotView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_left_bottom
                } else if (degreeNumber >= 8 && degreeNumber < 12) {
                    idx = IDX_LEFT_TOP
                    currentKeySetView = mLeftTopView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_left_top
                } else {
                    idx = IDX_RIGHT_TOP
                    currentKeySetView = mRightTopView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_right_top
                }
            } else {
                isCorner = false
                if (degreeNumber >= 2 && degreeNumber < 6) {
                    idx = IDX_MID_BOT
                    currentKeySetView = mMidBotView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_mid_bottom
                } else if (degreeNumber >= 6 && degreeNumber < 10) {
                    idx = IDX_LEFT_MID
                    currentKeySetView = mLeftMidView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_left_mid
                } else if (degreeNumber >= 10 && degreeNumber < 14) {
                    idx = IDX_MID_TOP
                    currentKeySetView = mMidTopView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_mid_top
                } else {
                    idx = IDX_RIGHT_MID
                    currentKeySetView = mRightMidView
                    currentKeyboardCenterImg = R.drawable.keyboard_center_right_mid
                }
            }
        } else {
            if (degreeNumber == 9 || degreeNumber == 10) {
                isCorner = true
                idx = IDX_LEFT_TOP
                currentKeySetView = mLeftTopView
                currentKeyboardCenterImg = R.drawable.keyboard_center_left_top
            } else if (degreeNumber == 11 || degreeNumber == 12) {
                isCorner = false
                idx = IDX_MID_TOP
                currentKeySetView = mMidTopView
                currentKeyboardCenterImg = R.drawable.keyboard_center_mid_top
            } else if (degreeNumber == 13 || degreeNumber == 14) {
                isCorner = true
                idx = IDX_RIGHT_TOP
                currentKeySetView = mRightTopView
                currentKeyboardCenterImg = R.drawable.keyboard_center_right_top
            } else if (degreeNumber == 15 || degreeNumber == 0) {
                isCorner = false
                idx = IDX_RIGHT_MID
                currentKeySetView = mRightMidView
                currentKeyboardCenterImg = R.drawable.keyboard_center_right_mid
            } else if (degreeNumber == 1 || degreeNumber == 2) {
                isCorner = true
                idx = IDX_RIGHT_BOT
                currentKeySetView = mRightBotView
                currentKeyboardCenterImg = R.drawable.keyboard_center_right_bottom
            } else if (degreeNumber == 3 || degreeNumber == 4) {
                isCorner = false
                idx = IDX_MID_BOT
                currentKeySetView = mMidBotView
                currentKeyboardCenterImg = R.drawable.keyboard_center_mid_bottom
            } else if (degreeNumber == 5 || degreeNumber == 6) {
                isCorner = true
                idx = IDX_LEFT_BOT
                currentKeySetView = mLeftBotView
                currentKeyboardCenterImg = R.drawable.keyboard_center_left_bottom
            } else if (degreeNumber == 7 || degreeNumber == 8) {
                isCorner = false
                idx = IDX_LEFT_MID
                currentKeySetView = mLeftMidView
                currentKeyboardCenterImg = R.drawable.keyboard_center_left_mid
            }
        }
        return mKeySetList!![idx]
    }

    private fun getDegreeNumber(degree: Double): Int {
        return if (degree >= 0 && degree < 23)
            0
        else if (degree >= 23 && degree < 45)
            1
        else if (degree >= 45 && degree < 68)
            2
        else if (degree >= 68 && degree < 90)
            3
        else if (degree >= 90 && degree < 113)
            4
        else if (degree >= 113 && degree < 135)
            5
        else if (degree >= 135 && degree < 158)
            6
        else if (degree >= 158 && degree < 180)
            7
        else if (degree >= 180 && degree < 203)
            8
        else if (degree >= 203 && degree < 225)
            9
        else if (degree >= 225 && degree < 248)
            10
        else if (degree >= 248 && degree < 270)
            11
        else if (degree >= 270 && degree < 293)
            12
        else if (degree >= 293 && degree < 315)
            13
        else if (degree >= 315 && degree < 338)
            14
        else
            15
    }

    companion object {

        private val IDX_LEFT_TOP = 0
        private val IDX_MID_TOP = 1
        private val IDX_RIGHT_TOP = 2
        private val IDX_LEFT_MID = 3
        private val IDX_RIGHT_MID = 4
        private val IDX_LEFT_BOT = 5
        private val IDX_MID_BOT = 6
        private val IDX_RIGHT_BOT = 7
        private var sIsVibrate = true
        fun setIsVibrate(isVibrate: Boolean) {
            sIsVibrate = isVibrate
        }
    }

}
