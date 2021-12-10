@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "PackageDirectoryMismatch")

package com.jetbrains.rd.cross.cases

import com.jetbrains.rd.cross.base.CrossTest_KtServer_Base
import com.jetbrains.rd.cross.statics.CrossTest_AllEntities.checkConstants
import com.jetbrains.rd.cross.statics.CrossTest_AllEntities.fireAll
import com.jetbrains.rd.cross.util.trackAction
import demo.demoModel
import demo.extModel

@Suppress("ClassName")
class CrossTest_AllEntities_KtServer : CrossTest_KtServer_Base() {
    override fun start(args: Array<String>) {
        trackAction("Checking constant") {
            checkConstants()
        }

        queue {
            val model = trackAction("Creating DemoModel") {
                protocol.demoModel
            }

            val extModel = trackAction("Creating ExtModel") {
                model.extModel
            }

            trackAction("Firing") {
                fireAll(model, extModel)
            }
        }
    }
}

fun main(args: Array<String>) {
    CrossTest_AllEntities_KtServer().run(args)
}