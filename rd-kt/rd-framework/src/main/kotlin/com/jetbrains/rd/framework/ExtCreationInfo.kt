package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.util.string.RName

data class ExtCreationInfo(
    val rName: RName,
    val rdId: RdId?,
    val hash: Long,
    val ext: RdExtBase?
)

data class ExtCreationInfoEx(
    val info: ExtCreationInfo,
    val isLocal: Boolean
)
