package com.jetbrains.rd.framework

import com.jetbrains.rd.util.string.RName

data class ExtCreationInfo(
    val rName: RName,
    val rdId: RdId?,
    val hash: Long
)
