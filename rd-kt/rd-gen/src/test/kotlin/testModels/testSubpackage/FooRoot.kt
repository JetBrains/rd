package testModels.testSubpackage

import com.jetbrains.rd.generator.nova.FlowTransform
import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.test.cases.generator.ModelPackageSearchTests
import java.io.File

object FooRoot : Root(
    Kotlin11Generator(FlowTransform.AsIs, "testModels.foo", File(ModelPackageSearchTests.kotlinTempOutputDir))
)
