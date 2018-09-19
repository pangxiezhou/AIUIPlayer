package com.iflytek.aiui.player.common.rpc.storage

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StorageTest {
    private val tempRepo = hashMapOf<String, Any>()
    private val editor:SharedPreferences.Editor = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val context: Context = mock()

    private lateinit var storage: Storage

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(editor)

        whenever(editor.putString(any(), any())).then {
            tempRepo[it.arguments[0] as String] = it.arguments[1]
            editor
        }

        whenever(editor.putInt(any(), any())).then {
            tempRepo[it.arguments[0] as String] = it.arguments[1]
            editor
        }

        whenever(sharedPreferences.getString(any(), any())).then {
            tempRepo[it.arguments[0]]
        }

        whenever(sharedPreferences.getInt(any(), any())).then {
            tempRepo[it.arguments[0]]
        }

        storage = Storage(context)
    }


    @Test
    fun saveString() {
        val key = "test.key"
        val value = "test.value"

        storage.put(key, value)

        assertEquals(storage.getString(key), value)
    }

    @Test
    fun saveInteger() {
        val key = "test.key"
        val value = 1123

        storage.put(key, value)

        assertEquals(storage.getInteger(key), value)
    }
}