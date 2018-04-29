package com.teuskim.nlkeyboard;

import java.util.List;

import android.content.Context;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.teuskim.nlkeyboard.NLKeyboard.KeySet;

/**
 * 키보드 뷰 클래스
 * 1. xml layout 으로 뷰 생성한다.
 * 2. Keyboard 객체로부터 키 데이터를 받아와서 작은원세트들을 출력한다.
 * 3. 사용자 터치를 분석하여, 리스너의 적절한 콜백 메소드를 호출한다.
 * 
 * @author kim5724
 *
 */
public class NLKeyboardView extends RelativeLayout {
	
	public interface OnKeyboardActionListener {
		/**
		 * MotionEvent.ACTION_DOWN 발생시 호출
		 */
		void onTouchDown(NLKeyboardView keyboardView);
		
		/**
		 * MotionEvent.ACTION_UP 발생시 호출
		 */
		void onTouchUp(NLKeyboardView keyboardView);
		
		/**
		 * 스와잎이 발생하면 호출한다. 첫 스와잎에서 keySet 이 결정된다.
		 */
		void onSwipe(NLKeyboardView keyboardView, NLKeyboard.KeySet keySet, int swipeCnt);
		
		/**
		 * 스와잎이 끝나면 호출한다. 최종 글자가 결정된다.
		 */
		void onSwipeFinish(NLKeyboardView keyboardView, int swipeCnt);
		
		/**
		 * 스와잎 없이 클릭이 발생하면 호출한다. 키보드 객체를 변경한다.
		 */
		void onClick(NLKeyboardView keyboardView);
	}
	
	private static final int IDX_LEFT_TOP = 0;
	private static final int IDX_MID_TOP = 1;
	private static final int IDX_RIGHT_TOP = 2;
	private static final int IDX_LEFT_MID = 3;
	private static final int IDX_RIGHT_MID = 4;
	private static final int IDX_LEFT_BOT = 5;
	private static final int IDX_MID_BOT = 6;
	private static final int IDX_RIGHT_BOT = 7;
	
	private OnKeyboardActionListener mKeyboardActionListener;
	private NLKeyboard mKeyboard;
	private int mSwipeTotalCnt;  // 터치한 상태에서 swipe한 횟수
	private double mMoveAngle;  // swipe하는 방향의 각도
	private PointF mDownPoint = new PointF();  // 터치 다운 이벤트 받은 위치값
	private PointF mPoint = new PointF();  // 터치 이벤트 받은 가장 최근 위치값
	private List<NLKeyboard.KeySet> mKeySetList;
	private NLKeyboard.KeySet mKeySet;
	private int mDirection;
	private boolean mIsLeft;  // 왼쪽이 X모양, 오른쪽이 +모양
	private int mSelectedKeyInt;
	
	private KeySetView mLeftTopView;
	private KeySetView mMidTopView;
	private KeySetView mRightTopView;
	private KeySetView mLeftMidView;
	private KeySetView mRightMidView;
	private KeySetView mLeftBotView;
	private KeySetView mMidBotView;
	private KeySetView mRightBotView;
	private KeySetView mCurrentKeySetView;  // 현재 입력 진행중인 키셋뷰
	private ImageView mKeyboardCenterX;
	private ImageView mKeyboardCenterPlus;
	private ImageView mKeyboardBgX;
	private ImageView mKeyboardBgPlus;
	private int mCurrentKeyboardCenterImg;
	private boolean mIsCorner;  // 코너쪽의 방향 키셋 ( X 모양 )이 선택되었나?
	
	private NLKeyboardView mOppositeKeyboardView;
	
	private Vibrator mVibrator;
	private static boolean sIsVibrate = true;
	public static void setIsVibrate(boolean isVibrate){
		sIsVibrate = isVibrate;
	}
	public void vibrate(){
		if(sIsVibrate){
			try{
				mVibrator.vibrate(30);
			}catch(Exception e){}
		}
	}

