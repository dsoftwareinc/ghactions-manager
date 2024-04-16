package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.psi.model.GitHubAction
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
class TimedCache(private var cacheTimeValidityInMillis: Long = 1000 * 60 * 60) :
    ConcurrentHashMap<String, GitHubAction>() {

    @Contextual
    private val creationTimeMap = ConcurrentHashMap<String, Long>()

    fun cleanup() {
        this.forEach { (key, value) ->
            if (isExpired(key, cacheTimeValidityInMillis)) {
                this.remove(key)
                creationTimeMap.remove(key)
            }
        }
    }

    override fun get(key: String): GitHubAction? {
        val value = super.get(key)
        if (value == null || isExpired(key, cacheTimeValidityInMillis)) {
            return null
        }
        return value
    }

    override fun containsKey(key: String): Boolean {
        return super.containsKey(key) && !isExpired(key, cacheTimeValidityInMillis)
    }

    override fun put(key: String, value: GitHubAction): GitHubAction {
        super.put(key, value)
        creationTimeMap[key] = now()
        return value
    }

    private fun isExpired(key: String, cacheTimeValidityInMillis: Long): Boolean {
        val creationTime = creationTimeMap[key]
        return if (creationTime == null) {
            true
        } else {
            (now() - creationTime) > cacheTimeValidityInMillis
        }
    }


    private fun now() = System.currentTimeMillis()
}