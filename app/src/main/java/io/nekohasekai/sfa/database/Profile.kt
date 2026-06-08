package io.nekohasekai.sfa.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Profile(
    var id: Long = 0L,
    var userOrder: Long = 0L,
    var name: String = "",
    var icon: String? = null,
    var typed: TypedProfile = TypedProfile(),
) : Parcelable
