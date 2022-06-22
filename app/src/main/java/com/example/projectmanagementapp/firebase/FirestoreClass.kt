package com.example.projectmanagementapp.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projectmanagementapp.activities.*
import com.example.projectmanagementapp.models.Board
import com.example.projectmanagementapp.models.User
import com.example.projectmanagementapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity : SignUpActivity, userInfo : User){
        mFireStore.collection(Constants.USERS).
        document(getCurrentUserId()).set(userInfo, SetOptions.merge()).
        addOnSuccessListener {
            activity.userRegisteredSuccess()
        }.addOnFailureListener { e->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName,"Error ---")
        }
    }

    fun getCurrentUserId() : String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if(currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun loadUserData(activity : Activity, readBoardsList: Boolean = false) {

        // Here we pass the collection name from which we wants the data.
        mFireStore.collection(Constants.USERS)
            // The document id to get the Fields of user.
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.toString())

                // Here we have received the document snapshot which is converted into the User Data model object.
                val loggedInUser = document.toObject(User::class.java)!!

                // Here call a function of base activity for transferring the result to it.
                when (activity) {
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                    }
                    is MyProfileActivity -> {
                        activity.setUserDataInUI(loggedInUser)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Here call a function of base activity for transferring the result to it.
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting loggedIn user details",
                    e
                )
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS) // Collection Name
            .document(getCurrentUserId()) // Document ID
            .update(userHashMap) // A hashmap of fields which are to be updated.
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Data updated successfully!")

                // Notify the success result.

                when (activity) {
                    is MainActivity -> {
                        //activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity -> {
                        activity.profileUpdateSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a board.",
                    e
                )
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board){
        mFireStore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName,"Board created successfully!")
                Toast.makeText(activity,"Board created successfully",Toast.LENGTH_SHORT).show()
                activity.boardCreatedSuccessfully()
            }
            .addOnFailureListener {
                exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a board", exception)
            }
    }

    fun getBoardsList(activity: MainActivity){
         mFireStore.collection(Constants.BOARDS)
             .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
             .get()
             .addOnSuccessListener {
                 document ->
                 Log.i(activity.javaClass.simpleName,document.documents.toString())
                 val boardsList: ArrayList<Board> = ArrayList();
                 for (i in document.documents){
                     val board = i.toObject(Board::class.java)!!
                     board.documentId = i.id
                     boardsList.add(board)
                 }

                 activity.populateBoardsListToUI(boardsList)
             }
             .addOnFailureListener {
                 e ->
                 activity.hideProgressDialog()
                 Log.e(activity.javaClass.simpleName, "Error while displaying board -- getBoardsList()")

             }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName,document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
            }
            .addOnFailureListener {
                    e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while displaying board -- getBoardDetails()")

            }
    }

    fun addUpdateTaskList(activity: TaskListActivity, board : Board){
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName,"Task List updated successfully!")
                activity.addUpdateTaskListSuccess()
            }
            .addOnFailureListener {
                e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a booard --- addUpdateTaskList()",e)
            }
    }

    fun getAssignedMembersList(activity: MembersActivity, assignedTo: ArrayList<String>){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener {
                document ->
                Log.e(activity.javaClass.simpleName,document.documents.toString())
                val usersList : ArrayList<User> = ArrayList()
                for(i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }

                activity.setUpMembersList(usersList)

            }
            .addOnFailureListener {
                e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a board",e)
            }
    }

    fun getMemberDetails(activity: MembersActivity,email : String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener {
                document ->
                if(document.documents.size>0){
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                }else{
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }
            }
            .addOnFailureListener {
                e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting user details -- getMemberDetails()", e)
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board,user: User){
        val assignedToHashMap = HashMap<String,Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }
            .addOnFailureListener {
                e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while assigning the member to the board --assignMemberToBoard()",e)
            }
    }

}