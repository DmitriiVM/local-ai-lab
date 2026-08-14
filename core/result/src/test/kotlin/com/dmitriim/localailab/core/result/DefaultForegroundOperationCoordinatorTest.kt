package com.dmitriim.localailab.core.result

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultForegroundOperationCoordinatorTest {
    @Test
    fun `interrupt cancels every active operation once`() {
        val coordinator = DefaultForegroundOperationCoordinator()
        var firstCancellationCount = 0
        var secondCancellationCount = 0
        coordinator.register { firstCancellationCount += 1 }
        coordinator.register { secondCancellationCount += 1 }

        coordinator.interruptActiveOperations()
        coordinator.interruptActiveOperations()

        assertEquals(1, firstCancellationCount)
        assertEquals(1, secondCancellationCount)
    }

    @Test
    fun `closed operation is not cancelled`() {
        val coordinator = DefaultForegroundOperationCoordinator()
        var cancellationCount = 0
        val registration = coordinator.register { cancellationCount += 1 }
        registration.close()

        coordinator.interruptActiveOperations()

        assertEquals(0, cancellationCount)
    }
}
