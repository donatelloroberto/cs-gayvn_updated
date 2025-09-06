package com.Gayxx

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.extractors.*
import android.content.Context
import com.Gayxx.Gayxx

@CloudstreamPlugin
class GayxxProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Gayxx())
        registerExtractorAPI(VoeExtractor())
        registerExtractorAPI(vvide0Extractor())
        registerExtractorAPI(HdgayPlayer())
        registerExtractorAPI(dsio())
        registerExtractorAPI(Doply())
        registerExtractorAPI(Voe())
    }
}
