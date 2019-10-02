package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.IRdReactive
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.*

@Suppress("UNCHECKED_CAST")
class ReflectionMarshaller<T : Any>(override val _type: KClass<T>) : IMarshaller<T> {

    val membersSerializers = mutableMapOf<KProperty1<T, *>, ISerializer<*>>()
    val ctorParamsSerializers = mutableListOf<ISerializer<*>>()

    companion object {
        private val serializers = mutableMapOf<KClass<*>, ISerializer<*>>()

        init {
            for (marshallerProperty in FrameworkMarshallers::class.memberProperties) {
                val kclass = marshallerProperty.returnType.classifier as? KClass<*> ?: continue
                if (!kclass.isSubclassOf(IMarshaller::class)) continue
                val argClass = marshallerProperty.returnType.arguments.first().type!!.classifier as KClass<*>
                serializers[argClass] = marshallerProperty.get(FrameworkMarshallers) as IMarshaller<*>
            }
        }

        fun findCompanionSerializer(kClass: KClass<*>) : ISerializer<Any>? {
            return kClass.companionObjectInstance as? ISerializer<Any>
        }

        inline operator fun<reified T:Any> invoke() = invoke(T::class)

        fun serializerFor(type: KType) : ISerializer<*> {
            val cl = type.classifier as KClass<*>
            val typeArg1 = type.arguments.firstOrNull()?.type
            val res: ISerializer<*> = when {
                cl.isSubclassOf(IRdReactive::class) -> findCompanionSerializer(cl)!!
                cl.isSubclassOf(kotlin.collections.List::class) -> serializerFor(typeArg1!!).list()
//                cl.isSubclassOf(kotlin.Array<Any>::class) -> serializerFor(typeArg1!!).array()
                type.arguments.isEmpty() -> invoke(cl)
                else -> error("Can't build reflection serializer for $type")
            }

            if (type.isMarkedNullable)
                return (res as ISerializer<Any>).nullable()
            else
                return res
        }


        operator fun <T:Any> invoke(kclass: KClass<T>) : ISerializer<T> {
            if (!kclass.typeParameters.isEmpty())
                error("Reflection marshaller for generics is unsupported")

            if (kclass.isAbstract)
                return Polymorphic()

            //if companion object implements ISerializer
            val companionSerializer = findCompanionSerializer(kclass)
            if (companionSerializer != null) return companionSerializer as ISerializer<T>

            return serializers[kclass] as? ISerializer<T> ?: let {

                val res = ReflectionMarshaller(kclass)
                serializers[kclass] = res


//                kclass.primaryConstructor?.valueParameters
                for (p in kclass.memberProperties) {
                    res.membersSerializers[p] = serializerFor(p.returnType)
                }

                for (p in kclass.primaryConstructor!!.parameters) {
                    res.ctorParamsSerializers.add(serializerFor(p.type))
                }

                res
            }
        }
    }

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): T {
        val args = ctorParamsSerializers.map { it.read(ctx, buffer) }.toTypedArray()
        return _type.primaryConstructor!!.call(*args)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: T) {
        for ((p, szr) in membersSerializers) {
            val propValue = p.get(value)
            (szr as ISerializer<Any?>).write(ctx, buffer, propValue)
        }
    }
}