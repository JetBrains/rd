package com.jetbrains.rd.framework.test.cases.entity

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.test.cases.openEntity.*
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenEntitiesTest : RdFrameworkTestBase(){

    @Test
    fun testClassProperties(){
        val serverProperty = RdOptionalProperty(ConcreteClassEntity).static(1).slave()
        val clientProperty = RdOptionalProperty(ConcreteClassEntity).static(1)

        serverProtocol.bindStatic(serverProperty, "classTest")
        clientProtocol.bindStatic(clientProperty, "classTest")

        val model = ConcreteClassEntity("test")
        serverProperty.set(model)

        val clientEntity = clientProperty.valueOrThrow

        clientEntity.concreteProperty.set(41.0)
        clientEntity.openClassProperty.set(42)
        clientEntity.baseOpenClassProperty.set("www")
        clientEntity.baseClassProperty.set(true)

        val serverEntity = serverProperty.valueOrThrow
        assertTrue(serverEntity.concreteProperty.valueOrThrow == 41.0)
        assertTrue(serverEntity.openClassProperty.valueOrThrow == 42)
        assertTrue(serverEntity.baseOpenClassProperty.valueOrThrow == "www" )
        assertTrue(serverEntity.baseClassProperty.valueOrThrow)

        serverEntity.concreteProperty.set(90.0)
        serverEntity.openClassProperty.set(100)
        serverEntity.baseOpenClassProperty.set("test")
        serverEntity.baseClassProperty.set(false)

        assertTrue(clientEntity.concreteProperty.valueOrThrow == 90.0)
        assertTrue(clientEntity.openClassProperty.valueOrThrow == 100)
        assertTrue(clientEntity.baseOpenClassProperty.valueOrThrow == "test" )
        assertTrue(!clientEntity.baseClassProperty.valueOrThrow)
    }

    @Test
    fun testStructures(){
        val serverProperty = RdOptionalProperty(ConcreteStructEntity).static(1).slave()
        val clientProperty = RdOptionalProperty(ConcreteStructEntity).static(1)

        serverProtocol.bindStatic(serverProperty, "structTest")
        clientProtocol.bindStatic(clientProperty, "structTest")

        val firstModel = ConcreteStructEntity(1.0, 2, "www", true)
        serverProperty.set(firstModel)

        assertTrue(clientProperty.valueOrThrow.concreteField == 1.0)
        assertTrue(clientProperty.valueOrThrow.openStructField == 2)
        assertTrue(clientProperty.valueOrThrow.baseOpenStructField == "www")
        assertTrue(clientProperty.valueOrThrow.baseStructField)

        val secondModel = ConcreteStructEntity(14.0, 12, "test", false)
        clientProperty.set(secondModel)
        assertTrue(serverProperty.valueOrThrow.concreteField == 14.0)
        assertTrue(serverProperty.valueOrThrow.openStructField == 12)
        assertTrue(serverProperty.valueOrThrow.baseOpenStructField == "test")
        assertTrue(!serverProperty.valueOrThrow.baseStructField)
    }


}