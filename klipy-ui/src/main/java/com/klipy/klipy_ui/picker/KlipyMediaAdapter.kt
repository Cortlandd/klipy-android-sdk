package com.klipy.klipy_ui.picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.klipy_ui.databinding.ItemKlipyMediaBinding
import com.klipy.sdk.model.MediaItem

class KlipyMediaAdapter(
    loadingIndicatorColor: Int,
    private val onClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, KlipyMediaAdapter.VH>(Diff) {

    private val binder = KlipyMediaCellBinder(
        loadingIndicatorColor = loadingIndicatorColor,
        onClick = onClick
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemKlipyMediaBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: VH) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class VH(
        private val binding: ItemKlipyMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem) {
            this@KlipyMediaAdapter.binder.bind(binding, item)
        }

        fun recycle() {
            this@KlipyMediaAdapter.binder.recycle(binding)
        }
    }

    private object Diff : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem
    }
}
