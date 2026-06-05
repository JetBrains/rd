package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.AllowBindingCookie
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RdIdIdentitiesCollisionMatrixTest {

    private enum class Derivation { Dynamic, Stable }

    @Suppress("DEPRECATION")
    private fun legacy(): IIdentities = Identities(IdKind.Server)
    private fun sequential(): IIdentities = SequentialIdentities(IdKind.Server)

    @Test
    fun `legacy dynamic ids collide even with the fix`() {
        assertFailsWith<IllegalArgumentException> { buildTree(legacy(), Derivation.Dynamic) }
    }

    @Test
    fun `legacy stable ids do not collide`() = buildTree(legacy(), Derivation.Stable)

    @Test
    fun `SequentialIdentities dynamic ids do not collide`() = buildTree(sequential(), Derivation.Dynamic)

    @Test
    fun `SequentialIdentities stable ids do not collide`() = buildTree(sequential(), Derivation.Stable)

    @Suppress("DEPRECATION")
    private fun buildTree(identities: IIdentities, mode: Derivation) {
        val ld = LifetimeDefinition()
        try {
            val lifetime = ld.lifetime
            val protocol = Protocol("MatrixCol", Serializers(), identities, TestScheduler, TestWire(TestScheduler), lifetime)

            // p1 (id 3) is identified/filled before p2 (id 1) — reverse id order is what lets legacy collide.
            val p1 = Node().also { it.identify(identities, RdId(3L), false) }
            val p2 = Node().also { it.identify(identities, RdId(1L), false) }

            AllowBindingCookie.allowBind {
                p1.preBind(lifetime, protocol, "p1"); p1.bind()
                p2.preBind(lifetime, protocol, "p2"); p2.bind()
                addChildren(identities, p1, mode, 31, lifetime)
                addChildren(identities, p2, mode, 1, lifetime)
            }
        } finally {
            ld.terminate()
        }
    }

    private fun addChildren(identities: IIdentities, parent: Node, mode: Derivation, count: Int, lifetime: Lifetime) {
        for (i in 0 until count) {
            val name = "x$i"
            val childId = when (mode) {
                Derivation.Dynamic -> identities.next(parent.rdid)         // dynamic (RdMap/RdList element) id
                Derivation.Stable -> identities.mix(parent.rdid, ".$name") // stable (structural child) id
            }
            val child = Node()
            child.identify(identities, childId, false)
            child.preBind(lifetime, parent, name)
            child.bind()
        }
    }

    private class Node : RdBindableBase()
}
