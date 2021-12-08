@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "PackageDirectoryMismatch")

package com.jetbrains.rd.cross.cases

import com.jetbrains.rd.cross.base.CrossTest_KtServer_Base
import demo.DemoModel

@Suppress("ClassName")
class CrossTest_BigBuffer_KtServer : CrossTest_KtServer_Base() {
    override fun start(args: Array<String>) {
        queue {
            val model = DemoModel.createOrThrow(protocol)

            val entity = model.property_with_default

            entity.set("".padStart(100000, '1'))
            entity.set("".padStart(100000, '3'))
        }
    }
}

fun main(args: Array<String>) {
    CrossTest_BigBuffer_KtServer().run(args)
}