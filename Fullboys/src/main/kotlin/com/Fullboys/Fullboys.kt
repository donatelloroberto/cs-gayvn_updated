package com.Fullboys

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/topic/video/muscle" to "Muscle",
        "/topic/video/korean" to "Korean",
        "/topic/video/japanese" to "Japanese",
        "/topic/video/taiwanese" to "Taiwanese",
        "/topic/video/viet-nam" to "Vietnamese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data}?page=$page"
        val document = app.get(pageUrl).document
        val items = document.select("article.movie-item").mapNotNull { toSearchItem(it) }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = items.isNotEmpty()
        )
    }

    private fun toSearchItem(element: Element): SearchResponse? {
        val aTag = element.selectFirst("a") ?: return null
        val url = fixUrl(aTag.attr("href"))
        val name = aTag.selectFirst("h2.title")?.text() ?: return null

        val image = aTag.selectFirst("img")?.let { img ->
            img.attr("data-cfsrc").takeIf { it.isNotBlank() } ?: img.attr("src")
        } ?: return null

        return MovieSearchResponse(
            name = name,
            url = url,
            apiName = this@Fullboys.name,
            type = TvType.NSFW,
            posterUrl = image
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/home?search=$query"
        val document = app.get(url).document
        return document.select("article.movie-item").mapNotNull { toSearchItem(it) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val name = doc.selectFirst("h1.title-detail")?.text()?.trim() ?: return null
        val iframeSrc = doc.selectFirst("iframe#ifvideo")?.attr("src")

        val videoUrl = iframeSrc?.let { frame ->
            Regex("[?&]video=([^&]+)").find(frame)?.groupValues?.getOrNull(1)
        }?.let { java.net.URLDecoder.decode(it, "UTF-8") }

        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: iframeSrc?.let { frame ->
                Regex("[?&]poster=([^&]+)").find(frame)?.groupValues?.getOrNull(1)
            }?.let { java.net.URLDecoder.decode(it, "UTF-8") }

        val description = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        val tags = doc.select("div.video-tags a").map { it.text().trim() }

        val recommendations = doc.select("article.movie-item").mapNotNull { el ->
            val aTag = el.selectFirst("a") ?: return@mapNotNull null
            val recUrl = fixUrl(aTag.attr("href"))
            val recName = aTag.attr("title") ?: aTag.selectFirst("h2.title")?.text() ?: return@mapNotNull null
            val recPoster = aTag.selectFirst("img")?.attr("src")
            MovieSearchResponse(
                name = recName,
                url = recUrl,
                apiName = this@Fullboys.name,
                type = TvType.NSFW,
                posterUrl = recPoster
            )
        }

        return newMovieLoadResponse(name, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
            if (!videoUrl.isNullOrEmpty()) {
                this.trailerUrl = videoUrl
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        val serverLinks = doc.select(".box-server button[onclick]").mapNotNull { btn ->
            val onclick = btn.attr("onclick")
            val regex = Regex("['\"](https?://[^'\"]+\\.mp4)['\"]")
            val match = regex.find(onclick)
            val url = match?.groupValues?.getOrNull(1)
            val label = btn.text().trim()
            if (url != null) Pair(url, label) else null
        }

        if (serverLinks.isNotEmpty()) {
            serverLinks.forEach { (streamUrl, label) ->
                callback(
                    newExtractorLink(
                        source = name,
                        name = label,
                        url = streamUrl,
                        referer = data,
                        quality = Qualities.Unknown.value,
                        isM3u8 = false
                    )
                )
            }
            return true
        }

        // Fallback: use iframe video URL if no server buttons are found
        val iframeSrc = doc.selectFirst("iframe#ifvideo")?.attr("src")
        val videoUrl = iframeSrc?.let { frame ->
            Regex("[?&]video=([^&]+)").find(frame)?.groupValues?.getOrNull(1)
        }?.let { java.net.URLDecoder.decode(it, "UTF-8") }

        if (!videoUrl.isNullOrEmpty()) {
            callback(
                newExtractorLink(
                    source = name,
                    name = "Default",
                    url = videoUrl,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    isM3u8 = false
                )
            )
        }
        return true
    }
}
