package com.klipy.klipy_ui.picker

import android.graphics.Bitmap
import android.content.res.ColorStateList
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
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cortlandwalker.klipy_ui.databinding.ItemKlipyMediaBinding
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

/**
 * Simple [ListAdapter] for displaying [MediaItem]s in a grid.
 *
 * This is used by [KlipyPickerDialogFragment] but can also be reused if you want
 * to build a custom UI around [com.klipy.sdk.KlipyRepository] results.
 *
 * It uses Glide under the hood and:
 * - Shows a fast-loading preview URL sized for the grid.
 * - Shows a "play" overlay for clips.
 * - Handles a simple skeleton/loading state.
 */
class KlipyMediaAdapter(
    private val loadingIndicatorColor: Int,
    private val onClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, KlipyMediaAdapter.VH>(Diff) {

    private companion object {
        private const val GRID_IMAGE_SIZE_PX = 360
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemKlipyMediaBinding.inflate(inflater, parent, false)
        return VH(binding, loadingIndicatorColor, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemKlipyMediaBinding,
        private val loadingIndicatorColor: Int,
        private val onClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            val context = binding.imageMedia.context

            // Grid cells should prefer lighter preview assets so results appear quickly.
            val meta = item.lowQualityMetaData ?: item.highQualityMetaData
            val url = meta?.url

            // Reset UI
            binding.skeletonView.visibility = View.VISIBLE
            binding.itemProgress.visibility = View.VISIBLE
            binding.itemProgress.indeterminateTintList =
                ColorStateList.valueOf(loadingIndicatorColor)
            binding.imageMedia.visibility = View.INVISIBLE
            binding.imageMedia.setImageDrawable(null)
            binding.playIcon.visibility =
                if (item.mediaType == MediaType.CLIP) View.VISIBLE else View.GONE

            if (!url.isNullOrBlank()) {
                Glide.with(context)
                    .asBitmap()
                    .load(url) // GIF, PNG, WebP, mp4 all handled
                    .apply(
                        RequestOptions()
                            .centerCrop()
                            .override(GRID_IMAGE_SIZE_PX, GRID_IMAGE_SIZE_PX)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.itemProgress.visibility = View.GONE
                            // keep skeleton + playIcon as fallback
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
                            // playIcon stays visible if this is a CLIP
                            return false
                        }
                    })
                    .into(binding.imageMedia)
            } else if (item.blurPreview != null) {
                binding.skeletonView.visibility = View.GONE
                binding.itemProgress.visibility = View.GONE
                binding.imageMedia.visibility = View.VISIBLE
                binding.imageMedia.setImageBitmap(item.blurPreview)
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
