package com.manager.mymanager.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.manager.mymanager.R
import com.manager.mymanager.adapters.TaskListItemsAdapter
import com.manager.mymanager.firebase.FirestoreClass
import com.manager.mymanager.models.Board
import com.manager.mymanager.models.Card
import com.manager.mymanager.models.Task
import com.manager.mymanager.models.User
import com.manager.mymanager.utils.Constants
import kotlinx.android.synthetic.main.activity_task_list.*

class TaskListActivity : BaseActivity() {
    private lateinit var mBoardDetails: Board
    private lateinit var mBoardDocumentId:String
    lateinit var mAssignedMemberDetailList:ArrayList<User>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)
        if(intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentId=intent.getStringExtra(Constants.DOCUMENT_ID)
        }
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDocumentId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK && requestCode== MEMBERS_REQUEST_CODE||requestCode== CARD_DETAILS_REQUEST_CODE||requestCode== BOARD_DETAILS_CHANGE_CODE){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardDetails(this,mBoardDocumentId)
        }else{
            Log.e("Cancelled","Cancelled")
        }
    }

    fun boardDetails(board: Board){
        mBoardDetails=board
        hideProgressDialog()
        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)
    }


    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDetails.documentId)
    }



    fun createTaskList(taskListName:String){
        val task=Task(taskListName,FirestoreClass().getCurrentUserId())
        mBoardDetails.taskList.add(0,task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun updateTaskList(position:Int,listName:String,model:Task){
        val task=Task(listName,model.createdBy)
        mBoardDetails.taskList[position]=task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)

    }

    fun deleteTaskList(position:Int){
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)

    }

    fun addCardToTaskList(position: Int,cardName:String){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        val cardAssignedUsersList:ArrayList<String> = ArrayList()
        cardAssignedUsersList.add(FirestoreClass().getCurrentUserId())
        val card = Card(cardName,FirestoreClass().getCurrentUserId(),cardAssignedUsersList)
        val cardsList=mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task=Task(mBoardDetails.taskList[position].title,mBoardDetails.taskList[position].createdBy,cardsList)
        mBoardDetails.taskList[position]=task
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_members->{
                val intent=Intent(this@TaskListActivity,MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
                startActivityForResult(intent, MEMBERS_REQUEST_CODE)
                return true
            }
            R.id.action_delete_board->{
                alertDialogForDeleteCard(1)
            }

            R.id.action_leave_board->{
                alertDialogForDeleteCard(2)

            }
            R.id.action_change_board_details->{
                val intent=Intent(this@TaskListActivity,ChangeBoardDetails::class.java)
                intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
                intent.putExtra(Constants.DOCUMENT_ID,mBoardDocumentId)
                startActivityForResult(intent, BOARD_DETAILS_CHANGE_CODE)
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setupActionBar(){
        setSupportActionBar(toolbar_task_list_activity)
        val actionBar=supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=mBoardDetails.name
        }
        toolbar_task_list_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun cardDetails(taskListPosition:Int,cardPosition: Int){
        val intent=Intent(this@TaskListActivity,CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION,taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION,cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST,mAssignedMemberDetailList)
        startActivityForResult(intent, CARD_DETAILS_REQUEST_CODE)
    }

    fun boardMembersDetailsList(list: ArrayList<User>){
        mAssignedMemberDetailList=list
        hideProgressDialog()

        val addTaskList= Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)
        rv_task_list.layoutManager= LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        rv_task_list.setHasFixedSize(true)
        val adapter= TaskListItemsAdapter(this,mBoardDetails.taskList)
        rv_task_list.adapter=adapter
    }

    fun updateCardsInTaskList(taskListPosition: Int, cards:ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        mBoardDetails.taskList[taskListPosition].cards=cards
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    companion object{
        const val MEMBERS_REQUEST_CODE:Int=13
        const val CARD_DETAILS_REQUEST_CODE:Int=14
        const val BOARD_DETAILS_CHANGE_CODE:Int=15
    }

    private fun alertDialogForDeleteCard(code:Int){
        val builder= AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(resources.getString(R.string.confirmation_message_to_delete_board))
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){dialogInterface, which->
            dialogInterface.dismiss()
            if (code==1){
                deleteBoard()
            }else if(code==2){
                leaveBoard()
            }
        }
        builder.setNegativeButton("No"){dialogInterface,which->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog =builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    fun deleteBoard(){
        if(mBoardDetails.assignedTo[0]==FirestoreClass().getCurrentUserId()){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().deleteBoardFromDatabase(this,mBoardDetails.documentId)
        }else{
            Toast.makeText(this,"Only the creator or top member can delete the board",Toast.LENGTH_SHORT).show()
        }
    }

    fun boardDeleted(){
        hideProgressDialog()
        val intent=Intent(this@TaskListActivity,MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun leaveBoard(){
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().leaveBoardFromDatabase(this,mBoardDetails)
    }

    fun leaveMemberSuccess(){
        hideProgressDialog()
        if(mBoardDetails.assignedTo.size==0){
            FirestoreClass().deleteBoardFromDatabase(this,mBoardDetails.documentId)
        }
        val intent=Intent(this@TaskListActivity,MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
