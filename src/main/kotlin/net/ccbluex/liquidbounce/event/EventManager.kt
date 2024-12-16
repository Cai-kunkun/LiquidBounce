/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
val ALL_EVENT_CLASSES: Array<KClass<out Event>> = arrayOf(
    GameTickEvent::class,
    GameRenderTaskQueueEvent::class,
    BlockChangeEvent::class,
    ChunkLoadEvent::class,
    ChunkDeltaUpdateEvent::class,
    ChunkUnloadEvent::class,
    DisconnectEvent::class,
    GameRenderEvent::class,
    WorldRenderEvent::class,
    OverlayRenderEvent::class,
    ScreenRenderEvent::class,
    WindowResizeEvent::class,
    FrameBufferResizeEvent::class,
    MouseButtonEvent::class,
    MouseScrollEvent::class,
    MouseCursorEvent::class,
    KeyboardKeyEvent::class,
    KeyboardCharEvent::class,
    InputHandleEvent::class,
    MovementInputEvent::class,
    KeyEvent::class,
    MouseRotationEvent::class,
    KeybindChangeEvent::class,
    AttackEntityEvent::class,
    SessionEvent::class,
    ScreenEvent::class,
    ChatSendEvent::class,
    ChatReceiveEvent::class,
    UseCooldownEvent::class,
    BlockShapeEvent::class,
    BlockBreakingProgressEvent::class,
    BlockVelocityMultiplierEvent::class,
    BlockSlipperinessMultiplierEvent::class,
    EntityMarginEvent::class,
    HealthUpdateEvent::class,
    DeathEvent::class,
    PlayerTickEvent::class,
    PlayerPostTickEvent::class,
    PlayerMovementTickEvent::class,
    PlayerNetworkMovementTickEvent::class,
    PlayerPushOutEvent::class,
    PlayerMoveEvent::class,
    RotatedMovementInputEvent::class,
    PlayerJumpEvent::class,
    PlayerAfterJumpEvent::class,
    PlayerUseMultiplier::class,
    PlayerInteractedItem::class,
    PlayerVelocityStrafe::class,
    PlayerStrideEvent::class,
    PlayerSafeWalkEvent::class,
    CancelBlockBreakingEvent::class,
    PlayerStepEvent::class,
    PlayerStepSuccessEvent::class,
    FluidPushEvent::class,
    PipelineEvent::class,
    PacketEvent::class,
    ClientStartEvent::class,
    ClientShutdownEvent::class,
    ValueChangedEvent::class,
    ModuleActivationEvent::class,
    ModuleToggleEvent::class,
    NotificationEvent::class,
    ClientChatStateChange::class,
    ClientChatMessageEvent::class,
    ClientChatErrorEvent::class,
    ClientChatJwtTokenEvent::class,
    WorldChangeEvent::class,
    AccountManagerMessageEvent::class,
    AccountManagerAdditionResultEvent::class,
    AccountManagerLoginResultEvent::class,
    VirtualScreenEvent::class,
    FpsChangeEvent::class,
    ClientPlayerDataEvent::class,
    SimulatedTickEvent::class,
    SplashOverlayEvent::class,
    SplashProgressEvent::class,
    RefreshArrayListEvent::class,
    BrowserReadyEvent::class,
    ServerConnectEvent::class,
    ServerPingedEvent::class,
    TargetChangeEvent::class,
    BlockCountChangeEvent::class,
    GameModeChangeEvent::class,
    ComponentsUpdate::class,
    ResourceReloadEvent::class,
    ProxyAdditionResultEvent::class,
    ProxyEditResultEvent::class,
    ProxyCheckResultEvent::class,
    ScaleFactorChangeEvent::class,
    DrawOutlinesEvent::class,
    OverlayMessageEvent::class,
    ScheduleInventoryActionEvent::class,
    SpaceSeperatedNamesChangeEvent::class,
    ClickGuiScaleChangeEvent::class,
    BrowserUrlChangeEvent::class,
    TagEntityEvent::class,
    MouseScrollInHotbarEvent::class,
    PlayerFluidCollisionCheckEvent::class,
    PlayerSneakMultiplier::class,
    PerspectiveEvent::class,
    ItemLoreQueryEvent::class,
    PlayerEquipmentChangeEvent::class,
    ClickGuiValueChangeEvent::class,
    BlockAttackEvent::class,
    QueuePacketEvent::class
)


/**
 * Bad name, ik. This class contains all handlers of a given event and keeps track of which of them should be called.
 */
class HookListManager<T : Event> {
    private val hookList = AtomicReference(HookList<T>(emptyList(), 0))

    val containedHooks: List<EventHook<T>>
        get() = hookList.get().containedHooks

    val containedActiveHooks: List<EventHook<T>>
        get() = hookList.get().containedActiveHooks

    /**
     * Generates a new inner state with [f]. Tries to insert a new inner state. If another thread did a change in that
     * time the process repeats (compare and swap)
     *
     * @param f Can be called multiple times. May not have side effects!
     */
    private inline fun editWith(crossinline f: (HookList<T>) -> HookList<T>) {
        this.hookList.updateAndGet {
            f(it)
        }
    }

    /**
     * Registers the given hook. Activates the hook if it's event listener says it should resume
     * [EventListener.shouldBeOnHookList]
     */
    fun registerHook(hook: EventHook<T>) {
        if (hook.handlerClass.shouldBeOnHookList()) {
            this.editWith { hookList ->
                hookList.withHandler(hook).withActivatedHandler(hook)
            }
        } else {
            this.editWith { hookList ->
                hookList.withHandler(hook)
            }
        }
    }

    fun unregisterHook(hook: EventHook<T>) {
        this.editWith { hookList ->
            hookList.withDeactivatedHandler(hook, unregister = true)
        }
    }

