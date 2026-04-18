package com.answufeng.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AwConvertersTest {

    private val converters = AwConverters

    // ==================== Date ↔ Long ====================

    @Test
    fun `fromTimestamp null returns null`() {
        assertNull(converters.fromTimestamp(null))
    }

    @Test
    fun `fromTimestamp valid returns Date`() {
        val ts = 1700000000000L
        val date = converters.fromTimestamp(ts)
        assertNotNull(date)
        assertEquals(ts, date!!.time)
    }

    @Test
    fun `fromTimestamp zero returns Date epoch`() {
        val date = converters.fromTimestamp(0L)
        assertNotNull(date)
        assertEquals(0L, date!!.time)
    }

    @Test
    fun `dateToTimestamp null returns null`() {
        assertNull(converters.dateToTimestamp(null))
    }

    @Test
    fun `dateToTimestamp roundtrip`() {
        val now = Date()
        val ts = converters.dateToTimestamp(now)
        val back = converters.fromTimestamp(ts)
        assertEquals(now, back)
    }

    // ==================== List<String> ↔ String (JSON) ====================

    @Test
    fun `fromStringList null returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList(null))
    }

    @Test
    fun `fromStringList blank returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList("   "))
    }

    @Test
    fun `fromStringList empty json array returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList("[]"))
    }

    @Test
    fun `fromStringList single item`() {
        assertEquals(listOf("hello"), converters.fromStringList("""["hello"]"""))
    }

    @Test
    fun `fromStringList multiple items`() {
        assertEquals(listOf("a", "b", "c"), converters.fromStringList("""["a","b","c"]"""))
    }

    @Test
    fun `fromStringList handles items with commas`() {
        val list = listOf("hello,world", "foo")
        val json = converters.stringListToString(list)
        val back = converters.fromStringList(json)
        assertEquals(list, back)
    }

    @Test
    fun `fromStringList handles items with special characters`() {
        val list = listOf("a\"b", "c\\d")
        val json = converters.stringListToString(list)
        val back = converters.fromStringList(json)
        assertEquals(list, back)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromStringList invalid json throws`() {
        converters.fromStringList("not json")
    }

    @Test
    fun `stringListToString null returns null`() {
        assertNull(converters.stringListToString(null))
    }

    @Test
    fun `stringListToString empty list returns empty json array`() {
        val result = converters.stringListToString(emptyList())
        assertEquals(emptyList<String>(), converters.fromStringList(result))
    }

    @Test
    fun `stringListToString roundtrip`() {
        val list = listOf("x", "y", "z")
        val json = converters.stringListToString(list)
        assertEquals(list, converters.fromStringList(json))
    }

    // ==================== List<Long> ↔ String (JSON) ====================

    @Test
    fun `fromLongList null returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList(null))
    }

    @Test
    fun `fromLongList blank returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList(""))
    }

    @Test
    fun `fromLongList empty json array returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList("[]"))
    }

    @Test
    fun `fromLongList valid json array`() {
        assertEquals(listOf(1L, 2L, 3L), converters.fromLongList("[1,2,3]"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromLongList invalid json throws`() {
        converters.fromLongList("not json")
    }

    @Test
    fun `longListToString null returns null`() {
        assertNull(converters.longListToString(null))
    }

    @Test
    fun `longListToString roundtrip`() {
        val list = listOf(10L, 20L, 30L)
        val json = converters.longListToString(list)
        assertEquals(list, converters.fromLongList(json))
    }

    @Test
    fun `longListToString empty list roundtrip`() {
        val json = converters.longListToString(emptyList())
        assertEquals(emptyList<Long>(), converters.fromLongList(json))
    }

    // ==================== Set<String> ↔ String ====================

    @Test
    fun `fromStringSet null returns empty`() {
        assertEquals(emptySet<String>(), converters.fromStringSet(null))
    }

    @Test
    fun `stringSetToString roundtrip`() {
        val set = setOf("a", "b", "c")
        val json = converters.stringSetToString(set)
        assertEquals(set, converters.fromStringSet(json))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromStringSet invalid json throws`() {
        converters.fromStringSet("not json")
    }

    // ==================== Set<Long> ↔ String ====================

    @Test
    fun `fromLongSet null returns empty`() {
        assertEquals(emptySet<Long>(), converters.fromLongSet(null))
    }

    @Test
    fun `longSetToString roundtrip`() {
        val set = setOf(1L, 2L, 3L)
        val json = converters.longSetToString(set)
        assertEquals(set, converters.fromLongSet(json))
    }

    // ==================== Map<String, String> ↔ String ====================

    @Test
    fun `fromStringMap null returns empty`() {
        assertEquals(emptyMap<String, String>(), converters.fromStringMap(null))
    }

    @Test
    fun `fromStringMap blank returns empty`() {
        assertEquals(emptyMap<String, String>(), converters.fromStringMap(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromStringMap invalid json throws`() {
        converters.fromStringMap("not json")
    }

    @Test
    fun `stringMapToString null returns null`() {
        assertNull(converters.stringMapToString(null))
    }

    @Test
    fun `stringMap roundtrip`() {
        val map = mapOf("key1" to "value1", "key2" to "value2")
        val json = converters.stringMapToString(map)
        assertNotNull(json)
        val back = converters.fromStringMap(json)
        assertEquals(map, back)
    }

    @Test
    fun `stringMap handles special characters in values`() {
        val map = mapOf("key" to "val;ue=with,special")
        val json = converters.stringMapToString(map)
        val back = converters.fromStringMap(json)
        assertEquals(map, back)
    }

    @Test
    fun `stringMap empty map roundtrip`() {
        val map = emptyMap<String, String>()
        val json = converters.stringMapToString(map)
        assertEquals(map, converters.fromStringMap(json))
    }

    // ==================== Map<String, Long> ↔ String ====================

    @Test
    fun `fromLongMap null returns empty`() {
        assertEquals(emptyMap<String, Long>(), converters.fromLongMap(null))
    }

    @Test
    fun `longMapToString roundtrip`() {
        val map = mapOf("a" to 1L, "b" to 2L)
        val json = converters.longMapToString(map)
        assertEquals(map, converters.fromLongMap(json))
    }

    // ==================== Boolean ↔ Int ====================

    @Test
    fun `fromIntToBoolean null returns null`() {
        assertNull(converters.fromIntToBoolean(null))
    }

    @Test
    fun `fromIntToBoolean 1 returns true`() {
        assertEquals(true, converters.fromIntToBoolean(1))
    }

    @Test
    fun `fromIntToBoolean 0 returns false`() {
        assertEquals(false, converters.fromIntToBoolean(0))
    }

    @Test
    fun `fromIntToBoolean 2 returns true`() {
        assertEquals(true, converters.fromIntToBoolean(2))
    }

    @Test
    fun `booleanToInt null returns null`() {
        assertNull(converters.booleanToInt(null))
    }

    @Test
    fun `booleanToInt true returns 1`() {
        assertEquals(1, converters.booleanToInt(true))
    }

    @Test
    fun `booleanToInt false returns 0`() {
        assertEquals(0, converters.booleanToInt(false))
    }

    @Test
    fun `boolean roundtrip`() {
        assertEquals(true, converters.fromIntToBoolean(converters.booleanToInt(true)))
        assertEquals(false, converters.fromIntToBoolean(converters.booleanToInt(false)))
    }

    // ==================== ByteArray ↔ Base64 ====================

    @Test
    fun `byteArray roundtrip`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte())
        val encoded = converters.byteArrayToBase64(data)
        if (encoded != null) {
            val decoded = converters.fromBase64(encoded)
            assertArrayEquals(data, decoded)
        }
    }

    @Test
    fun `byteArrayToBase64 null returns null`() {
        assertNull(converters.byteArrayToBase64(null))
    }

    @Test
    fun `fromBase64 null returns null`() {
        assertNull(converters.fromBase64(null))
    }
}
