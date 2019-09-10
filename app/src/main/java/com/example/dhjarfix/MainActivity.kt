package com.example.dhjarfix

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.vova.testlibrary.TestFile

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var test = TestFile()
        test.string
    }

    override fun getDelegate(): AppCompatDelegate {
        var name: String? = null
        return super.getDelegate()
    }
}
