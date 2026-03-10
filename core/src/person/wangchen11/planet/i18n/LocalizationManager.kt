package person.wangchen11.planet.i18n

import com.badlogic.gdx.Gdx
import java.util.Properties

object LocalizationManager {
    private const val BUNDLE_DIR = "i18n"
    private const val BASE_FILE = "messages.properties"
    private const val PREFS_NAME = "frontier_of_strains_settings"
    private const val PREF_LANGUAGE = "language"

    private val supportedLanguages = setOf("en", "zh")
    private val strings = Properties()
    private var currentLanguage = "en"

    fun initialize() {
        val savedLanguage = Gdx.app.getPreferences(PREFS_NAME).getString(PREF_LANGUAGE, "en")
        setLanguage(savedLanguage)
    }

    fun currentLanguageCode(): String = currentLanguage

    fun currentLanguageLabel(): String {
        return when (currentLanguage) {
            "zh" -> tr("language.label.zh", "Chinese")
            else -> tr("language.label.en", "English")
        }
    }

    fun setLanguage(languageCode: String) {
        currentLanguage = if (languageCode in supportedLanguages) languageCode else "en"
        strings.clear()
        loadInto(strings, "$BUNDLE_DIR/$BASE_FILE")
        if (currentLanguage != "en") {
            loadInto(strings, "$BUNDLE_DIR/messages_${currentLanguage}.properties")
        }
        Gdx.app.getPreferences(PREFS_NAME)
            .putString(PREF_LANGUAGE, currentLanguage)
            .flush()
    }

    fun toggleLanguage() {
        setLanguage(if (currentLanguage == "en") "zh" else "en")
    }

    fun tr(key: String): String {
        return strings.getProperty(key, key)
    }

    fun tr(key: String, fallback: String): String {
        return strings.getProperty(key, fallback)
    }

    fun format(key: String, vararg args: Any?): String {
        return tr(key).format(*args)
    }

    private fun loadInto(properties: Properties, path: String) {
        val file = Gdx.files.internal(path)
        if (!file.exists()) return
        file.reader("UTF-8").use { reader ->
            val loaded = Properties()
            loaded.load(reader)
            loaded.forEach { key, value ->
                properties[key] = value
            }
        }
    }
}
