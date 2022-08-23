package com.manager.mymanager.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.manager.mymanager.BuildConfig
import com.manager.mymanager.R
import com.manager.mymanager.adapters.BoardItemsAdapter
import com.manager.mymanager.firebase.FirestoreClass
import com.manager.mymanager.models.Board
import com.manager.mymanager.models.User
import com.manager.mymanager.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.main_content.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object{
        const val MY_PROFILE_REQUEST_CODE:Int=11
        const val CREATE_BOARD_REQUEST_CODE:Int=12
    }
    private val TAG = "MainActivity"

    private  lateinit var mUserName:String
    private lateinit var mSharedPreferences: SharedPreferences
    lateinit var mAdView : AdView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar()

        nav_view.setNavigationItemSelectedListener(this)

        mSharedPreferences=this.getSharedPreferences(Constants.PROGEMANAG_PREFERENCES,Context.MODE_PRIVATE)
        val tokenUpdated=mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED,false)
        if (tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this,true)
        }else{
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this@MainActivity){instanceIdResult->
                updateFCMToken(instanceIdResult.token)
            }
        }

        FirestoreClass().loadUserData(this,true)

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.bannerAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        bannerAdView.visibility=View.GONE
        bannerAdView.adListener=object: AdListener(){
            override fun onAdLoaded() {
                bannerAdView.visibility=View.VISIBLE
                super.onAdLoaded()
            }

        }

        fab_create_board.setOnClickListener {
            val intent= Intent(this,CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME,mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }
    }
    fun populateBoardsListToUI(boardsList:ArrayList<Board>){
        hideProgressDialog()
        if(boardsList.size>0){
            rv_boards_list.visibility= View.VISIBLE
            tv_no_boards_available.visibility= View.GONE
            rv_boards_list.layoutManager= LinearLayoutManager(this)
            rv_boards_list.setHasFixedSize(true)
            val adapter= BoardItemsAdapter(this,boardsList)
            rv_boards_list.adapter=adapter
            adapter.setOnClickListener(object :BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent= Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID,model.documentId)
                    startActivity(intent)

                }
            })
        }else{
            rv_boards_list.visibility= View.GONE
            tv_no_boards_available.visibility= View.VISIBLE
        }
    }
    private fun setupActionBar(){
        setSupportActionBar(toolbar_main_activity)
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }
    private fun toggleDrawer(){
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
        }
    }
    fun updateNavigationUserDetails(user: User, readBoardsList:Boolean){
        hideProgressDialog()
        mUserName=user.name
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(nav_user_image);
        tv_username.text=user.name
        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK && requestCode== MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        }
        else if(resultCode== Activity.RESULT_OK && requestCode== CREATE_BOARD_REQUEST_CODE){
            FirestoreClass().getBoardsList(this)
        }
        else{
            Log.e("Cancelled","Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile->{
                startActivityForResult(
                    Intent(this,MyProfileActivity::class.java),
                    MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out->{
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent= Intent(this,IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            R.id.nav_delete_account->{
                alertDialogForDeleteCard()
            }
            R.id.terms-> {
                startActivity(
                    Intent(this, PolicyActivity::class.java))
            }
            R.id.credits->{
                startActivity(
                    Intent(this, CreditsActivity::class.java))
            }
            R.id.share->{
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "MyManager")
                    var shareMessage = "\nDownload the best project management application there is available right now.\nForm your own timetables or distribute project work with your team mates.\n Install the application from the link below.\n Happy Managing :)\n\n"
                    shareMessage =
                        shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    startActivity(Intent.createChooser(shareIntent, "choose one"))
                } catch (e: Exception) {
                }
            }
            R.id.rate->{
                val uri: Uri = Uri.parse("market://details?id=" + this@MainActivity.packageName)
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this@MainActivity.packageName)
                        )
                    )
                }
            }
            R.id.moreapps->{
                val uri: Uri = Uri.parse("https://play.google.com/store/apps/developer?id=VipulSharma")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/developer?id=VipulSharma")
                        )
                    )
                }
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor:SharedPreferences.Editor=mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED,true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this,true)
    }
    private fun updateFCMToken(token:String){
        val userHashMap=HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN]=token
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this,userHashMap)
    }
    private fun alertDialogForDeleteCard(){
        val builder= AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(resources.getString(R.string.confirmation_message_to_delete_board))
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){dialogInterface, which->
            dialogInterface.dismiss()
            deleteAccount()
            finish()
        }
        builder.setNegativeButton("No"){dialogInterface,which->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog =builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun deleteAccount(){
        val user = FirebaseAuth.getInstance().currentUser!!
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User account deleted.")
                }
            }
    }

    override fun onResume() {
        super.onResume()
        FirestoreClass().loadUserData(this,true)
    }
}
