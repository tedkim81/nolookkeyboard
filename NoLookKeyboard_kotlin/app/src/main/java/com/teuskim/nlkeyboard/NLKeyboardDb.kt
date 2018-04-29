package com.teuskim.nlkeyboard

import java.util.ArrayList
import java.util.Calendar
import java.util.Date

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.view.inputmethod.EditorInfo

class NLKeyboardDb private constructor(private val mContext: Context) {

    private var mDb: SQLiteDatabase? = null

    /**
     * 4방향인지 8방향인지 구하기
     * @return
     */
    val direction: Int
        get() {
            var direction = 8
            try {
                val cursor = mDb!!.query(MyInfo.TABLE_NAME, arrayOf(MyInfo.DIRECTION), null, null, null, null, null)

                if (cursor.moveToFirst()) {
                    direction = cursor.getInt(0)
                }
                cursor.close()
            } catch (e: Exception) {
            }

            return direction
        }

    /**
     * 사용자 정의 키보드셋 리스트 구하기
     * @return
     */
    val customKeySetList: List<CustomKeyset>
        get() {
            val result = ArrayList<CustomKeyset>()
            try {
                val cursor = mDb!!.query(CustomKeyset.TABLE_NAME, arrayOf(CustomKeyset._ID, CustomKeyset.NAME, CustomKeyset.SHOW_4, CustomKeyset.SHOW_8), null, null, null, null, null)

                if (cursor.moveToFirst()) {
                    do {
                        val keyset = CustomKeyset()
                        keyset.mId = cursor.getInt(0)
                        keyset.mName = cursor.getString(1)
                        keyset.mShow4 = cursor.getInt(2)
                        keyset.mShow8 = cursor.getInt(3)
                        result.add(keyset)
                    } while (cursor.moveToNext())
                }
                cursor.close()
            } catch (e: Exception) {
            }

            return result
        }

    /**
     * 저장된 최근단어 총갯수 구하기
     * @return
     */
    val historyCount: Int
        get() {
            var count = 0
            try {
                val cursor = mDb!!.rawQuery("select count(*) from " + History.TABLE_NAME, null)
                if (cursor.moveToFirst())
                    count = cursor.getInt(0)
                cursor.close()
            } catch (e: Exception) {
            }

            return count
        }

    /**
     * 내정보 가져오기
     * @return
     */
    val myInfo: MyInfo
        get() {
            val myInfo = MyInfo()
            try {
                val cursor = mDb!!.query(MyInfo.TABLE_NAME, arrayOf(MyInfo.DIRECTION, MyInfo.USE_HISTORY, MyInfo.USE_CNT_TOTAL_WEIGHT, MyInfo.USE_CNT_N_WEIGHT, MyInfo.USE_CNT_XXX_WEIGHT, MyInfo.AVAILABLE_PERIOD), null, null, null, null, null)

                if (cursor.moveToFirst()) {
                    myInfo.mDirection = cursor.getInt(0)
                    myInfo.mUseHistory = cursor.getInt(1)
                    myInfo.mUseCntTotalWeight = if (cursor.getInt(2) > 0) cursor.getInt(2) else WEIGHT_INIT_TOTAL
                    myInfo.mUseCntNWeight = if (cursor.getInt(3) > 0) cursor.getInt(3) else WEIGHT_INIT_TOTAL
                    myInfo.mUseCntXxxWeight = if (cursor.getInt(4) > 0) cursor.getInt(4) else WEIGHT_INIT_TOTAL
                    myInfo.mAvailablePeriod = cursor.getInt(5)
                }
                cursor.close()
            } catch (e: Exception) {
            }

            return myInfo
        }

    /**
     * TODO: For Test
     * word 목록 모두 가져오기
     */
    val wordList: List<Word>
        get() {
            val wordList = ArrayList<Word>()
            try {
                val cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(Word.WORD, Word.COMPOSITION, Word.USE_CNT_TOTAL, Word.USE_CNT_0, Word.USE_CNT_4, Word.USE_CNT_8, Word.USE_CNT_12, Word.USE_CNT_16, Word.USE_CNT_20, Word.USE_CNT_NORMAL, Word.USE_CNT_EMAIL_ADDRESS, Word.USE_CNT_EMAIL_SUBJECT, Word.USE_CNT_URI, Word.USE_CNT_POSTAL_ADDRESS, Word.USE_CNT_PERSON_NAME, Word.USE_CNT_NUMBER, Word.UPD_DT, Word.CRT_DT, Word.WORD_ID), null, null, null, null, null)

                if (cursor.count > 0 && cursor.moveToFirst()) {
                    do {
                        val word = Word()
                        word.mWord = cursor.getString(0)
                        word.mComposition = cursor.getString(1)
                        word.mUseCntTotal = cursor.getInt(2)
                        word.mUseCnt0 = cursor.getInt(3)
                        word.mUseCnt4 = cursor.getInt(4)
                        word.mUseCnt8 = cursor.getInt(5)
                        word.mUseCnt12 = cursor.getInt(6)
                        word.mUseCnt16 = cursor.getInt(7)
                        word.mUseCnt20 = cursor.getInt(8)
                        word.mUseCntNormal = cursor.getInt(9)
                        word.mUseCntEmailAddress = cursor.getInt(10)
                        word.mUseCntEmailSubject = cursor.getInt(11)
                        word.mUseCntUri = cursor.getInt(12)
                        word.mUseCntPostalAddress = cursor.getInt(13)
                        word.mUseCntPersonName = cursor.getInt(14)
                        word.mUseCntNumber = cursor.getInt(15)
                        word.mUpdDt = cursor.getString(16)
                        word.mCrtDt = cursor.getString(17)
                        word.mWordId = cursor.getInt(18)
                        wordList.add(word)
                    } while (cursor.moveToNext())
                }
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return wordList
        }

