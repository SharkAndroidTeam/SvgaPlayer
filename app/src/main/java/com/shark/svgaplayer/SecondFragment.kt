package com.shark.svgaplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shark.svgaplayer.databinding.FragmentSecondBinding
import com.shark.svgaplayer_base.util.loadAsset

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

        binding.btnAudio.setOnClickListener {
            if (binding.svgaImgAudio.isAnimating) {
                binding.root.removeView(binding.svgaImgAudio)
                return@setOnClickListener
            }
            binding.svgaImgAudio.loadAsset("mp3_to_long.svga")
        }

        binding.btn.setOnClickListener {
            if (binding.svgaImg.isAnimating) {
                binding.root.removeView(binding.svgaImg)
                return@setOnClickListener
            }
            binding.svgaImg.loadAsset("mp3_to_long.svga")
        }
        binding.btn2.setOnClickListener {
            if (binding.svgaImgAudio2.isAnimating) {
                binding.root.removeView(binding.svgaImgAudio2)
                return@setOnClickListener
            }
            binding.svgaImgAudio2.loadAsset("mp3_to_long.svga")
        }
        binding.btn1.setOnClickListener {
            if (binding.svgaImgAudio1.isAnimating) {
                binding.root.removeView(binding.svgaImgAudio1)
                return@setOnClickListener
            }
            binding.svgaImgAudio1.loadAsset("mp3_to_long.svga")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}