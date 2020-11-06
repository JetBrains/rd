package callTest2

import java.io.File
import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator

object MyOrgRoot : Root(
    Kotlin11Generator(FlowTransform.AsIs, "callTest2.foo", File("build/generatedOutputCallTest"))) {

}

object MyOrgSolution : Ext(MyOrgRoot) {

    init {
        call("get", PredefinedType.int, classdef("myClass") {
            set("mySet", PredefinedType.string)
        })
    }
}