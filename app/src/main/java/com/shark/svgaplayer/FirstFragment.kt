package com.shark.svgaplayer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.shark.svgaplayer.databinding.FragmentFirstBinding
import com.shark.svgaplayer_base.util.load
import com.shark.svgaplayer_base.util.loadAny
import com.shark.svgaplayer_base.util.loadAsset

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.svgaImg.post {

        }

        binding.btn.setOnClickListener {
            binding.svgaImg.load("https://media.kaiyinhn.cn/MTY1MjA4Njg1Nzk4OCM2Mjkjc3ZnYQ==.svga")

        }

        binding.btnJump.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}