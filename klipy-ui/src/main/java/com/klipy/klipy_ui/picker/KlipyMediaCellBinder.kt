package com.klipy.klipy_ui.picker

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import co.kikliko.android.ads_sdk.GIFWebView
import co.kikliko.android.ads_sdk.KlipyContent
import com.cortlandwalker.klipy_ui.databinding.ItemKlipyMediaBinding
import com.klipy.klipy_ui.calculateAdCellHeightPx
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import com.klipy.sdk.model.isAD

internal class KlipyMediaCellBinder(
    private val loadingIndicatorColor: Int,
    private val onClick: (MediaItem) -> Unit
) {

    private companion object {
        private const val GRID_IMAGE_SIZE_PX = 360
        private const val DEFAULT_CELL_HEIGHT_DP = 120
        private const val MIN_AD_HEIGHT_DP = 72
        private const val MAX_AD_HEIGHT_DP = 180
    }

    fun bind(
        binding: ItemKlipyMediaBinding,
        item: MediaItem,
        overrideWidthPx: Int? = null,
        overrideHeightPx: Int? = null
    ) {
        val context = binding.imageMedia.context
        val meta = item.lowQualityMetaData ?: item.highQualityMetaData
        val url = meta?.url

        clearAdContainer(binding)
        binding.skeletonView.visibility = View.VISIBLE
        binding.itemProgress.visibility = View.VISIBLE
        binding.itemProgress.indeterminateTintList = ColorStateList.valueOf(loadingIndicatorColor)
        binding.imageMedia.visibility = View.INVISIBLE
        binding.imageMedia.setImageDrawable(null)
        binding.playIcon.visibility =
            if (item.mediaType == MediaType.CLIP) View.VISIBLE else View.GONE
        updateCellHeights(binding, meta, item.isAD(), overrideWidthPx, overrideHeightPx)

        if (item.isAD() && meta != null) {
            binding.skeletonView.visibility = View.GONE
            binding.itemProgress.visibility = View.GONE
            binding.imageMedia.visibility = View.GONE
            binding.playIcon.visibility = View.GONE
            binding.adContainer.visibility = View.VISIBLE
            binding.root.post {
                updateCellHeights(binding, meta, isAd = true, overrideWidthPx, overrideHeightPx)
                clearAdContainer(binding)
                binding.adContainer.visibility = View.VISIBLE

                val renderWidth = (
                    overrideWidthPx?.takeIf { it > 0 }
                        ?: binding.adContainer.width.takeIf { it > 0 }
                        ?: binding.root.width.takeIf { it > 0 }
                        ?: meta.width
                    ).coerceAtLeast(1)
                val renderHeight = (
                    overrideHeightPx?.takeIf { it > 0 }
                        ?: binding.adContainer.layoutParams.height.takeIf { it > 0 }
                        ?: calculateAdHeight(binding, renderWidth, meta)
                    ).coerceAtLeast(1)

                val adView = GIFWebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                binding.adContainer.addView(adView)
                adView.loadContent(
                    KlipyContent(
                        isWebView = false,
                        content = meta.url,
                        width = renderWidth,
                        height = renderHeight
                    )
                )
            }
            binding.root.setOnClickListener(null)
            return
        }

        if (!url.isNullOrBlank()) {
            Glide.with(context)
                .asDrawable()
                .load(url)
                .apply(
                    RequestOptions()
                        .centerCrop()
                        .override(GRID_IMAGE_SIZE_PX, GRID_IMAGE_SIZE_PX)
                )
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.itemProgress.visibility = View.GONE
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
                        return false
                    }
                })
                .into(binding.imageMedia)
        } else if (item.blurPreview != null && !item.isAD()) {
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

    fun recycle(binding: ItemKlipyMediaBinding) {
        Glide.with(binding.imageMedia).clear(binding.imageMedia)
        clearAdContainer(binding)
        binding.root.setOnClickListener(null)
    }

    private fun updateCellHeights(
        binding: ItemKlipyMediaBinding,
        meta: MetaData?,
        isAd: Boolean,
        overrideWidthPx: Int?,
        overrideHeightPx: Int?
    ) {
        val targetHeight = when {
            overrideHeightPx != null && overrideHeightPx > 0 -> overrideHeightPx
            isAd && meta != null -> {
                val containerWidth = overrideWidthPx?.takeIf { it > 0 } ?: (
                    binding.root.width -
                        binding.root.paddingLeft -
                        binding.root.paddingRight
                    ).takeIf { it > 0 } ?: (
                    binding.root.resources.displayMetrics.widthPixels -
                        binding.root.paddingLeft -
                        binding.root.paddingRight
                    )
                calculateAdHeight(binding, containerWidth.coerceAtLeast(1), meta)
            }
            else -> dpToPx(binding, DEFAULT_CELL_HEIGHT_DP)
        }

        binding.skeletonView.layoutParams = binding.skeletonView.layoutParams.apply {
            height = targetHeight
        }
        binding.imageMedia.layoutParams = binding.imageMedia.layoutParams.apply {
            height = targetHeight
        }
        binding.adContainer.layoutParams = binding.adContainer.layoutParams.apply {
            height = targetHeight
        }
    }

    private fun calculateAdHeight(
        binding: ItemKlipyMediaBinding,
        containerWidth: Int,
        meta: MetaData
    ): Int {
        return calculateAdCellHeightPx(
            containerWidthPx = containerWidth,
            metaWidthPx = meta.width,
            metaHeightPx = meta.height,
            density = binding.root.resources.displayMetrics.density,
            minHeightDp = MIN_AD_HEIGHT_DP,
            maxHeightDp = MAX_AD_HEIGHT_DP
        )
    }

    private fun clearAdContainer(binding: ItemKlipyMediaBinding) {
        repeat(binding.adContainer.childCount) {
            val child = binding.adContainer.getChildAt(0)
            binding.adContainer.removeViewAt(0)
            if (child is GIFWebView) {
                child.removeAllViews()
                child.destroy()
            }
        }
        binding.adContainer.visibility = View.GONE
    }

    private fun dpToPx(binding: ItemKlipyMediaBinding, dp: Int): Int =
        (dp * binding.root.resources.displayMetrics.density).toInt()
}
