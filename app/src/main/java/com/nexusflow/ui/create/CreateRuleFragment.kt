package com.nexusflow.ui.create

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nexusflow.NexusFlowApp
import com.nexusflow.R
import com.nexusflow.core.model.ActionType
import com.nexusflow.core.model.ConditionType
import com.nexusflow.core.model.TriggerType
import com.nexusflow.databinding.FragmentCreateRuleBinding
import com.nexusflow.databinding.LayoutSelectionSheetBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreateRuleFragment : Fragment() {

    private var _binding: FragmentCreateRuleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateRuleViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = requireActivity().application as NexusFlowApp
                return CreateRuleViewModel(app.ruleRepository) as T
            }
        }
    }

    private lateinit var actionsAdapter: SelectedActionsAdapter
    private lateinit var conditionsAdapter: SelectedConditionsAdapter
    private var ruleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ruleId = arguments?.getString("rule_id")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRuleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        binding.ruleNameInput.doAfterTextChanged { viewModel.setName(it.toString()) }

        actionsAdapter = SelectedActionsAdapter { index -> viewModel.removeAction(index) }
        binding.actionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.actionsRecycler.adapter = actionsAdapter

        conditionsAdapter = SelectedConditionsAdapter { index -> viewModel.removeCondition(index) }
        binding.conditionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.conditionsRecycler.adapter = conditionsAdapter

        binding.triggerCard.setOnClickListener { showTriggerSelectionSheet() }
        binding.addConditionBtn.setOnClickListener { showConditionSelectionSheet() }
        binding.addActionBtn.setOnClickListener { showActionSelectionSheet() }
        binding.saveRuleBtn.setOnClickListener { viewModel.saveRule() }

        if (ruleId != null) {
            viewModel.loadRule(ruleId!!)
            binding.toolbar.title = "Edit Rule"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val trigger = state.selectedTriggers.firstOrNull()
                if (trigger != null) {
                    binding.triggerText.text = "${trigger.first.displayName}\n${trigger.second.values.joinToString()}"
                    binding.triggerIcon.setImageResource(trigger.first.iconRes)
                    binding.triggerCard.setCardBackgroundColor(requireContext().getColor(R.color.accent_blue))
                    binding.triggerText.setTextColor(requireContext().getColor(android.R.color.white))
                    binding.triggerIcon.setColorFilter(requireContext().getColor(android.R.color.white))
                } else {
                    binding.triggerText.text = "Add what will start this flow"
                    binding.triggerIcon.setImageResource(android.R.drawable.ic_menu_add)
                    binding.triggerCard.setCardBackgroundColor(requireContext().getColor(R.color.background_dark))
                    binding.triggerText.setTextColor(requireContext().getColor(R.color.text_primary_dark))
                    binding.triggerIcon.clearColorFilter()
                }

                actionsAdapter.submitList(state.selectedActions)
                conditionsAdapter.submitList(state.selectedConditions)

                if (state.saveSuccess) {
                    parentFragmentManager.popBackStack()
                }
                state.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showTriggerSelectionSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutSelectionSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.sheetTitle.text = "Select Trigger"
        val types = TriggerType.values().toList()
        
        sheetBinding.selectionRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        sheetBinding.selectionRecycler.adapter = SelectionGridAdapter(
            items = types,
            getName = { it.displayName },
            getIcon = { it.iconRes },
            onClick = { type ->
                dialog.dismiss()
                handleTriggerSelection(type)
            }
        )
        dialog.show()
    }

    private fun handleTriggerSelection(type: TriggerType) {
        when (type) {
            TriggerType.BATTERY_LEVEL -> {
                val input = EditText(requireContext())
                input.hint = "Percentage (0-100)"
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Battery Level")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addTrigger(type, mapOf("threshold" to input.text.toString(), "operator" to "below"))
                    }.show()
            }
            TriggerType.TIME_OF_DAY -> {
                val picker = android.widget.TimePicker(requireContext())
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Select Time")
                    .setView(picker)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addTrigger(type, mapOf("hour" to picker.hour.toString(), "minute" to picker.minute.toString()))
                    }.show()
            }
            TriggerType.APP_OPENED -> showAppPickerDialog { packageName ->
                viewModel.addTrigger(type, mapOf("package" to packageName))
            }
            else -> viewModel.addTrigger(type, emptyMap())
        }
    }

    private fun showConditionSelectionSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutSelectionSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.sheetTitle.text = "Select Condition"
        val types = ConditionType.values().toList()
        
        sheetBinding.selectionRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        sheetBinding.selectionRecycler.adapter = SelectionGridAdapter(
            items = types,
            getName = { it.displayName },
            getIcon = { android.R.drawable.ic_menu_info_details },
            onClick = { type ->
                dialog.dismiss()
                handleConditionSelection(type)
            }
        )
        dialog.show()
    }

    private fun handleConditionSelection(type: ConditionType) {
        when (type) {
            ConditionType.BATTERY_BELOW, ConditionType.BATTERY_ABOVE -> {
                val input = EditText(requireContext())
                input.hint = "Percentage"
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle(type.displayName)
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addCondition(type, mapOf("threshold" to input.text.toString()))
                    }.show()
            }
            ConditionType.WIFI_IS -> {
                val input = EditText(requireContext())
                input.hint = "SSID (leave empty for any)"
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Wi-Fi Condition")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addCondition(type, mapOf("ssid" to input.text.toString(), "connected" to "true"))
                    }.show()
            }
            ConditionType.SCREEN_STATE -> {
                val options = arrayOf("Screen ON", "Screen OFF")
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Screen State")
                    .setItems(options) { _, which ->
                        viewModel.addCondition(type, mapOf("state" to if (which == 0) "on" else "off"))
                    }.show()
            }
            else -> viewModel.addCondition(type, emptyMap())
        }
    }

    private fun showActionSelectionSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutSelectionSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.sheetTitle.text = "Add Action"
        val types = ActionType.values().toList()
        
        sheetBinding.selectionRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        sheetBinding.selectionRecycler.adapter = SelectionGridAdapter(
            items = types,
            getName = { it.displayName },
            getIcon = { it.iconRes },
            onClick = { type ->
                dialog.dismiss()
                handleActionSelection(type)
            }
        )
        dialog.show()
    }

    private fun handleActionSelection(type: ActionType) {
        when (type) {
            ActionType.SET_BRIGHTNESS -> {
                val input = EditText(requireContext())
                input.hint = "Level (0-100)"
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Brightness")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addAction(type, mapOf("brightness" to input.text.toString()))
                    }.show()
            }
            ActionType.SHOW_NOTIFICATION -> {
                val layout = android.widget.LinearLayout(requireContext())
                layout.orientation = android.widget.LinearLayout.VERTICAL
                val titleInput = EditText(requireContext()).apply { hint = "Title" }
                val msgInput = EditText(requireContext()).apply { hint = "Message" }
                layout.addView(titleInput)
                layout.addView(msgInput)
                
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Notification")
                    .setView(layout)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addAction(type, mapOf(
                            "title" to titleInput.text.toString(),
                            "message" to msgInput.text.toString()
                        ))
                    }.show()
            }
            ActionType.LAUNCH_APP -> showAppPickerDialog { packageName ->
                viewModel.addAction(type, mapOf("package" to packageName))
            }
            ActionType.OPEN_WEBSITE -> {
                val input = EditText(requireContext())
                input.hint = "URL (e.g. google.com)"
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Open Website")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addAction(type, mapOf("url" to input.text.toString()))
                    }.show()
            }
            ActionType.SET_VOLUME -> {
                val input = EditText(requireContext())
                input.hint = "Volume (0-15)"
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Set Volume")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addAction(type, mapOf("level" to input.text.toString()))
                    }.show()
            }
            ActionType.SET_WIFI, ActionType.SET_BLUETOOTH, ActionType.SET_FLASHLIGHT, ActionType.SET_DND -> {
                showBinaryOptionDialog(type)
            }
            ActionType.SPEAK_TEXT -> {
                val input = EditText(requireContext())
                input.hint = "Text to speak"
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Speak Text")
                    .setView(input)
                    .setPositiveButton("Set") { _, _ ->
                        viewModel.addAction(type, mapOf("text" to input.text.toString()))
                    }.show()
            }
            ActionType.MEDIA_CONTROL -> {
                val options = arrayOf("Play/Pause", "Next", "Previous", "Play", "Pause")
                val values = arrayOf("play_pause", "next", "previous", "play", "pause")
                AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
                    .setTitle("Media Control")
                    .setItems(options) { _, which ->
                        viewModel.addAction(type, mapOf("command" to values[which]))
                    }.show()
            }
            else -> viewModel.addAction(type, emptyMap())
        }
    }

    private fun showBinaryOptionDialog(type: ActionType) {
        val options = arrayOf("Turn ON", "Turn OFF")
        AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
            .setTitle(type.displayName)
            .setItems(options) { _, which ->
                val enabled = which == 0
                viewModel.addAction(type, mapOf("enabled" to enabled.toString()))
            }
            .show()
    }

    private fun showAppPickerDialog(onAppSelected: (String) -> Unit) {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(pm).toString() }
        
        val names = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        
        AlertDialog.Builder(requireContext(), R.style.Theme_NexusFlow_Dialog)
            .setTitle("Select App")
            .setItems(names) { _, which ->
                onAppSelected(apps[which].packageName)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}