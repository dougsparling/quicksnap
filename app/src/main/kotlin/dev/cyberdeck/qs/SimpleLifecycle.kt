package dev.cyberdeck.qs

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleLifecycle : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    suspend fun start() = withContext(Dispatchers.Main) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    suspend fun stop() = withContext(Dispatchers.Main) {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle = lifecycleRegistry
}

fun CoroutineScope.launchWithLifecycle(block: suspend CoroutineScope.(LifecycleOwner) -> Unit): Job {
    val lifecycle = SimpleLifecycle()
    return launch {
        try {
            lifecycle.start()
            block(lifecycle)
        } finally {
            lifecycle.stop()
        }
    }
}
