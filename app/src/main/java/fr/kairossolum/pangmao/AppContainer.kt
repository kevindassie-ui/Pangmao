package fr.kairossolum.pangmao

import android.content.Context
import fr.kairossolum.pangmao.data.dictionary.DictionaryDataSource
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.dictionary.OfflineDictionaryRepository
import fr.kairossolum.pangmao.data.user.PangmaoUserDatabase
import fr.kairossolum.pangmao.data.user.StudyRepository

class AppContainer(context: Context) {
    val dictionary: DictionaryRepository = OfflineDictionaryRepository(DictionaryDataSource(context))
    private val userDatabase = PangmaoUserDatabase.create(context)
    val study = StudyRepository(userDatabase.userDao(), dictionary)
}

