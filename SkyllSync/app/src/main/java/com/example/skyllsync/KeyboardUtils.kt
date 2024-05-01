package com.example.skyllsync

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

//object to show and hide the keyboard
object KeyboardUtils {
    fun hideKeyboard(activity: Activity, editText: EditText) {
        editText.requestFocus()
        val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    fun showKeyboard(activity: Activity, editText: EditText) {
        editText.requestFocus()
        val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
}