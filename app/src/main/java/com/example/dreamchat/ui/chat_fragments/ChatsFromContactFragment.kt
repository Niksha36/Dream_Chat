package com.example.dreamchat.ui.chat_fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.adapters.UserListAdapter
import com.example.dreamchat.databinding.FragmentChatsFromContactBinding
import com.example.dreamchat.ui.BindingFragment
import com.example.dreamchat.util.UserContacts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatsFromContactFragment : BindingFragment<FragmentChatsFromContactBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChatsFromContactBinding::inflate
    private val viewModel: ChatViewModel by navGraphViewModels(R.id.chat_nav_graph) { defaultViewModelProviderFactory }
    lateinit var myAdapter: UserListAdapter
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchContacts()
        } else {
            Toast.makeText(
                requireContext(),
                "Permission denied to read contacts",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                    findNavController().getBackStackEntry(R.id.chat_nav_graph).viewModelStore.clear()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Set up the toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(
                requireContext(),
                R.color.whatsapp_secondary
            )
        )
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility = View.VISIBLE
        if (viewModel.loadContactsFlag) {
            viewModel.loadContactsFlag = false
            checkAndRequestContactsPermission()
        }
        setUpRecyclerView()
        subscribeToEvent()
        myAdapter.setOnItemClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected == true) {
                viewModel.saveSelectedContact(it)
            } else {
                viewModel.deleteUnselectedContact(it)
            }
            myAdapter.notifyItemChanged(myAdapter.differ.currentList.indexOf(it))
        }

        binding.fabNext.setOnClickListener {
            Log.e(
                "SIZE",
                "${viewModel.selectedContacts.value.size}, ${viewModel.selectedContacts.value}"
            )
            if (viewModel.selectedContacts.value.size > 0) {
                findNavController().navigate(R.id.action_chatsFromContactFragment_to_createChannelFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select users to add in the channel",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun subscribeToEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registeredUsersFromContacts.collect {
                    myAdapter.differ.submitList(it) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            binding.progressBar.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }
    }

    fun setUpRecyclerView() {
        myAdapter = UserListAdapter()
        with(binding.rvUserList) {
            adapter = myAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                private val spaceHeight =
                    resources.getDimensionPixelSize(R.dimen.recycler_view_item_space)

                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = spaceHeight
                    outRect.left = spaceHeight
                }
            })
        }
    }

    private fun fetchContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contacts = UserContacts.getContactList(this@ChatsFromContactFragment).toSet()
            viewModel.findRegisteredUsersFromContacts(contacts)
        }
    }

    private fun checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            fetchContacts()
        }
    }
}