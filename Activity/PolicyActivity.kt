package com.manager.mymanager.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.manager.mymanager.R
import kotlinx.android.synthetic.main.activity_policy.*

class PolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_policy)
        setupActionBar()
    }
    private fun setupActionBar(){
        setSupportActionBar(terms_toolbar)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        terms_toolbar.setNavigationOnClickListener { onBackPressed()}
    }
}
