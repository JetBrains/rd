package com.jetbrains.rd.framework

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import java.io.InputStream

interface MarshallersProvider {
    object Dummy : MarshallersProvider {
        override fun getMarshaller(id: RdId): IMarshaller<*>? = null
    }

    companion object {
        fun extractMarshallers(
            stream: InputStream,
            classsLoader: ClassLoader
        ): List<IMarshaller<*>> = stream.reader().useLines { lines: Sequence<String> ->
            lines.map<String, List<String>> { it.split(":") }.map<List<String>, LazyCompanionMarshaller<Any>> {
                LazyCompanionMarshaller<Any>(RdId(it.first().toLong()), classsLoader, it.last())
            }.toList()
        }
    }

    fun getMarshaller(id: RdId): IMarshaller<*>?
}

class AggregatedMarshallersProvider(val providers: Sequence<MarshallersProvider>) : MarshallersProvider {
    override fun getMarshaller(id: RdId): IMarshaller<*>? {
        return providers.mapNotNull { it.getMarshaller(id) }.firstOrNull()
    }
}

abstract class MarhallersProviderFromResourcesBase(val resourceName: String) : MarshallersProvider {
    private val map: Map<RdId, IMarshaller<*>> by lazy {
        val classLoader = javaClass.classLoader
        val resource = classLoader.getResourceAsStream(resourceName) ?: run {
            getLogger(this::class).error { "$resourceName is not found" }
            return@lazy emptyMap()
        }

        MarshallersProvider.extractMarshallers(resource, classsLoader = classLoader).associateBy { it.id }
    }

    override fun getMarshaller(id: RdId): IMarshaller<*>? = map[id]
}