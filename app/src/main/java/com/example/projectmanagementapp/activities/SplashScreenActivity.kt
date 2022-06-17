package com.example.projectmanagementapp.activities

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import com.example.projectmanagementapp.R
import com.example.projectmanagementapp.firebase.FirestoreClass
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val typeFace: Typeface = Typeface.createFromAsset(assets,"BLKCHCRY.TTF")
        tv_app_name.typeface = typeFace

        Handler().postDelayed({
            val currentUserID = FirestoreClass().getCurrentUserId()
            if(!currentUserID.equals("")){
                startActivity(Intent(this,MainActivity::class.java))
            }else {
                startActivity(Intent(this, IntroActivity::class.java))
            }
            finish()
        },2500)

    }
}