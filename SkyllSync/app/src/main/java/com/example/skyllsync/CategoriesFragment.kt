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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyllsync.databinding.FragmentCategoriesBinding

class CategoriesFragment : Fragment(), CategoryItemAdapter.OnItemClickListener {

    private lateinit var viewModel: CategoriesViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var adapter: CategoryItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentCategoriesBinding = FragmentCategoriesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        userDataSharedViewModel = ViewModelProvider(requireActivity())[UserDataSharedViewModel::class.java]
        (activity as AppCompatActivity).supportActionBar?.title = userDataSharedViewModel.loggedInUser.value


        // Add UI elements
        val addCategoryFloatingActionButton = binding.addCategoryFloatingActionButton
        val addCategoryButton = binding.addCategoryButton
        val cancelCategoryButton = binding.cancelCategoryButton
        val addCategoryWindow = binding.addCategoryWindow
        val deleteCategoryButton = binding.deleteCategoryButton
        val cancelDeleteCategoryButton = binding.cancelDeleteCategoryButton
        val swipeRefreshCategories = binding.swipeRefreshCategories


        // Set the swipeRefreshCategories to refresh the categories
        swipeRefreshCategories.setOnRefreshListener {
            viewModel.getCategories()
            swipeRefreshCategories.isRefreshing = false
        }

        addCategoryFloatingActionButton.setOnClickListener {
            // Make the addCategoryWindow visible and show the Keyboard
            addCategoryWindow.visibility = View.VISIBLE
            addCategoryFloatingActionButton.visibility = View.GONE
            binding.addCategoryEditText.setText("")
            KeyboardUtils.showKeyboard(requireActivity(), binding.addCategoryEditText)

            // Set onClickListeners for the addCategoryButton and cancelCategoryButton
            addCategoryButton.setOnClickListener {
                // Add the category to the database
                val category = binding.addCategoryEditText.text.toString()
                if(viewModel.addCategory(category, context) == false){
                    Toast.makeText(context, "Category already exists", Toast.LENGTH_SHORT).show()
                } else {
                    // Hide the addCategoryWindow and Keyboard
                    KeyboardUtils.hideKeyboard(requireActivity(), binding.addCategoryEditText)
                    addCategoryWindow.visibility = View.GONE
                    addCategoryFloatingActionButton.visibility = View.VISIBLE

                }


            }
            cancelCategoryButton.setOnClickListener {
                // Hide the addCategoryWindow and the Keyboard
                KeyboardUtils.hideKeyboard(requireActivity(), binding.addCategoryEditText)
                addCategoryWindow.visibility = View.GONE
                addCategoryFloatingActionButton.visibility = View.VISIBLE
            }
        }

        // Set the LayoutManager that this RecyclerView will use.
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)

        // Initialize the adapter with an empty list
        adapter = CategoryItemAdapter(requireContext(), arrayListOf(), arrayListOf(), this)
        binding.categoriesRecyclerView.adapter = adapter

        // get categories from firebase
        viewModel.getCategories()
        viewModel.updateCategoriesAdapter.observe(viewLifecycleOwner, Observer {updateCategoriesAdapter ->
            if(updateCategoriesAdapter == true){
                // Update the data in the adapter
                adapter.itemsCategories =   ArrayList(viewModel.categories.map { it.name })
                adapter.itemsNoSkills =     ArrayList(viewModel.categories.map { it.numberOfSkills } )
                adapter.notifyDataSetChanged()
                viewModel.resetUpdate()
            }
        })

        // Set onClickListeners for the deleteCategoryButton and cancelDeleteCategoryButton
        deleteCategoryButton.setOnClickListener {
            viewModel.deleteCategory(context)
            binding.deleteCategoryWindow.visibility = View.GONE
        }
        cancelDeleteCategoryButton.setOnClickListener {
            binding.deleteCategoryWindow.visibility = View.GONE
            viewModel.categoryToDelete = null
        }

        return binding.root
    }

    //Callback from CategoryItemAdapter when clicked on item
    override fun onItemClick(position: Int) {
        userDataSharedViewModel.selectCategory(viewModel.categories[position].name)
        //navigate to skillListFragment
        val directions = CategoriesFragmentDirections.actionCategoriesFragmentToSkillListFragment()
        findNavController().navigate(directions)
    }

    //Callback from CategoryItemAdapter when clicked on delete button of item
    override fun onDeleteClick(position: Int) {
        //show delete category window
        val deleteCategoryWindow = view?.findViewById<View>(R.id.deleteCategoryWindow)
        deleteCategoryWindow?.visibility = View.VISIBLE
        viewModel.categoryToDelete = position
    }

    override fun onResume() {
        super.onResume()
        viewModel.key = userDataSharedViewModel.loggedInUserKey.value
        viewModel.getCategories()
        (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
    }
}