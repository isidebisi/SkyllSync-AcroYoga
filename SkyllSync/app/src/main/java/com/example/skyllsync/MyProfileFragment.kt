package com.example.skyllsync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skyllsync.databinding.FragmentMyProfileBinding

class MyProfileFragment : Fragment() {

    private lateinit var viewModel: MyProfileViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var binding: FragmentMyProfileBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MyProfileViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)
        (activity as AppCompatActivity).supportActionBar?.title = "My Profile"

        viewModel.key = userDataSharedViewModel.loggedInUserKey.value.toString()
        userDataSharedViewModel.getUserImage(context)

        binding.myProfileTextView.text = userDataSharedViewModel.loggedInUser.value
        Glide.with(this).load(userDataSharedViewModel.loggedInUserImage.value)
            .into(binding.myProfileImageView)

        //add UI element interactions
        val myProfileImageView = binding.myProfileImageView
        val logoutButton = binding.logoutButton

        myProfileImageView.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
            imgIntent.setType("image/*")
            resultLauncher.launch(imgIntent)
        }

        logoutButton.setOnClickListener {
            userDataSharedViewModel.resetUserData()
            findNavController().navigate(R.id.action_myProfileFragment_to_loginProfileFragment)
        }

        userDataSharedViewModel.userImageReady.observe(viewLifecycleOwner) { ready ->
            if (ready == true) {
                Glide.with(this).load(userDataSharedViewModel.loggedInUserImage.value)
                    .into(binding.myProfileImageView)
                viewModel.imageUri = userDataSharedViewModel.loggedInUserImage.value
                userDataSharedViewModel.resetUserImageReady()
            }
        }

        viewModel.uploadSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Glide.with(this).load(userDataSharedViewModel.loggedInUserImage.value)
                    .into(binding.myProfileImageView)
                viewModel.resetUploadSuccess()
            }
        }


        return binding.root
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                viewModel.imageUri = imageUri
                userDataSharedViewModel.loggedInUserImage.value = imageUri
                viewModel.modifyProfileImage(context)
            }
        }

    override fun onResume() {
        super.onResume()
        viewModel.key = userDataSharedViewModel.loggedInUserKey.value.toString()
        //userDataSharedViewModel.getUserImage(context)
    }

}