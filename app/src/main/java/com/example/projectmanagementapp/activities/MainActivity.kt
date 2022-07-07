package com.example.projectmanagementapp.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projectmanagementapp.R
import com.example.projectmanagementapp.adapters.BoardItemsAdapter
import com.example.projectmanagementapp.firebase.FirestoreClass
import com.example.projectmanagementapp.models.Board
import com.example.projectmanagementapp.models.User
import com.example.projectmanagementapp.utils.Constants
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object{
        const val MY_PROFILE_REQUEST_CODE : Int = 11
        const val CREATE_BOARD_REQUEST_CODE : Int = 12
    }

    private lateinit var mUserName : String
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpActionBar()
        nav_view.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(Constants.PROJECTMANAGEMENTAPP_PREFERENCES, Context.MODE_PRIVATE)

        FirestoreClass().loadUserData(this, true)

        fab_create_board.setOnClickListener {
            val intent = Intent(this,CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED,false)
        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this,true)
        }else{
            //if we don't have updated token, have it
                Log.i("Token Update","Entered")
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener(this@MainActivity) {
                    updateFcmToken(it)
                }
        }

        // Refresh function for the layout
        refreshLayout.setOnRefreshListener{

            // This line is important as it explicitly refreshes only once
            // If "true" it implicitly refreshes forever
            refreshLayout.isRefreshing = false
            FirestoreClass().loadUserData(this, true)
        }
    }

    private fun setUpActionBar(){
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode==Activity.RESULT_OK && requestCode== MY_PROFILE_REQUEST_CODE){
            FirestoreClass().loadUserData(this)
        }else if(resultCode==Activity.RESULT_OK && requestCode== CREATE_BOARD_REQUEST_CODE){
            FirestoreClass().getBoardsList(this)
        } else{
            Log.e("Cancelled","Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(this@MainActivity, MyProfileActivity::class.java),
                    MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigationUserDetails(user : User, readBoardsList : Boolean){
        hideProgressDialog()
        mUserName = user.name

        val headerView = nav_view.getHeaderView(0)

        val navUserImage = headerView.findViewById<ImageView>(R.id.nav_user_image)
        Glide
            .with(this@MainActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)

        val navUsername = headerView.findViewById<TextView>(R.id.tv_username)
        navUsername.text = user.name

        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }

    }

    fun populateBoardsListToUI(boardsList: ArrayList<Board>){
        hideProgressDialog()
        if(boardsList.size > 0){
            tv_no_boards_available.visibility = View.GONE
            rv_boards_list.visibility = View.VISIBLE

            rv_boards_list.layoutManager = LinearLayoutManager(this)
            rv_boards_list.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this,boardsList)
            rv_boards_list.adapter = adapter

            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }

            })


        }else{
            tv_no_boards_available.visibility = View.VISIBLE
            rv_boards_list.visibility = View.GONE
        }
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this,true)
    }

    private fun updateFcmToken(token: String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this@MainActivity,userHashMap)
    }
}