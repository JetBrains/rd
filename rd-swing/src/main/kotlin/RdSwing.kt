package com.jetbrains.rd.swing

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.beans.PropertyChangeEvent
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

fun JComponent.sizeProperty(): IPropertyView<Dimension> = object : IPropertyView<Dimension> {
    override val change: ISource<Dimension>
        get() = object : ISource<Dimension> {
            override fun advise(lifetime: Lifetime, handler: (Dimension) -> Unit) {
                val listener = object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent?) {
                        handler(this@sizeProperty.size)
                    }
                }

                this@sizeProperty.addComponentListener(listener)
                lifetime.add { this@sizeProperty.removeComponentListener(listener) }
            }
        }
    override val value: Dimension
        get() = this@sizeProperty.size
}

fun JTextComponent.textProperty(): IOptProperty<String> = object : IOptProperty<String> {
    override val change: ISource<String>
        get() = object : ISource<String> {
            override fun advise(lifetime: Lifetime, handler: (String) -> Unit) {
                val listener: DocumentListener = object : DocumentListener {
                    override fun changedUpdate(e: DocumentEvent?) {
                        handler(this@textProperty.text)
                    }

                    override fun insertUpdate(e: DocumentEvent?) {
                        handler(this@textProperty.text)
                    }

                    override fun removeUpdate(e: DocumentEvent?) {
                        handler(this@textProperty.text)
                    }
                }
                this@textProperty.document.addDocumentListener(listener)
                lifetime.add { this@textProperty.document.removeDocumentListener(listener) }
            }
        }
    override val valueOrNull: String?
        get() = this@textProperty.text

    override fun set(newValue: String) {
        if (this@textProperty.text != newValue) {
            this@textProperty.text = newValue
        }
    }
}

fun <T> JComboBox<T>.selectedItemProperty(): IPropertyView<T?> = object : IPropertyView<T?> {
    override val change: ISource<T?>
        get() = object : ISource<T?> {
            override fun advise(lifetime: Lifetime, handler: (T?) -> Unit) {
                val ls = ActionListener { e ->
                    @Suppress("UNCHECKED_CAST")
                    handler(this@selectedItemProperty.selectedItem as T?)
                }
                this@selectedItemProperty.addActionListener(ls)
                lifetime.add {
                    this@selectedItemProperty.removeActionListener(ls)
                }
            }
        }
    override val value: T?
        @Suppress("UNCHECKED_CAST")
        get() = this@selectedItemProperty.selectedItem as T?
}

fun Component.visibleProperty(): IPropertyView<Boolean> = object : IPropertyView<Boolean> {
    override val change: ISource<Boolean>
        get() = object : ISource<Boolean> {
            override fun advise(lifetime: Lifetime, handler: (Boolean) -> Unit) {
                val listener = object : ComponentAdapter() {
                    override fun componentShown(e: ComponentEvent?) {
                        handler(true)
                    }

                    override fun componentHidden(e: ComponentEvent?) {
                        handler(false)
                    }
                }

                this@visibleProperty.addComponentListener(listener)
                lifetime.add {
                    this@visibleProperty.removeComponentListener(listener)
                }

            }
        }
    override val value: Boolean
        get() = this@visibleProperty.isVisible
}

fun JComponent.isInHierarchyProperty(): IPropertyView<Boolean> = object : IPropertyView<Boolean> {
    override val change: ISource<Boolean>
        get() = object : ISource<Boolean> {
            override fun advise(lifetime: Lifetime, handler: (Boolean) -> Unit) {
                val listener = object : AncestorListener {
                    override fun ancestorAdded(event: AncestorEvent?) {
                        handler(true)
                    }

                    override fun ancestorRemoved(event: AncestorEvent) {
                        handler(false)
                    }

                    override fun ancestorMoved(event: AncestorEvent?) {
                    }
                }

                this@isInHierarchyProperty.addAncestorListener(listener)
                lifetime.add { this@isInHierarchyProperty.removeAncestorListener(listener) }
            }
        }

    override val value: Boolean
        get() = this@isInHierarchyProperty.topLevelAncestor != null
}

fun JComponent.namedProperty(name: String, lifetime: Lifetime): IOptPropertyView<Any> {
    val prop: IOptProperty<Any> = OptProperty()
    val listener: (PropertyChangeEvent) -> Unit = { e ->
        if (name == e.propertyName) {
            prop.set(e.newValue)
        }
    }

    addPropertyChangeListener(listener)
    lifetime.add {
        removePropertyChangeListener(listener)
    }

    return prop
}
