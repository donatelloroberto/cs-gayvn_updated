package com.BoyfriendTV

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper
import org.json.JSONObject
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element

private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

class BoyfriendTV : MainAPI() {
    override var mainUrl = "https://www.boyfriendtv.com"
    override var name = "Boyfriendtv"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
            ""                                           to "Trending",
            "/?filter_quality=hd&s=&sort=newest"         to "Mới nhất",
            "/?filter_quality=hd&s=&sort=most-popular"   to "Phổ biến",
            "/search/?q=Vietnamese"                      to "Việt Nam",
            "/search/?q=asian&hot="                      to "Asian",
            "/?filter_quality=hd&s=&sort=most-popular"   to "Phổ biến",
            "/search/?q=chinese&hot=&quality=hd"         to "Tung Của",
            "/tags/brazilian/?filter_quality=hd"         to "Bờ ra sin",
            "/tags/gangbang/?filter_quality=hd"          to "Chịch tập thể",
            "/tags/latinos/?filter_quality=hd"           to "Mỹ da màu",
            "/search/?q=facedownassup&quality=hd"        to "Face down Ass up",
            "/search/?q=sketchysex&quality=hd"           to "Sketchy Sex",
            "/search/?q=fraternity&quality=hd"           to "Fraternity X",
            "/search/?q=slamrush"                        to "Slam Rush",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data}/?page=$page"
        val document = app.get(pageUrl).document

        val items = document.select("ul.media-listing-grid.main-listing-grid-offset li").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = items.isNotEmpty()
        )
    }
    
    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("p.titlevideospot a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            // BoyfriendTV only hosts adult videos; mark search results accordingly
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.selectFirst(".media-item__title")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            // Recommendations should also be marked NSFW
            this.posterUrl = posterUrl
        }
    }

     override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/?q=$query"
        val document = app.get(url).document
        return document.select("ul.media-listing-grid.main-listing-grid-offset li").mapNotNull { it.toSearchResult() }
    }


    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    // Parse từ application/ld+json
    val ldJson = JSONObject(document.selectFirst("script[type=application/ld+json]")?.data() ?: return null)

    val title = ldJson.getString("name")
    val description = ldJson.optString("description", "")
    val poster = fixUrlNull(ldJson.getJSONArray("thumbnailUrl").optString(0))

    val tags = description
        .split(",")
        .map { it.trim().replace("-", "") }
        .filter { it.isNotBlank() && !StringUtil.isNumeric(it) }

    val recommendations = document.select(".media-listing-grid .media-item").mapNotNull {
            it.toRecommendResult()
    }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
    }
}

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        val html = response.text

        val sourcesRegex = Regex("""var\s+sources\s*=\s*(\[[\s\S]*?]);""")
        val match = sourcesRegex.find(html) ?: return false
        val sourcesJsonText = match.groupValues[1].replace("\\/", "/").trim()

        val sourcesArray = JSONArray(sourcesJsonText)
        val extlinkList = mutableListOf<ExtractorLink>()

        for (i in 0 until sourcesArray.length()) {
        val source = sourcesArray.getJSONObject(i)
        val rawUrl = source.optString("src") ?: continue
        val videoUrl = rawUrl.replace("\\/", "/")
        val qualityLabel = source.optString("desc", "Unknown")
        val isHls = source.optBoolean("hls", false)

            extlinkList.add(
    newExtractorLink(
        source = name,
        name = "BoyfriendTV [$qualityLabel]",
        url = videoUrl,
        type = ExtractorLinkType.VIDEO
    ) {
        this.referer = "https://www.boyfriendtv.com/"
        this.headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Origin" to "https://www.boyfriendtv.com",
            "Referer" to "https://www.boyfriendtv.com/"
        )
        // KHÔNG gán lại isM3u8 ở đây!
    }
)

    }
    extlinkList.forEach(callback)
    return extlinkList.isNotEmpty()
}
}
