package com.example.skyllsync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.skyllsync.databinding.FragmentSkillBinding

class SkillFragment : Fragment(), SkillPictureItemAdapter.OnItemClickListener {

    private lateinit var viewModel: SkillViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var adapter: SkillPictureItemAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentSkillBinding = FragmentSkillBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(SkillViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)
        (activity as MainActivity).supportActionBar?.title = userDataSharedViewModel.loggedInUser.value

        //Load database context to viewModel
        viewModel.key = userDataSharedViewModel.loggedInUserKey.value.toString()
        viewModel.chosenSkill = userDataSharedViewModel.chosenSkill.value.toString()
        viewModel.chosenCategory = userDataSharedViewModel.chosenCategory.value.toString()
        binding.skillNameTextView.text = viewModel.chosenSkill

        //Add UI elements

        val progressionButton = binding.progressionCardView
        val priorityButton = binding.priorityCardView
        val swipeRefreshSkill = binding.swipeRefreshSkill

        val addSkillPictureFloatingActionButton = binding.addSkillPictureFloatingActionButton
        val addSkillPictureWindow = binding.addSkillPictureWindow
        val addSkillPictureGallery = binding.addSkillPictureGallery
        val addSkillPictureCamera = binding.addSkillPictureCamera
        val addSkillPictureInstagram = binding.addSkillPictureInstagram

        val addInstagramWindow = binding.addInstagramWindow
        val addInstagramButton = binding.addInstagramButton
        val cancelInstagramButton = binding.cancelInstagramButton
        val instagramImageView = binding.instagramImageView

        //Pair UP UI elements
        val addPairEditText = binding.addPairEditText
        val pairUpButton = binding.pairUpCardView
        val addPairWindow = binding.addPairWindow
        val addPairButton = binding.addPairButton
        val cancelPairButton = binding.cancelPairButton
        val responsePairUpWindow = binding.responsePairUpWindow
        val acceptPairUpButton = binding.acceptPairUpButton
        val declinePairUpButton = binding.declinePairUpButton
        val deleteMediaButton = binding.deleteMediaButton


        // Set the swipeRefreshSkill to refresh the skill
        swipeRefreshSkill.setOnRefreshListener {
            viewModel.getSkillData(context)
            swipeRefreshSkill.isRefreshing = false
        }

        //setOnClickListener for Add picture floating action button
        addSkillPictureFloatingActionButton.setOnClickListener {
            //show dialog window with two options: gallery and camera
            addSkillPictureWindow.visibility = View.VISIBLE
            addSkillPictureFloatingActionButton.visibility = View.GONE
            addSkillPictureGallery.setOnClickListener {
                addSkillPictureWindow.visibility = View.GONE
                addSkillPictureFloatingActionButton.visibility = View.VISIBLE
                val imgVideoIntent = Intent(Intent.ACTION_GET_CONTENT)
                imgVideoIntent.type = "*/*"
                imgVideoIntent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                resultLauncher.launch(imgVideoIntent)
            }

            addSkillPictureCamera.setOnClickListener {
                addSkillPictureWindow.visibility = View.GONE
                addSkillPictureFloatingActionButton.visibility = View.VISIBLE
                findNavController().navigate(R.id.action_skillFragment_to_takePictureFragment)
            }

            addSkillPictureInstagram.setOnClickListener {
                addSkillPictureWindow.visibility = View.GONE
                addInstagramWindow.visibility = View.VISIBLE
                binding.addInstagramEditText.setText(viewModel.skillInstagramLink)
            }
        }

