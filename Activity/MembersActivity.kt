package com.manager.mymanager.activities

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.manager.mymanager.R
import com.manager.mymanager.adapters.MemberListItemsAdapter
import com.manager.mymanager.firebase.FirestoreClass
import com.manager.mymanager.models.Board
import com.manager.mymanager.models.User
import com.manager.mymanager.utils.Constants
import kotlinx.android.synthetic.main.activity_members.*
import kotlinx.android.synthetic.main.dialog_search_member.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    private lateinit var mBoardDetails:Board
    private lateinit var mAssignedMembersList:ArrayList<User>
    private lateinit var mInterstitialAd: InterstitialAd

    private var anyChangesMade:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails=intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)
        }

        if(mBoardDetails.createdBy==FirestoreClass().getCurrentUserId()){

        }
        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)

        MobileAds.initialize(this,getString(R.string.admob_app_id))
        mInterstitialAd= InterstitialAd(this)
        mInterstitialAd.adUnitId=getString(R.string.interstitial_ad_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

    }
    private fun setupActionBar(){
        setSupportActionBar(toolbar_members_activity)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=resources.getString(R.string.members)
        }
        toolbar_members_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun setupMembersList(list:ArrayList<User>){
        mAssignedMembersList=list
        hideProgressDialog()
        rv_members_list.layoutManager=LinearLayoutManager(this)
        rv_members_list.setHasFixedSize(true)
        val adapter=MemberListItemsAdapter(this,list)
        rv_members_list.adapter=adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member->{
                dialogSearchMember()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun memberDetails(user: User){
        mBoardDetails.assignedTo.add(user.id)
        FirestoreClass().assignMemberToBoard(this,mBoardDetails,user)
    }


    private fun dialogSearchMember(){
        val dialog=Dialog(this)
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.tv_add.setOnClickListener {
            val email=dialog.et_email_search_member.text.toString()
            if(email.isNotEmpty()){
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this,email)
                dialog.dismiss()

            }else{
                Toast.makeText(this,"Enter email address to add member",Toast.LENGTH_SHORT).show()
            }
        }
        dialog.tv_cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    fun memberAssignSuccess(user:User){
        hideProgressDialog()
        mAssignedMembersList.add(user)
        anyChangesMade=true
        setupMembersList(mAssignedMembersList)
        SendNotificationToUserAsyncTask(mBoardDetails.name,user.fcmToken).execute()
        mInterstitialAd.show()
    }

    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    private inner class SendNotificationToUserAsyncTask(val boardName:String, val token:String): AsyncTask<Any,Void,String>(){

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }


        override fun doInBackground(vararg params: Any?): String {
            var result:String
            var connection: HttpURLConnection?=null
            try{
                val url= URL(Constants.FCM_BASE_URL)
                connection=url.openConnection() as HttpURLConnection
                connection.doOutput=true
                connection.doInput=true
                connection.instanceFollowRedirects=false
                connection.requestMethod="POST"
                connection.setRequestProperty("Content-Type","application/json")
                connection.setRequestProperty("charset","utf-8")
                connection.setRequestProperty("Accept","application/json")
                connection.setRequestProperty(Constants.FCM_AUTHORIZATION,"${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}")
                connection.useCaches=false
                val wr=DataOutputStream(connection.outputStream)
                val jsonRequest= JSONObject()
                val dataObject= JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE,"Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE,"You have been assigned to the board by ${mAssignedMembersList[0].name}")
                jsonRequest.put(Constants.FCM_KEY_DATA,dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO,token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult:Int=connection.responseCode
                if(httpResult==HttpURLConnection.HTTP_OK){
                    val inputStream=connection.inputStream
                    val reader= BufferedReader(InputStreamReader(inputStream))
                    val sb=StringBuilder()
                    var line:String?
                    try {
                        while (reader.readLine().also{line=it}!=null){
                            sb.append(line+"\n")
                        }
                    }catch (e:IOException){
                        e.printStackTrace()
                    }finally {
                        try {
                            inputStream.close()
                        }catch (e:IOException){
                            e.printStackTrace()
                        }
                    }
                    result=sb.toString()
                }else{
                    result=connection.responseMessage
                }

            }catch (e:SocketTimeoutException){
                result="Connection Timeout"
            }catch (e:Exception){
                result="Error : "+e.message
            }finally {
                connection?.disconnect()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
        }



    }

    fun memberRemovalSuccess(){
        hideProgressDialog()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)
    }
}
