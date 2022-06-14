package com.shark.svgaplayer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shark.svgaplayer.databinding.FragmentListBinding
import com.shark.svgaplayer.databinding.ItemSvgaBinding
import com.shark.svgaplayer_base.util.loadAsset
import kotlin.random.Random

class SVGAListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = mutableListOf<SVGAModel>()
        for (index in 0..100) {
            list.add(
                SVGAModel(
                    when (index % 3) {
                        1 -> "angel.svga"
                        2 -> "jojo_audio.svga"
                        else -> "angel.svga"
                    }, index
                )
            )
        }

        binding.recyclerView.adapter = Adapter().apply {
            submitList(list)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    internal class Adapter : ListAdapter<SVGAModel, ViewHolder>(object :
        DiffUtil.ItemCallback<SVGAModel>() {
        override fun areItemsTheSame(oldItem: SVGAModel, newItem: SVGAModel): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: SVGAModel, newItem: SVGAModel): Boolean =
            oldItem.url == newItem.url
                    && oldItem.position == newItem.position

    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ItemSvgaBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindData(getItem(position))
        }

    }

    internal class ViewHolder(val itemBinding: ItemSvgaBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bindData(model: SVGAModel) {
            itemBinding.svgaImgAudio.loadAsset(model.url)
            itemBinding.root.setBackgroundColor(randomColor())
        }

        @ColorInt
        fun randomColor(): Int {
            return Color.argb(128, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        }
    }

    internal data class SVGAModel(val url: String, val position: Int) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SVGAModel

            if (url != other.url) return false
            if (position != other.position) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + position
            return result
        }
    }

}