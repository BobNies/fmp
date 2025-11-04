package org.missionpinball.mpf.core

/**
 * A map which lowercases all string keys.
 * Based on Python's case-insensitive dictionary implementation.
 */
class CaseInsensitiveMap<V>(
    private val delegate: MutableMap<String, V> = mutableMapOf()
) : MutableMap<String, V> by delegate {

    companion object {
        private fun lowerKey(key: String): String = key.lowercase()
    }

    override fun get(key: String): V? {
        return delegate[lowerKey(key)]
    }

    override fun put(key: String, value: V): V? {
        return delegate.put(lowerKey(key), value)
    }

    override fun remove(key: String): V? {
        return delegate.remove(lowerKey(key))
    }

    override fun containsKey(key: String): Boolean {
        return delegate.containsKey(lowerKey(key))
    }

    override fun putAll(from: Map<out String, V>) {
        from.forEach { (key, value) ->
            put(key, value)
        }
    }
}

/**
 * Create a case-insensitive map with initial values.
 */
fun <V> caseInsensitiveMapOf(vararg pairs: Pair<String, V>): CaseInsensitiveMap<V> {
    val map = CaseInsensitiveMap<V>()
    pairs.forEach { (key, value) ->
        map[key] = value
    }
    return map
}
