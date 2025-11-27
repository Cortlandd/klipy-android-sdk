package com.klipy.klipy_ui.tray

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cortlandwalker.klipy_ui.databinding.ItemKlipyMediaBinding
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

/**
 * Adapter for XML-based Klipy tray grid.
 *
 * - Uses Glide.asBitmap() to avoid GifDrawable/WebP issues.
 * - For CLIP mediaType, shows lowQuality (gif/webp/png) as static thumb with play icon.
 */
class KlipyTrayAdapter(
    private val onClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, KlipyTrayAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemKlipyMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
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
            val context = binding.imageMedia.context

            val meta = when (item.mediaType) {
                MediaType.CLIP -> item.lowQualityMetaData ?: item.highQualityMetaData
                else           -> item.highQualityMetaData ?: item.lowQualityMetaData
            }
            val url = meta?.url

            binding.skeletonView.visibility = View.VISIBLE
            binding.itemProgress.visibility = View.VISIBLE
            binding.imageMedia.visibility = View.INVISIBLE
            binding.imageMedia.setImageDrawable(null)
            binding.playIcon.visibility =
                if (item.mediaType == MediaType.CLIP) View.VISIBLE else View.GONE

            if (!url.isNullOrBlank()) {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.itemProgress.visibility = View.GONE
                            binding.imageMedia.visibility = View.INVISIBLE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.skeletonView.visibility = View.GONE
                            binding.itemProgress.visibility = View.GONE
                            binding.imageMedia.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(binding.imageMedia)
            } else {
                binding.itemProgress.visibility = View.GONE
                binding.imageMedia.visibility = View.INVISIBLE
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
