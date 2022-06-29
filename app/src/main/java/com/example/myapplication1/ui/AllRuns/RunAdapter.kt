package com.example.myapplication1.ui.AllRuns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.R
import com.example.myapplication1.data.RunRepository
import com.example.myapplication1.databinding.ItemRunBinding
import com.example.myapplication1.di.RunRepositoryFactory
import com.example.myapplication1.models.Run

class RunAdapter : RecyclerView.Adapter<RunViewHolder>() {
    val runs = mutableListOf<Run>()

    fun setRuns(runs: List<Run>) {
        this.runs.clear()
        this.runs.addAll(runs)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_run, parent, false)
        return RunViewHolder(view)
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = runs[position]
        holder.bind(run)
    }

    override fun getItemCount(): Int = runs.count()
}


class RunViewHolder(runView: View) : RecyclerView.ViewHolder(runView) {

    private var runRepository = RunRepositoryFactory.runRepository

    fun bind(run : Run) {
        val binding = ItemRunBinding.bind(itemView)
        binding.itemRunTitle.text = run.title
        binding.itemRunDistance.text = run.distance
        binding.itemRunDuration.text = run.duration
        binding.itemRunPace.text = run.pace
        binding.btnDelete.setOnClickListener{
            runRepository.delete(run)
        }
    }


}