    fun suspendHook(hook: EventHook<T>) {
        this.editWith { hookList ->
            hookList.withDeactivatedHandler(hook, unregister = false)
        }
    }

    fun resumeHook(hook: EventHook<T>) {
        this.editWith { hookList ->
            hookList.withActivatedHandler(hook)
        }
    }

    fun clear() {
        this.editWith {
            HookList(emptyList(), 0)
        }
    }

    /**
     * An *immutable* structure which keeps track of the *current* status of which events listen and not listen to an
     * event.
     */
    private class HookList<T : Event>(
        /**
         * All hooks of the event. Contains active *and* inactive hooks.
         */
        val containedHooks: List<EventHook<in T>>,
        /**
         * The first [activeLen] hooks of the [containedHooks] list are actually active
         */
        val activeLen: Int
    ) {
        val containedActiveHooks: List<EventHook<T>>
            get() = this.containedHooks.subList(0, this.activeLen)

        /**
         * Returns a new instance with [hook]. The event hook is **NOT** automatically activated!
         */
        fun withHandler(hook: EventHook<T>): HookList<T> {
            check(hook !in containedHooks) { "The hook $hook is already registered!" }

            return HookList(this.containedHooks + listOf(hook), this.activeLen)
        }

        fun withActivatedHandler(hook: EventHook<T>): HookList<T> {
            val idxOf = this.indexOfHook(hook)

            if (idxOf < this.activeLen) {
                // The event is already active, nothing to do here.
                return this
            }

            val newContainedEvents = containedHooks.toMutableList()

            newContainedEvents.removeAt(idxOf)

            // The range from 0 to activeLen contains all active events
            val subListOfActiveEvents = newContainedEvents.subList(0, this.activeLen)

            // Now insert it at the position it should be.
            subListOfActiveEvents.sortedInsert(hook) { -it.priority }

            // There is a new active member in the list
            val newActiveLen = this.activeLen + 1

            return HookList(newContainedEvents, newActiveLen)
        }

        /**
         * @param unregister removes the [hook] entirely from the hook list.
         */
        fun withDeactivatedHandler(hook: EventHook<T>, unregister: Boolean = false): HookList<T> {
            val idxOf = this.indexOfHook(hook)

            val wasActive = idxOf < this.activeLen
            val newContainedHooks = containedHooks.toMutableList()

            val removedHook = newContainedHooks.removeAt(idxOf)

            val newActiveLen = if (wasActive) this.activeLen - 1 else this.activeLen

            if (!unregister) {
                newContainedHooks.add(removedHook)
            }

            return if (unregister || wasActive) {
                HookList(newContainedHooks, newActiveLen)
            } else {
                // No event was unregistered or deactivated, nothing changed
                this
            }
        }

        private fun indexOfHook(hook: EventHook<T>): Int {
            val idxOf = this.containedHooks.indexOf(hook)

            check(idxOf != -1) { "The event hook $hook is not part of the hook list for the event!" }

            return idxOf
        }
    }
}

/**
 * A modern and fast event handler using lambda handlers
 */
object EventManager {

    private val registry: Map<Class<out Event>, HookListManager<Event>> =
        ALL_EVENT_CLASSES.associate { Pair(it.java, HookListManager()) }

    init {
        kotlin.runCatching {
            SequenceManager
        }.onFailure {
            it.printStackTrace()

            throw it
        }
    }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>): EventHook<T> {
        val handlers = registry[eventClass]
            ?: error("The event '${eventClass.name}' is not registered in Events.kt::ALL_EVENT_CLASSES.")

//        check(eventHook.handlerClass.parent()?.children()?.contains(eventHook.handlerClass) != false) { "The event listener ${eventHook.handlerClass} has ${eventHook.handlerClass.parent()} as a parent, but it does not reference it as a child!" }

        handlers.registerHook(eventHook as EventHook<Event>)

        return eventHook
    }

    /**
     * Unregisters a handler.
     */
    fun <T : Event> unregisterEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry[eventClass]?.unregisterHook(eventHook as EventHook<in Event>)
    }

    fun <T : Event> resumeEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry[eventClass]?.resumeHook(eventHook as EventHook<in Event>)
    }

    fun <T : Event> suspendEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry[eventClass]?.suspendHook(eventHook as EventHook<in Event>)
    }

    private fun forEachHookOf(eventListener: EventListener, f: (HookListManager<*>, EventHook<Event>) -> Unit) {
        registry.values.forEach { hookList ->
            hookList.containedHooks
                .filter { it.handlerClass == eventListener }
                .forEach {
                    f(hookList, it)
                }
        }
    }

    fun unregisterEventHandler(eventListener: EventListener) {
        forEachHookOf(eventListener) { hookList, hook ->
            hookList.unregisterHook(hook)
        }
    }

    fun resumeEventHandler(eventListener: EventListener) {
        forEachHookOf(eventListener) { hookList, hook ->
            hookList.resumeHook(hook)
        }
    }

    fun suspendEventHandler(eventListener: EventListener) {
        forEachHookOf(eventListener) { hookList, hook ->
            hookList.suspendHook(hook)
        }
    }

    fun unregisterAll() {
        registry.values.forEach {
            it.clear()
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun <T : Event> callEvent(event: T): T {
        val target = registry[event.javaClass] ?: return event

        for (eventHook in target.containedActiveHooks) {
            EventScheduler.process(event)

            if (!eventHook.ignoreNotRunning && !eventHook.handlerClass.running) {
                continue
            }

            runCatching {
                eventHook.handler(event)
            }.onFailure {
                logger.error("Exception while executing handler.", it)
            }
        }

        return event
    }
}
