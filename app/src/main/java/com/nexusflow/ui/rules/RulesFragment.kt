package com.nexusflow.ui.rules

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
import com.nexusflow.R
import com.nexusflow.databinding.FragmentRulesBinding
import com.nexusflow.ui.create.CreateRuleFragment
import com.nexusflow.ui.theme.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RulesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = requireActivity().application as NexusFlowApp
                return RulesViewModel(app.ruleRepository, app.engine) as T
            }
        }
    }

    private lateinit var adapter: RulesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateBackgroundShapes()

        adapter = RulesAdapter(
            onToggle = { id, enabled -> viewModel.toggleRule(id, enabled) },
            onClick = { rule -> 
                // Navigate to Rule Detail instead of showing a dialog
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up, R.anim.slide_in_up, R.anim.slide_out_up)
                    .replace(R.id.nav_host_fragment, RuleDetailFragment.newInstance(rule.id))
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rulesRecycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rules.collectLatest { rules ->
                if (rules.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rulesRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rulesRecycler.visibility = View.VISIBLE
                    adapter.submitList(rules)
                }
                
                // Update Stats Header
                binding.statTotalRules.text = rules.size.toString()
                binding.statActiveRules.text = rules.count { it.isEnabled }.toString()
                
                val totalFires = rules.sumOf { it.triggerCount }
                binding.statActivityPercentage.text = if (totalFires > 0) "${totalFires}%" else "0%"
                
                binding.statusText.text = "ENGINE ACTIVE • ${rules.count { it.isEnabled }} RULES RUNNING"
            }
        }
        
        binding.fabAddRule.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up, R.anim.slide_in_up, R.anim.slide_out_up)
                .replace(R.id.nav_host_fragment, CreateRuleFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateBackgroundShapes() {
        val family = ThemeManager.getThemeFamily(requireContext())
        val resId = when (family) {
            ThemeManager.ThemeFamily.FIRE -> R.drawable.bg_fire_pattern
            ThemeManager.ThemeFamily.SOLAR -> R.drawable.bg_solar_pattern
            ThemeManager.ThemeFamily.PASTEL -> R.drawable.bg_pastel_pattern
        }
        binding.bgShapes.setImageResource(resId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}