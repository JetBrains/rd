package callTest2.foo

import java.io.File
import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator

object MyOrgFooRoot : Root(
    Kotlin11Generator(FlowTransform.AsIs, "callTest2.foo", File("build/generatedOutputCallTest"))) {

}
object MyOrgFooSolution : Ext(MyOrgFooRoot) {

    init {
        call("get", PredefinedType.int, classdef("myClass") {
            set("mySet", PredefinedType.string)
        })
    }
}