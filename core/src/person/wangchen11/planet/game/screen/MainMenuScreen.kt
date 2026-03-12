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
import com.badlogic.gdx.utils.Align
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
        root.defaults().pad(14f)
        stage?.addActor(root)

        val title = Label(LocalizationManager.tr("menu.title"), skin, "title")
        title.setAlignment(Align.left)
        val subtitle = Label(LocalizationManager.tr("menu.subtitle"), skin)
        subtitle.color = Color(0.84f, 0.90f, 0.88f, 1f)
        subtitle.setAlignment(Align.left)
        val help = Label(LocalizationManager.tr("menu.help"), skin)
        help.setWrap(true)
        help.color = Color(0.69f, 0.76f, 0.74f, 1f)
        help.setAlignment(Align.topLeft)

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

        val shell = Table()
        shell.defaults().pad(18f)

        val overview = Table()
        overview.background = skin.getDrawable("panel-strong")
        overview.defaults().left().pad(8f)
        overview.add(Label(LocalizationManager.tr("menu.kicker"), skin, "kicker")).left().row()
        overview.add(title).left().padTop(10f).row()
        overview.add(subtitle).width(520f).left().padTop(4f).row()
        overview.add(help).width(520f).left().padTop(20f).row()

        val featureList = Label(
            LocalizationManager.tr("menu.features"),
            skin,
            "muted"
        )
        featureList.setAlignment(Align.topLeft)
        overview.add(featureList).width(520f).left().padTop(12f).row()

        val actionCard = Table()
        actionCard.background = skin.getDrawable("panel")
        actionCard.defaults().pad(8f)
        val actionTitle = Label(LocalizationManager.tr("menu.consoleTitle"), skin, "section")
        val actionHint = Label(
            LocalizationManager.tr("menu.consoleHint"),
            skin,
            "muted"
        )
        actionHint.setWrap(true)
        actionHint.setAlignment(Align.topLeft)
        actionCard.add(actionTitle).left().width(260f).row()
        actionCard.add(actionHint).width(260f).left().padBottom(18f).row()
        actionCard.add(startButton).width(260f).height(60f).row()
        actionCard.add(languageButton).width(260f).height(52f).row()
        actionCard.add(exitButton).width(260f).height(52f).row()

        shell.add(overview).expand().fill()
        shell.add(actionCard).width(300f).top()
        root.add(shell).expand().fill()

        Gdx.input.inputProcessor = stage
    }

    private fun createSkin(): Skin {
        val skin = Skin()
        val font = FontManager.baseFont
        skin.add("default-font", font)
        skin.add("panel", ColorDrawable(Color(0.07f, 0.10f, 0.11f, 0.94f), 14f), Drawable::class.java)
        skin.add("panel-strong", ColorDrawable(Color(0.09f, 0.13f, 0.12f, 0.96f), 18f), Drawable::class.java)
        skin.add("button-up", ColorDrawable(Color(0.18f, 0.31f, 0.27f, 1f), 10f), Drawable::class.java)
        skin.add("button-down", ColorDrawable(Color(0.25f, 0.46f, 0.38f, 1f), 10f), Drawable::class.java)
        skin.add("button-over", ColorDrawable(Color(0.22f, 0.39f, 0.33f, 1f), 10f), Drawable::class.java)

        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("title", Label.LabelStyle(font, Color(0.72f, 0.95f, 0.79f, 1f)))
        skin.add("section", Label.LabelStyle(font, Color(0.91f, 0.95f, 0.93f, 1f)))
        skin.add("muted", Label.LabelStyle(font, Color(0.70f, 0.77f, 0.75f, 1f)))
        skin.add("kicker", Label.LabelStyle(font, Color(0.52f, 0.83f, 0.71f, 1f)))

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
        Gdx.gl.glClearColor(0.04f, 0.06f, 0.06f, 1f)
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
        if (skin.has("section", LabelStyle::class.java)) {
            skin.remove("section", LabelStyle::class.java)
        }
        if (skin.has("muted", LabelStyle::class.java)) {
            skin.remove("muted", LabelStyle::class.java)
        }
        if (skin.has("kicker", LabelStyle::class.java)) {
            skin.remove("kicker", LabelStyle::class.java)
        }
        if (skin.has("default", TextButtonStyle::class.java)) {
            skin.remove("default", TextButtonStyle::class.java)
        }
        skin.dispose()
    }
}
