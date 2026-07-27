package io.nekohasekai.sfa.compose.screen.profileoverride

object RussianAppPreset {
    val yandexPackages = setOf(
        "com.yandex.browser",
        "ru.yandex.yandexnav",
        "ru.yandex.yandexmaps",
        "ru.yandex.searchplugin",
        "ru.yandex.taxi",
        "ru.yandex.music",
        "ru.yandex.market",
        "ru.yandex.disk",
        "ru.yandex.mail",
        "ru.yandex.kinopoisk",
        "ru.yandex.eats",
        "ru.yandex.lavka",
        "ru.yandex.pay"
    )

    val vkPackages = setOf(
        "com.vkontakte.android",
        "com.vk.im",
        "com.vk.music",
        "com.vk.video",
        "com.vk.clips",
        "ru.mail.mailapp",
        "ru.mail.cloud",
        "ru.ok.android"
    )

    val bankingPackages = setOf(
        "ru.sberbankmobile",
        "com.sberbank.sberpay",
        "com.tinkoff.bank",
        "com.tbank",
        "ru.alfabank.o2o",
        "ru.alfabank.mobile.android",
        "ru.vtb24.mobilebanking.android",
        "ru.raiffeisennews",
        "ru.gpb.mobile",
        "ru.ozon.bank",
        "ru.sovcomcard.subscribers",
        "ru.nspk.mirpay",
        "ru.rshb.sub"
    )

    val marketplacePackages = setOf(
        "ru.ozon.app.android",
        "com.wildberries.ru",
        "ru.megamarket.app",
        "ru.avito.app",
        "ru.sbermarket",
        "ru.samokat.app",
        "ru.dns.shop",
        "ru.lamoda.android",
        "ru.magnit.app",
        "ru.pyaterochka.app"
    )

    val gosuslugiPackages = setOf(
        "ru.gosuslugi.mop",
        "ru.gosuslugi.auto",
        "ru.gosuslugi.pos",
        "ru.nalog.NalogFL",
        "ru.mos.app"
    )

    val mediaPackages = setOf(
        "ru.rutube.app",
        "ru.premier",
        "ru.ivi.client",
        "ru.okko.tv",
        "ru.kion",
        "ru.wink.app",
        "ru.2gis.m33",
        "ru.rzhd.passengers",
        "ru.headhunter.android"
    )

    val allRussianPackages: Set<String> by lazy {
        yandexPackages + vkPackages + bankingPackages + marketplacePackages + gosuslugiPackages + mediaPackages
    }
}
