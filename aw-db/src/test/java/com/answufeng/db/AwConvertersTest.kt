package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AwConvertersTest {

    private val converters = AwConverters()

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

    @Test
    fun `fromStringList invalid json returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList("not json"))
    }

    @Test
    fun `stringListToString null returns null`() {
        assertNull(converters.stringListToString(null))
    }

    @Test
    fun `stringListToString empty list returns empty json array`() {
        val result = converters.stringListToString(emptyList())
        assertEquals("[]", result)
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

    @Test
    fun `fromLongList invalid json returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList("not json"))
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
    fun `longListToString empty list returns empty json array`() {
        assertEquals("[]", converters.longListToString(emptyList()))
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

    @Test
    fun `fromStringMap invalid json returns empty`() {
        assertEquals(emptyMap<String, String>(), converters.fromStringMap("not json"))
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
        assertEquals("{}", json)
        assertEquals(map, converters.fromStringMap(json))
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
}
