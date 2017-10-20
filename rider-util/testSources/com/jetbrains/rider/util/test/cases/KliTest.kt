package com.jetbrains.rider.util.test.cases

import com.jetbrains.rider.util.kli.Kli
import org.testng.annotations.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KliTest {

    @Test
    fun testSampleKli() {
        val kli = object : Kli() {

            override val description: String
                get() = "Sample kli description"

            override val comments: String
                get() = "You should only "

            val verbose =   option_flag('v', "verbose", "Verbosity level")
            val size =      option_int ('s', "size", "Sample size", 10)
            val name =      option_string(null, "name", "Sample name")
            val notSort =   option_flag('U', null, "Do not sort")
            val width =     option_long('w', null, "Sample width", 100)
            val dir =       option_path('d', "dir", "Parent folder", Paths.get("."))

            val file = arg("FILE", "Sample file") { Paths.get(it)}

        }

        kli.resetAndParse("-Us3", "--name=X3", "c:/tmp");
        assert(kli.error == null) { kli.error!! }
        kli.apply {
            assertFalse (+verbose)
            assertEquals(3, +size)
            assertEquals("X3", +name)
            assertTrue (+notSort)
            assertEquals(100L, +width)
            assertEquals(Paths.get("."), +dir)
            assertEquals(Paths.get("c:/tmp"), +file)
        }


        println(kli.help())
    }
}