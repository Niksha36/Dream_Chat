package com.example.dreamchat

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.util.putObject
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        if (viewModel.isLoggedIn()) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.enterPhoneFragment, true)
                .build()
            navController.navigate(R.id.chatListFragment, null, navOptions)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.e("OnStop", "OnStop called!")
        val user = viewModel.getUser()
        user?.let { Log.e("OnStop", it.id) }
        sharedPreferences.putObject("user", user)
    }
}