@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "PackageDirectoryMismatch")

package com.jetbrains.rd.cross.cases

import com.jetbrains.rd.cross.base.CrossTestKtClientBase
import com.jetbrains.rd.cross.statics.CrossTestAllEntities.checkConstants
import com.jetbrains.rd.cross.statics.CrossTestAllEntities.fireAll
import com.jetbrains.rd.cross.util.trackAction
import demo.*

@Suppress("unused")
class CrossTestKtClientAllEntities : CrossTestKtClientBase() {
    override fun start(args: Array<String>) {
        trackAction("Checking constant") {
            checkConstants()
        }

        queue {
            val model = trackAction("Creating DemoModel") {
                DemoModel.create(modelLifetime, protocol)
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
    CrossTestKtClientAllEntities().run(args)
}