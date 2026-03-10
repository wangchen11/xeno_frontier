package person.wangchen11.planet.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import person.wangchen11.gdx.assets.FontManager
import person.wangchen11.gdx.drawable.ColorDrawable
import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.gdx.game.screen.BaseScreen
import person.wangchen11.planet.i18n.LocalizationManager

class MainMenuScreen(game: BaseGame) : BaseScreen(game) {
    private lateinit var skin: Skin

    override fun show() {
        super.show()
        rebuildUi()
    }

    private fun rebuildUi() {
        stage?.dispose()
        if (::skin.isInitialized) {
            disposeSkinSafely()
        }

        skin = createSkin()
        stage = Stage(ScreenViewport())

        val root = Table()
        root.setFillParent(true)
        root.defaults().pad(12f)
        stage?.addActor(root)

        val title = Label(LocalizationManager.tr("menu.title"), skin, "title")
        val subtitle = Label(LocalizationManager.tr("menu.subtitle"), skin)
        val help = Label(LocalizationManager.tr("menu.help"), skin)
        help.setWrap(true)

        val startButton = TextButton(LocalizationManager.tr("menu.start"), skin)
        startButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.startScreen(MainScreen(game))
            }
        })

        val languageButton = TextButton(
            LocalizationManager.format("menu.language", LocalizationManager.currentLanguageLabel()),
            skin
        )
        languageButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                LocalizationManager.toggleLanguage()
                rebuildUi()
            }
        })

        val exitButton = TextButton(LocalizationManager.tr("menu.exit"), skin)
        exitButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })

        root.add(title).padBottom(16f).row()
        root.add(subtitle).row()
        root.add(help).width(560f).padBottom(20f).row()
        root.add(startButton).width(260f).height(56f).row()
        root.add(languageButton).width(260f).height(56f).row()
        root.add(exitButton).width(260f).height(56f)

        Gdx.input.inputProcessor = stage
    }

    private fun createSkin(): Skin {
        val skin = Skin()
        val font = FontManager.baseFont
        skin.add("default-font", font)
        skin.add("panel", ColorDrawable(Color(0.07f, 0.09f, 0.12f, 0.92f), 10f), Drawable::class.java)
        skin.add("button-up", ColorDrawable(Color(0.15f, 0.27f, 0.24f, 1f), 8f), Drawable::class.java)
        skin.add("button-down", ColorDrawable(Color(0.24f, 0.47f, 0.40f, 1f), 8f), Drawable::class.java)
        skin.add("button-over", ColorDrawable(Color(0.21f, 0.36f, 0.31f, 1f), 8f), Drawable::class.java)

        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("title", Label.LabelStyle(font, Color(0.72f, 0.95f, 0.79f, 1f)))

        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = font
        buttonStyle.up = skin.getDrawable("button-up")
        buttonStyle.down = skin.getDrawable("button-down")
        buttonStyle.over = skin.getDrawable("button-over")
        buttonStyle.fontColor = Color.WHITE
        skin.add("default", buttonStyle)
        return skin
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.05f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.render(delta)
    }

    override fun dispose() {
        if (::skin.isInitialized) {
            disposeSkinSafely()
        }
        super.dispose()
    }

    private fun disposeSkinSafely() {
        if (skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)) {
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
        }
        if (skin.has("default", LabelStyle::class.java)) {
            skin.remove("default", LabelStyle::class.java)
        }
        if (skin.has("title", LabelStyle::class.java)) {
            skin.remove("title", LabelStyle::class.java)
        }
        if (skin.has("default", TextButtonStyle::class.java)) {
            skin.remove("default", TextButtonStyle::class.java)
        }
        skin.dispose()
    }
}
