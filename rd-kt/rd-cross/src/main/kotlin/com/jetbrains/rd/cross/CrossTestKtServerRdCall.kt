package com.jetbrains.rd.cross

import com.jetbrains.rd.cross.base.CrossTestKtServerBase
import demo.DemoModel

class CrossKtServerRdCall : CrossTestKtServerBase() {
    override fun start(args: Array<String>) {
        before(args)

        queue {
            val model = DemoModel.create(modelLifetime, protocol)

            model.callback.set {
                s -> s.length
            }

            model.call.start('K')
        }

        after()
    }
}

fun main(args: Array<String>) {
    CrossKtServerRdCall().run(args)
}