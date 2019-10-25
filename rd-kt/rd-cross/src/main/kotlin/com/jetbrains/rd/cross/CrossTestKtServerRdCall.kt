package com.jetbrains.rd.cross

import com.jetbrains.rd.cross.base.CrossTestKtServerBase
import demo.DemoModel

class CrossKtServerRdCall : CrossTestKtServerBase() {
    override fun start(args: Array<String>) {
        queue {
            val model = DemoModel.create(modelLifetime, protocol)

            model.callback.set {
                s -> s.length
            }

            model.call.start('K')
        }
    }
}

fun main(args: Array<String>) {
    CrossKtServerRdCall().run(args)
}