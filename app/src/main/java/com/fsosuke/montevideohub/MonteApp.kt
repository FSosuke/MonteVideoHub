package com.fsosuke.montevideohub

import android.app.Application
import com.fsosuke.montevideohub.data.Http
import com.fsosuke.montevideohub.data.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class MonteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(
            NewPipeDownloader(Http.client),
            Localization("de", "DE"),
            ContentCountry("DE"),
        )
    }
}
