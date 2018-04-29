package com.teuskim.nlkeyboard

class RecommendWord : Comparable<RecommendWord> {

    var mWordId: Int = 0
    var mWord: String? = null
    var mUseCntNext: Double = 0.0
    var mUseCntNextSum: Double = 0.0
    var mUseCntXxx: Double = 0.0
    var mUseCntXxxSum: Double = 0.0
    var mUseCntTotal: Double = 0.0
    var mUseCntTotalSum: Double = 0.0
    var mUseCntN: Double = 0.0
    var mUseCntNSum: Double = 0.0

    var mPoint: Double = 0.toDouble()

    var mUpdDt: String? = null
    var mDiffTime: Long = 0

    fun generatePoint(affinityWeight: Double, useCntXxxWeight: Double, useCntTotalWeight: Double, useCntNWeight: Double, availableTime: Long) {
        val p1: Double = if (mUseCntNextSum > 0) mUseCntNext / mUseCntNextSum else 0.0
        val p2: Double = if (mUseCntXxxSum > 0) mUseCntXxx / mUseCntXxxSum else 0.0
        val p3: Double = if (mUseCntTotalSum > 0) mUseCntTotal / mUseCntTotalSum else 0.0
        val p4: Double = if (mUseCntNSum > 0) mUseCntN / mUseCntNSum else 0.0

        mPoint = p1 * affinityWeight + p2 * useCntXxxWeight + p3 * useCntTotalWeight + p4 * useCntNWeight
        if (mDiffTime > 0 && mDiffTime > availableTime)
            mPoint = mPoint * 0.5
    }

    fun generateDiffTime(currTime: Long) {
        if (mUpdDt != null && mUpdDt!!.length > 0) {
            mDiffTime = currTime - java.lang.Long.valueOf(mUpdDt)!!
        }
    }

    override fun compareTo(another: RecommendWord): Int {
        val diff = mPoint - another.mPoint
        var result = 0
        if (diff < 0)
            result = 1
        else if (diff > 0)
            result = -1
        return result
    }

}
