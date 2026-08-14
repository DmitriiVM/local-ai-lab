package com.dmitriim.localailab.source.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyChatDefaultsMigrationTest {
    private val maxOutput = intPreferencesKey("max-output")
    private val contextSize = intPreferencesKey("context-size")
    private val completed = booleanPreferencesKey("completed")
    private val migration = LegacyChatDefaultsMigration(maxOutput, contextSize, completed)

    @Test
    fun `legacy defaults are upgraded once`() = runBlocking {
        val legacy = mutablePreferencesOf().apply {
            this[maxOutput] = 128
            this[contextSize] = 512
        }

        val migrated = migration.migrate(legacy)

        assertEquals(256, migrated[maxOutput])
        assertEquals(2_048, migrated[contextSize])
        assertTrue(migrated[completed] == true)
        assertFalse(migration.shouldMigrate(migrated))
    }

    @Test
    fun `custom values are preserved`() = runBlocking {
        val preferences = mutablePreferencesOf().apply {
            this[maxOutput] = 384
            this[contextSize] = 4_096
        }

        val migrated = migration.migrate(preferences)

        assertEquals(384, migrated[maxOutput])
        assertEquals(4_096, migrated[contextSize])
    }
}
