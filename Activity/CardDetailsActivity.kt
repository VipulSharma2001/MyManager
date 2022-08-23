
package com.manager.mymanager.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.manager.mymanager.R
import com.manager.mymanager.adapters.CardMemberListItemsAdapter
import com.manager.mymanager.dialogs.LabelColorListDialog
import com.manager.mymanager.dialogs.MembersListDialog
import com.manager.mymanager.firebase.FirestoreClass
import com.manager.mymanager.models.*
import com.manager.mymanager.utils.Constants
import kotlinx.android.synthetic.main.activity_card_details.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CardDetailsActivity : BaseActivity() {
    lateinit var  cb_yes:CheckBox
    lateinit var result_cb:String

    private lateinit var mBoardDetails:Board
    private var mTaskListPosition=-1
    private var mCardListPosition=-1
    private var mSelectedColor=""
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMilliSeconds: Long=0
    private var mSelectedDueTime: String=""
    var timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private lateinit var mInterstitialAd: InterstitialAd


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        getIntentData()
        setupActionBar()
        setupSelectedMembersList()
        cb_yes=findViewById(R.id.cb_tick) as CheckBox
        if(mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].done=="true"){
            result_cb="true"
            cb_tick.isChecked=true
        }else{
            result_cb="false"
            cb_tick.isChecked=false
        }
        mSelectedDueDateMilliSeconds=mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].dueDate
        if(mSelectedDueDateMilliSeconds>0){
            val simpleDateFormat= SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate= simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
            tv_select_due_date.text=selectedDate
        }

        et_name_card_details.setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].name)
        et_name_card_details.setSelection(et_name_card_details.text.toString().length)


        mSelectedColor= mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].labelColor
        if(mSelectedColor.isNotEmpty()){
            setColor()
        }

        MobileAds.initialize(this,getString(R.string.admob_app_id))
        mInterstitialAd= InterstitialAd(this)
        mInterstitialAd.adUnitId=getString(R.string.interstitial_ad_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        btn_update_card_details.setOnClickListener {
            mInterstitialAd.show()
            if(et_name_card_details.text.toString().isNotEmpty()){
                updateCardDetails()
            }else{
                Toast.makeText(this,"Please Enter A Card Name",Toast.LENGTH_SHORT).show()
            }
        }

        tv_select_label_color.setOnClickListener {
            labelColorsListDialog()
        }

        tv_select_members.setOnClickListener {
            membersListDialog()
        }

        tv_select_due_date.setOnClickListener {
            showDatePicker()
        }

        tv_select_due_time.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val now = Calendar.getInstance()
        val timePicker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            val selectedTime = Calendar.getInstance()
            selectedTime.set(Calendar.HOUR_OF_DAY,hourOfDay)
            selectedTime.set(Calendar.MINUTE,minute)
            tv_select_due_time.text = timeFormat.format(selectedTime.time)
            mSelectedDueTime=timeFormat.format(selectedTime.time)
        },
            now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),false)
        timePicker.show()
    }

    private fun setupActionBar(){
        setSupportActionBar(toolbar_card_details_activity)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].name

        }
        toolbar_card_details_activity.setNavigationOnClickListener { onBackPressed() }
    }

    private fun getIntentData(){
        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails= intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if(intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            mTaskListPosition=intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            mCardListPosition=intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            mMembersDetailList=intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card->{
                alertDialogForDeleteCard(mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun updateCardDetails(){
        if(cb_tick.isChecked==true){
            result_cb="true"
        }else{
            result_cb="false"
        }
        val card=Card(et_name_card_details.text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds, mSelectedDueTime,result_cb)

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)

        mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition]=card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)
    }

    private fun deleteCard(){
        val cardsList: ArrayList<Card> =mBoardDetails.taskList[mTaskListPosition].cards

        cardsList.removeAt(mCardListPosition)
        val taskList: ArrayList<Task> =mBoardDetails.taskList

        taskList.removeAt(taskList.size-1)

        taskList[mTaskListPosition].cards=cardsList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)
    }

    private fun alertDialogForDeleteCard(cardName:String){
        val builder= AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(resources.getString(R.string.confirmation_message_to_delete_card,cardName))
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){dialogInterface, which->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton("No"){dialogInterface,which->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog =builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun colorsList():ArrayList<String>{
        val colorsList:ArrayList<String> = ArrayList()

        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")
        colorsList.add("#FFFFFF")

        return colorsList
    }

    private fun setColor(){
        tv_select_label_color.text=""
        tv_select_label_color.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorsListDialog(){
        val colorsList:ArrayList<String> = colorsList()
        val listDialog= object :LabelColorListDialog(
            this,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor){
            override fun onItemSelected(color: String) {
                mSelectedColor=color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun membersListDialog(){
        var cardAssignedMembersList=mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo
        if(cardAssignedMembersList.size>0){
            for (i in mMembersDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(mMembersDetailList[i].id==j){
                        mMembersDetailList[i].selected=true
                    }
                }
            }
        }else{
            for (i in mMembersDetailList.indices){
                mMembersDetailList[i].selected=false
            }

        }
        val listDialog=object : MembersListDialog(this,
            mMembersDetailList
        ,resources.getString(R.string.str_select_member)

        ){
            override fun onItemSelected(user: User, action: String) {
                if(action==Constants.SELECT){
                    if(!mBoardDetails.taskList[mTaskListPosition].
                            cards[mCardListPosition].assignedTo.
                            contains(user.id)){
                        mBoardDetails.taskList[mTaskListPosition].
                            cards[mCardListPosition].assignedTo.add(user.id)
                    }

                }else{
                    mBoardDetails.taskList[mTaskListPosition].
                        cards[mCardListPosition].assignedTo.remove(user.id)
                    for (i in mMembersDetailList.indices){
                        if(mMembersDetailList[i].id==user.id){
                            mMembersDetailList[i].selected=false
                        }
                    }
                }
                setupSelectedMembersList()
            }
        }
        listDialog.show()
    }

    private fun setupSelectedMembersList(){
        val cardAssignedMemberList=
            mBoardDetails.
            taskList[mTaskListPosition].
            cards[mCardListPosition].assignedTo

        val selectedMembersList:ArrayList<SelectedMembers> = ArrayList()

        for (i in mMembersDetailList.indices){
            for(j in cardAssignedMemberList){
                if(mMembersDetailList[i].id==j){
                    val selectedMember=SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }
        if(selectedMembersList.size>0){
            selectedMembersList.add(SelectedMembers("",""))
            tv_select_members.visibility=View.GONE
            rv_selected_members_list.visibility=View.VISIBLE
            rv_selected_members_list.layoutManager=GridLayoutManager(
                this,6
            )
            val adapter = CardMemberListItemsAdapter(this@CardDetailsActivity, selectedMembersList,true)
            rv_selected_members_list.adapter=adapter
            adapter.setOnClickListener(
                object : CardMemberListItemsAdapter.OnClickListener{
                    override fun onClick() {
                        membersListDialog()
                    }
                }
            )


        }else{
            tv_select_members.visibility=View.VISIBLE
            rv_selected_members_list.visibility=View.GONE
        }
    }

    private fun showDatePicker(){
        val c= Calendar.getInstance()
        val year=c.get(Calendar.YEAR)
        val month=c.get(Calendar.MONTH)
        val day=c.get(Calendar.DAY_OF_MONTH)
        val dpd=DatePickerDialog(
            this,DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth=if(dayOfMonth<10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear=if((monthOfYear+1)<10) "0${monthOfYear+1}" else "${monthOfYear+1}"
                val selectedDate="$sDayOfMonth/$sMonthOfYear/$year"
                tv_select_due_date.text=selectedDate

                val sdf=SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val theDate=sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds=theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show()
    }
}
