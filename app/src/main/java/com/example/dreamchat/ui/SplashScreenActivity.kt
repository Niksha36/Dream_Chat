package com.example.dreamchat.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dreamchat.ChatActivity
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.databinding.ActivitySplashScreenBinding
import com.example.dreamchat.util.CreatingStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()
    lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        subscribeToEvents()
        if (viewModel.getUser() == null && viewModel.isLoggedIn()) {
            viewModel.setUser()
        } else {
            navigateToMainActivity()
        }
    }

    private fun subscribeToEvents() {
        lifecycleScope.launch {
            viewModel.loginEvent.collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is CreatingStates.Error -> {
                            hideProgressBar()
                            Toast.makeText(
                                this@SplashScreenActivity,
                                it.message.toString(),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is CreatingStates.Success -> {
                            hideProgressBar()
                            Toast.makeText(
                                this@SplashScreenActivity,
                                "Successfully logged in!",
                                Toast.LENGTH_LONG
                            ).show()
                            navigateToMainActivity()
                        }
                    }
                }
            }
        }
    }

    private fun hideProgressBar() {
        binding.progressBar2.visibility = View.INVISIBLE
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this@SplashScreenActivity, ChatActivity::class.java))
        finish()
    }
}