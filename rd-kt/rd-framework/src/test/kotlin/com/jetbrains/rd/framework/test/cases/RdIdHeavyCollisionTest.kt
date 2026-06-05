package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.AllowBindingCookie
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RdIdHeavyCollisionTest {

    private companion object {
        const val NODE_COUNT = 20000
        const val SEED = 1L
    }

    private class Node : RdBindableBase()

    @Suppress("DEPRECATION")
    private fun legacy(): IIdentities = Identities(IdKind.Server)
    private fun sequential(): IIdentities = SequentialIdentities(IdKind.Server)

    @Test
    fun `legacy dynamic ids collide in a large decoupled graph`() {
        assertFailsWith<IllegalArgumentException> { buildHeavyGraph(legacy()) }
    }

    @Test
    fun `SequentialIdentities stays unique across a large decoupled graph`() {
        buildHeavyGraph(sequential()) // must complete without a duplicate-id exception
    }

    @Suppress("DEPRECATION")
    private fun buildHeavyGraph(identities: IIdentities) {
        val ld = LifetimeDefinition()
        try {
            val lifetime = ld.lifetime
            val protocol = Protocol("Heavy", Serializers(), identities, TestScheduler, TestWire(TestScheduler), lifetime)

            val root = Node().also { it.identify(identities, RdId(1L), false) }
            val nodes = ArrayList<Node>().apply { add(root) }

            AllowBindingCookie.allowBind {
                root.preBind(lifetime, protocol, "root")

                val random = java.util.Random(SEED)
                for (k in 0 until NODE_COUNT) {
                    // Attach to an arbitrary earlier node: decoupling parent id from allocation order is
                    // what exposes legacy next's poor distribution (a balanced tree would not collide).
                    val parent = nodes[random.nextInt(nodes.size)]
                    val child = Node()
                    child.identify(identities, identities.next(parent.rdid), false)
                    child.preBind(lifetime, parent, "n$k") // registers; throws on a duplicate RdId
                    nodes.add(child)
                }
            }
        } finally {
            ld.terminate()
        }
    }
}
