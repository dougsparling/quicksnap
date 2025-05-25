package dev.cyberdeck.qs.common

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

/**
 * Normally on Android, we start coroutine scopes bound to a lifecycle.
 *
 * However, because the CameraX API has everything bound to a lifecycle, which our foreground
 * service doesn't really have, we invert control and have an artificial lifecycle that gets stopped
 * automatically after a launched unit of work completes.
 */
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
