package com.gta.myapplication.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.gta.myapplication.R
import com.gta.myapplication.databinding.FragmentSearchbarBinding
import com.gta.widget.search.MotionVoiceSearchBar


class SearchBarFragment : Fragment() {

    private lateinit var binding: FragmentSearchbarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchBarPlayground.setStartIconOnClickListener {
            Toast.makeText(requireContext(), "Clicked start icon, current state: ${binding.searchBarPlayground.getState()}", Toast.LENGTH_SHORT).show()
            if (binding.searchBarPlayground.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                binding.searchBarPlayground.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                binding.toggleGroup.check(R.id.btnSecond)
            }
        }

        binding.searchBarPlayground.setEndIconOnClickListener {
            Toast.makeText(requireContext(), "Clicked end icon, current state: ${binding.searchBarPlayground.getState()}", Toast.LENGTH_SHORT).show()
        }

        binding.searchBarPlayground.setStartIconOnLongClickListener {
            Toast.makeText(requireContext(), "Clicked start icon, current state: ${binding.searchBarPlayground.getState()}", Toast.LENGTH_SHORT).show()
            true
        }

        binding.searchBarPlayground.setEndIconOnLongClickListener {
            Toast.makeText(requireContext(), "Long clicked end icon, current state: ${binding.searchBarPlayground.getState()}", Toast.LENGTH_SHORT).show()
            true
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked){
                return@addOnButtonCheckedListener
            }
            when (checkedId) {
                R.id.btnFirst -> {
                    binding.searchBarPlayground.setState(MotionVoiceSearchBar.MotionState.IDLE)
                }

                R.id.btnSecond -> {
                    binding.searchBarPlayground.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                }

                R.id.btnThird -> {
                    binding.searchBarPlayground.setState(MotionVoiceSearchBar.MotionState.RECORDING)
                }

                R.id.btnForth -> {
                    binding.searchBarPlayground.setState(MotionVoiceSearchBar.MotionState.PROCESSING)
                }
            }
        }
    }
}