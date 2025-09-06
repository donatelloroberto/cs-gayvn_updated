package com.Gaycock4U

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.*
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response

class Gaycock4U : MainAPI() {
    override var mainUrl = "https://gaycock4u.com"
    override var name = "Gaycock4U"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Latest Updates",
        "$mainUrl/category/amateur/" to "Amateur",
        "$mainUrl/category/bareback/" to "Bareback",
        "$mainUrl/category/bigcock/" to "Big Cock",
        "$mainUrl/category/group/" to "Group",
        "$mainUrl/category/hardcore/" to "Hardcore",
        "$mainUrl/category/latino/" to "Latino",
        "$mainUrl/category/interracial/" to "Interracial",
        "$mainUrl/category/twink/" to "Twink",
        "$mainUrl/studio/asianetwork/" to "Asianetwork",
        "$mainUrl/studio/bromo/" to "Bromo",
        "$mainUrl/studio/latinonetwork/" to "Latino Network",
        "$mainUrl/studio/lucasentertainment/" to "Lucas Entertainment",
        "$mainUrl/studio/onlyfans/" to "Onlyfans",
        "$mainUrl/studio/rawfuckclub/" to "Raw Fuck Club",
        "$mainUrl/studio/ragingstallion/" to "Ragingstallion",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) {
            "${request.data}page/$page"
        } else {
            "${request.data}"
        }

        val ua = mapOf("User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        val document = app.get(url, headers = ua).document
        // Fixed selector - using correct container class
        val home = document.select("div.elementor-widget-container article.elementor-post").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        // Fixed selectors to match actual HTML structure
        val title = this.selectFirst("p.elementor-heading-title a")?.text()?.trim() ?: ""
        val href = this.selectFirst("a")?.attr("href")?.trim() ?: ""
        val posterUrl = this.selectFirst("a img")?.attr("src")?.trim() ?: ""
        
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.selectFirst("p.elementor-heading-title a")?.text()?.trim() ?: ""
        val href = this.selectFirst("a")?.attr("href")?.trim() ?: ""
        val posterUrl = this.selectFirst("a img")?.attr("src")?.trim() ?: ""
        
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/page/$i/?s=$query").document

            val results = document.select("div.elementor-widget-container article.elementor-post").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }
       
        
        override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        val recommendations = document.select("div.elementor-widget-container article.elementor-post").mapNotNull {
            it.toRecommendResult()
    }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val document = app.get(data).document
    var found = false

    fun normalize(u: String): String {
        val url = u.trim()
        return when {
            url.isEmpty() -> ""
            url.startsWith("//") -> "https:$url"
            else -> url
        }
    }

    document.select("iframe[src], iframe[data-src]").forEach { f ->
        val url = f.absUrl("src").ifBlank { f.attr("src") }
            .ifBlank { f.absUrl("data-src") }
            .ifBlank { f.attr("data-src") }
        if (url.isNotBlank()) {
            found = true
            loadExtractor(normalize(url), subtitleCallback, callback)
        }
    }

    return found
}
}
