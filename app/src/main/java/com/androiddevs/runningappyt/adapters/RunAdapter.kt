package com.androiddevs.runningappyt.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.ActivityMainBinding
import com.androiddevs.runningappyt.databinding.ItemRunBinding
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.other.TrackingUtility
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {
    private lateinit var itemRunBinding: ItemRunBinding
    inner class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    val diffCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {

        itemRunBinding= DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.item_run,parent,false);
        return  RunViewHolder(itemRunBinding.root);
    }

    override fun getItemCount(): Int {
       return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]
        holder.itemView.apply {
            Glide.with(this).load(run.img).into(itemRunBinding.ivRunImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timestamp
            }
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            itemRunBinding.tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${run.avgSpeedInKMH}km/h"
            itemRunBinding.tvAvgSpeed.text = avgSpeed

            val distanceInKm = "${run.distanceInMeters / 1000f}km"
            itemRunBinding.tvDistance.text = distanceInKm

            itemRunBinding.tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

            val caloriesBurned = "${run.caloriesBurned}kcal"
            itemRunBinding.tvCalories.text = caloriesBurned
        }
    }
}














