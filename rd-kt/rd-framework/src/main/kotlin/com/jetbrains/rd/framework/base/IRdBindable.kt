@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.IIdentities
import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.IRdDynamic
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
     * Assigns IDs to this node and its child nodes in the graph.
     */
    fun identify(identities: IIdentities, id: RdId)

    /**
     * Creates a clone of this IRdBindable not bound to any protocol
     */
    fun deepClone() : IRdBindable = TODO("This is a base implementation of deepClone. Shouldn't be invoked. Introduced for AWS plugin to compile with Rider SDK 19.2.")
}

//generator comprehension methods
fun <T:IRdBindable?> T.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = this?.preBind(lf, parent, name)
fun <T:IRdBindable?> T.bind() = this?.bind()
fun <T:IRdBindable?> T.identify(identities: IIdentities, ids: RdId) = this?.identify(identities, ids)

fun <T:IRdBindable?> Array<T>.identify(identities: IIdentities, ids: RdId) = forEachIndexed { i, v ->  v?.identify(identities, ids.mix(i))}
fun <T:IRdBindable?> Array<T>.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.preBind(lf,parent, "$name[$i]")}
fun <T:IRdBindable?> Array<T>.bind() = forEachIndexed { i, v ->  v?.bind()}

fun <T:IRdBindable?> List<T>.identify(identities: IIdentities, ids: RdId) = forEachIndexed { i, v ->  v?.identify(identities, ids.mix(i))}
fun <T:IRdBindable?> List<T>.preBind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.preBind(lf,parent, "$name[$i]")}
fun <T:IRdBindable?> List<T>.bind() = forEachIndexed { i, v ->  v?.bind()}

internal fun Any?.identifyPolymorphic(identities: IIdentities, ids: RdId) {
    if (this is IRdBindable) {
        this.identify(identities, ids)
    } else {
        (this as? Array<*>)?.forEachIndexed { i, v  ->  (v as? IRdBindable)?.identify(identities, ids.mix(i))}
        (this as? List<*>)?.forEachIndexed { i, v  ->  (v as? IRdBindable)?.identify(identities, ids.mix(i))}
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
