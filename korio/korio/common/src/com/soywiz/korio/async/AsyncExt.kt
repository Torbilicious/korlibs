package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*
import kotlin.coroutines.experimental.*

// @TODO: BUG: kotlin-js bug :: Uncaught ReferenceError: CoroutineImpl is not defined
//Coroutine$await$lambda.$metadata$ = {kind: Kotlin.Kind.CLASS, simpleName: null, interfaces: [CoroutineImpl]};
//Coroutine$await$lambda.prototype = Object.create(CoroutineImpl.prototype);

//suspend inline fun <T, R> (suspend T.() -> R).await(receiver: T): R = withContext(coroutineContext.dispatcher) { this(receiver) }
//suspend inline fun <R> (suspend () -> R).await(): R = withContext(coroutineContext.dispatcher) { this() }

suspend fun <T, R> (suspend T.() -> R).await(receiver: T): R =
	withContext(coroutineContext.dispatcher) { this(receiver) }

suspend fun <R> (suspend () -> R).await(): R = withContext(coroutineContext.dispatcher) { this() }

// @TODO: Try to get in subinstance
val CoroutineContext.tryDispatcher: CoroutineDispatcher? get() = this as? CoroutineDispatcher?
val CoroutineContext.dispatcher: CoroutineDispatcher get() = this.tryDispatcher ?: KorioDefaultDispatcher

// @TODO: Do this better! (JS should use requestAnimationFrame)
suspend fun delayNextFrame() = _delayNextFrame()

interface DelayFrame {
	fun delayFrame(continuation: Continuation<Unit>) {
		launch(continuation.context) {
			delay(16)
			continuation.resume(Unit)
		}
	}
}

suspend fun DelayFrame.delayFrame() = suspendCoroutine<Unit> { c -> delayFrame(c) }

val DefaultDelayFrame: DelayFrame = object : DelayFrame {}

val CoroutineContext.delayFrame: DelayFrame
	get() = get(ContinuationInterceptor) as? DelayFrame ?: DefaultDelayFrame


private suspend fun _delayNextFrame() {
	coroutineContext.delayFrame.delayFrame()
}

suspend fun CoroutineContext.delayNextFrame() {
	withContext(this) {
		_delayNextFrame()
	}
}

suspend fun CoroutineContext.delay(time: Int) {
	withContext(this) {
		kotlinx.coroutines.experimental.delay(time)
	}
}

suspend fun delay(time: TimeSpan) = delay(time.milliseconds)

suspend fun CoroutineContext.delay(time: TimeSpan) = delay(time.milliseconds)

fun CoroutineContext.animationFrameLoop(callback: suspend (Closeable) -> Unit): Closeable {
	var running = true
	val close = Closeable {
		running = false
	}
	launch(this) {
		while (running) {
			callback(close)
			delayNextFrame()
		}
	}
	return close
}

interface CoroutineContextHolder {
	val coroutineContext: CoroutineContext
}

class TestCoroutineDispatcher(val frameTime: Int = 16) :
	CoroutineDispatcher(),
	ContinuationInterceptor,
	Delay, DelayFrame {
	var time = 0L; private set

	class TimedTask(val time: Long, val callback: suspend () -> Unit) {
		override fun toString(): String = "TimedTask(time=$time)"
	}

	val tasks = PriorityQueue<TimedTask>(Comparator { a, b -> a.time.compareTo(b.time) })

	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
		return object : Continuation<T> {
			override val context: CoroutineContext = continuation.context

			override fun resume(value: T) {
				continuation.resume(value)
			}

			override fun resumeWithException(exception: Throwable) {
				continuation.resumeWithException(exception)
			}
		}
	}

	private fun scheduleAfter(time: Int, callback: suspend () -> Unit) {
		tasks += TimedTask(this.time + time) {
			callback()
		}
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		scheduleAfter(0) { block.run() }
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		scheduleAfter(unit.toMillis(time).toInt()) { continuation.resume(Unit) }
	}

	override fun delayFrame(continuation: Continuation<Unit>) {
		scheduleAfter(frameTime) { continuation.resume(Unit) }
	}

	var exception: Throwable? = null
	fun loop() {
		//println("doStep: currentThreadId=$currentThreadId")
		if (exception != null) throw exception ?: error("error")
		//println("TASKS: ${tasks.size}")
		while (tasks.isNotEmpty()) {
			val task = tasks.removeHead()
			this.time = task.time
			//println("RUN: $task")
			task.callback.startCoroutine(object : Continuation<Unit> {
				override val context: CoroutineContext = this@TestCoroutineDispatcher
				override fun resume(value: Unit) = Unit
				override fun resumeWithException(exception: Throwable) {
					exception.printStackTrace()
					this@TestCoroutineDispatcher.exception = exception
				}
			})
		}
	}

	fun loop(entry: suspend () -> Unit) {
		entry.startCoroutine(object : Continuation<Unit> {
			override val context: CoroutineContext = this@TestCoroutineDispatcher
			override fun resume(value: Unit) = Unit
			override fun resumeWithException(exception: Throwable) {
				exception.printStackTrace()
				this@TestCoroutineDispatcher.exception = exception
			}
		})
		loop()
	}
}

suspend fun <T> executeInNewThread(task: suspend () -> T): T = KorioNative.executeInWorker(task)
suspend fun <T> executeInWorker(task: suspend () -> T): T = KorioNative.executeInWorker(task)

fun suspendTest(
	dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
	callback: suspend TestCoroutineDispatcher.() -> Unit
) {
	Korio(dispatcher) {
		dispatcher.loop {
			withTimeout(10, TimeUnit.SECONDS) {
				callback(dispatcher)
			}
		}
	}
}

fun suspendTestExceptJs(
	dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
	callback: suspend TestCoroutineDispatcher.() -> Unit
) {
	if (OS.isJs) return
	suspendTest(dispatcher, callback)
}