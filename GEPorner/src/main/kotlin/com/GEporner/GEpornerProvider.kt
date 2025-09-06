package com.GEporner

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GEpornerProvider : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(GEporner())
    }
}
