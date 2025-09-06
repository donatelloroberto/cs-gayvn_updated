package com.BoyfriendTV

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class BoyfriendTVProvider : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(BoyfriendTV())
    }
}
