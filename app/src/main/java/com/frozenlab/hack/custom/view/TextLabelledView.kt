package com.frozenlab.hack.custom.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Spinner
import com.frozenlab.extensions.setCustomAppearance
import com.frozenlab.hack.R
import com.frozenlab.hack.databinding.CompoundViewSpinnerWithLabelBinding
import com.frozenlab.hack.databinding.ViewTextLabelledBinding

class TextLabelledView(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {

    private var _binding: ViewTextLabelledBinding? = null
    private val binding: ViewTextLabelledBinding get() = _binding!!

    var text: String?
        get() { return binding.textValue.text.toString() }
        set(value) {
            binding.textValue.text = value
        }

    var label: String? = null
        set(value) {
            field = value
            binding.textLabel.text = value
        }

    init {

        _binding = ViewTextLabelledBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.TextLabelledView, 0, 0)

        try {

            label = typedArray.getString(R.styleable.TextLabelledView_label)
            text  = typedArray.getString(R.styleable.TextLabelledView_android_text)

            val labelAppearance = typedArray.getResourceId(R.styleable.TextLabelledView_labelAppearance, -1)

            if(labelAppearance > 0)
                binding.textLabel.setCustomAppearance(labelAppearance)

        } finally {
            typedArray.recycle()
        }
    }

}