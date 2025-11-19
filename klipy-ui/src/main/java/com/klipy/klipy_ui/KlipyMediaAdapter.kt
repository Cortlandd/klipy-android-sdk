package com.klipy.klipy_ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.klipy_ui.databinding.ItemKlipyMediaBinding
import com.klipy.sdk.model.MediaItem

class KlipyMediaAdapter(
    private val onClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, KlipyMediaAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemKlipyMediaBinding.inflate(inflater, parent, false)
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemKlipyMediaBinding,
        private val onClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            // TODO: use Coil/Glide; for now just placeholder bitmap if present
            val view: ImageView = binding.imageMedia

            val url = item.lowQualityMetaData?.url ?: item.highQualityMetaData?.url
            // Example with Coil (if you add it as a dependency):
            // view.load(url)

            if (item.placeHolder != null) {
                view.setImageBitmap(item.placeHolder)
            } else {
                // you can set a placeholder drawable here
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem
    }
}