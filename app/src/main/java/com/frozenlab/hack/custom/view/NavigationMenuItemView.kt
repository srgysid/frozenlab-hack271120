package com.frozenlab.hack.custom.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.frozenlab.hack.R
import com.frozenlab.hack.databinding.ViewNavigationMenuItemBinding


class NavigationMenuItemView(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {

    private var _binding: ViewNavigationMenuItemBinding? = null
    private val binding: ViewNavigationMenuItemBinding get() = _binding!!

    var title: String? = null
        set(value) {
            field = value
            binding.textTitle.text = value
        }

    var hint:     String? = null
        set(value) {
            field = value
            binding.textHint.isVisible = (value != null)
            binding.textHint.text = value
        }

    var icon: Drawable? = null
        set(value) {
            field = value
            binding.image.setImageDrawable(value)
        }

    var iconSize: Int = 50
        set(value) {
            field = value
            binding.image.updateLayoutParams {
                this.height = iconSize
                this.width = iconSize
            }
        }

    init {

        _binding = ViewNavigationMenuItemBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.NavigationMenuItemView, 0, 0)

        try {

            title     = typedArray.getString(R.styleable.NavigationMenuItemView_title)
            hint      = typedArray.getString(R.styleable.NavigationMenuItemView_hint)
            icon      = typedArray.getDrawable(R.styleable.NavigationMenuItemView_icon)
            iconSize  = typedArray.getDimensionPixelSize(R.styleable.NavigationMenuItemView_iconSize, 50)

        } finally {
            typedArray.recycle()
        }
    }
}