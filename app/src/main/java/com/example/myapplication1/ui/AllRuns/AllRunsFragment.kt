package com.example.myapplication1.ui.AllRuns

import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Adapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.R
import com.example.myapplication1.data.RunRepository
import com.example.myapplication1.databinding.FragmentAllrunsBinding
import com.example.myapplication1.di.RunRepositoryFactory



class AllRunsFragment : Fragment(){

    private lateinit var _binding: FragmentAllrunsBinding
    private lateinit var adapter: RunAdapter

    private lateinit var runRepository: RunRepository

    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAllrunsBinding.inflate(layoutInflater)


        setupRecyclerView()


        val root: View = binding.root


        return root
    }

    private fun setupRecyclerView() {
        _binding.recyclerView.layoutManager =  LinearLayoutManager(
            context,
                LinearLayoutManager.VERTICAL,
            false
            )
        runRepository = RunRepositoryFactory.runRepository
        adapter = RunAdapter()
        _binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        updateData()
    }

    private fun updateData() {
        adapter.setRuns(runRepository.getAllRuns())
    }

    companion object{
        val Tag  ="RunsList"

        fun create(): Fragment{
            return AllRunsFragment()
        }
    }


}