package com.klipy.sdk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class MediaServicesContractTest {

    @Test
    fun `gif search uses the documented Klipy query parameters`() {
        val method = GifService::class.java.declaredMethods.single { it.name == "search" }

        assertEquals("gifs/search", method.requireAnnotation(GET::class.java).value)
        assertEquals(
            listOf("q", "page", "per_page", "customer_id", "locale", "content_filter", "format_filter"),
            queryNames(method)
        )
    }

    @Test
    fun `gif trending uses customer_id instead of the legacy customerId query name`() {
        val method = GifService::class.java.declaredMethods.single { it.name == "getTrending" }

        assertEquals("gifs/trending", method.requireAnnotation(GET::class.java).value)
        val queries = queryNames(method)
        assertEquals(listOf("page", "per_page", "customer_id", "locale", "format_filter"), queries)
        assertFalse(queries.contains("customerId"))
    }

    @Test
    fun `sticker clip and meme services expose the documented items endpoints`() {
        assertEquals(
            "stickers/items",
            StickersService::class.java.declaredMethods
                .single { it.name == "getItems" }
                .requireAnnotation(GET::class.java)
                .value
        )
        assertEquals(
            "clips/items",
            ClipsService::class.java.declaredMethods
                .single { it.name == "getItems" }
                .requireAnnotation(GET::class.java)
                .value
        )
        assertEquals(
            "static-memes/items",
            MemesService::class.java.declaredMethods
                .single { it.name == "getItems" }
                .requireAnnotation(GET::class.java)
                .value
        )
    }

    @Test
    fun `share and report contracts match the live Klipy docs`() {
        val shareMethod = GifService::class.java.declaredMethods.single { it.name == "triggerShare" }
        val reportMethod = GifService::class.java.declaredMethods.single { it.name == "report" }
        val hideMethod = GifService::class.java.declaredMethods.single { it.name == "hideFromRecent" }

        assertEquals("gifs/share/{slug}", shareMethod.requireAnnotation(POST::class.java).value)
        assertEquals(listOf("slug"), pathNames(shareMethod))
        assertEquals(1, bodyCount(shareMethod))

        assertEquals("gifs/report/{slug}", reportMethod.requireAnnotation(POST::class.java).value)
        assertEquals(listOf("slug"), pathNames(reportMethod))
        assertEquals(1, bodyCount(reportMethod))

        assertEquals("gifs/recent/{customerId}", hideMethod.requireAnnotation(DELETE::class.java).value)
        assertEquals(listOf("customerId"), pathNames(hideMethod))
        assertEquals(listOf("slug"), queryNames(hideMethod))
    }

    private fun queryNames(method: java.lang.reflect.Method): List<String> =
        method.parameterAnnotations.mapNotNull { annotations ->
            annotations.filterIsInstance<Query>().firstOrNull()?.value
        }

    private fun pathNames(method: java.lang.reflect.Method): List<String> =
        method.parameterAnnotations.mapNotNull { annotations ->
            annotations.filterIsInstance<Path>().firstOrNull()?.value
        }

    private fun bodyCount(method: java.lang.reflect.Method): Int =
        method.parameterAnnotations.count { annotations ->
            annotations.any { it is Body }
        }

    private fun <T : Annotation> java.lang.reflect.Method.requireAnnotation(annotation: Class<T>): T =
        requireNotNull(getAnnotation(annotation)) {
            "Expected ${annotation.simpleName} on ${declaringClass.simpleName}.${name}"
        }
}
