package com.fsosuke.montevideohub

import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.SubscriptionImport
import com.fsosuke.montevideohub.data.YouTubeSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** Läuft bei jedem Build (ohne Netzwerk). */
class SubscriptionImportTest {

    @Test
    fun takeoutCsvGerman() {
        val csv = "﻿Kanal-ID,Kanal-URL,Kanaltitel\n" +
            "UCvpredjG93ifbCP1Y77JyFA,http://www.youtube.com/channel/UCvpredjG93ifbCP1Y77JyFA,YOASOBI\n" +
            "UC1234567890123456789012,http://www.youtube.com/channel/UC1234567890123456789012,\"Band, mit Komma\"\n" +
            "\n"
        val subs = SubscriptionImport.parse(csv)
        assertEquals(2, subs.size)
        assertEquals("UCvpredjG93ifbCP1Y77JyFA", subs[0].id)
        assertEquals("YOASOBI", subs[0].name)
        assertEquals("Band, mit Komma", subs[1].name)
        assertEquals(Source.YOUTUBE, subs[1].source)
    }

    @Test
    fun newPipeJson() {
        val json = """{"app_version":"0.27","subscriptions":[
            {"service_id":0,"url":"https://www.youtube.com/channel/UCvpredjG93ifbCP1Y77JyFA","name":"YOASOBI"},
            {"service_id":1,"url":"https://soundcloud.com/x","name":"SoundCloud"}]}"""
        val subs = SubscriptionImport.parse(json)
        assertEquals(1, subs.size)
        assertEquals("YOASOBI", subs[0].name)
    }

    @Test
    fun rssParsing() {
        val xml = """<?xml version="1.0"?><feed><author><name>YOASOBI &amp; Co</name></author>
            <entry><yt:videoId>ZRtdQ81jPUQ</yt:videoId><title>アイドル &quot;Idol&quot;</title>
            <published>2023-04-12T15:00:00+00:00</published>
            <media:group><media:title>x</media:title><media:thumbnail url="https://i.ytimg.com/vi/ZRtdQ81jPUQ/hqdefault.jpg" width="480" height="360"/>
            <media:community><media:statistics views="123456"/></media:community></media:group></entry></feed>"""
        val tracks = YouTubeSource.parseRss(xml, "UCvpredjG93ifbCP1Y77JyFA")
        assertEquals(1, tracks.size)
        assertEquals("ZRtdQ81jPUQ", tracks[0].id)
        assertEquals("アイドル \"Idol\"", tracks[0].title)
        assertEquals("YOASOBI & Co", tracks[0].artist)
        assertEquals(123456L, tracks[0].views)
        assertEquals(1681311600000L, tracks[0].uploadedAt)
    }
}
