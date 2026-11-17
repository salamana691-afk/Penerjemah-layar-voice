package com.autodub.edge

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.data.database.CharacterDao
import com.autodub.edge.data.database.CharacterDatabase
import com.autodub.edge.data.repository.CharacterRepository
import com.autodub.edge.domain.model.CharacterProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterDatabaseIntegrationTest {

    private lateinit var db: CharacterDatabase
    private lateinit var dao: CharacterDao
    private lateinit var repository: CharacterRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CharacterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.characterDao()
        repository = CharacterRepository(dao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun persistAndQueryCharacterProfileAndProtectDefaultNarrator() = runBlocking {
        repository.ensureSeeded()

        repository.saveCharacter(
            CharacterProfile(
                characterName = "lucy",
                voiceProvider = AppConstants.PROVIDER_ANDROID_TTS,
                voiceId = "id-id-x-idd-local",
                pitch = 1.12f,
                speechRate = 1.04f,
                volume = 0.95f,
                enabled = true
            )
        )

        val all = repository.observeCharacters().first()
        val lucy = all.firstOrNull { it.characterName == "LUCY" }
        assertNotNull(lucy)
        assertEquals(1.12f, lucy!!.pitch, 0.001f)

        val narrator = all.first { it.characterName == AppConstants.DEFAULT_NARRATOR_NAME }
        repository.deleteCharacter(narrator.id)
        val afterDelete = repository.observeCharacters().first()
        assertNotNull(afterDelete.firstOrNull { it.characterName == AppConstants.DEFAULT_NARRATOR_NAME })
    }
}
