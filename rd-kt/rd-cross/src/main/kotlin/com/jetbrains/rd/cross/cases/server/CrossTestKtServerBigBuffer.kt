@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "PackageDirectoryMismatch")

package com.jetbrains.rd.cross.cases

import com.jetbrains.rd.cross.base.CrossTestKtServerBase
import demo.DemoModel

class CrossTestKtServerBigBuffer : CrossTestKtServerBase() {
    override fun start(args: Array<String>) {
        queue {
            val model = DemoModel.create(modelLifetime, protocol)

            val entity = model.property_with_default

            entity.set("".padStart(100000, '1'))
            entity.set("".padStart(100000, '3'))
        }
    }
}

fun main(args: Array<String>) {
    CrossTestKtServerBigBuffer().run(args)
}