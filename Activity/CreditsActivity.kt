package com.manager.mymanager.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import com.manager.mymanager.R
import kotlinx.android.synthetic.main.activity_credits.*

class CreditsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)
        setupActionBar()

        links1.movementMethod = LinkMovementMethod.getInstance()
        links2.movementMethod = LinkMovementMethod.getInstance()
    }
    private fun setupActionBar(){
        setSupportActionBar(credits_toolbar)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        credits_toolbar.setNavigationOnClickListener { onBackPressed()}
    }
}
