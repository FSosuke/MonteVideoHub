package com.fsosuke.montevideohub

import android.app.Application
import com.fsosuke.montevideohub.data.History
import com.fsosuke.montevideohub.data.Http
import com.fsosuke.montevideohub.data.NicoAccount
import com.fsosuke.montevideohub.data.Settings
import com.fsosuke.montevideohub.data.SubscriptionStore
import com.fsosuke.montevideohub.download.DownloadManager
import com.fsosuke.montevideohub.download.DownloadStore
import com.fsosuke.montevideohub.data.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class MonteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        NicoAccount.init(this)
        History.init(this)
        SubscriptionStore.init(this)
        DownloadStore.init(this)
        DownloadManager.init(this)
        val region = Settings.value.region
        NewPipe.init(
            NewPipeDownloader(Http.client),
            Localization(region.language, region.country),
            ContentCountry(region.country),
        )
    }
}
