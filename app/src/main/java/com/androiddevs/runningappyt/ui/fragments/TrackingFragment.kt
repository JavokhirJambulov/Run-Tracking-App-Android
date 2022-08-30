package com.androiddevs.runningappyt.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentTrackingBinding
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_STOP_SERVICE
import com.androiddevs.runningappyt.other.Constants.MAP_ZOOM
import com.androiddevs.runningappyt.other.Constants.POLYLINE_COLOR
import com.androiddevs.runningappyt.other.Constants.POLYLINE_WIDTH
import com.androiddevs.runningappyt.other.TrackingUtility
import com.androiddevs.runningappyt.services.Polyline
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.math.round

const val CANCEL_TRACKING_TAG = "CancelDialog"
@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    private var curTimeInMillis = 0L

    //private var menu: Menu? = null
    private lateinit var fragmentTrackingBinding: FragmentTrackingBinding
    @set:Inject
    var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentTrackingBinding = DataBindingUtil.inflate(inflater,R.layout.fragment_tracking,container,false)
        return fragmentTrackingBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        fragmentTrackingBinding.mapView.onCreate(savedInstanceState)
        fragmentTrackingBinding.btnToggleRun.setOnClickListener {
            toggleRun()
        }
        if(savedInstanceState!=null){
            val cancelTrackingFragment = parentFragmentManager.findFragmentByTag(CANCEL_TRACKING_TAG) as CancelTrackingFragment?
            cancelTrackingFragment?.setListener { stopRun() }
        }

        fragmentTrackingBinding.btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        fragmentTrackingBinding.mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        TrackingService.pathPoints.observe(viewLifecycleOwner) {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        }

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner) {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            fragmentTrackingBinding.tvTimer.text = formattedTime
        }
    }

    private fun toggleRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }


    private fun showCancelTrackingDialog() {
        CancelTrackingFragment().apply {
            setListener {
                stopRun()
            }
        }.show(parentFragmentManager,CANCEL_TRACKING_TAG)
    }

    private fun stopRun() {
        fragmentTrackingBinding.tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking && curTimeInMillis>0) {
            fragmentTrackingBinding.btnToggleRun.text = "Start"
            fragmentTrackingBinding.btnFinishRun.visibility = View.VISIBLE
        } else if(isTracking){
            val menuHost: MenuHost = requireActivity()

            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // Add menu items here
                    menuInflater.inflate(R.menu.toolbar_tracking_menu, menu)

                }

                override fun onPrepareMenu(menu: Menu) {
                    super.onPrepareMenu(menu)
                    if(isTracking) {
                        menu.getItem(0).isVisible = true
                    }
                    if(curTimeInMillis > 0L) {
                        menu.getItem(0).isVisible = true
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // Handle the menu selection
                    return when (menuItem.itemId) {
                        R.id.miCancelTracking -> {
                            showCancelTrackingDialog()
                            true
                        }
                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
            fragmentTrackingBinding.btnToggleRun.text = "Stop"
            fragmentTrackingBinding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints) {
            for(pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                fragmentTrackingBinding.mapView.width,
                fragmentTrackingBinding.mapView.height,
                (fragmentTrackingBinding.mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for(polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
            val run = Run(bmp, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        fragmentTrackingBinding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        fragmentTrackingBinding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        fragmentTrackingBinding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        fragmentTrackingBinding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        fragmentTrackingBinding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fragmentTrackingBinding.mapView.onSaveInstanceState(outState)
    }
}











