package io.nekohasekai.sfa.database

object ProfileManager {
    private val callbacks = mutableListOf<() -> Unit>()
    private val profiles = mutableListOf<Profile>()
    private var idCounter = 1L

    fun registerCallback(callback: () -> Unit) {
        callbacks.add(callback)
    }

    fun unregisterCallback(callback: () -> Unit) {
        callbacks.remove(callback)
    }

    suspend fun nextOrder(): Long {
        return profiles.maxOfOrNull { it.userOrder }?.plus(1) ?: 0L
    }

    suspend fun nextFileID(): Long {
        return idCounter++
    }

    suspend fun get(id: Long): Profile? {
        return profiles.find { it.id == id }
    }

    suspend fun create(profile: Profile, andSelect: Boolean = false): Profile {
        profile.id = nextFileID()
        profiles.add(profile)
        if (andSelect) {
            Settings.selectedProfile = profile.id
        }
        for (callback in callbacks.toList()) {
            callback()
        }
        return profile
    }

    suspend fun update(profile: Profile): Int {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            profiles[index] = profile
            for (callback in callbacks.toList()) {
                callback()
            }
            return 1
        }
        return 0
    }

    suspend fun update(updateProfiles: List<Profile>): Int {
        var count = 0
        for (profile in updateProfiles) {
            val index = profiles.indexOfFirst { it.id == profile.id }
            if (index != -1) {
                profiles[index] = profile
                count++
            }
        }
        for (callback in callbacks.toList()) {
            callback()
        }
        return count
    }

    suspend fun delete(profile: Profile): Int {
        val removed = profiles.removeIf { it.id == profile.id }
        if (removed) {
            for (callback in callbacks.toList()) {
                callback()
            }
            return 1
        }
        return 0
    }

    suspend fun delete(deleteProfiles: List<Profile>): Int {
        var count = 0
        for (profile in deleteProfiles) {
            if (profiles.removeIf { it.id == profile.id }) {
                count++
            }
        }
        for (callback in callbacks.toList()) {
            callback()
        }
        return count
    }

    suspend fun list(): List<Profile> {
        return profiles.sortedBy { it.userOrder }.toList()
    }
}