	public NLKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public NLKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public NLKeyboardView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context){
		// xml layout 으로 뷰를 생성하고, 멤버 변수들을 초기화 한다.
		LayoutInflater.from(context).inflate(R.layout.keyboard, this);
		
		mLeftTopView = (KeySetView) findViewById(R.id.keyset_left_top);
		mMidTopView = (KeySetView) findViewById(R.id.keyset_mid_top);
		mRightTopView = (KeySetView) findViewById(R.id.keyset_right_top);
		mLeftMidView = (KeySetView) findViewById(R.id.keyset_left_mid);
		mRightMidView = (KeySetView) findViewById(R.id.keyset_right_mid);
		mLeftBotView = (KeySetView) findViewById(R.id.keyset_left_bot);
		mMidBotView = (KeySetView) findViewById(R.id.keyset_mid_bot);
		mRightBotView = (KeySetView) findViewById(R.id.keyset_right_bot);
		mKeyboardCenterX = (ImageView) findViewById(R.id.keyboard_center_x);
		mKeyboardCenterPlus = (ImageView) findViewById(R.id.keyboard_center_plus);
		mKeyboardBgX = (ImageView) findViewById(R.id.keyboard_bg_x);
		mKeyboardBgPlus = (ImageView) findViewById(R.id.keyboard_bg_plus);
		
		mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}
	
	public void setSize(int layoutSize, int centerSize, int keysetSize){
		ViewGroup.LayoutParams lp = getLayoutParams();
		lp.width = layoutSize;
		lp.height = layoutSize;
		setLayoutParams(lp);
		
		ViewGroup.LayoutParams lp2 = mKeyboardCenterX.getLayoutParams();
		lp2.width = centerSize;
		lp2.height = centerSize;
		mKeyboardCenterX.setLayoutParams(lp2);
		
		ViewGroup.LayoutParams lp3 = mKeyboardCenterPlus.getLayoutParams();
		lp3.width = centerSize;
		lp3.height = centerSize;
		mKeyboardCenterPlus.setLayoutParams(lp3);
		
		setKeysetSize(mLeftTopView, keysetSize);
		setKeysetSize(mMidTopView, keysetSize);
		setKeysetSize(mRightTopView, keysetSize);
		setKeysetSize(mLeftMidView, keysetSize);
		setKeysetSize(mRightMidView, keysetSize);
		setKeysetSize(mLeftBotView, keysetSize);
		setKeysetSize(mMidBotView, keysetSize);
		setKeysetSize(mRightBotView, keysetSize);
	}
	
	private void setKeysetSize(KeySetView keysetView, int keysetSize){
		if(keysetView.getChildCount() == 0)
			return;
		
		View view = keysetView.getChildAt(0);
		if(view == null)
			return;
		
		ViewGroup.LayoutParams lp = view.getLayoutParams();
		lp.width = keysetSize;
		lp.height = keysetSize;
		view.setLayoutParams(lp);
		
		if((view instanceof ViewGroup) == false || ((ViewGroup)view).getChildCount() < 2)
			return;
		
		int keysetHalf = keysetSize / 2 - 1;
		View v1 = ((ViewGroup)view).getChildAt(0);
		ViewGroup.LayoutParams lp1 = v1.getLayoutParams();
		lp1.height = keysetHalf;
		v1.setLayoutParams(lp1);
		
		View v11 = ((ViewGroup)v1).getChildAt(0);
		ViewGroup.LayoutParams lp11 = v11.getLayoutParams();
		lp11.width = keysetHalf;
		v11.setLayoutParams(lp11);
		
		View v12 = ((ViewGroup)v1).getChildAt(1);
		ViewGroup.LayoutParams lp12 = v12.getLayoutParams();
		lp12.width = keysetHalf;
		v12.setLayoutParams(lp12);
		
		View v2 = ((ViewGroup)view).getChildAt(1);
		ViewGroup.LayoutParams lp2 = v1.getLayoutParams();
		lp2.height = keysetHalf;
		v2.setLayoutParams(lp2);
		
		View v21 = ((ViewGroup)v2).getChildAt(0);
		ViewGroup.LayoutParams lp21 = v21.getLayoutParams();
		lp21.width = keysetHalf;
		v21.setLayoutParams(lp21);
		
		View v22 = ((ViewGroup)v2).getChildAt(1);
		ViewGroup.LayoutParams lp22 = v22.getLayoutParams();
		lp22.width = keysetHalf;
		v22.setLayoutParams(lp22);
	}
	
	public void setOppositeKeyboardView(NLKeyboardView keyboardView){
		mOppositeKeyboardView = keyboardView;
	}
	
	public void setKeyboard(NLKeyboard keyboard){
		// 키보드 멤버 변수 셋팅하고, 뷰를 갱신한다.
		mKeyboard = keyboard;
		mKeySetList = mKeyboard.getKeySets();
		mLeftTopView.setKeySet(mKeySetList.get(IDX_LEFT_TOP));
		mMidTopView.setKeySet(mKeySetList.get(IDX_MID_TOP));
		mRightTopView.setKeySet(mKeySetList.get(IDX_RIGHT_TOP));
		mLeftMidView.setKeySet(mKeySetList.get(IDX_LEFT_MID));
		mRightMidView.setKeySet(mKeySetList.get(IDX_RIGHT_MID));
		mLeftBotView.setKeySet(mKeySetList.get(IDX_LEFT_BOT));
		mMidBotView.setKeySet(mKeySetList.get(IDX_MID_BOT));
		mRightBotView.setKeySet(mKeySetList.get(IDX_RIGHT_BOT));
	}
	
	public NLKeyboard getKeyboard(){
		return mKeyboard;
	}
	
	public void setOnKeyboardActionListener(OnKeyboardActionListener listener){
		// 리스너 멤버 변수 셋팅한다.
		mKeyboardActionListener = listener;
	}
	
	public boolean isLeft(){
		return mIsLeft;
	}
	
	public int getDirection(){
		return mDirection;
	}
	
	public boolean isCorner(){
		return mIsCorner;
	}
	
	public int getSelectedKeyInt(){
		return mSelectedKeyInt;
	}
	public void setSelectedKeyInt(int keyInt){
		mSelectedKeyInt = keyInt;
	}
	
	public void setDirectionInfo(int direction, boolean isLeft){
		mDirection = direction;
		mIsLeft = isLeft;
		
		if(mDirection == 8){
			mLeftTopView.setVisibility(View.VISIBLE);
			mMidTopView.setVisibility(View.VISIBLE);
			mRightTopView.setVisibility(View.VISIBLE);
			mRightMidView.setVisibility(View.VISIBLE);
			mRightBotView.setVisibility(View.VISIBLE);
			mMidBotView.setVisibility(View.VISIBLE);
			mLeftBotView.setVisibility(View.VISIBLE);
			mLeftMidView.setVisibility(View.VISIBLE);
			
			mKeyboardCenterX.setVisibility(View.VISIBLE);
			mKeyboardCenterPlus.setVisibility(View.VISIBLE);
		}
		else if(mDirection == 4){
			if(isLeft){  // X모양
				mLeftTopView.setVisibility(View.VISIBLE);
				mMidTopView.setVisibility(View.GONE);
				mRightTopView.setVisibility(View.VISIBLE);
				mRightMidView.setVisibility(View.GONE);
				mRightBotView.setVisibility(View.VISIBLE);
				mMidBotView.setVisibility(View.GONE);
				mLeftBotView.setVisibility(View.VISIBLE);
				mLeftMidView.setVisibility(View.GONE);
				
				mKeyboardCenterX.setVisibility(View.VISIBLE);
				mKeyboardCenterPlus.setVisibility(View.GONE);
			}
			else{  // +모양
				mLeftTopView.setVisibility(View.GONE);
				mMidTopView.setVisibility(View.VISIBLE);
				mRightTopView.setVisibility(View.GONE);
				mRightMidView.setVisibility(View.VISIBLE);
				mRightBotView.setVisibility(View.GONE);
				mMidBotView.setVisibility(View.VISIBLE);
				mLeftBotView.setVisibility(View.GONE);
				mLeftMidView.setVisibility(View.VISIBLE);
				
				mKeyboardCenterX.setVisibility(View.GONE);
				mKeyboardCenterPlus.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void showKeyboardArrowBg(){
		if(mDirection == 8){
			mKeyboardBgX.setVisibility(View.VISIBLE);
			mKeyboardBgPlus.setVisibility(View.VISIBLE);
		}
		else if(mDirection == 4){
			if(mIsLeft){  // X 모양
				mKeyboardBgX.setVisibility(View.VISIBLE);
				mKeyboardBgPlus.setVisibility(View.GONE);
			}
			else{  // + 모양
				mKeyboardBgX.setVisibility(View.GONE);
				mKeyboardBgPlus.setVisibility(View.VISIBLE);
			}
		}
	}
	
	public void hideKeyboardArrowBg(){
		mKeyboardBgX.setVisibility(View.GONE);
		mKeyboardBgPlus.setVisibility(View.GONE);
	}
	
	private int mMyIndex = -1;
	private int mOppositeIndex = -1;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 터치 분석하여 상황에 맞는 콜백 메소드를 호출해 준다.
		int action = event.getAction();
		switch(action & MotionEvent.ACTION_MASK){
		case MotionEvent.ACTION_DOWN:
			mMyIndex = getPointerIndex(action);
			mOppositeIndex = -1;
			actionDown(this, event.getX(mMyIndex), event.getY(mMyIndex));			
			break;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			if(mMyIndex != -1){
				mOppositeIndex = getPointerIndex(action);
				actionDown(mOppositeKeyboardView, event.getX(mOppositeIndex), event.getY(mOppositeIndex));
			}
			else{
				mMyIndex = getPointerIndex(action);
				actionDown(this, event.getX(mMyIndex), event.getY(mMyIndex));
			}
			break;
			
		case MotionEvent.ACTION_MOVE:
			if(mMyIndex != -1){
				float x = event.getX(mMyIndex);
				float y = event.getY(mMyIndex);
				actionMove(this, x, y);
			}
			if(mOppositeIndex != -1){
				float x = event.getX(mOppositeIndex);
				float y = event.getY(mOppositeIndex);
				actionMove(mOppositeKeyboardView, x, y);
			}
			break;
			
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_UP:
			if(getPointerIndex(action) == mMyIndex){
				actionUp(this, event.getX(mMyIndex), event.getY(mMyIndex));
				mMyIndex = -1;
			}
			else{
				actionUp(mOppositeKeyboardView, event.getX(mOppositeIndex), event.getY(mOppositeIndex));
				mOppositeIndex = -1;
			}
			break;
		}
		return true;
	}
	
	private int getPointerIndex(int action){
		return (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	}
	
	public void actionDown(NLKeyboardView keyboardView, float x, float y){
		// 변수값 초기화하고, 터치위치 저장
		keyboardView.setSwipeTotalCnt(0);
		keyboardView.setMoveAngle(-1);
		keyboardView.setDownPoint(x, y);
		keyboardView.setPoint(x, y);
		if(keyboardView.getKeyboardActionListener() != null)
			keyboardView.getKeyboardActionListener().onTouchDown(keyboardView);
		keyboardView.showKeyboardArrowBg();
	}
	
	public void actionMove(NLKeyboardView keyboardView, float x, float y){
		// 이전 터치위치와 비교하여 방향값 구하고, 처음이면 KeySet 저장, 방향값이 유효범위 밖이면 swipe cnt 증가
		PointF point = keyboardView.getPoint();
		PointF downPoint = keyboardView.getDownPoint();
		double diffx = x - point.x;
		double diffy = y - point.y;			
		double diffxFromDown = x - downPoint.x;
		double diffyFromDown = y - downPoint.y;
		
		double moveAngle = 0;
		double moveAngleFromDown = 0;
		if((keyboardView.getSwipeTotalCnt() == 0 && (Math.abs(diffx) > keyboardView.getWidth()/10 || Math.abs(diffy) > keyboardView.getHeight()/10))
				|| (keyboardView.getSwipeTotalCnt() > 0 && (Math.abs(diffx) > 20 || Math.abs(diffy) > 20))){
			moveAngle = Math.toDegrees(Math.atan2(diffy, diffx));
			if(moveAngle < 0)
				moveAngle = moveAngle + 360;
			keyboardView.setPoint(x, y);
			
			moveAngleFromDown = Math.toDegrees(Math.atan2(diffyFromDown, diffxFromDown));
			if(moveAngleFromDown < 0)
				moveAngleFromDown = moveAngleFromDown + 360;
		}
		else{
			return;
		}
		
		if(keyboardView.getSwipeTotalCnt() == 0){
			keyboardView.setMoveAngle(moveAngleFromDown);
			keyboardView.setKeySet(keyboardView.getKeySetByDegree(keyboardView.getMoveAngle()));
			if(keyboardView.getKeySet().size() > 0){
				keyboardView.setSwipeTotalCnt(1);
				vibrate();
			}
			else{
				keyboardView.setDownPoint(x, y);
			}
		}
		else{
			double diffAngle = Math.abs(keyboardView.getMoveAngle()-moveAngle);
			if(diffAngle > 180)
				diffAngle = 360 - diffAngle;
			
			if(keyboardView.getSwipeTotalCnt() == 1){
				if(diffAngle > 90){
					keyboardView.setMoveAngle(moveAngle);
					keyboardView.setSwipeTotalCnt(keyboardView.getSwipeTotalCnt()+1);
					
					vibrate();
				}
				else if(diffAngle < 40){
					keyboardView.setMoveAngle(moveAngleFromDown);
					KeySet keyset = keyboardView.getKeySetByDegree(keyboardView.getMoveAngle());
					
					if(keyboardView.getKeySet() != keyset){
						keyboardView.setKeySet(keyset);
						
						vibrate();
					}
				}
			}
			else{
				if(diffAngle > 60){
					keyboardView.setMoveAngle(moveAngle);
					keyboardView.setSwipeTotalCnt(keyboardView.getSwipeTotalCnt()+1);
					
					vibrate();
				}
			}
		}
		
		if(keyboardView.getKeySet() != null && keyboardView.getSwipeTotalCnt() > 0){
			int swipeCnt = keyboardView.getSwipeCnt();
			keyboardView.getCurrentKeySetView().setSelected(swipeCnt-1);
			if(keyboardView.isCorner())
				keyboardView.getKeyboardCenterX().setImageResource(keyboardView.getCurrentKeyboardCenterImg());
			else
				keyboardView.getKeyboardCenterX().setImageResource(R.drawable.keyboard_center_x);
				
			if(keyboardView.isCorner())
				keyboardView.getKeyboardCenterPlus().setImageResource(R.drawable.keyboard_center_plus);
			else
				keyboardView.getKeyboardCenterPlus().setImageResource(keyboardView.getCurrentKeyboardCenterImg());
				
			keyboardView.setSelectedKeyInt(keyboardView.getKeySet().getKey(swipeCnt-1));
			keyboardView.getKeyboardActionListener().onSwipe(keyboardView, keyboardView.getKeySet(), swipeCnt);
		}
	}
	
	public void actionUp(NLKeyboardView keyboardView, float x, float y){
		// swipe cnt가 0보다 크면 swipe 이고, 그렇지 않으면 click으로 처리
		if(keyboardView.getCurrentKeySetView() != null){
			keyboardView.getCurrentKeySetView().resetSelected();
			keyboardView.getKeyboardCenterX().setImageResource(R.drawable.keyboard_center_x);
			keyboardView.getKeyboardCenterPlus().setImageResource(R.drawable.keyboard_center_plus);
		}
		if(keyboardView.getKeyboardActionListener() != null){
			PointF point = keyboardView.getPoint();
			double dx = x - point.x;
			double dy = y - point.y;
			if(keyboardView.getSwipeTotalCnt() > 0){  // swipe
				keyboardView.getKeyboardActionListener().onSwipeFinish(keyboardView, keyboardView.getSwipeCnt());
			}
			else if(Math.abs(dx) < 15 && Math.abs(dy) < 15){  // click
				keyboardView.getKeyboardActionListener().onClick(keyboardView);
				vibrate();
			}
			keyboardView.getKeyboardActionListener().onTouchUp(keyboardView);
		}
		
		keyboardView.hideKeyboardArrowBg();
	}
	
	public int getSwipeTotalCnt(){
		return mSwipeTotalCnt;
	}
	public void setSwipeTotalCnt(int swipeTotalCnt){
		mSwipeTotalCnt = swipeTotalCnt;
	}
	public double getMoveAngle(){
		return mMoveAngle;
	}
	public void setMoveAngle(double moveAngle){
		mMoveAngle = moveAngle;
	}
	public PointF getDownPoint(){
		return mDownPoint;
	}
	public void setDownPoint(float x, float y){
		mDownPoint.x = x;
		mDownPoint.y = y;
	}
	public PointF getPoint(){
		return mPoint;
	}
	public void setPoint(float x, float y){
		mPoint.x = x;
		mPoint.y = y;
	}
	public OnKeyboardActionListener getKeyboardActionListener(){
		return mKeyboardActionListener;
	}
	public KeySet getKeySet(){
		return mKeySet;
	}
	public void setKeySet(KeySet keySet){
		mKeySet = keySet;
	}
	public KeySetView getCurrentKeySetView(){
		return mCurrentKeySetView;
	}
	public void setCurrentKeySetView(KeySetView keySetView){
		if(mCurrentKeySetView != null && mCurrentKeySetView != keySetView)
			mCurrentKeySetView.resetSelected();
		mCurrentKeySetView = keySetView;
	}
	public ImageView getKeyboardCenterX(){
		return mKeyboardCenterX;
	}
	public ImageView getKeyboardCenterPlus(){
		return mKeyboardCenterPlus;
	}
	public int getCurrentKeyboardCenterImg(){
		return mCurrentKeyboardCenterImg;
	}
	
	public int getSwipeCnt(){
		int swipeCnt = mSwipeTotalCnt % mKeySet.size();
		if(swipeCnt == 0)
			swipeCnt = mKeySet.size();
		return swipeCnt;
	}
	
	public KeySet getKeySetByDegree(double degree){
		int idx = 0;
		int degreeNumber = getDegreeNumber(degree);
		if(mDirection == 4){
			if(mIsLeft){
				mIsCorner = true;
				if(degreeNumber >= 0 && degreeNumber < 4){
					idx = IDX_RIGHT_BOT;
					setCurrentKeySetView(mRightBotView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_bottom;
				}
				else if(degreeNumber >= 4 && degreeNumber < 8){
					idx = IDX_LEFT_BOT;
					setCurrentKeySetView(mLeftBotView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_bottom;
				}
				else if(degreeNumber >= 8 && degreeNumber < 12){
					idx = IDX_LEFT_TOP; 
					setCurrentKeySetView(mLeftTopView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_top;
				}
				else{
					idx = IDX_RIGHT_TOP;
					setCurrentKeySetView(mRightTopView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_top;
				}
			}
			else{
				mIsCorner = false;
				if(degreeNumber >= 2 && degreeNumber < 6){
					idx = IDX_MID_BOT;
					setCurrentKeySetView(mMidBotView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_mid_bottom;
				}
				else if(degreeNumber >= 6 && degreeNumber < 10){
					idx = IDX_LEFT_MID;
					setCurrentKeySetView(mLeftMidView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_mid;
				}
				else if(degreeNumber >= 10 && degreeNumber < 14){
					idx = IDX_MID_TOP;
					setCurrentKeySetView(mMidTopView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_mid_top;
				}
				else{
					idx = IDX_RIGHT_MID;
					setCurrentKeySetView(mRightMidView);
					mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_mid;
				}
			}
		}
		else{
			if(degreeNumber == 9 || degreeNumber == 10){
				mIsCorner = true;
				idx = IDX_LEFT_TOP;
				setCurrentKeySetView(mLeftTopView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_top;
			}
			else if(degreeNumber == 11 || degreeNumber == 12){
				mIsCorner = false;
				idx = IDX_MID_TOP;
				setCurrentKeySetView(mMidTopView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_mid_top;
			}
			else if(degreeNumber == 13 || degreeNumber == 14){
				mIsCorner = true;
				idx = IDX_RIGHT_TOP;
				setCurrentKeySetView(mRightTopView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_top;
			}
			else if(degreeNumber == 15 || degreeNumber == 0){
				mIsCorner = false;
				idx = IDX_RIGHT_MID;
				setCurrentKeySetView(mRightMidView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_mid;
			}
			else if(degreeNumber == 1 || degreeNumber == 2){
				mIsCorner = true;
				idx = IDX_RIGHT_BOT;
				setCurrentKeySetView(mRightBotView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_right_bottom;
			}
			else if(degreeNumber == 3 || degreeNumber == 4){
				mIsCorner = false;
				idx = IDX_MID_BOT;
				setCurrentKeySetView(mMidBotView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_mid_bottom;
			}
			else if(degreeNumber == 5 || degreeNumber == 6){
				mIsCorner = true;
				idx = IDX_LEFT_BOT;
				setCurrentKeySetView(mLeftBotView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_bottom;
			}
			else if(degreeNumber == 7 || degreeNumber == 8){
				mIsCorner = false;
				idx = IDX_LEFT_MID;
				setCurrentKeySetView(mLeftMidView);
				mCurrentKeyboardCenterImg = R.drawable.keyboard_center_left_mid;
			}
		}
		return mKeySetList.get(idx);
	}
	
	private int getDegreeNumber(double degree){
		if(degree >= 0 && degree < 23)
			return 0;
		else if(degree >= 23 && degree < 45)
			return 1;
		else if(degree >= 45 && degree < 68)
			return 2;
		else if(degree >= 68 && degree < 90)
			return 3;
		else if(degree >= 90 && degree < 113)
			return 4;
		else if(degree >= 113 && degree < 135)
			return 5;
		else if(degree >= 135 && degree < 158)
			return 6;
		else if(degree >= 158 && degree < 180)
			return 7;
		else if(degree >= 180 && degree < 203)
			return 8;
		else if(degree >= 203 && degree < 225)
			return 9;
		else if(degree >= 225 && degree < 248)
			return 10;
		else if(degree >= 248 && degree < 270)
			return 11;
		else if(degree >= 270 && degree < 293)
			return 12;
		else if(degree >= 293 && degree < 315)
			return 13;
		else if(degree >= 315 && degree < 338)
			return 14;
		else
			return 15;
	}
	
}
