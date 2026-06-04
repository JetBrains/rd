package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.AllowBindingCookie
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.test.util.TestScheduler
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RdIdLegacyCollisionTest {

    private class Leaf : RdBindableBase()

    private class CollisionModel(childCount: Int) : RdBindableBase() {
        val kids: List<Leaf> = (0 until childCount).map { Leaf() }
        init {
            kids.forEachIndexed { i, leaf -> bindableChildren.add("x$i" to leaf) }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacy(): IIdentities = Identities(IdKind.Server)
    private fun sequential(): IIdentities = SequentialIdentities(IdKind.Server)

    @Test
    fun `legacy named child rdids do not collide across parents`() = childRdIdsDoNotCollide(legacy())

    @Test
    fun `SequentialIdentities named child rdids do not collide across parents`() = childRdIdsDoNotCollide(sequential())

    @Suppress("DEPRECATION")
    private fun childRdIdsDoNotCollide(identities: IIdentities) {
        val ld = LifetimeDefinition()
        val lifetime = ld.lifetime

        val protocol = Protocol("Collision", Serializers(), identities, TestScheduler, TestWire(TestScheduler), lifetime)

        val model1 = CollisionModel(31)
        val model2 = CollisionModel(1)

        AllowBindingCookie.allowBind {
            model1.identify(identities, RdId(3L), false)
            model2.identify(identities, RdId(1L), false)

            model1.preBind(lifetime, protocol, "m1")
            model1.bind()
            // Without the fix this is where model2.x0 duplicates model1.x0 and register() throws.
            model2.preBind(lifetime, protocol, "m2")
            model2.bind()
        }

        assertNotEquals(model1.kids[0].rdid.hash, model2.kids[0].rdid.hash,
            "named children of different parents must not collide")

        val all = HashSet<Long>()
        (model1.kids + model2.kids).forEach { assertTrue(all.add(it.rdid.hash), "duplicate child id ${it.rdid.hash}") }

        ld.terminate()
    }
}
