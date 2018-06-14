package com.soywiz.korio.lang

interface Closeable {
	fun close(): Unit
}

interface OptionalCloseable : Closeable {
	override fun close(): Unit = Unit
}

fun Closeable(callback: () -> Unit) = object : Closeable {
	override fun close() = callback()
}

//java.lang.NoClassDefFoundError: com/soywiz/korio/lang/CloseableKt$Closeable$1 (wrong name: com/soywiz/korio/lang/CloseableKt$closeable$1)
//  at java.lang.ClassLoader.defineClass1(Native Method)
//fun Iterable<Closeable>.closeable(): Closeable = Closeable {
//	for (closeable in this@closeable) closeable.close()
//}

fun <TCloseable : Closeable, T : Any> TCloseable.use(callback: (TCloseable) -> T): T {
	try {
		return callback(this)
	} finally {
		this.close()
	}
}

interface Cancellable {
	fun cancel(e: Throwable = com.soywiz.korio.CancellationException("")): Unit

	interface Listener {
		fun onCancel(handler: (Throwable) -> Unit): Unit
	}

	companion object {
		operator fun invoke(callback: (Throwable) -> Unit) = object : Cancellable {
			override fun cancel(e: Throwable) = callback(e)
		}
	}
}

fun Iterable<Cancellable>.cancel(e: Throwable = com.soywiz.korio.CancellationException("")): Unit =
	run { for (c in this) c.cancel(e) }

fun Iterable<Cancellable>.cancellable() = Cancellable { this.cancel() }

fun Iterable<Closeable>.close() = run { for (c in this) c.close() }
fun Iterable<Closeable>.closeable() = Closeable { this.close() }

fun Closeable.cancellable() = Cancellable { this.close() }
fun Cancellable.closeable(e: () -> Throwable = { com.soywiz.korio.CancellationException("") }) =
	Closeable { this.cancel(e()) }