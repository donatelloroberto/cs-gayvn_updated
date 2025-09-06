package com.Gaycock4U

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.*

@CloudstreamPlugin
class Gaycock4UPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Gaycock4U())
        registerExtractorAPI(DoodLaExtractor())
        registerExtractorAPI(dsio())
        registerExtractorAPI(DoodstreamCom())
        registerExtractorAPI(vide0())
    }
}
