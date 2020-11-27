package com.frozenlab.hack.api

import com.google.gson.Gson

interface HackApiContext {
    val hackApi: HackApi
    val hackGson: Gson
}