@file:Suppress("PackageDirectoryMismatch")

package com.jetbrains.rd.cross.cases

import com.jetbrains.rd.cross.base.CrossTest_KtServer_Base
import demo.demoModel

@Suppress("ClassName")
class CrossTest_RdCall_KtServer : CrossTest_KtServer_Base() {
    override fun start(args: Array<String>) {
        queue {
            val model = protocol.demoModel

            model.callback.set { it -> it.length }

            model.call.start('K')
        }
    }
}

fun main(args: Array<String>) {
    CrossTest_RdCall_KtServer().run(args)
}