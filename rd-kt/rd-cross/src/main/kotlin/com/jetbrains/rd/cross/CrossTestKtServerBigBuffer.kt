@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.jetbrains.rd.cross

import com.jetbrains.rd.cross.base.CrossTestKtServerBase
import demo.DemoModel

class CrossTestKtServerBigBuffer : CrossTestKtServerBase() {
    override fun start(args: Array<String>) {
        before(args)

        queue {
            val model = DemoModel.create(modelLifetime, protocol)

            val entity = model.property_with_default

            entity.set("".padStart(100000, '1'))
            entity.set("".padStart(100000, '3'))
        }

        after()
    }
}

fun main(args: Array<String>) {
    CrossTestKtServerBigBuffer().run(args)
}