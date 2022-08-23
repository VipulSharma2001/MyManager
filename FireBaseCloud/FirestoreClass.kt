package com.manager.mymanager.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.ActionCodeEmailInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.manager.mymanager.activities.*
import com.manager.mymanager.models.Board
import com.manager.mymanager.models.User
import com.manager.mymanager.utils.Constants


class FirestoreClass {
    private val mFireStore= FirebaseFirestore.getInstance()
    fun registerUser(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)        //access "users" collection in database
            .document(getCurrentUserId())             //access document with the user id given
            .set(userInfo, SetOptions.merge())        //replaces all similar data with new data
            .addOnSuccessListener {
                activity.userRegisteredSuccess()       //prints success message
            }.addOnFailureListener {
                e->
                Log.e(activity.javaClass.simpleName,"Error writing document")  //prints fail
            }
    }
    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)       //access "boards" collection in database
            .document(documentId)                     //access document in collection with given documentId
            .get()                                    //retrieves data if match is found
            .addOnSuccessListener { document->
                Log.i(activity.javaClass.simpleName,document.toString())    //print document data in logs
                val board=document.toObject(Board::class.java)!!        //stores retrieved data in variable

                board.documentId=document.id                        //stores document id in variable

                activity.boardDetails(board)                     //displays all data collected

            } .addOnFailureListener {e->
                activity.hideProgressDialog()                   //hides please wait message
                Log.e(activity.javaClass.simpleName,"Error while creating")   //print error message
            }

    }
    fun createBoard(activity: CreateBoardActivity, board: Board){
        mFireStore.collection(Constants.BOARDS)                 //access collection named "boards"
            .document()                                        //access its documents
            .set(board,SetOptions.merge())                     //save a new document with board details
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName,"Boards created Successfully")   //prints success signal
                Toast.makeText(activity,"Board created successfully",Toast.LENGTH_SHORT).show()  //success message
                activity.boardCreatedSuccessfully()                             //new project set up
            }.addOnFailureListener {
                exception ->
                activity.hideProgressDialog()           //hide please wait message
                Log.e(activity.javaClass.simpleName,"Error creating board",exception)  //prints fail message
            }
    }
    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO,getCurrentUserId())
            .get()
            .addOnSuccessListener { document->
                Log.e(activity.javaClass.simpleName,document.documents.toString())
                val boardList:ArrayList<Board> = ArrayList()
                for(i in document.documents){
                    val board=i.toObject(Board::class.java)!!
                    board.documentId=i.id
                    boardList.add(board)
                }
                activity.populateBoardsListToUI(boardList)
            } .addOnFailureListener {e->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a board")
            }
    }

    fun addUpdateTaskList(activity:Activity,board:Board){
        val taskListHashMap=HashMap<String,Any>()
        taskListHashMap[Constants.TASK_LIST]=board.taskList
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName,"Tasklist updated")
                if(activity is TaskListActivity) {
                    activity.addUpdateTaskListSuccess()
                }else if(activity is CardDetailsActivity){
                    activity.addUpdateTaskListSuccess()
                }
            }.addOnFailureListener { exception ->
                if(activity is TaskListActivity) {
                    activity.hideProgressDialog()
                }else if(activity is CardDetailsActivity){
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName,"Failed",exception)
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap:HashMap<String,Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName,"Profile data updated!")
                Toast.makeText(activity,"Profile Updated",Toast.LENGTH_SHORT).show()
                if(activity is MyProfileActivity){
                    activity.profileUpdateSuccess()
                }
                else if(activity is MainActivity) {
                    activity.tokenUpdateSuccess()
                }
            }.addOnFailureListener {
                e->
                if(activity is MyProfileActivity) {
                    activity.hideProgressDialog()
                }else if(activity is MainActivity) {
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName,"Error while creating a board",e)
                Toast.makeText(activity,"Profile Update Failed!",Toast.LENGTH_SHORT).show()
            }
    }

    fun updateBoardDetails(activity:ChangeBoardDetails,boardHashMap:HashMap<String,Any>,mBoardDocumentId:String){
        mFireStore.collection(Constants.BOARDS)
            .document(mBoardDocumentId)
            .update(boardHashMap)
            .addOnSuccessListener {
                activity.updateBoardSuccess()
                Toast.makeText(activity,"Board Updated",Toast.LENGTH_SHORT).show()

            }.addOnFailureListener {
                    e->
                Log.e(activity.javaClass.simpleName,"Error while updating the board",e)
                Toast.makeText(activity,"Board Update Failed!",Toast.LENGTH_SHORT).show()
            }
    }
    fun loadUserData(activity: Activity,readBoardsList:Boolean=false){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener {document->
                val loggedInUser=document.toObject(User::class.java)!!
                when(activity){
                    is SignInActivity ->{
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity->{
                        activity.updateNavigationUserDetails(loggedInUser,readBoardsList)
                    }
                    is MyProfileActivity->{
                        activity.setUserDataInUI(loggedInUser)
                    }
                }

            }.addOnFailureListener {

                    e->
                when(activity){
                    is SignInActivity->{
                        activity.hideProgressDialog()
                    }
                    is MainActivity->{
                        activity.hideProgressDialog()
                    }
                }
                Log.e("SignInUser","Error writing document")
            }
    }


    fun getCurrentUserId():String{
        val currentUser=FirebaseAuth.getInstance().currentUser
        var currentUserID=""
        if(currentUser!=null){
            currentUserID=currentUser.uid
        }
        return currentUserID
    }

    fun getAssignedMembersListDetails(activity:Activity,assignedTo: ArrayList<String> ){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID,assignedTo)
            .get()
            .addOnSuccessListener {
                document->
                Log.e(activity.javaClass.simpleName,document.documents.toString())
                val usersList: ArrayList<User> = ArrayList()
                for(i in document.documents){
                    val user=i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if(activity is MembersActivity) {
                    activity.setupMembersList(usersList)
                }else if(activity is TaskListActivity){
                    activity.boardMembersDetailsList(usersList)
                }
            }.addOnFailureListener {e->
                if(activity is MembersActivity) {
                    activity.hideProgressDialog()
                }else if(activity is TaskListActivity) {
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName,"Error while getting members list",e)
            }
    }

    fun getMemberDetails(activity:MembersActivity,email:String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL,email)
            .get()
            .addOnSuccessListener {
                document->
                if(document.documents.size>0){
                    val user=document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                    activity.showCorrectSnackBar("Member Added")
                }else{
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }
            }.addOnFailureListener {e->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while searching member",e)
            }
    }

    fun assignMemberToBoard(activity:MembersActivity,board: Board,user: User){
        val assignedToHashMap=HashMap<String,Any>()             //create hash object
        assignedToHashMap[Constants.ASSIGNED_TO]=board.assignedTo    //add new member details

        mFireStore.collection(Constants.BOARDS)            //access collection named "boards"
            .document(board.documentId)                     //access document from collection with documentId
            .update(assignedToHashMap)                    //update assigned members list
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)       //prints success message
            }.addOnFailureListener {e->
                activity.hideProgressDialog()      //hide please wait message
                Log.e(activity.javaClass.simpleName,"Error while adding member",e)   //print fail message
            }
    }

    fun deleteBoardFromDatabase(activity:TaskListActivity, documentId:String){
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                activity.boardDeleted()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while adding member",e)}

    }

    fun leaveBoardFromDatabase(activity:TaskListActivity,board:Board){
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .get()
            .addOnSuccessListener {
                var ogMembers=board.assignedTo
                ogMembers.remove(getCurrentUserId())
                mFireStore.collection(Constants.BOARDS)
                    .document(board.documentId)
                    .update(Constants.ASSIGNED_TO,ogMembers)
                    .addOnSuccessListener{
                        activity.leaveMemberSuccess()
                    }
                    .addOnFailureListener {e->
                        activity.hideProgressDialog()
                        Log.e(activity.javaClass.simpleName,"Error while adding member",e)
                    }
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while adding member",e)}
    }

    fun deleteMemberFromBoard(activity:MembersActivity,board:Board,id:String){
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .get()
            .addOnSuccessListener {
                var ogMembers=board.assignedTo
                ogMembers.remove(id)
                mFireStore.collection(Constants.BOARDS)
                    .document(board.documentId)
                    .update(Constants.ASSIGNED_TO,ogMembers)
                    .addOnSuccessListener{
                        activity.memberRemovalSuccess()
                    }
                    .addOnFailureListener {e->
                        activity.hideProgressDialog()
                        Log.e(activity.javaClass.simpleName,"Error while adding member",e)
                    }
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while adding member",e)}
    }
}