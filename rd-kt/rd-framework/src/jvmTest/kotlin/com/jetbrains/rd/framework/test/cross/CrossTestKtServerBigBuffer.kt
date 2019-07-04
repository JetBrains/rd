@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.jetbrains.rd.framework.test.cross

import com.jetbrains.rd.framework.impl.RdProperty
import demo.DemoModel

class CrossTestKtServerBigBuffer : CrossTestServerBase() {
    override val testName = "CrossTestKtServerBigBuffer"

    override fun run(args: Array<String>) {
        before(args)

        scheduler.queue {
            val model = DemoModel.create(lifetime, protocol)

            val entity = model.property_with_default

            entity.advise(lifetime) {
                if (!entity.isLocalChange && (entity as RdProperty<*>).defaultValueChanged) {
                    printer.printIfRemoteChange(entity, "property_with_default", it)

                    finished = true
                }
            }

            entity.set("".padStart(100000, '1'))
            entity.set("".padStart(100000, '3'))
        }

        after()
    }
}

fun main(args: Array<String>) {
    CrossTestKtServerBigBuffer().run(args)
}