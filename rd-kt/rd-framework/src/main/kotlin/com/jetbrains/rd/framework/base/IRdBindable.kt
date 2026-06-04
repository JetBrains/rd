@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.IIdentities
import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.IRdDynamic
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.util.lifetime.Lifetime

/**
 * A non-root node in an object graph which can be synchronized with its remote copy over a network or a similar connection.
 */
interface IRdBindable : IRdDynamic {
    val rdid: RdId

    /**
     * Inserts the node into the object graph under the given [parent] and assigns the specified [name] to it. The node will
     * be removed from the graph when the specified [lf] lifetime is terminated.
     */
    fun preBind(lf: Lifetime, parent: IRdDynamic, name: String)

    /**
     * Sends child objects to the wire. This method is called after the parent sends this instance to the wire.
     * */
    fun bind()

    /**
     * Assigns an [RdId] to this node and recursively to all its child nodes.
     *
     * @param identities the identity source used to generate child IDs.
     * @param id the [RdId] to assign to this node.
     * @param stable a recommendation for how child RdIds should be generated. Entities may override this value.
     *   - `true` — uses [IIdentities.mix] to produce hash-based, deterministic IDs derived from the parent ID
     *     and child name. Used when the same entity exists on both protocol sides and its children need
     *     matching IDs to find each other (e.g., extensions with built-in maps, sets, properties).
     *     Given the same parent ID, both sides will compute identical child IDs.
     *   - `false` — uses [IIdentities.next] to produce dynamic IDs.
     *     Used for entities created at runtime (e.g., items added to RdMap/RdList) where each side assigns
     *     its own IDs independently.
     *
     * Entities can override this parameter when their children require a specific strategy. For example,
     * [RdExtBase] forces `stable = true` regardless of the incoming value, because its built-in children
     * are part of a statically known structure that must match on both protocol sides. So even if an ext
     * is encountered during dynamic (non-stable) identification, it will switch to stable IDs for its own subtree.
     */
    fun identify(identities: IIdentities, id: RdId, stable: Boolean)

    /**
     * Creates a clone of this IRdBindable not bound to any protocol
     */
    fun deepClone() : IRdBindable = TODO("This is a base implementation of deepClone. Shouldn't be invoked. Introduced for AWS plugin to compile with Rider SDK 19.2.")
}

private fun computeChildRdId(identities: IIdentities, parent: RdId, stable: Boolean, i: Int): RdId {
    return if (stable) {
        if (identities is Identities) {
            // for backward compatibility
            identities.mix(parent, i)
        } else {
            identities.mix(parent, i.toString(2))
        }
    } else {
        identities.next(parent)
    }
}

//generator comprehension methods
fun <T:IRdBindable?> T.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = this?.preBind(lf, parent, name)
fun <T:IRdBindable?> T.bind() = this?.bind()
fun <T:IRdBindable?> T.identify(identities: IIdentities, ids: RdId, stable: Boolean) = this?.identify(identities, ids, stable)

fun <T:IRdBindable?> Array<T>.identify(identities: IIdentities, ids: RdId, stable: Boolean) = forEachIndexed { i, v ->  v?.identify(
    identities,
    computeChildRdId(identities, ids, stable, i),
    stable
)}
fun <T:IRdBindable?> Array<T>.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.preBind(lf,parent, "$name[$i]")}
fun <T:IRdBindable?> Array<T>.bind() = forEachIndexed { i, v ->  v?.bind()}

fun <T:IRdBindable?> List<T>.identify(identities: IIdentities, ids: RdId, stable: Boolean) = forEachIndexed { i, v ->  v?.identify(
    identities,
    computeChildRdId(identities, ids, stable, i),
    stable,
)}
fun <T:IRdBindable?> List<T>.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.preBind(lf,parent, "$name[$i]")}
fun <T:IRdBindable?> List<T>.bind() = forEachIndexed { i, v ->  v?.bind()}

internal fun Any?.identifyPolymorphic(identities: IIdentities, ids: RdId, stable: Boolean) {
    if (this is IRdBindable) {
        this.identify(identities, ids, stable)
    } else {
        (this as? Array<*>)?.forEachIndexed { i, v  ->  (v as? IRdBindable)?.identify(
            identities,
            computeChildRdId(identities, ids, stable, i),
            stable,
        )}
        (this as? List<*>)?.forEachIndexed { i, v  ->  (v as? IRdBindable)?.identify(
            identities,
            computeChildRdId(identities, ids, stable, i),
            stable,
        )}
    }

}

internal fun Any?.preBindPolymorphic(lf: Lifetime, parent: IRdDynamic, name: String) {
    if (this is IRdBindable)
        this.preBind(lf, parent, name)
    else {
        //Don't remove 'else'. RdList is bindable and collection simultaneously.
        (this as? Array<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.preBind(lf,parent, "$name[$i]")}
        (this as? List<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.preBind(lf,parent, "$name[$i]")}
    }
}

internal fun Any?.bindPolymorphic() {
    if (this is IRdBindable)
        this.bind()
    else {
        //Don't remove 'else'. RdList is bindable and collection simultaneously.
        (this as? Array<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.bind()}
        (this as? List<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.bind()}
    }
}

internal fun <T : IRdBindable> T?.bindTopLevel(lf: Lifetime, parent: IProtocol, name: String) {
    if (this == null)
        return

    preBind(lf, parent, name)
    bind()
}

internal fun <T> T.isBindable(): Boolean {
    return when (this) {
        is IRdBindable -> true
        is Array<*> -> this.firstOrNull() is IRdBindable
        is List<*> -> this.firstOrNull() is IRdBindable
        else -> false
    }
}

@Suppress("UNCHECKED_CAST")
fun <T:Any?> T.deepClonePolymorphic() : T {
    return when (this) {
        is IRdBindable -> deepClone() as T
        is Array<*> -> Array(size) {i -> this[i].deepClonePolymorphic()} as T
        is List<*> -> List(size) {i -> this[i].deepClonePolymorphic()} as T
        else -> this
    }
}
