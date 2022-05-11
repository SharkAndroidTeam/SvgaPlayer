package com.shark.svgaplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.shark.svgaplayer.databinding.FragmentSecondBinding
import com.shark.svgaplayer_base.util.load

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btn.setOnClickListener {
            binding.svgaImg.load("https://media.kaiyinhn.cn/MTY1MjA4Njg1Nzk4OCM2Mjkjc3ZnYQ==.svga")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}