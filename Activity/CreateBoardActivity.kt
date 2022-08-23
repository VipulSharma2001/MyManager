package com.manager.mymanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_create_board.*
import java.io.IOException

class CreateBoardActivity : BaseActivity() {
    private var mSelectedImageFileUri: Uri?=null
    private lateinit var mUsername:String
    private var mBoardImageURL:String=""
    private lateinit var mInterstitialAd: InterstitialAd


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)
        setupActionBar()
        if(intent.hasExtra(Constants.NAME)){
            mUsername=intent.getStringExtra(Constants.NAME)
        }

        MobileAds.initialize(this,getString(R.string.admob_app_id))
        mInterstitialAd= InterstitialAd(this)
        mInterstitialAd.adUnitId=getString(R.string.interstitial_ad_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        iv_board_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }

        }
        btn_create.setOnClickListener {
            if(mSelectedImageFileUri!=null){
                uploadBoardImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }
    }
    private fun createBoard(){
        val assignedUsersArrayList:ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())
        var board= Board(
            et_board_name.text.toString(),mBoardImageURL,mUsername,assignedUsersArrayList)
        FirestoreClass().createBoard(this,board)
    }
    private fun uploadBoardImage(){
        showProgressDialog(resources.getString((R.string.please_wait)))
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child("BOARD_IMAGE"+System.currentTimeMillis()+"."+Constants.getFileExtension(this,mSelectedImageFileUri))
        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
            Log.i("Board Image URL",taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri->
                Log.i("Downloadable Image URL", uri.toString())
                mBoardImageURL=uri.toString()
                createBoard()
            }
        }.addOnFailureListener {
                exception ->
            Toast.makeText(this,exception.message, Toast.LENGTH_LONG).show()
            hideProgressDialog()
        }
    }
    fun boardCreatedSuccessfully(){
        hideProgressDialog()
        mInterstitialAd.show()
        setResult(Activity.RESULT_OK)
        finish()
    }
    private fun setupActionBar(){
        setSupportActionBar(toolbar_create_board_activity)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=resources.getString(R.string.create_board_title)
        }
        toolbar_create_board_activity.setNavigationOnClickListener { onBackPressed() }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== Constants.READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)
            }
        }else{
            Toast.makeText(this,"Oops, you just denied the permission for storage. You can allow from settings",
                Toast.LENGTH_SHORT).show()

        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode== CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode== Activity.RESULT_OK && data!=null) {
            val result = CropImage.getActivityResult(data)
            mSelectedImageFileUri = result.uri

            Glide
                .with(this@CreateBoardActivity)
                .load(mSelectedImageFileUri)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(iv_board_image)

        }
        if(resultCode== Activity.RESULT_OK && requestCode== Constants.PICK_IMAGE_REQUEST_CODE && data!!.data!=null){
            mSelectedImageFileUri=data.data
            try {
                CropImage.activity(mSelectedImageFileUri)
                    .start(this@CreateBoardActivity)
            }
            catch(e: IOException){
                e.printStackTrace()
            }
        }
    }


}
