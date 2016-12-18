package com.isel.pdm.yawa.tools


interface ICacheSystem<T> {
    class CacheEntry<T>(val key: String, val item: T, val expiresAt: Long) {
        var found: Boolean = false

        constructor(key: String, item: T, expiresAt: Long, found: Boolean): this(key, item, expiresAt) {
            this.found = found
        }
    }

    /**
     * Get an item from the cache - L1 or L2.
     * Return an CacheEntry
    **/
    fun getItem(key: String): CacheEntry<T>

    /**
     * Set a new CacheEntry
     */
    fun setItem(item: CacheEntry<T>)
}