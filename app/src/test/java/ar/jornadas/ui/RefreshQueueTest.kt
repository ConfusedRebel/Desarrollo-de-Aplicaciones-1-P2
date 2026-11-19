package ar.jornadas.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class RefreshQueueTest {
    @Test
    fun tapsDuringAnActiveRequestAddOnlyOneFollowingAttempt() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val releaseFirst = CompletableDeferred<Unit>()
            val releaseSecond = CompletableDeferred<Unit>()
            var attempts = 0
            val queue = RefreshQueue(scope, {
                attempts++
                when (attempts) {
                    1 -> releaseFirst.await()
                    2 -> releaseSecond.await()
                }
            }, { throw AssertionError("Unexpected failure", it) })

            queue.solicitar()
            assertEquals(1, attempts)
            queue.solicitar()
            queue.solicitar()
            assertEquals(1, attempts)

            releaseFirst.complete(Unit)
            yield()
            assertEquals(2, attempts)
            releaseSecond.complete(Unit)
            yield()
            assertFalse(queue.cargando.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failureStillRunsOnePendingAttemptAndAllowsLaterRefresh() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val releaseFirst = CompletableDeferred<Unit>()
            var attempts = 0
            var failures = 0
            val queue = RefreshQueue(scope, {
                attempts++
                if (attempts == 1) {
                    releaseFirst.await()
                    throw IOException("offline")
                }
            }, { failures++ })

            queue.solicitar()
            queue.solicitar()
            queue.solicitar()
            releaseFirst.complete(Unit)
            yield()
            assertEquals(2, attempts)
            assertEquals(1, failures)
            assertFalse(queue.cargando.value)

            queue.solicitar()
            yield()
            assertEquals(3, attempts)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancellationDoesNotReportFailureOrRunPendingAttempt() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val neverCompletes = CompletableDeferred<Unit>()
        var attempts = 0
        var failures = 0
        val queue = RefreshQueue(scope, {
            attempts++
            neverCompletes.await()
        }, { failures++ })

        queue.solicitar()
        queue.solicitar()
        scope.cancel()
        yield()

        assertEquals(1, attempts)
        assertEquals(0, failures)
        assertFalse(queue.cargando.value)
    }
}
