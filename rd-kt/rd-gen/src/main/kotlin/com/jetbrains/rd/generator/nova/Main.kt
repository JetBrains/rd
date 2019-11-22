package com.jetbrains.rd.generator.nova

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    RdGen().apply {
        parse(args)
        if (!run()) exitProcess(1)
    }
}
