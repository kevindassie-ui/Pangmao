package fr.kairossolum.pangmao

import android.app.Application

class PangmaoApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
