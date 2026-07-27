package io.nekohasekai.sfa.compose.screen.profileoverride

object RussianAppPreset {
    val yandexPackages = setOf(
        "com.yandex.browser",
        "ru.yandex.searchplugin",
        "ru.yandex.yandexnavi",
        "ru.yandex.yandexnav",
        "ru.yandex.yandexmaps",
        "ru.yandex.taxi",
        "ru.yandex.music",
        "ru.yandex.market",
        "ru.yandex.disk",
        "ru.yandex.mail",
        "ru.yandex.kinopoisk",
        "ru.yandex.eats",
        "ru.yandex.lavka",
        "ru.yandex.pay",
        "ru.yandex.weatherplugin",
        "ru.yandex.taximeter",
        "ru.yandex.translate",
        "ru.yandex.plus"
    )

    val vkPackages = setOf(
        "com.vkontakte.android",
        "com.vk.im",
        "com.vk.music",
        "com.vk.video",
        "com.vk.clips",
        "ru.vk.store",
        "ru.mail.mailapp",
        "ru.mail.cloud",
        "ru.ok.android",
        "ru.vk.calls"
    )

    val bankingPackages = setOf(
        "ru.sberbankmobile",
        "com.sberbank.sberpay",
        "ru.sberbank.sberpay",
        "ru.sberbank.investor",
        "ru.sberbank_investor",
        "com.tinkoff.bank",
        "com.tbank",
        "com.idamob.tinkoff.android",
        "com.tinkoff.investing",
        "ru.tinkoff.invest",
        "ru.alfabank.o2o",
        "ru.alfabank.mobile.android",
        "ru.vtb24.mobilebanking.android",
        "ru.vtb.invest.android",
        "ru.raiffeisennews",
        "ru.raiffeisen.invest",
        "ru.gpb.mobile",
        "ru.mw.gpb",
        "ru.ozon.bank",
        "ru.sovcomcard.subscribers",
        "ru.sovcomcard.superapp",
        "ru.nspk.mirpay",
        "ru.rshb.sub",
        "ru.rshb.farmseller",
        "ru.psbank.morpheus",
        "ru.open.bank",
        "ru.mkb.mobile"
    )

    val marketplacePackages = setOf(
        "ru.ozon.app.android",
        "com.wildberries.ru",
        "ru.wildberries.work",
        "ru.megamarket.app",
        "com.sberbank.megamarket",
        "ru.avito.app",
        "ru.avito",
        "ru.aliexpress.buyer",
        "ru.sbermarket",
        "ru.samokat.app",
        "ru.dns.shop",
        "ru.dns_shop",
        "ru.lamoda.android",
        "ru.magnit.app",
        "ru.magnit.express",
        "ru.pyaterochka.app",
        "ru.x5.card",
        "ru.perekrestok.app",
        "ru.wb.drive"
    )

    val gosuslugiPackages = setOf(
        "ru.gosuslugi.app",
        "ru.gosuslugi.mop",
        "ru.gosuslugi.auto",
        "ru.gosuslugi.pos",
        "ru.gosuslugi.culture",
        "ru.gosuslugi.health",
        "ru.gosuslugi.services",
        "ru.nalog.NalogFL",
        "ru.nalog.pd",
        "ru.mos.app",
        "ru.mos.pgu",
        "ru.pension.fund"
    )

    val mediaPackages = setOf(
        "ru.rutube.app",
        "ru.premier",
        "ru.ivi.client",
        "ru.papeba.ivimobile",
        "ru.okko.tv",
        "ru.okko.online",
        "ru.kion",
        "ru.wink.app",
        "ru.rt.wink",
        "ru.start.android",
        "ru.2gis.m33",
        "ru.dublgis.dgismobile",
        "ru.rzhd.passengers",
        "ru.rzd.pass",
        "ru.headhunter.android",
        "ru.tuturu.tutu",
        "ru.tutu.app",
        "ru.mts.telecom",
        "ru.mts.mytravel",
        "ru.beeline.services",
        "ru.megafon.ml",
        "ru.tele2.mytele2",
        "ru.rt.life"
    )

    val allRussianPackages: Set<String> by lazy {
        yandexPackages + vkPackages + bankingPackages + marketplacePackages + gosuslugiPackages + mediaPackages
    }
}
