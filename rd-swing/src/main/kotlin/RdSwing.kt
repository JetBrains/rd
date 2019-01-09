package com.jetbrains.rd.swing

import com.jetbrains.rider.util.asProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.isAlive
import com.jetbrains.rider.util.lifetime.onTermination
import com.jetbrains.rider.util.reactive.*
import java.awt.*
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionListener
import javax.swing.text.JTextComponent

fun <T> proxyProperty(default: T, onAdvise: (lifetime: Lifetime, set: (T) -> Unit) -> Unit) = object : IPropertyView<T> {
    var vl: T = default

    override val change: ISource<T> = object : ISource<T> {
        override fun advise(lifetime: Lifetime, handler: (T) -> Unit) = onAdvise(lifetime) {
            vl = it
            handler(it)
        }
    }

    override val value: T
        get() = vl
}

private class AWTEventSource(eventMask : Long) : ISource<AWTEvent> {
    val rdSet: ViewableSet<(AWTEvent) -> Unit> = ViewableSet()

    init {
        val listener = AWTEventListener { event ->
            rdSet.forEach { it(event) }
        }

        rdSet.advise(Lifetime.Eternal.createNested()) { addRemove, function ->
            if (rdSet.isEmpty() && addRemove == AddRemove.Remove) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
            } else if (rdSet.size == 1 && addRemove == AddRemove.Add) {
                Toolkit.getDefaultToolkit().addAWTEventListener(
                        listener,
                        eventMask
                )
            }
        }
    }

    override fun advise(lifetime: Lifetime, handler: (AWTEvent) -> Unit) {
        rdSet.add(handler)

        lifetime.onTermination {
            rdSet.remove(handler)
        }
    }
}

val awtMouseOrKeyEvent: ISource<AWTEvent> by lazy {
    AWTEventSource(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK)
}

fun mouseOrKeyReleased(): IVoidSource = awtMouseOrKeyEvent.filter {it.id == MouseEvent.MOUSE_PRESSED || it.id == KeyEvent.KEY_RELEASED}.map { Unit }

val awtMousePoint: ISource<AWTEvent> by lazy {
    AWTEventSource(AWTEvent.MOUSE_MOTION_EVENT_MASK or
            AWTEvent.FOCUS_EVENT_MASK or
            AWTEvent.WINDOW_FOCUS_EVENT_MASK)
}

val awtKeyEvent: ISource<AWTEvent> by lazy {
    AWTEventSource(AWTEvent.KEY_EVENT_MASK)
}

val awtMouseEvent: ISource<AWTEvent> by lazy {
    AWTEventSource(AWTEvent.MOUSE_EVENT_MASK)
}

val awtMouseWithMotionAdapterEvent: ISource<AWTEvent> by lazy {
    AWTEventSource(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
}

fun escPressedSource(): IVoidSource = awtKeyEvent.filter {it.id == KeyEvent.KEY_PRESSED && (it as KeyEvent).keyCode == KeyEvent.VK_ESCAPE}.map { Unit}

fun Component.pressOutside(): IVoidSource {
    return object : IVoidSource {
        var myEverEntered = false
        override fun advise(lifetime: Lifetime, handler: (Unit) -> Unit) {
            awtMouseWithMotionAdapterEvent.filter { it.id == MouseEvent.MOUSE_ENTERED || it.id == MouseEvent.MOUSE_PRESSED }.advise(lifetime) {
                when (it.id) {
                    MouseEvent.MOUSE_ENTERED -> {
                        val componentMousePoint = this@pressOutside.componentHoverPoint()
                        myEverEntered = componentMousePoint != null && Rectangle(this@pressOutside.size).contains(componentMousePoint)
                    }

                    MouseEvent.MOUSE_PRESSED -> if (!myEverEntered) {
                        handler(Unit)
                    }
                }
            }
        }
    }
}

fun JComponent.awtMousePoint(): IPropertyView<Point?> =
        awtMousePoint.map { this@awtMousePoint.componentHoverPoint() }.asProperty(null)

fun Component.componentHoverPoint(): Point? {
    val point = MouseInfo.getPointerInfo().location

    SwingUtilities.convertPointFromScreen(point, this@componentHoverPoint)
    return if (Rectangle(this@componentHoverPoint.size).contains(point)) point else null
}

fun JList<*>.hoveredIndex() = awtMousePoint().map {
    if (it != null) locationToIndex(it) else null
}

fun ListSelectionModel.selectedIndexes(): IPropertyView<List<Int>> {
    return proxyProperty(null as List<Int>) { lifetime, set ->
        val listener = ListSelectionListener {
            val lsm = it.source as ListSelectionModel
            val list = ArrayList<Int>()
            for (i in lsm.minSelectionIndex..lsm.maxSelectionIndex) {
                if (lsm.isSelectedIndex(i))
                    list.add(i)
            }
            set(list)
        }

        this@selectedIndexes.addListSelectionListener(listener)
        lifetime.onTermination {
            this@selectedIndexes.removeListSelectionListener(listener)
        }
    }
}


fun <E> JList<E>.selectedItems() = this@selectedItems.selectionModel.selectedIndexes().map { list ->
    list.map { this@selectedItems.model.getElementAt(it) }
}



fun JComponent.mouseClicked(): ISource<MouseEvent> {
    return object : ISource<MouseEvent> {
        override fun advise(lifetime: Lifetime, handler: (MouseEvent) -> Unit) {
            val clickListener = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e != null) {
                        handler(e)
                    }
                }
            }

            this@mouseClicked.addMouseListener(clickListener)
            lifetime.onTermination {
                this@mouseClicked.removeMouseListener(clickListener)
            }
        }
    }
}