        //set observer for mediaUpload Success
        viewModel.mediaUploadSuccess.observe(viewLifecycleOwner, Observer {mediaUploadSuccess ->
            if(mediaUploadSuccess){
                Toast.makeText(context, "Media uploaded successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetMediaUploadSuccess()
            }
        })

        //set observer for the camera result
        userDataSharedViewModel.mediaUri.observe(viewLifecycleOwner, Observer {mediaUri ->
            if(mediaUri != null){
                //figure out mediaUri is an image or video and call addMedia accordingly
                val mediaType = context?.contentResolver?.getType(mediaUri)
                if (mediaType != null) {
                    if (mediaType.startsWith("image/")) {
                        viewModel.addMedia("image", mediaUri, context)
                    } else if (mediaType.startsWith("video/")) {
                        viewModel.addMedia("video", mediaUri, context)
                    }
                }
                userDataSharedViewModel.resetMediaUri()
            }
        })

        //when coming from the camera  we need to pop the backstack once, else if we push the back button
        //we go back to the skill fragment from the skill fragment
        userDataSharedViewModel.popBackStackOnce.observe(viewLifecycleOwner, Observer {popBackStackOnce ->
            if(popBackStackOnce){
                findNavController().popBackStack()
                userDataSharedViewModel.resetPopBackStackOnce()
            }
        })

        //setOnClickListener for Add and Cancel Instagram button
        addInstagramButton.setOnClickListener {
            KeyboardUtils.hideKeyboard(requireActivity(), binding.addInstagramEditText)
            addInstagramWindow.visibility = View.GONE
            addSkillPictureFloatingActionButton.visibility = View.VISIBLE
            viewModel.addInstagramLink(context, binding.addInstagramEditText.text.toString())
            if(viewModel.skillInstagramLink != ""){
                instagramImageView.visibility = View.VISIBLE
                instagramImageView.isClickable = true
            } else {
                instagramImageView.visibility = View.INVISIBLE
                instagramImageView.isClickable = false
            }
        }
        cancelInstagramButton.setOnClickListener {
            addInstagramWindow.visibility = View.GONE
            addSkillPictureFloatingActionButton.visibility = View.VISIBLE
        }

        //setOnClickListener for Instagram image
        instagramImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.skillInstagramLink))
            startActivity(intent)
        }



        //setOnClickListener for Progression button, iterate on progressions for each click
        progressionButton.setOnClickListener{
            if(viewModel.skillProgression < 5){
                viewModel.skillProgression++

            } else {
                viewModel.skillProgression = 0

            }
            viewModel.updateProgressionPriority(context)
            updateBinding(binding, (activity as MainActivity))
        }

        //setOnClickListener for Priority button, iterate on priority for each click
        priorityButton.setOnClickListener{
            if (viewModel.skillPriority < 5){
                viewModel.skillPriority++
            } else {
                viewModel.skillPriority = 0
            }
            viewModel.updateProgressionPriority(context)
            updateBinding(binding, (activity as MainActivity))
        }

        //setOnClickListener for PairUp button
        pairUpButton.setOnClickListener{
            if(viewModel.skillPairUpStatus == 0){
                //if not paired up: show addPairWindow and show keyboard
                addPairWindow.visibility = View.VISIBLE
                KeyboardUtils.showKeyboard(requireActivity(), addPairEditText)
            } else if(viewModel.skillPairUpStatus == 1){
                //if request received: show responsePairUpWindow
                responsePairUpWindow.visibility = View.VISIBLE
            } else if(viewModel.skillPairUpStatus == 2){
                //nothing for now
            } else if(viewModel.skillPairUpStatus == 3){
                //if paired up: unpair
                viewModel.unpairSkill(context)
            }
        }

        //setOnClickListener to send request to PairUp partner and hide keyboard
        addPairButton.setOnClickListener{
            viewModel.addPairUpPartner(context, addPairEditText.text.toString())
        }
        //setOnClickListener to hide keyboard and close PairUp window
        cancelPairButton.setOnClickListener{
            addPairEditText.text.clear()
            KeyboardUtils.hideKeyboard(requireActivity(), addPairEditText)
            addPairWindow.visibility = View.GONE
        }

        //accept PairUp request
        acceptPairUpButton.setOnClickListener{
            viewModel.acceptPairUpRequest(context)
            responsePairUpWindow.visibility = View.GONE
        }
        //decline PairUp request
        declinePairUpButton.setOnClickListener{
            viewModel.declinePairUpRequest(context)
            responsePairUpWindow.visibility = View.GONE
        }

        //Set the LayoutManager that this RecyclerView will use.
        binding.picturesRecyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.HORIZONTAL, false)
        val layoutManager = binding.picturesRecyclerView.layoutManager as LinearLayoutManager


        // Initialize the adapter with empty list
        adapter = SkillPictureItemAdapter(requireContext(), arrayListOf(), this)
        binding.picturesRecyclerView.adapter = adapter
        viewModel.itemAdapter = adapter


        deleteMediaButton.setOnClickListener {
            val position = layoutManager.findFirstVisibleItemPosition()
            viewModel.deleteMedia(position)
        }

        // Add the SnapHelper to the RecyclerView:
        // This makes the RecyclerView snap to the item closest to the center of the screen.
        // Instead of scrolling continuously
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.picturesRecyclerView)

        //get all necessary information from Firebase database
        viewModel.getSkillData(context)
        viewModel.updateBinding.observe(viewLifecycleOwner, Observer {updateBinding ->
            if (updateBinding) {
                viewModel.assembleMediaURLs()
                adapter.mediaItems = viewModel.mediaURLs
                adapter.notifyDataSetChanged()
                updateBinding(binding, (activity as MainActivity))

                if(viewModel.pairUpAccepted == true) {
                    viewModel.pushPairedSkillToLoggedInCloud(context)
                    viewModel.pairUpAccepted = false
                }
                if(viewModel.skillInstagramLink != ""){
                    instagramImageView.visibility = View.VISIBLE
                    instagramImageView.isClickable = true
                } else {
                    instagramImageView.visibility = View.INVISIBLE
                    instagramImageView.isClickable = false
                }

                if(viewModel.mediaURLs.isEmpty()){
                    deleteMediaButton.visibility = View.GONE
                } else {
                    deleteMediaButton.visibility = View.VISIBLE
                }

                viewModel.resetUpdateBinding()
            }
        })

        //observe if pairUp request was sent, then close PairUp window and hide keyboard
        viewModel.pairUpRequestSent.observe(viewLifecycleOwner, Observer {pairUpRequestSent ->
            if(pairUpRequestSent){
                addPairEditText.text.clear()
                KeyboardUtils.hideKeyboard(requireActivity(), addPairEditText)
                addPairWindow.visibility = View.GONE
                updateBinding(binding, (activity as MainActivity))
                viewModel.resetPairUpRequest()
            }
        })

        return binding.root
    }

    //Update binging with new information
    fun updateBinding(binding: FragmentSkillBinding, context : Context){
        // Progression: Update progress bar and text
        binding.progressBar.progress = viewModel.skillProgression * 20

        val progressionText = SkillClass.getProgressionNameFromNumber(viewModel.skillProgression)
        binding.progressionName.text = progressionText

        // Priority: Update button text and color
        binding.priorityNumber.text = viewModel.skillPriority.toString()
        val imagePath = SkillClass.getPriorityPathFromNumber(viewModel.skillPriority)
        binding.priorityMark.setImageResource(context.resources.getIdentifier(imagePath, "drawable", context.packageName))

        // PairUp: Update button text and partner name
        binding.pairUpCardView.isClickable = true

        binding.pairUpTextView.text = SkillClass.getPairUpActionFromNumber(viewModel.skillPairUpStatus)
        if(viewModel.skillPairUpStatus > SkillClass.pairUpStatus.NOT_PAIRED.status){
            binding.partnerTextView.text = viewModel.skillPairUpName
        } else {
            binding.partnerTextView.text = ""
        }
        if (viewModel.skillPairUpStatus == SkillClass.pairUpStatus.PENDING_SENT.status){
            binding.pairUpCardView.isClickable = false
        }
    }

    //Get image or video from gallery
    //distinguish if its an image or video and call the addMedia function accordingly
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    val mimeType = context?.contentResolver?.getType(uri)
                    if (mimeType != null) {
                        if (mimeType.startsWith("image/")) {
                            // Handle image
                            viewModel.addMedia("image", uri, context)
                        } else if (mimeType.startsWith("video/")) {
                            // Handle video
                            viewModel.addMedia("video", uri, context)
                        }
                    }
                }
            }
        }

    override fun onItemClick(position: Int) {
        //when clicked on a media item in the recyclerView. No functionality needed yet
    }

    override fun onResume() {
        super.onResume()
        viewModel.key = userDataSharedViewModel.loggedInUserKey.value.toString()
        viewModel.chosenSkill = userDataSharedViewModel.chosenSkill.value.toString()
        viewModel.chosenCategory = userDataSharedViewModel.chosenCategory.value.toString()
        viewModel.setUpdateBinding()
        (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
    }
}