package com.teuskim.nlkeyboard

import java.util.ArrayList
import java.util.HashMap
import java.util.TreeSet

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Xml

/**
 * 키보드 모델 클래스
 * 1. xml 로부터 키 데이터를 만들어 저장하고,
 * 2. 키 데이터를 작은원세트의 리스트로 반환하는 메소드를 만든다.
 *
 * @author kim5724
 */
class NLKeyboard {

    private var mContext: Context? = null
    private var mXmlLayoutResId = 0
    private var mKeySetList: MutableList<KeySet>? = null
    var isShift = false
    var keyStringList: List<Map<Int, String>>? = null
    var isCustom = false

    // 키 데이터를 리턴한다.
    val keySets: List<KeySet>?
        get() = mKeySetList

    constructor(context: Context, xmlLayoutResId: Int) {
        // 필요한 멤버 변수 초기화하고, xml 로딩한다.
        mContext = context
        mXmlLayoutResId = xmlLayoutResId
        loadKeyboard()
    }

    constructor(context: Context, keyStringList: List<Map<Int, String>>) {
        mContext = context
        this.keyStringList = keyStringList
        loadKeyboard()
        isCustom = true
    }

    fun loadKeyboard() {
        if (mXmlLayoutResId > 0) {
            loadKeyboardByXml()
        } else {
            loadKeyboardByList()
        }
    }

    private fun loadKeyboardByXml() {
        // xml 파싱하여 키 데이터를 만들어서 저장한다.
        var keySet: KeySet? = null
        var inKeySet = false
        var inKey = false
        mKeySetList = ArrayList()
        val parser = mContext!!.resources.getXml(mXmlLayoutResId)

        try {
            var event: Int = parser.next()
            while (event != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_KEYBOARD == tag) {

                    } else if (TAG_KEYSET == tag) {
                        inKeySet = true
                        keySet = KeySet()
                    } else if (TAG_KEY == tag) {
                        inKey = true

                        val `as` = Xml.asAttributeSet(parser)
                        var key = `as`.getAttributeIntValue(0, 0)
                        var keyLabel = `as`.getAttributeValue(1)
                        if (isShift) {
                            key = Character.toUpperCase(key)
                            keyLabel = "" + Character.toUpperCase(keyLabel[0])  // TODO:어거지스럽다..
                        }
                        val iconResId = `as`.getAttributeResourceValue(2, 0)
                        keySet!!.setKey(key, keyLabel, iconResId)
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKeySet) {
                        inKeySet = false
                        mKeySetList!!.add(keySet!!)
                    } else if (inKey) {
                        inKey = false
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            //			Log.e("NLKeyboard", "keyboard parse error", e);
        }

    }

    private fun loadKeyboardByList() {
        mKeySetList = ArrayList()
        for (keysetMap in keyStringList!!) {
            val keyset = KeySet()
            val set = TreeSet(keysetMap.keys)
            val it = set.iterator()
            while (it.hasNext()) {
                val key = it.next()
                val data = keysetMap[key]
                var iconResId = 0
                if ("\b" == data)
                    iconResId = R.drawable.key_backspace
                else if (" " == data)
                    iconResId = R.drawable.key_space
                else if ("\n" == data)
                    iconResId = R.drawable.key_enter
                keyset.setKey(key, data!!.substring(0, 1), iconResId)
            }
            mKeySetList!!.add(keyset)
        }
    }

    /**
     * 작은원세트 객체
     * 1. 3~4개 정도의 글자 집합이다.
     */
    class KeySet {

        private val mKeyList: MutableList<Int>
        private val mKeyLabelMap: MutableMap<Int, String>
        private val mKeyIconMap: MutableMap<Int, Int>

        val keyList: List<Int>
            get() = mKeyList

        val keyLabelMap: Map<Int, String>
            get() = mKeyLabelMap

        init {
            mKeyList = ArrayList()
            mKeyLabelMap = HashMap()
            mKeyIconMap = HashMap()
        }

        fun setKey(key: Int, keyLabel: String, iconResId: Int) {
            mKeyList.add(key)
            mKeyLabelMap[key] = keyLabel
            if (iconResId > 0) {
                mKeyIconMap[key] = iconResId
            }
        }

        fun getKey(location: Int): Int {
            return mKeyList[location]
        }

        fun getKeyIcon(key: Int): Int {
            return if (mKeyIconMap.containsKey(key)) mKeyIconMap[key]!! else 0
        }

        fun size(): Int {
            return mKeyList.size
        }
    }

    companion object {

        // Keyboard XML Tags
        private val TAG_KEYBOARD = "Keyboard"
        private val TAG_KEYSET = "KeySet"
        private val TAG_KEY = "Key"
    }

}
