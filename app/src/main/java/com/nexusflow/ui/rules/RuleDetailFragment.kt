package com.nexusflow.ui.rules

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexusflow.NexusFlowApp
import com.nexusflow.R
import com.nexusflow.core.model.Rule
import com.nexusflow.databinding.FragmentRuleDetailBinding
import com.nexusflow.ui.create.CreateRuleFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.*

class RuleDetailFragment : Fragment() {

    private var _binding: FragmentRuleDetailBinding? = null
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

    private var ruleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ruleId = arguments?.getString(ARG_RULE_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRuleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rules
                .filter { it.isNotEmpty() }
                .collectLatest { rules ->
                    val rule = rules.find { it.id == ruleId }
                    if (rule != null) {
                        bindRule(rule)
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                }
        }
    }

    private fun bindRule(rule: Rule) {
        binding.detailRuleName.text = rule.name
        binding.detailStatusBadge.text = if (rule.isEnabled) "● ACTIVE" else "● PAUSED"
        binding.detailStatusBadge.setTextColor(if (rule.isEnabled) requireContext().getColor(R.color.accent_green) else requireContext().getColor(R.color.text_secondary_dark))
        
        binding.detailTriggerText.text = rule.triggers.joinToString("\n") { it.describe() }
        
        // Stats
        binding.statFired.statValue.text = rule.triggerCount.toString()
        binding.statFired.statLabel.text = "Fired times"
        
        val lastTriggered = rule.lastTriggeredAt ?: 0L
        binding.statLastRun.statValue.text = if (lastTriggered > 0) {
            val diff = (System.currentTimeMillis() - lastTriggered) / 60000
            if (diff < 1) "Just now" else if (diff < 60) "${diff}m ago" else "${diff / 60}h ago"
        } else "Never"
        binding.statLastRun.statLabel.text = "Last run"
        
        binding.statPriority.statValue.text = when(rule.priority) {
            in 10..Int.MAX_VALUE -> "High"
            in 1..9 -> "Medium"
            else -> "Low"
        }
        binding.statPriority.statLabel.text = "Priority"

        // Clear and rebuild the content list
        binding.detailActionsList.removeAllViews()
        
        val density = resources.displayMetrics.density
        
        if (rule.conditions.isNotEmpty()) {
            val header = TextView(requireContext()).apply {
                text = "CONDITIONS (ONLY IF)"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(requireContext().getColor(R.color.accent_green))
                setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
            }
            binding.detailActionsList.addView(header)
            
            rule.conditions.forEach { condition ->
                val textView = TextView(requireContext()).apply {
                    text = "• ${condition.describe()}"
                    setTextColor(requireContext().getColor(R.color.text_primary_dark))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding((16 * density).toInt(), (4 * density).toInt(), (16 * density).toInt(), (4 * density).toInt())
                }
                binding.detailActionsList.addView(textView)
            }
            
            val taskHeader = TextView(requireContext()).apply {
                text = "TASKS (THEN)"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(requireContext().getColor(R.color.accent_purple))
                setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
            }
            binding.detailActionsList.addView(taskHeader)
        }

        rule.actions.forEachIndexed { index, action ->
            val textView = TextView(requireContext()).apply {
                text = "${index + 1}. ${action.describe()}"
                setTextColor(requireContext().getColor(R.color.fire_dark_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding((8 * density).toInt())
            }
            binding.detailActionsList.addView(textView)
        }

        binding.btnEditRule.setOnClickListener {
            val fragment = CreateRuleFragment().apply {
                arguments = Bundle().apply {
                    putString("rule_id", rule.id)
                }
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up, R.anim.slide_in_up, R.anim.slide_out_up)
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnDeleteRule.setOnClickListener {
            viewModel.deleteRule(rule.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RULE_ID = "rule_id"
        fun newInstance(ruleId: String) = RuleDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_RULE_ID, ruleId)
            }
        }
    }
}