    /**
     * TODO: For Test
     * next_word_group 모두 가져오기
     */
    val nextWordGroupList: List<NextWordGroup>
        get() {
            val nwgList = ArrayList<NextWordGroup>()
            try {
                val cursor = mDb!!.query(NextWordGroup.TABLE_NAME, arrayOf(NextWordGroup.WORD, NextWordGroup.NEXT_WORD, NextWordGroup.USE_CNT), null, null, null, null, null)

                if (cursor.count > 0 && cursor.moveToFirst()) {
                    do {
                        val nwg = NextWordGroup()
                        nwg.mWord = cursor.getString(0)
                        nwg.mNextWord = cursor.getString(1)
                        nwg.mUseCnt = cursor.getInt(2)
                        nwgList.add(nwg)
                    } while (cursor.moveToNext())
                }
                cursor.close()
            } catch (e: Exception) {
            }

            return nwgList
        }

    private fun open(context: Context): Boolean {
        val dbHelper: DbOpenHelper
        dbHelper = DbOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)
        mDb = dbHelper.writableDatabase
        return if (mDb == null) false else true
    }

    fun close() {
        mDb!!.close()
    }

    fun getKeyboardName(type: Int): String {
        when (type) {
            KEYBOARD_TYPE_ENGLISH -> return mContext.getString(R.string.txt_english)
            KEYBOARD_TYPE_HANGUL -> return mContext.getString(R.string.txt_hangul)
            KEYBOARD_TYPE_NUMBERS -> return mContext.getString(R.string.txt_numbers)
            KEYBOARD_TYPE_SYMBOLS -> return mContext.getString(R.string.txt_symbols)
        }
        return "Unknown"
    }

    /**
     * 4방향인지 8방향인지 업데이트 하기
     * @param direction
     * @return
     */
    fun updateDirection(direction: Int): Boolean {
        val values = ContentValues()
        values.put(MyInfo.DIRECTION, direction)

        return mDb!!.update(MyInfo.TABLE_NAME, values, null, null) > 0
    }

    /**
     * 최근단어 저장/입력 기능을 사용할지 여부 구하기
     * @return
     */
    fun useHistory(): Boolean {
        var useHistory = false
        try {
            val cursor = mDb!!.query(MyInfo.TABLE_NAME, arrayOf(MyInfo.USE_HISTORY), null, null, null, null, null)

            if (cursor.moveToFirst() && cursor.getInt(0) == 1) {
                useHistory = true
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return useHistory
    }

    /**
     * 최근단어 저장/입력 기능 사용여부 업데이트 하기
     * @param useHistory
     * @return
     */
    fun updateUseHistory(useHistory: Boolean): Boolean {
        val values = ContentValues()
        values.put(MyInfo.USE_HISTORY, if (useHistory) 1 else 0)

        return mDb!!.update(MyInfo.TABLE_NAME, values, null, null) > 0
    }

    /**
     * 키보드셋 리스트 구하기
     * @param side
     * @return
     */
    fun getKeySetList(side: Int): List<KeySet> {
        val result = ArrayList<KeySet>()
        try {
            val cursor = mDb!!.query(KeySet.TABLE_NAME, arrayOf(KeySet._ID, KeySet.TYPE, KeySet.SHOW_YN, KeySet.SIDE), KeySet.SIDE + "=" + side, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val keyset = KeySet()
                    keyset.mId = cursor.getInt(0)
                    keyset.mType = cursor.getInt(1)
                    keyset.mShowYN = cursor.getString(2)
                    keyset.mDirection = cursor.getInt(3)
                    result.add(keyset)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return result
    }

    /**
     * 키보드셋 노출여부 변경
     * @param id
     * @param isChecked
     * @return
     */
    fun updateKeySetChecked(id: Int, isChecked: Boolean): Boolean {
        val values = ContentValues()
        values.put(KeySet.SHOW_YN, if (isChecked) "Y" else "N")

        return mDb!!.update(KeySet.TABLE_NAME, values, KeySet._ID + "=" + id, null) > 0
    }

    /**
     * 사용자 정의 키보드셋 이름 구하기
     * @param id
     * @return
     */
    fun getCustomKeySetName(id: Int): String? {
        var name: String? = null
        try {
            val cursor = mDb!!.query(CustomKeyset.TABLE_NAME, arrayOf(CustomKeyset.NAME), CustomKeyset._ID + "=" + id, null, null, null, null)
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return name
    }

    /**
     * 사용자 정의 키보드셋 노출여부 구하기
     * @param id
     * @param direction
     * @return
     */
    fun getCustomKeySetShow(id: Int, direction: Int): Int {
        var show = CustomKeyset.SHOW_NONE
        try {
            val column: String
            if (direction == 4)
                column = CustomKeyset.SHOW_4
            else
                column = CustomKeyset.SHOW_8
            val cursor = mDb!!.query(CustomKeyset.TABLE_NAME, arrayOf(column), CustomKeyset._ID + "=" + id, null, null, null, null)
            if (cursor.moveToFirst()) {
                show = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return show
    }

    /**
     * 사용자 정의 키보드셋 노출여부 업데이트
     * @param id
     * @param show
     * @return
     */
    fun updateCustomKeySetShow(id: Int, show: Int, direction: Int): Boolean {
        val values = ContentValues()
        if (direction == 4)
            values.put(CustomKeyset.SHOW_4, show)
        else
            values.put(CustomKeyset.SHOW_8, show)

        return mDb!!.update(CustomKeyset.TABLE_NAME, values, CustomKeyset._ID + "=" + id, null) > 0
    }

    /**
     * 사용자 정의 키보드셋의 저장된 값 구하기
     * @param id
     * @return
     */
    fun getCustomKeySetDataList(id: Int): List<CustomKeysetData> {
        val result = ArrayList<CustomKeysetData>()
        try {
            val cursor = mDb!!.query(CustomKeysetData.TABLE_NAME, arrayOf(CustomKeysetData._ID, CustomKeysetData.CUSTOM_KEYSET_ID, CustomKeysetData.KEYSET_NUMBER, CustomKeysetData.DATA), CustomKeysetData.CUSTOM_KEYSET_ID + "=?", arrayOf("" + id), null, null, CustomKeysetData._ID + " asc")

            if (cursor.moveToFirst()) {
                do {
                    val keysetData = CustomKeysetData()
                    keysetData.mId = cursor.getInt(0)
                    keysetData.mCustomKeysetId = cursor.getInt(1)
                    keysetData.mKeysetNumber = cursor.getInt(2)
                    keysetData.mData = cursor.getString(3)
                    result.add(keysetData)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return result
    }

    /**
     * 사용자 정의 키보드셋 저장하기
     * @param name
     * @param showYN
     * @param side
     * @param leftTop
     * @param midTop
     * @param rightTop
     * @param leftMid
     * @param rightMid
     * @param leftBot
     * @param midBot
     * @param rightBot
     * @return
     */
    fun insertCustomKeyset(name: String?, show: Int, direction: Int, leftTop: List<String>?, midTop: List<String>?, rightTop: List<String>?, leftMid: List<String>?, rightMid: List<String>?, leftBot: List<String>?, midBot: List<String>?, rightBot: List<String>?): Boolean {

        if (name == null || name.length == 0)
            return false

        if ((leftTop == null || leftTop.size == 0)
                && (midTop == null || midTop.size == 0)
                && (rightTop == null || rightTop.size == 0)
                && (leftMid == null || leftMid.size == 0)
                && (rightMid == null || rightMid.size == 0)
                && (leftBot == null || leftBot.size == 0)
                && (midBot == null || midBot.size == 0)
                && (rightBot == null || rightBot.size == 0)) {
            return false
        }

        val values = ContentValues()
        values.put(CustomKeyset.NAME, name)
        if (direction == 4)
            values.put(CustomKeyset.SHOW_4, show)
        else
            values.put(CustomKeyset.SHOW_8, show)

        val rowID = mDb!!.insert(CustomKeyset.TABLE_NAME, null, values)
        if (rowID <= 0) {
            throw SQLException("Failed to insert row into " + CustomKeyset.TABLE_NAME)
        }

        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_TOP, leftTop)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_MID_TOP, midTop)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_TOP, rightTop)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_MID, leftMid)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_MID, rightMid)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_LEFT_BOT, leftBot)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_MID_BOT, midBot)
        insertCustomKeysetData(rowID, CustomKeysetData.KEYSET_NUMBER_RIGHT_BOT, rightBot)

        return true
    }

    private fun insertCustomKeysetData(id: Long, keysetNumber: Int, datas: List<String>?): Boolean {
        if (datas != null && datas.size > 0) {
            for (data in datas) {
                val values = ContentValues()
                values.put(CustomKeysetData.CUSTOM_KEYSET_ID, id)
                values.put(CustomKeysetData.KEYSET_NUMBER, keysetNumber)
                values.put(CustomKeysetData.DATA, data)
                val rowID = mDb!!.insert(CustomKeysetData.TABLE_NAME, null, values)
                if (rowID <= 0) {
                    throw SQLException("Failed to insert row into " + CustomKeysetData.TABLE_NAME)
                }
            }
            return true
        }
        return false
    }

    /**
     * 사용자정의 키보드셋 삭제하기
     * @param id
     * @return
     */
    fun deleteCustomKeyset(id: Long): Boolean {
        val deleteCnt = mDb!!.delete(CustomKeyset.TABLE_NAME, CustomKeyset._ID + "=" + id, null)
        mDb!!.delete(CustomKeysetData.TABLE_NAME, CustomKeysetData.CUSTOM_KEYSET_ID + "=" + id, null)
        return deleteCnt > 0
    }

    /**
     * 입력 히스토리 추가 또는 업데이트하기
     * @param keyboard
     * @param word
     * @param data
     * @return
     */
    fun insertORupdateHistory(keyboard: Int, word: String, data: String): Boolean {
        val historyId = getHistoryId(word)
        if (historyId > 0) {
            val useCnt = getHistoryUseCnt(historyId) + 1
            val values = ContentValues()
            values.put(History.USE_CNT, useCnt)
            mDb!!.update(History.TABLE_NAME, values, History._ID + "=?", arrayOf("" + historyId))
        } else {
            val values = ContentValues()
            values.put(History.KEYBOARD, keyboard)
            values.put(History.WORD, word)
            values.put(History.DATA, data)
            mDb!!.insert(History.TABLE_NAME, null, values)
        }
        return true
    }

    private fun getHistoryUseCnt(historyId: Int): Int {
        var useCnt = 0
        try {
            val cursor = mDb!!.query(History.TABLE_NAME, arrayOf(History.USE_CNT), History._ID + "=?", arrayOf("" + historyId), null, null, null)

            if (cursor.count > 0 && cursor.moveToFirst()) {
                useCnt = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return useCnt
    }

    /**
     * 입력단어로 아이디 가져오기
     * @param word
     * @return
     */
    fun getHistoryId(word: String): Int {
        var historyId = 0
        try {
            val cursor = mDb!!.query(History.TABLE_NAME, arrayOf(History._ID), History.WORD + "=?", arrayOf(word), null, null, null)

            if (cursor.count > 0 && cursor.moveToFirst()) {
                historyId = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return historyId
    }

    /**
     * 현재까지 입력된 값으로 like검색한 결과 구하기
     * @param preData
     * @param keyboard
     * @return
     */
    fun getHistoryList(preData: String, keyboard: Int): Cursor {
        return mDb!!.query(History.TABLE_NAME, arrayOf(History.WORD, History._ID, History.USE_CNT), History.DATA + " like ? and " + History.KEYBOARD + "=?", arrayOf(preData + "%", "" + keyboard), null, null, History.USE_CNT + " desc")
    }

    /**
     * 전체 히스토리 삭제
     * @return
     */
    fun deleteHistory(): Boolean {
        return mDb!!.delete(History.TABLE_NAME, null, null) > 0
    }

    /**
     * 입력단어로 word 객체 가져오기
     * @param word
     * @return
     */
    fun getWord(word: String): Word? {
        var wordObj: Word? = null
        try {
            val cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(Word.WORD, Word.COMPOSITION, Word.USE_CNT_TOTAL, Word.USE_CNT_0, Word.USE_CNT_4, Word.USE_CNT_8, Word.USE_CNT_12, Word.USE_CNT_16, Word.USE_CNT_20, Word.USE_CNT_NORMAL, Word.USE_CNT_EMAIL_ADDRESS, Word.USE_CNT_EMAIL_SUBJECT, Word.USE_CNT_URI, Word.USE_CNT_PERSON_NAME, Word.USE_CNT_POSTAL_ADDRESS, Word.USE_CNT_NUMBER, Word.UPD_DT, Word.CRT_DT, Word.WORD_ID), Word.WORD + "=?", arrayOf(word), null, null, null)

            if (cursor.count > 0 && cursor.moveToFirst()) {
                wordObj = Word()
                wordObj.mWord = cursor.getString(0)
                wordObj.mComposition = cursor.getString(1)
                wordObj.mUseCntTotal = cursor.getInt(2)
                wordObj.mUseCnt0 = cursor.getInt(3)
                wordObj.mUseCnt4 = cursor.getInt(4)
                wordObj.mUseCnt8 = cursor.getInt(5)
                wordObj.mUseCnt12 = cursor.getInt(6)
                wordObj.mUseCnt16 = cursor.getInt(7)
                wordObj.mUseCnt20 = cursor.getInt(8)
                wordObj.mUseCntNormal = cursor.getInt(9)
                wordObj.mUseCntEmailAddress = cursor.getInt(10)
                wordObj.mUseCntEmailSubject = cursor.getInt(11)
                wordObj.mUseCntUri = cursor.getInt(12)
                wordObj.mUseCntPersonName = cursor.getInt(13)
                wordObj.mUseCntPostalAddress = cursor.getInt(14)
                wordObj.mUseCntNumber = cursor.getInt(15)
                wordObj.mUpdDt = cursor.getString(16)
                wordObj.mCrtDt = cursor.getString(17)
                wordObj.mWordId = cursor.getInt(18)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return wordObj
    }

    /**
     * 입력 단어 추가 / 갱신
     * @param word
     * @param composition
     * @param typeTextVariation
     * @return
     */
    fun insertOrUpdateWord(word: String, composition: String, typeTextVariation: Int, columnUseCntXxx: String, hour: Int, columnUseCntN: String): Boolean {

        val wordObj = getWord(word)
        val now = Date().time

        if (wordObj != null && wordObj.mWord != null) {
            val useCntTotal = wordObj.mUseCntTotal + 1
            var useCntN = wordObj.mUseCnt0
            if (hour >= 4 && hour < 8)
                useCntN = wordObj.mUseCnt4
            else if (hour >= 8 && hour < 12)
                useCntN = wordObj.mUseCnt8
            else if (hour >= 12 && hour < 16)
                useCntN = wordObj.mUseCnt12
            else if (hour >= 16 && hour < 20)
                useCntN = wordObj.mUseCnt16
            else if (hour >= 20)
                useCntN = wordObj.mUseCnt20
            useCntN++

            var useCntXxx = wordObj.mUseCntNormal
            when (typeTextVariation) {
                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> useCntXxx = wordObj.mUseCntEmailAddress
                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> useCntXxx = wordObj.mUseCntEmailSubject
                EditorInfo.TYPE_TEXT_VARIATION_URI -> useCntXxx = wordObj.mUseCntUri
                EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME -> useCntXxx = wordObj.mUseCntPersonName
                EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> useCntXxx = wordObj.mUseCntPostalAddress
                EditorInfo.TYPE_CLASS_NUMBER -> useCntXxx = wordObj.mUseCntNumber
            }
            useCntXxx++

            val values = ContentValues()
            values.put(Word.USE_CNT_TOTAL, useCntTotal)
            values.put(columnUseCntN, useCntN)
            values.put(columnUseCntXxx, useCntXxx)
            return mDb!!.update(Word.TABLE_NAME, values, Word.WORD + "=?", arrayOf("" + wordObj.mWord!!)) > 0
        } else {
            val values = ContentValues()
            values.put(Word.WORD, word)
            values.put(Word.COMPOSITION, composition)
            values.put(Word.USE_CNT_TOTAL, 1)
            values.put(columnUseCntN, 1)
            values.put(columnUseCntXxx, 1)
            values.put(Word.UPD_DT, now)
            values.put(Word.CRT_DT, now)
            return mDb!!.insert(Word.TABLE_NAME, null, values) > 0
        }
    }

    fun getColumnUseCntN(hour: Int): String {
        var columnUseCntN = Word.USE_CNT_0
        if (hour >= 4 && hour < 8)
            columnUseCntN = Word.USE_CNT_4
        else if (hour >= 8 && hour < 12)
            columnUseCntN = Word.USE_CNT_8
        else if (hour >= 12 && hour < 16)
            columnUseCntN = Word.USE_CNT_12
        else if (hour >= 16 && hour < 20)
            columnUseCntN = Word.USE_CNT_16
        else if (hour >= 20)
            columnUseCntN = Word.USE_CNT_20

        return columnUseCntN
    }

    fun getColumnUseCntXXX(typeTextVariation: Int): String {
        var columnUseCntXxx = Word.USE_CNT_NORMAL  // 0
        when (typeTextVariation) {
            EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> columnUseCntXxx = Word.USE_CNT_EMAIL_ADDRESS
            EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> columnUseCntXxx = Word.USE_CNT_EMAIL_SUBJECT
            EditorInfo.TYPE_TEXT_VARIATION_URI -> columnUseCntXxx = Word.USE_CNT_URI
            EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME -> columnUseCntXxx = Word.USE_CNT_PERSON_NAME
            EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> columnUseCntXxx = Word.USE_CNT_POSTAL_ADDRESS
            EditorInfo.TYPE_CLASS_NUMBER -> columnUseCntXxx = Word.USE_CNT_NUMBER
        }// 32
        // 48
        // 16
        // 96
        // 112
        // 2

        return columnUseCntXxx
    }

    /**
     * 다음 단어 NextWordGroup 객체 가져오기
     */
    fun getNextWordGroup(word: String, nextWord: String): NextWordGroup? {
        var nextWordGroup: NextWordGroup? = null
        try {
            val cursor = mDb!!.query(NextWordGroup.TABLE_NAME, arrayOf(NextWordGroup.WORD, NextWordGroup.NEXT_WORD, NextWordGroup.USE_CNT), NextWordGroup.WORD + "=? and " + NextWordGroup.NEXT_WORD + "=?", arrayOf(word, nextWord), null, null, null)

            if (cursor.count > 0 && cursor.moveToFirst()) {
                nextWordGroup = NextWordGroup()
                nextWordGroup.mWord = cursor.getString(0)
                nextWordGroup.mNextWord = cursor.getString(1)
                nextWordGroup.mUseCnt = cursor.getInt(2)
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return nextWordGroup
    }

    /**
     * 다음 단어 정보 추가 / 갱신
     */
    fun insertOrUpdateNextWordGroup(word: String, nextWord: String): Boolean {
        val nextWordGroup = getNextWordGroup(word, nextWord)
        if (nextWordGroup != null && nextWordGroup.mNextWord != null) {
            val values = ContentValues()
            values.put(NextWordGroup.USE_CNT, nextWordGroup.mUseCnt + 1)
            return mDb!!.update(NextWordGroup.TABLE_NAME, values, NextWordGroup.WORD + "=? and " + NextWordGroup.NEXT_WORD + "=?", arrayOf(word, nextWord)) > 0
        } else {
            val values = ContentValues()
            values.put(NextWordGroup.WORD, word)
            values.put(NextWordGroup.NEXT_WORD, nextWord)
            values.put(NextWordGroup.USE_CNT, 1)
            return mDb!!.insert(NextWordGroup.TABLE_NAME, null, values) > 0
        }
    }

    /**
     * 다음단어 목록 구하기
     */
    fun getNextWordList(prevWord: String, composition: String?): List<RecommendWord> {
        var composition = composition
        val word = getWord(prevWord)
        val wordList = ArrayList<RecommendWord>()
        if (word != null && word.mWord != null) {
            try {
                var sql = ("select b." + NextWordGroup.NEXT_WORD + ",b." + NextWordGroup.USE_CNT
                        + " from " + Word.TABLE_NAME + " a," + NextWordGroup.TABLE_NAME + " b"
                        + " where a." + Word.WORD + "=b." + NextWordGroup.WORD
                        + " and b." + NextWordGroup.WORD + "=? and a." + Word.COMPOSITION + " like ?"
                        + " order by b." + NextWordGroup.USE_CNT + " desc, a." + Word.UPD_DT + " desc"
                        + " limit 100")
                sql = ("select tb1.*, tb2." + Word.WORD_ID + ", tb2." + Word.UPD_DT
                        + " from ( " + sql + " ) tb1, " + Word.TABLE_NAME + " tb2"
                        + " where tb1." + NextWordGroup.NEXT_WORD + "=tb2." + Word.WORD)
                if (composition == null) composition = ""
                val cursor = mDb!!.rawQuery(sql, arrayOf<String>(word.mWord!!, composition + "_%"))

                if (cursor.count > 0 && cursor.moveToFirst()) {
                    var sum = 0
                    do {
                        val rw = RecommendWord()
                        rw.mWord = cursor.getString(0)
                        rw.mUseCntNext = cursor.getInt(1).toDouble()
                        rw.mWordId = cursor.getInt(2)
                        rw.mUpdDt = cursor.getString(3)
                        wordList.add(rw)

                        sum += rw.mUseCntNext.toInt()
                    } while (cursor.moveToNext())

                    for (rw in wordList) {
                        rw.mUseCntNextSum = sum.toDouble()
                    }
                }
                cursor.close()
            } catch (e: Exception) {
            }

        }
        return wordList
    }

    /**
     * 입력영역속성에 따른 단어 목록 구하기
     */
    fun getWordListByAttr(composition: String?, typeTextVariation: Int): List<RecommendWord> {
        var composition = composition
        val wordList = ArrayList<RecommendWord>()
        try {
            var columnUseCntXxx = Word.USE_CNT_NORMAL
            when (typeTextVariation) {
                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> columnUseCntXxx = Word.USE_CNT_EMAIL_ADDRESS
                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> columnUseCntXxx = Word.USE_CNT_EMAIL_SUBJECT
                EditorInfo.TYPE_TEXT_VARIATION_URI -> columnUseCntXxx = Word.USE_CNT_URI
                EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME -> columnUseCntXxx = Word.USE_CNT_PERSON_NAME
                EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> columnUseCntXxx = Word.USE_CNT_POSTAL_ADDRESS
                EditorInfo.TYPE_CLASS_NUMBER -> columnUseCntXxx = Word.USE_CNT_NUMBER
            }

            if (composition == null) composition = ""
            val cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(Word.WORD, columnUseCntXxx, Word.WORD_ID, Word.UPD_DT), Word.COMPOSITION + " like ?", arrayOf(composition + "_%"), null, null, columnUseCntXxx + " desc, " + Word.UPD_DT + " desc", "100")

            if (cursor.count > 0 && cursor.moveToFirst()) {
                var sum = 0
                do {
                    val rw = RecommendWord()
                    rw.mWord = cursor.getString(0)
                    rw.mUseCntXxx = cursor.getInt(1).toDouble()
                    rw.mWordId = cursor.getInt(2)
                    rw.mUpdDt = cursor.getString(3)
                    wordList.add(rw)

                    sum += rw.mUseCntXxx.toInt()
                } while (cursor.moveToNext())

                for (rw in wordList) {
                    rw.mUseCntXxxSum = sum.toDouble()
                }
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return wordList
    }

    /**
     * 전체입력횟수에 따른 단어목록 구하기
     */
    fun getWordListByUseCnt(composition: String?): List<RecommendWord> {
        var composition = composition
        val wordList = ArrayList<RecommendWord>()
        try {
            if (composition == null) composition = ""
            val cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(Word.WORD, Word.USE_CNT_TOTAL, Word.WORD_ID, Word.UPD_DT), Word.COMPOSITION + " like ?", arrayOf(composition + "_%"), null, null, Word.USE_CNT_TOTAL + " desc, " + Word.UPD_DT + " desc", "100")

            if (cursor.count > 0 && cursor.moveToFirst()) {
                var sum = 0
                do {
                    val rw = RecommendWord()
                    rw.mWord = cursor.getString(0)
                    rw.mUseCntTotal = cursor.getInt(1).toDouble()
                    rw.mWordId = cursor.getInt(2)
                    rw.mUpdDt = cursor.getString(3)
                    wordList.add(rw)

                    sum += rw.mUseCntTotal.toInt()
                } while (cursor.moveToNext())

                for (rw in wordList) {
                    rw.mUseCntTotalSum = sum.toDouble()
                }
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return wordList
    }

    /**
     * 시간대별 입력횟수에 따른 단어목록 구하기
     */
    fun getWordListByUseTime(composition: String?): List<RecommendWord> {
        var composition = composition
        val wordList = ArrayList<RecommendWord>()
        try {
            var columnUseCntN = Word.USE_CNT_0
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 4 && hour < 8)
                columnUseCntN = Word.USE_CNT_4
            else if (hour >= 8 && hour < 12)
                columnUseCntN = Word.USE_CNT_8
            else if (hour >= 12 && hour < 16)
                columnUseCntN = Word.USE_CNT_12
            else if (hour >= 16 && hour < 20)
                columnUseCntN = Word.USE_CNT_16
            else if (hour >= 20)
                columnUseCntN = Word.USE_CNT_20

            if (composition == null) composition = ""
            val cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(Word.WORD, columnUseCntN, Word.WORD_ID, Word.UPD_DT), Word.COMPOSITION + " like ?", arrayOf(composition + "_%"), null, null, columnUseCntN + " desc, " + Word.UPD_DT + " desc", "100")

            if (cursor.count > 0 && cursor.moveToFirst()) {
                var sum = 0
                do {
                    val rw = RecommendWord()
                    rw.mWord = cursor.getString(0)
                    rw.mUseCntN = cursor.getInt(1).toDouble()
                    rw.mWordId = cursor.getInt(2)
                    rw.mUpdDt = cursor.getString(3)
                    wordList.add(rw)

                    sum += rw.mUseCntN.toInt()
                } while (cursor.moveToNext())

                for (rw in wordList) {
                    rw.mUseCntNSum = sum.toDouble()
                }
            }
            cursor.close()
        } catch (e: Exception) {
        }

        return wordList
    }

    /**
     * 갱신되는 가중치값 구하기
     * 0 이 리턴되면 가중치를 갱신하지 않는다.
     */
    fun getUpdatedWeight(w: Double, columnUseCnt: String): Double {
        var w = w
        var useCntSum = 0
        var useCntCnt = 1
        var useCntAvg = 0.0
        var standardDeviation = 0.0
        try {
            var cursor = mDb!!.rawQuery("select sum(" + columnUseCnt + "), count(" + columnUseCnt + ") from " + Word.TABLE_NAME, null)
            if (cursor.moveToFirst()) {
                useCntSum = cursor.getInt(0)
                useCntCnt = cursor.getInt(1)
            }
            cursor.close()

            if (useCntSum < 100)
                return 0.0

            useCntAvg = useCntSum.toDouble() / useCntCnt

            cursor = mDb!!.query(Word.TABLE_NAME, arrayOf(columnUseCnt), null, null, null, null, columnUseCnt + " desc", "100")
            if (cursor.moveToFirst()) {
                var temp: Double
                do {
                    temp = cursor.getInt(0) - useCntAvg
                    standardDeviation += temp * temp
                } while (cursor.moveToNext())
                standardDeviation = Math.sqrt(standardDeviation / cursor.count)

                w = w * (1 + standardDeviation)
            }
        } catch (e: Exception) {
        }

        return w
    }

    fun updateWeight(weightTotal: Int, weightN: Int, weightXXX: Int): Boolean {
        try {
            val values = ContentValues()
            values.put(MyInfo.USE_CNT_TOTAL_WEIGHT, weightTotal)
            values.put(MyInfo.USE_CNT_N_WEIGHT, weightN)
            values.put(MyInfo.USE_CNT_XXX_WEIGHT, weightXXX)
            return mDb!!.update(MyInfo.TABLE_NAME, values, null, null) > 0
        } catch (e: Exception) {
        }

        return false
    }


    /**
     * db open helper
     */
    class DbOpenHelper : SQLiteOpenHelper {

        constructor(context: Context) : super(context, DATABASE_NAME, null, DATABASE_VERSION) {}

        constructor(context: Context, name: String, factory: CursorFactory?, version: Int) : super(context, name, factory, version) {}

        override fun onCreate(db: SQLiteDatabase) {

            MyInfo.onCreate(db)
            KeySet.onCreate(db)
            CustomKeyset.onCreate(db)
            CustomKeysetData.onCreate(db)
            Word.onCreate(db)
            NextWordGroup.onCreate(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // 버전 업그레이드 할때 필요한 동작은 여기에 추가.
            if (oldVersion == 1 && newVersion == 2) {
                updateVer1to2(db)
            }
        }

        private fun updateVer1to2(db: SQLiteDatabase) {
            // 단어예측 기능 추가에 따라 myinfo에 컬럼 추가 및 기존 단어히스토리 내용은 삭제
            try {
                Word.onCreate(db)
                NextWordGroup.onCreate(db)
                db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_TOTAL_WEIGHT + " INT DEFAULT " + WEIGHT_INIT_TOTAL)
                db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_N_WEIGHT + " INT DEFAULT " + WEIGHT_INIT_N)
                db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.USE_CNT_XXX_WEIGHT + " INT DEFAULT " + WEIGHT_INIT_XXX)
                db.execSQL("ALTER TABLE " + MyInfo.TABLE_NAME + " ADD COLUMN " + MyInfo.AVAILABLE_PERIOD + " INT DEFAULT " + AVAILABLE_PERIOD_INIT)
            } catch (e: Exception) {
            }

        }
    }


    /**
     * MyInfo 테이블 구조.
     */
    class MyInfo {

        var mDirection: Int = 0
        var mUseHistory: Int = 0
        var mUseCntTotalWeight: Int = 0
        var mUseCntNWeight: Int = 0
        var mUseCntXxxWeight: Int = 0
        var mAvailablePeriod: Int = 0

        companion object {

            val TABLE_NAME = "myinfo"
            val _ID = "_id"
            val DIRECTION = "direction"
            val USE_HISTORY = "use_history"  // 1:use, 0:don't
            val USE_CNT_TOTAL_WEIGHT = "use_cnt_total_weight"
            val USE_CNT_N_WEIGHT = "use_cnt_n_weight"
            val USE_CNT_XXX_WEIGHT = "use_cnt_xxx_weight"
            val AVAILABLE_PERIOD = "available_period"

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + _ID + " INTEGER primary key autoincrement, "
                            + DIRECTION + " INTEGER, "
                            + USE_HISTORY + " INTEGER, "
                            + USE_CNT_TOTAL_WEIGHT + " INTEGER, "
                            + USE_CNT_N_WEIGHT + " INTEGER, "
                            + USE_CNT_XXX_WEIGHT + " INTEGER, "
                            + AVAILABLE_PERIOD + " INTEGER"
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)

                val values = ContentValues()
                values.put(DIRECTION, 8)
                values.put(USE_HISTORY, 0)
                values.put(USE_CNT_TOTAL_WEIGHT, WEIGHT_INIT_TOTAL)
                values.put(USE_CNT_N_WEIGHT, WEIGHT_INIT_N)
                values.put(USE_CNT_XXX_WEIGHT, WEIGHT_INIT_XXX)
                values.put(AVAILABLE_PERIOD, 60)
                db.insert(TABLE_NAME, null, values)
            }
        }
    }

    /**
     * KeySet 테이블 구조.
     */
    class KeySet {

        var mId: Int = 0
        var mType: Int = 0
        var mShowYN: String? = null
        var mDirection: Int = 0

        companion object {

            val TABLE_NAME = "keyset"
            val _ID = "_id"
            val TYPE = "type"
            val SHOW_YN = "show_yn"
            val SIDE = "side"  // 0:전체(4방향일때), 1:왼쪽, 2:오른쪽

            val SIDE_ALL = 0
            val SIDE_LEFT = 1
            val SIDE_RIGHT = 2

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + _ID + " INTEGER primary key autoincrement, "
                            + TYPE + " INTEGER,"
                            + SHOW_YN + " TEXT,"
                            + SIDE + " INTEGER"
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)

                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_ALL))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_ALL))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_ALL))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_ALL))

                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_LEFT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_LEFT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_LEFT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_LEFT))

                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_ENGLISH, "Y", SIDE_RIGHT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_HANGUL, "Y", SIDE_RIGHT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_NUMBERS, "Y", SIDE_RIGHT))
                db.insert(TABLE_NAME, null, getContentValues(KEYBOARD_TYPE_SYMBOLS, "Y", SIDE_RIGHT))
            }

            private fun getContentValues(type: Int, showYN: String, side: Int): ContentValues {
                val values = ContentValues()
                values.put(TYPE, type)
                values.put(SHOW_YN, showYN)
                values.put(SIDE, side)
                return values
            }
        }
    }

    /**
     * CustomKeyset 테이블 구조.
     */
    class CustomKeyset {

        var mId: Int = 0
        var mName: String? = null
        var mShow4: Int = 0
        var mShow8: Int = 0

        companion object {

            val TABLE_NAME = "customkeyset"
            val _ID = "_id"
            val NAME = "name"
            val SHOW_4 = "show_4"
            val SHOW_8 = "show_8"

            // 4방향일때 *10 해서 들어가고, 8방향일때는 *1 해서 들어간다.
            val SHOW_NONE = 0
            val SHOW_ALL = 1
            val SHOW_LEFT = 2
            val SHOW_RIGHT = 3

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + _ID + " INTEGER primary key autoincrement, "
                            + NAME + " TEXT, "
                            + SHOW_4 + " INTEGER, "
                            + SHOW_8 + " INTEGER "
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)
            }
        }
    }

    /**
     * CustomKeysetData 테이블 구조.
     */
    class CustomKeysetData {

        var mId: Int = 0
        var mCustomKeysetId: Int = 0
        var mKeysetNumber: Int = 0
        var mData: String? = null

        companion object {

            val TABLE_NAME = "customkeysetdata"
            val _ID = "_id"
            val CUSTOM_KEYSET_ID = "custom_keyset_id"
            val KEYSET_NUMBER = "keyset_number"
            val DATA = "data"

            val KEYSET_NUMBER_LEFT_TOP = 0
            val KEYSET_NUMBER_MID_TOP = 1
            val KEYSET_NUMBER_RIGHT_TOP = 2
            val KEYSET_NUMBER_LEFT_MID = 3
            val KEYSET_NUMBER_RIGHT_MID = 4
            val KEYSET_NUMBER_LEFT_BOT = 5
            val KEYSET_NUMBER_MID_BOT = 6
            val KEYSET_NUMBER_RIGHT_BOT = 7

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + _ID + " INTEGER primary key autoincrement, "
                            + CUSTOM_KEYSET_ID + " INTEGER,"
                            + KEYSET_NUMBER + " INTEGER,"
                            + DATA + " TEXT"
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)
            }
        }
    }

    /**
     * History 테이블 구조.
     */
    object History {

        val TABLE_NAME = "history"
        val _ID = "_id"
        val KEYBOARD = "keyboard"  // 키보드종류
        val WORD = "word"
        val DATA = "data"  // 검색시 태그의 역할 ( 코드번호를 컴마로 구분 )
        val USE_CNT = "use_cnt"

        val KEYBOARD_HANGUL = 1
        val KEYBOARD_ETC = 2

        val CREATE = (
                "CREATE TABLE " + TABLE_NAME + "( "
                        + _ID + " INTEGER primary key autoincrement, "
                        + KEYBOARD + " INTEGER,"
                        + WORD + " TEXT,"
                        + DATA + " TEXT,"
                        + USE_CNT + " INTEGER"
                        + ");")

        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE)
        }
    }

    /**
     * word 테이블 구조
     */
    class Word {

        var mWord: String? = null
        var mComposition: String? = null
        var mUpdDt: String? = null
        var mCrtDt: String? = null
        var mUseCntTotal: Int = 0
        var mUseCnt0: Int = 0
        var mUseCnt4: Int = 0
        var mUseCnt8: Int = 0
        var mUseCnt12: Int = 0
        var mUseCnt16: Int = 0
        var mUseCnt20: Int = 0
        var mUseCntNormal: Int = 0
        var mUseCntEmailAddress: Int = 0
        var mUseCntEmailSubject: Int = 0
        var mUseCntUri: Int = 0
        var mUseCntPersonName: Int = 0
        var mUseCntPostalAddress: Int = 0
        var mUseCntNumber: Int = 0
        var mWordId: Int = 0

        companion object {

            val TABLE_NAME = "word"
            val WORD = "word"
            val COMPOSITION = "composition"
            val USE_CNT_TOTAL = "use_cnt_total"
            val USE_CNT_0 = "use_cnt_0"
            val USE_CNT_4 = "use_cnt_4"
            val USE_CNT_8 = "use_cnt_8"
            val USE_CNT_12 = "use_cnt_12"
            val USE_CNT_16 = "use_cnt_16"
            val USE_CNT_20 = "use_cnt_20"
            val USE_CNT_NORMAL = "use_cnt_normal"
            val USE_CNT_EMAIL_ADDRESS = "use_cnt_address"
            val USE_CNT_EMAIL_SUBJECT = "use_cnt_subject"
            val USE_CNT_URI = "use_cnt_uri"
            val USE_CNT_PERSON_NAME = "use_cnt_person_name"
            val USE_CNT_POSTAL_ADDRESS = "use_cnt_postal_address"
            val USE_CNT_NUMBER = "use_cnt_number"
            val UPD_DT = "upd_dt"
            val CRT_DT = "crt_dt"
            val WORD_ID = "word_id"

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + WORD_ID + " INTEGER primary key autoincrement, "
                            + WORD + " TEXT,"
                            + COMPOSITION + " TEXT,"
                            + USE_CNT_TOTAL + " INTEGER,"
                            + USE_CNT_0 + " INTEGER,"
                            + USE_CNT_4 + " INTEGER,"
                            + USE_CNT_8 + " INTEGER,"
                            + USE_CNT_12 + " INTEGER,"
                            + USE_CNT_16 + " INTEGER,"
                            + USE_CNT_20 + " INTEGER,"
                            + USE_CNT_NORMAL + " INTEGER,"
                            + USE_CNT_EMAIL_ADDRESS + " INTEGER,"
                            + USE_CNT_EMAIL_SUBJECT + " INTEGER,"
                            + USE_CNT_URI + " INTEGER,"
                            + USE_CNT_PERSON_NAME + " INTEGER,"
                            + USE_CNT_POSTAL_ADDRESS + " INTEGER,"
                            + USE_CNT_NUMBER + " INTEGER,"
                            + UPD_DT + " TEXT,"
                            + CRT_DT + " TEXT"
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)
            }
        }
    }

    /**
     * next_word_group 테이블 구조
     */
    class NextWordGroup {

        var mUseCnt: Int = 0
        var mWord: String? = null
        var mNextWord: String? = null

        companion object {

            val TABLE_NAME = "next_word_group"
            val WORD = "word"
            val NEXT_WORD = "next_word"
            val USE_CNT = "use_cnt"

            val CREATE = (
                    "CREATE TABLE " + TABLE_NAME + "( "
                            + WORD + " TEXT,"
                            + NEXT_WORD + " TEXT,"
                            + USE_CNT + " INTEGER"
                            + ");")

            fun onCreate(db: SQLiteDatabase) {
                db.execSQL(CREATE)
            }
        }
    }

    companion object {

        private val DATABASE_NAME = "nolookbrd.db"

        private val DATABASE_VERSION = 2
        private var sInstance: NLKeyboardDb? = null

        private val KEYBOARD_TYPE_ENGLISH = 0
        private val KEYBOARD_TYPE_HANGUL = 1
        private val KEYBOARD_TYPE_NUMBERS = 2
        private val KEYBOARD_TYPE_SYMBOLS = 3

        val WEIGHT_SUM = 100
        val WEIGHT_INIT_NEXT = 50
        val WEIGHT_INIT_TOTAL = 10
        val WEIGHT_INIT_N = 10
        val WEIGHT_INIT_XXX = 30
        val AVAILABLE_PERIOD_INIT = 60

        fun getInstance(context: Context): NLKeyboardDb? {

            if (sInstance != null) {
                return sInstance
            }

            sInstance = NLKeyboardDb(context)
            return if (sInstance!!.open(context)) {
                sInstance
            } else {
                null
            }

        }
    }

}
