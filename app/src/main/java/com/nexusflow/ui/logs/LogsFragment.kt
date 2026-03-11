package com.nexusflow.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexusflow.NexusFlowApp
import com.nexusflow.databinding.FragmentLogsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = requireActivity().application as NexusFlowApp
                return LogsViewModel(app.logRepository) as T
            }
        }
    }

    private lateinit var adapter: LogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LogsAdapter()
        binding.logsRecycler.adapter = adapter

        binding.clearLogsBtn.setOnClickListener {
            viewModel.clearLogs()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logs.collectLatest { logs ->
                adapter.submitList(logs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}