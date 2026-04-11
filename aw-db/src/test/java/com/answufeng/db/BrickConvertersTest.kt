package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * BrickConverters 类型转换器的双向转换正确性测试。
 */
class BrickConvertersTest {

    private val converters = BrickConverters()

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

    // ==================== List<String> ↔ String ====================

    @Test
    fun `fromStringList null returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList(null))
    }

    @Test
    fun `fromStringList blank returns empty`() {
        assertEquals(emptyList<String>(), converters.fromStringList("   "))
    }

    @Test
    fun `fromStringList single item`() {
        assertEquals(listOf("hello"), converters.fromStringList("hello"))
    }

    @Test
    fun `fromStringList multiple items trims whitespace`() {
        assertEquals(listOf("a", "b", "c"), converters.fromStringList("a, b , c"))
    }

    @Test
    fun `stringListToString null returns null`() {
        assertNull(converters.stringListToString(null))
    }

    @Test
    fun `stringListToString roundtrip`() {
        val list = listOf("x", "y", "z")
        val str = converters.stringListToString(list)
        assertEquals("x,y,z", str)
        assertEquals(list, converters.fromStringList(str))
    }

    // ==================== List<Long> ↔ String ====================

    @Test
    fun `fromLongList null returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList(null))
    }

    @Test
    fun `fromLongList blank returns empty`() {
        assertEquals(emptyList<Long>(), converters.fromLongList(""))
    }

    @Test
    fun `fromLongList skips non-numeric entries`() {
        assertEquals(listOf(1L, 3L), converters.fromLongList("1,abc,3"))
    }

    @Test
    fun `longListToString roundtrip`() {
        val list = listOf(10L, 20L, 30L)
        val str = converters.longListToString(list)
        assertEquals("10,20,30", str)
        assertEquals(list, converters.fromLongList(str))
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
}
