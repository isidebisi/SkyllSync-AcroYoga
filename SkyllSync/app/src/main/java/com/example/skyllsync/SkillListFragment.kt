package com.example.skyllsync

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyllsync.databinding.FragmentSkillListBinding

class SkillListFragment : Fragment(), SkillListItemAdapter.OnItemClickListener {


    private lateinit var viewModel: SkillListViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var adapter: SkillListItemAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentSkillListBinding = FragmentSkillListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SkillListViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)
        (activity as AppCompatActivity).supportActionBar?.title = userDataSharedViewModel.loggedInUser.value

        // Add UI elements
        val addSkillFloatingActionButton = binding.addSkillFloatingActionButton
        val addSkillButton = binding.addSkillButton
        val cancelSkillButton = binding.cancelSkillButton
        val addSkillWindow = binding.addSkillWindow
        val deleteSkillButton = binding.deleteSkillButton
        val cancelDeleteSkillButton = binding.cancelDeleteSkillButton
        val sortButton = binding.sortButton
        val swipeRefreshSkills = binding.swipeRefreshSkills
        val categoryName = binding.currentCategoryTextView

        sortButton.text = viewModel.sortBy.description
        categoryName.text = userDataSharedViewModel.chosenCategory.value


        //Set onRefreshListener for the swipeRefreshSkills
        swipeRefreshSkills.setOnRefreshListener {
                viewModel.getSkills(context, userDataSharedViewModel.chosenCategory.value.toString())
                swipeRefreshSkills.isRefreshing = false
            }

        //Set onClickListeners for the sortButton
        sortButton.setOnClickListener {
            //sort the skills by another attribute
            viewModel.changeSort()
            sortButton.text = viewModel.sortBy.description
        }
        //Open dialog window to add new Skill when the addSkillFloatingActionButton is clicked
        addSkillFloatingActionButton.setOnClickListener {
            // Make the addSkillWindow visible
            addSkillWindow.visibility = View.VISIBLE
            addSkillFloatingActionButton.visibility = View.GONE
            binding.addSkillEditText.setText("")
            KeyboardUtils.showKeyboard(requireActivity(), binding.addSkillEditText)

            // Set onClickListeners for the addSkillButton and cancelSkillButton
            addSkillButton.setOnClickListener {
                // Add the Skill to the database
                val skill = binding.addSkillEditText.text.toString()
                if(!viewModel.addSkill(skill, userDataSharedViewModel.chosenCategory.value.toString(), context)){
                    Toast.makeText(context, "Skill already exists", Toast.LENGTH_SHORT).show()
                } else {
                    // Hide the addSkillWindow and Keyboard
                    KeyboardUtils.hideKeyboard(requireActivity(), binding.addSkillEditText)
                    addSkillWindow.visibility = View.GONE
                    addSkillFloatingActionButton.visibility = View.VISIBLE
                }


            }
            cancelSkillButton.setOnClickListener {
                // Hide the addSkillWindow and Keyboard
                KeyboardUtils.hideKeyboard(requireActivity(), binding.addSkillEditText)
                addSkillWindow.visibility = View.GONE
                addSkillFloatingActionButton.visibility = View.VISIBLE
            }
        }

        //Set the LayoutManager that this RecyclerView will use.
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)

        //Initialize the adapter with empty list
        adapter = SkillListItemAdapter(requireContext(), arrayListOf(), arrayListOf(), arrayListOf(), this)
        binding.skillsRecyclerView.adapter = adapter

        //get skills from firebase
        viewModel.getSkills(context, userDataSharedViewModel.chosenCategory.value.toString())
        viewModel.updateSkillsAdapter.observe(viewLifecycleOwner, Observer {updateSkillsAdapter ->
            if (updateSkillsAdapter == true) {
                adapter.items = ArrayList(viewModel.skills.map { it.name })
                adapter.itemsProgressLvl = ArrayList(viewModel.skills.map { it.progression })
                adapter.itemsPriorityLvl = ArrayList(viewModel.skills.map { it.priority })
                adapter.notifyDataSetChanged()
                viewModel.resetUpdate()
            }
        })

        //observe deletionCompleted (if skill is deleted) and update the adapter
        viewModel.deletionCompleted.observe(viewLifecycleOwner, Observer {deletionCompleted ->
            if(deletionCompleted == true){
                adapter.items = ArrayList(viewModel.skills.map { it.name })
                adapter.itemsPriorityLvl = ArrayList(viewModel.skills.map { it.priority })
                adapter.itemsProgressLvl = ArrayList(viewModel.skills.map { it.progression })
                adapter.notifyDataSetChanged()
                viewModel.resetDelete()
            }
        })

        //Set onClickListeners for the deleteCategoryButton and cancelDeleteCategoryButton
        deleteSkillButton.setOnClickListener {
            //delete the Skill from the database
            viewModel.deleteSkill(context, userDataSharedViewModel.chosenCategory.value.toString())
            binding.deleteSkillWindow.visibility = View.GONE
        }
        cancelDeleteSkillButton.setOnClickListener {
            // Hide the deleteSkillWindow
            binding.deleteSkillWindow.visibility = View.GONE
            viewModel.skillToDelete = null
        }


        return binding.root
    }

    //Callback from itemAdapter of the RecyclerView: navigate to skillFragment
    override fun onItemClick(position: Int) {
        userDataSharedViewModel.chosenSkill.value = adapter.items[position]

        //navigate to skillFragment
        val directions = SkillListFragmentDirections.actionSkillListFragmentToSkillFragment()
        findNavController().navigate(directions)
    }

    //Callback from itemAdapter of the RecyclerView: delete skill
    override fun onDeleteClick(position: Int) {
        val deleteSkillWindow = view?.findViewById<View>(R.id.deleteSkillWindow)
        deleteSkillWindow?.visibility = View.VISIBLE
        viewModel.skillToDelete = position
    }

    override fun onResume(){
        super.onResume()
        viewModel.key = userDataSharedViewModel.loggedInUserKey.value
        viewModel.getSkills(context, userDataSharedViewModel.chosenCategory.value.toString())
        (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
    }
}