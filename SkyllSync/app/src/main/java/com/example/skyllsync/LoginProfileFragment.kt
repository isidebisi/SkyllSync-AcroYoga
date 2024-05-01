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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skyllsync.databinding.FragmentLoginProfileBinding

class LoginProfileFragment : Fragment() {

    private lateinit var viewModel: LoginProfileViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var binding: FragmentLoginProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentLoginProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(LoginProfileViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)

        //Set up image picker
        binding.Userimage.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
            imgIntent.setType("image/*")
            resultLauncher.launch(imgIntent)
        }

        //Set up observer for image upload
        viewModel.uploadSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success == false){
                Toast.makeText(context,"Profile image upload to firebase failed.",
                    Toast.LENGTH_SHORT).show()
            }
        })

        //OnClickListener for the Sign Up (Register) Button
        //The username length is limited to 20 characters for binding in other app fragments
        binding.SignUp.setOnClickListener { view : View ->

            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            //Limit username length for binding in other app fragments
            } else if (binding.Username.text.toString().length > 20) {
                Toast.makeText(context,"Username too long", Toast.LENGTH_SHORT).show()
            } else if (viewModel.imageUri == null) {
                Toast.makeText(context,"Pick an image", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()

                //Make sure username is unique
                viewModel.checkUsername()
            }
        }

        viewModel.usernameExist.observe(viewLifecycleOwner, Observer { exist ->
            if (exist == true){
                Toast.makeText(context,"Username already exists", Toast.LENGTH_SHORT).show()
            } else if (exist == false) {
                viewModel.key = viewModel.username
                viewModel.sendDataToFireBase(activity?.applicationContext)

                activity?.applicationContext?.let { viewModel.createDefaultCategories(it) }


                userDataSharedViewModel.selectUser(viewModel.username)
                userDataSharedViewModel.selectUserKey(viewModel.username)
                if (viewModel.imageUri != null) {
                    userDataSharedViewModel.selectUserImage(viewModel.imageUri!!)
                }

                val directions = LoginProfileFragmentDirections.actionLoginProfileFragmentToCategoriesFragment()
                findNavController().navigate(directions)

                (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
            }
        })


        viewModel.profilePresent.observe(viewLifecycleOwner, Observer { success ->
            if (success == false){
                Toast.makeText(context,"Incorrect password/username",
                    Toast.LENGTH_LONG).show()
            }
            else if (success == true){
                userDataSharedViewModel.selectUser(viewModel.username)
                userDataSharedViewModel.selectUserKey(viewModel.username)
                if (viewModel.imageUri != null) {
                    userDataSharedViewModel.selectUserImage(viewModel.imageUri!!)
                }

                val directions = LoginProfileFragmentDirections.actionLoginProfileFragmentToCategoriesFragment()
                findNavController().navigate(directions)

                (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
            }
        })

        binding.SignIn.setOnClickListener { view : View ->
            if (binding.Username.text.toString() == "") {
                Toast.makeText(context, "Enter username", Toast.LENGTH_SHORT).show()
            } else if (binding.Password.text.toString() == "") {
                Toast.makeText(context,"Enter password", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()

                viewModel.fetchProfile()
            }
        }


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBottomNavigationVisibility(View.GONE)

    }

    override fun onPause() {
        super.onPause()
        viewModel.resetUserData()
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                viewModel.imageUri = imageUri
                Glide.with(this).load(imageUri).into(binding.Userimage)
            }
        }

}