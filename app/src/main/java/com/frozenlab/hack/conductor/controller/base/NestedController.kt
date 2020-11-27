package com.frozenlab.hack.conductor.controller.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.frozenlab.hack.conductor.controller.base.BaseController

abstract class NestedController: BaseController {

    constructor(): super()
    constructor(args: Bundle): super(args)

    val topRouter: Router by lazy {

        var p = (this as Controller)

        while(p.parentController != null) {
            p = p.parentController!!
        }

        p.router
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        topRouter
        return super.onCreateView(inflater, container, savedViewState)
    }

}