fun JLabel.tooltipForCropped(lifetime: Lifetime) {
    this.sizeProperty().advise(lifetime) {
        this.updateTooltip()
    }

    val listener = PropertyChangeListener { this.updateTooltip() }
    this.addPropertyChangeListener("text", listener)
    lifetime.onTermination {
        this.removePropertyChangeListener(listener)
    }
}

private fun JLabel.updateTooltip() {
    if ((this.width - this.preferredSize.width) < 10) {
        this.toolTipText = this.text
    } else {
        this.toolTipText = null
    }
}

fun Graphics2D.fillRect(rect: Rectangle) {
    fillRect(rect.x, rect.y, rect.width, rect.height)
}

fun <T> JComboBox<T>.addItem(lifetime: Lifetime, item: T) {
    if (lifetime.isAlive) {
        addItem(item)
        lifetime.onTermination {
            removeItem(item)
        }
    }
}

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
                lifetime.onTermination { this@sizeProperty.removeComponentListener(listener) }
            }
        }
    override val value: Dimension
        get() = this@sizeProperty.size
}

fun JTextComponent.textProperty(): IPropertyView<String> {
    fun getValue(): String = this@textProperty.text

    return proxyProperty(getValue()) { lifetime, set ->
        val listener: DocumentListener = object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                set(getValue())
            }

            override fun insertUpdate(e: DocumentEvent?) {
                set(getValue())
            }

            override fun removeUpdate(e: DocumentEvent?) {
                set(getValue())
            }
        }
        this@textProperty.document.addDocumentListener(listener)
        lifetime.onTermination { this@textProperty.document.removeDocumentListener(listener) }
    }
}

fun <T> JComboBox<T>.selectedItemProperty(): IPropertyView<T?> {
    @Suppress("UNCHECKED_CAST")
    fun getValue(): T? = this@selectedItemProperty.selectedItem as T?

    return proxyProperty(getValue()) { lifetime, set ->
        val ls = ActionListener { e ->
            set(getValue())
        }
        this@selectedItemProperty.addActionListener(ls)
        lifetime.onTermination {
            this@selectedItemProperty.removeActionListener(ls)
        }
    }
}


fun Component.visibleProperty(): IPropertyView<Boolean> {
    return proxyProperty(this@visibleProperty.isVisible) { lifetime, set ->
        val listener = object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                set(true)
            }

            override fun componentHidden(e: ComponentEvent?) {
                set(false)
            }
        }

        this@visibleProperty.addComponentListener(listener)
        lifetime.onTermination {
            this@visibleProperty.removeComponentListener(listener)
        }
    }
}

fun JComponent.isInHierarchyProperty(): IPropertyView<Boolean> {
    @Suppress("UNCHECKED_CAST")
    return proxyProperty(this@isInHierarchyProperty.topLevelAncestor != null) { lifetime, set ->
        val listener = object : AncestorListener {
            override fun ancestorAdded(event: AncestorEvent?) {
                set(true)
            }

            override fun ancestorRemoved(event: AncestorEvent) {
                set(false)
            }

            override fun ancestorMoved(event: AncestorEvent?) {
            }
        }

        this@isInHierarchyProperty.addAncestorListener(listener)
        lifetime.onTermination { this@isInHierarchyProperty.removeAncestorListener(listener) }
    }
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
