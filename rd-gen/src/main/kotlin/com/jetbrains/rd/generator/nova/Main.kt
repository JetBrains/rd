package com.jetbrains.rd.generator.nova

fun main(args: Array<String>) {
    RdGen().apply {
        parse(args)
        if (!run()) System.exit(1)
    }
}
