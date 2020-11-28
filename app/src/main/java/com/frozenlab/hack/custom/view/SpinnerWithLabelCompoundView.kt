package com.frozenlab.hack.custom.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Spinner
import com.frozenlab.extensions.setCustomAppearance
import com.frozenlab.hack.R
import com.frozenlab.hack.databinding.CompoundViewSpinnerWithLabelBinding

class SpinnerWithLabelCompoundView(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {

    private var _binding: CompoundViewSpinnerWithLabelBinding? = null
    private val binding: CompoundViewSpinnerWithLabelBinding get() = _binding!!

    val spinner: Spinner
        get() { return binding.spinner }

    var label: String? = null
        set(value) {
            field = value
            binding.textLabel.text = value
        }

    init {

        _binding = CompoundViewSpinnerWithLabelBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.SpinnerWithLabelCompoundView, 0, 0)

        try {

            label = typedArray.getString(R.styleable.SpinnerWithLabelCompoundView_label)

            val labelAppearance = typedArray.getResourceId(R.styleable.SpinnerWithLabelCompoundView_labelAppearance, -1)

            if(labelAppearance > 0)
                binding.textLabel.setCustomAppearance(labelAppearance)

        } finally {
            typedArray.recycle()
        }
    }

}