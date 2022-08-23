package com.manager.mymanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.manager.mymanager.R
import com.manager.mymanager.firebase.FirestoreClass
import com.manager.mymanager.models.Board
import com.manager.mymanager.utils.Constants
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_change_board_details.*
import java.io.IOException

class ChangeBoardDetails : BaseActivity() {
    private lateinit var mBoardDetails:Board
    private lateinit var mBoardDocumentId:String
    private var mSelectedImageFileUri: Uri?=null
    private var mBoardImageURL:String=""
    private lateinit var mInterstitialAd: InterstitialAd


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_board_details)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails=intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)
            mBoardDocumentId=intent.getStringExtra(Constants.DOCUMENT_ID)
        }
        setupActionBar()
        setDataInUI(mBoardDetails)

        MobileAds.initialize(this,getString(R.string.admob_app_id))
        mInterstitialAd= InterstitialAd(this)
        mInterstitialAd.adUnitId=getString(R.string.interstitial_ad_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        iv_board_change_image.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }
        btn_update_board.setOnClickListener {
            mInterstitialAd.show()
            if(mSelectedImageFileUri!=null){
                uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }

    }
    private fun setupActionBar(){
        setSupportActionBar(toolbar_change_board_activity)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=resources.getString(R.string.my_profile_title)
        }
        toolbar_change_board_activity.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== Constants.READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)
            }
        }else{
            Toast.makeText(this,"Oops, you just denied the permission for storage. You can allow from settings",
                Toast.LENGTH_LONG).show()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode== CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode== Activity.RESULT_OK && data!=null) {
            val result = CropImage.getActivityResult(data)
            mSelectedImageFileUri = result.uri

            Glide
                .with(this@ChangeBoardDetails)
                .load(mSelectedImageFileUri)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(iv_board_change_image)

        }
        if(resultCode== Activity.RESULT_OK && requestCode== Constants.PICK_IMAGE_REQUEST_CODE && data!!.data!=null){
            mSelectedImageFileUri=data.data
            try {
                CropImage.activity(mSelectedImageFileUri)
                    .start(this@ChangeBoardDetails)
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }
    fun setDataInUI(details:Board){
        Glide
            .with(this)
            .load(mBoardDetails.image)
            .centerCrop()
            .placeholder(R.drawable.ic_board_place_holder)
            .into(iv_board_change_image)
        et_name_change_board.setText(details.name)
    }

    private fun updateUserProfileData(){
        val userHashMap=HashMap<String,Any>()
        if(mBoardImageURL.isNotEmpty() && mBoardImageURL!=mBoardDetails.image){
            userHashMap[Constants.IMAGE]=mBoardImageURL
        }
        if(et_name_change_board.text.toString()!=mBoardDetails.name){
            userHashMap[Constants.NAME]=et_name_change_board.text.toString()
        }
        FirestoreClass().updateBoardDetails(this,userHashMap,mBoardDocumentId)

    }

    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mSelectedImageFileUri!=null){
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child("BOARD_IMAGE"+ System.currentTimeMillis()+"."+Constants.getFileExtension(this,mSelectedImageFileUri))
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                    taskSnapshot ->
                Log.i("Firebase Image URL",taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri->
                    Log.i("Downloadable Image URL", uri.toString())
                    mBoardImageURL=uri.toString()
                    updateUserProfileData()
                }
            }.addOnFailureListener {
                    exception ->
                Toast.makeText(this@ChangeBoardDetails,exception.message, Toast.LENGTH_LONG).show()
                hideProgressDialog()
            }
        }
    }
    fun updateBoardSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}
