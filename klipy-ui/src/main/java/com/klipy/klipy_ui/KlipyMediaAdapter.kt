package com.klipy.klipy_ui

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
            val context = binding.imageMedia.context

            // For clips: show GIF (selector) in the grid.
            // For others: keep using highQuality (gif/webp) first.
            val meta = when (item.mediaType) {
                MediaType.CLIP -> item.lowQualityMetaData ?: item.highQualityMetaData
                else           -> item.highQualityMetaData ?: item.lowQualityMetaData
            }
            val url = meta?.url

            Log.d(
                "KlipyMediaAdapter",
                "bind id=${item.id}, type=${item.mediaType}, url=$url"
            )

            // Reset UI
            binding.skeletonView.visibility = View.VISIBLE
            binding.itemProgress.visibility = View.VISIBLE
            binding.imageMedia.visibility = View.INVISIBLE
            binding.imageMedia.setImageDrawable(null)
            binding.playIcon.visibility =
                if (item.mediaType == MediaType.CLIP) View.VISIBLE else View.GONE

            if (!url.isNullOrBlank()) {
                Glide.with(context)
                    .load(url) // GIF, PNG, WebP, mp4 all handled
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e(
                                "KlipyMediaAdapter",
                                "Glide load failed for id=${item.id}, url=$url",
                                e
                            )
                            binding.itemProgress.visibility = View.GONE
                            // keep skeleton + playIcon as fallback
                            binding.imageMedia.visibility = View.INVISIBLE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.skeletonView.visibility = View.GONE
                            binding.itemProgress.visibility = View.GONE
                            binding.imageMedia.visibility = View.VISIBLE
                            // playIcon stays visible if this is a CLIP
                            return false
                        }
                    })
                    .into(binding.imageMedia)
            } else if (item.placeHolder != null) {
                binding.skeletonView.visibility = View.GONE
                binding.itemProgress.visibility = View.GONE
                binding.imageMedia.visibility = View.VISIBLE
                binding.imageMedia.setImageBitmap(item.placeHolder)
            } else {
                binding.itemProgress.visibility = View.GONE
                binding.imageMedia.visibility = View.INVISIBLE
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
