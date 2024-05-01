package com.example.skyllsync

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyllsync.databinding.FragmentTrainingStatsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable

class TrainingStatsFragment : Fragment(), TrainingStatsItemAdapter.OnItemClickListener {

    companion object {
        fun newInstance() = TrainingStatsFragment()
    }

    private lateinit var viewModel: TrainingStatsViewModel
    private lateinit var userDataSharedViewModel: UserDataSharedViewModel
    private lateinit var binding: FragmentTrainingStatsBinding

    private lateinit var heartRatePlot: LineChart

    private lateinit var adapter: TrainingStatsItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTrainingStatsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(TrainingStatsViewModel::class.java)
        userDataSharedViewModel = ViewModelProvider(requireActivity()).get(UserDataSharedViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.title = userDataSharedViewModel.loggedInUser.value

        //Observe the heartRate LiveData and update the UI
        viewModel.heartRate.observe(viewLifecycleOwner, Observer { newHR ->
            binding.HRText.text = "$newHR \n BPM"
            val bpmPercent = ((newHR - viewModel.MIN_HR).toFloat()) / (viewModel.MAX_HR - viewModel.MIN_HR) * 100
            binding.HRProgressBar.progress = if (bpmPercent < 100) bpmPercent.toInt() else 100
        })

        //Observe the HR array LiveData and update the UI
        heartRatePlot = binding.HRPlot
        configurePlot((activity as MainActivity))
        viewModel.xyList.observe(viewLifecycleOwner, Observer { newXY ->
            plotHR(newXY)
        })

        //Load database context to viewModel
        viewModel.key = userDataSharedViewModel.loggedInUser.value.toString()

        //Set onRefreshListener for the swipeRefreshSkills
        binding.swipeRefreshTrainingStats.setOnRefreshListener {
            viewModel.getTrainings(context)
            binding.swipeRefreshTrainingStats.isRefreshing = false
        }

        //Set the LayoutManager that this RecyclerView will use.
        binding.trainingRecyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)

        //Initialize the adapter with empty list
        adapter = TrainingStatsItemAdapter(requireContext(), arrayListOf(), arrayListOf(), this)
        binding.trainingRecyclerView.adapter = adapter

        //get trainings from firebase
        viewModel.getTrainings(context)
        viewModel.updateTrainingsAdapter.observe(viewLifecycleOwner, Observer {updateTrainingsAdapter ->
            if (updateTrainingsAdapter == true) {
                adapter.itemsDate = ArrayList(viewModel.trainings.map { it.date })
                adapter.itemsDuration = ArrayList(viewModel.trainings.map { it.duration })
                adapter.notifyDataSetChanged()
                viewModel.resetUpdate()
            }
        })

        //observe deletionCompleted (if training is deleted) and update the adapter
        viewModel.deletionCompleted.observe(viewLifecycleOwner, Observer {deletionCompleted ->
            if(deletionCompleted == true){
                adapter.itemsDate = ArrayList(viewModel.trainings.map { it.date })
                adapter.itemsDuration = ArrayList(viewModel.trainings.map { it.duration })
                adapter.notifyDataSetChanged()
                viewModel.resetDelete()
            }
        })

        //Set onClickListeners for the deleteTrainingButton and cancelDeleteTrainingButton
        binding.confirmDeleteTrainingButton.setOnClickListener {
            //delete the Training from the database
            viewModel.deleteTraining(context)
            binding.deleteTrainingWindow.visibility = View.GONE
        }
        binding.cancelDeleteTrainingButton.setOnClickListener {
            // Hide the deleteTrainingWindow
            binding.deleteTrainingWindow.visibility = View.GONE
            viewModel.trainingToDelete = null
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TrainingStatsViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        viewModel.key = userDataSharedViewModel.loggedInUser.value
        viewModel.getTrainings(context)
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)
        sendCommandToWear("Start")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
        sendCommandToWear("Stop")
    }

    private fun configurePlot(context: Context) {
        heartRatePlot.description.isEnabled = false
        heartRatePlot.setTouchEnabled(true)
        heartRatePlot.isDragXEnabled = true
        heartRatePlot.isDragEnabled = true
        heartRatePlot.setScaleEnabled(true)
        heartRatePlot.setDrawGridBackground(false)
        heartRatePlot.setPinchZoom(true)
        heartRatePlot.setBackgroundColor(ContextCompat.getColor(context, R.color.design_default_color_background))
        heartRatePlot.axisRight.isEnabled = false

        heartRatePlot.xAxis.position = XAxis.XAxisPosition.BOTTOM
        val xAxisLabels = listOf(" ", " ", " ", " ", " ", " ", " ", " ")
        heartRatePlot.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)

        // Position the legend at the top right
        val legend = heartRatePlot.legend
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.orientation = Legend.LegendOrientation.VERTICAL

        //set min and max values of x-axis
        heartRatePlot.axisLeft.axisMinimum = viewModel.MIN_HR.toFloat()
        heartRatePlot.axisLeft.axisMaximum = viewModel.MAX_HR.toFloat()
        heartRatePlot.axisRight.axisMinimum = viewModel.MIN_HR.toFloat()
        heartRatePlot.axisRight.axisMaximum = viewModel.MAX_HR.toFloat()

        //set maximum number of visible entries
        heartRatePlot.setVisibleXRangeMaximum(viewModel.NUMBER_OF_POINTS.toFloat())
    }

    private fun plotHR(newXY: ArrayList<Number>) {
        val entries = ArrayList<Entry>()
        for (i in newXY.indices step 2) {
            entries.add(Entry(newXY[i].toFloat(), newXY[i + 1].toFloat()))
        }
        val dataSet = LineDataSet(entries, "Heart rate")
        dataSet.lineWidth = 1.75f
        dataSet.circleRadius = 2.5f
        dataSet.circleHoleRadius = 2.5f
        dataSet.setCircleColor(Color.WHITE)
        dataSet.highLightColor = Color.WHITE
        dataSet.color = Color.RED
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        // Make the line smooth
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        heartRatePlot.data = lineData
        heartRatePlot.invalidate() // refresh
    }

    //Send command to wear to start/stop the heart rate sensor
    private fun sendCommandToWear(command: String){
        Thread(Runnable {
            val connectedNodes: List<String> = Tasks
                .await(
                    Wearable
                        .getNodeClient(activity as MainActivity).connectedNodes)
                .map { it.id }
            connectedNodes.forEach {
                val messageClient: MessageClient = Wearable
                    .getMessageClient(activity as AppCompatActivity)
                messageClient.sendMessage(it, "/command", command.toByteArray())
            }
        }).start()
    }

    override fun onItemClick(position: Int) {
    }

    override fun onDeleteClick(position: Int) {
        val deleteTrainingWindow = view?.findViewById<View>(R.id.deleteTrainingWindow)
        deleteTrainingWindow?.visibility = View.VISIBLE
        viewModel.trainingToDelete = position
    }


}