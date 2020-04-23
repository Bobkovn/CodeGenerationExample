package com.example.codegenerationexample.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.annotation_example.Inject
import com.example.codegenerationexample.ExampleClass
import com.example.codegenerationexample.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var firstExample: ExampleClass
    @Inject
    lateinit var secondExample: ExampleClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ExampleInjector.inject(this)
        button.setOnClickListener {
            firstExample.someMethod(this)
        }
    }
}
