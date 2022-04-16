package com.example.projectmanagementapp.firebase

import android.app.Activity
import android.util.Log
import com.example.projectmanagementapp.activities.MainActivity
import com.example.projectmanagementapp.activities.SignInActivity
import com.example.projectmanagementapp.activities.SignUpActivity
import com.example.projectmanagementapp.models.User
import com.example.projectmanagementapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity : SignUpActivity,userInfo : User){
        mFireStore.collection(Constants.USERS).
                document(getCurrentUserId()).set(userInfo, SetOptions.merge()).
                addOnSuccessListener {
                    activity.userRegisteredSuccess()
                }.addOnFailureListener { e->
                    activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName,"Error ---")
                }
    }

    fun signInUser(activity: Activity) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                Log.e("SignInUser-------------"+activity.javaClass.simpleName, document.toString())
                val loggedInUser = document.toObject(User::class.java)!!
                when (activity) {
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser)
                    }
                }
            }.addOnFailureListener { e ->
            when (activity) {
                is SignInActivity -> {
                    activity.hideProgressDialog()
                }
                is MainActivity -> {
                    activity.hideProgressDialog()
                }
            }
            Log.e("signInUser", "Error ---", e)
        }
    }
    fun getCurrentUserId() : String {
        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if(currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }
}