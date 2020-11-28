package com.frozenlab.hack.conductor.controller.base

import android.os.Bundle
import androidx.annotation.StringRes
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.frozenlab.hack.R
import com.frozenlab.hack.conductor.controller.base.BaseController

abstract class ToolbarController: BaseController {

    constructor(): super()
    constructor(args: Bundle): super(args)

    abstract val showToolbar: Boolean

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)

        setOptionsMenuHidden(!changeType.isEnter)

        if(changeType.isEnter) {
            setupToolbar()
        }
    }

    fun setTitle(@StringRes resId: Int) {
        setTitle(mainActivity.getString(resId))
    }

    fun setTitle(title: String) {
        val actionBar = mainActivity.supportActionBar ?: return
        actionBar.title = title
    }

    private fun setupToolbar() {

        val actionBar = mainActivity.supportActionBar ?: return

        if(showToolbar) {
            val rId = if(router.backstackSize > 1) {
                mainActivity.blockDrawer(true)
                R.drawable.ic_round_arrow_back_24
            } else {
                mainActivity.blockDrawer(false)
                R.drawable.ic_round_menu_24
            }

            actionBar.setHomeAsUpIndicator(rId)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)

            actionBar.show()

        } else {
            mainActivity.blockDrawer(true)
            actionBar.hide()
        }
    }
}