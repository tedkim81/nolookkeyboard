package com.teuskim.nlkeyboard;

public class RecommendWord implements Comparable<RecommendWord> {
	
	public int mWordId;
	public String mWord;
	public double mUseCntNext;
	public double mUseCntNextSum;
	public double mUseCntXxx;
	public double mUseCntXxxSum;
	public double mUseCntTotal;
	public double mUseCntTotalSum;
	public double mUseCntN;
	public double mUseCntNSum;
	
	public double mPoint;
	
	public String mUpdDt;
	public long mDiffTime;
	
	public void generatePoint(double affinityWeight, double useCntXxxWeight, double useCntTotalWeight, double useCntNWeight, long availableTime){
		double p1 = (mUseCntNextSum > 0) ? (mUseCntNext / mUseCntNextSum) : 0;
		double p2 = (mUseCntXxxSum > 0) ? (mUseCntXxx / mUseCntXxxSum) : 0;
		double p3 = (mUseCntTotalSum > 0) ? (mUseCntTotal / mUseCntTotalSum) : 0;
		double p4 = (mUseCntNSum > 0) ? (mUseCntN / mUseCntNSum) : 0;
		double w1 = affinityWeight;
		double w2 = useCntXxxWeight;
		double w3 = useCntTotalWeight;
		double w4 = useCntNWeight;
//		Log.e("AAAA", "generatePoint: "+mWord+" , "+p1+","+p2+","+p3+","+p4+" -- "+w1+","+w2+","+w3+","+w4);
		
		mPoint = (p1*w1) + (p2*w2) + (p3*w3) + (p4*w4);
		if(mDiffTime > 0 && mDiffTime > availableTime)
			mPoint = mPoint * 0.5;
	}
	
	public void generateDiffTime(long currTime){
		if(mUpdDt != null && mUpdDt.length() > 0){
			mDiffTime = currTime - Long.valueOf(mUpdDt);
		}
	}
	
	@Override
	public int compareTo(RecommendWord another) {
		double diff = mPoint - another.mPoint;
		int result = 0;
		if(diff < 0)
			result = 1;
		else if(diff > 0)
			result = -1;
		return result;
	}
	
}
