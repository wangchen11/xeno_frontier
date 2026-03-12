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

class TestFeatureListScreen(game: BaseGame) : BaseScreen(game) {
    private lateinit var skin: Skin

    override fun show() {
        super.show()
        skin = createSkin()
        stage = Stage(ScreenViewport())
        rebuildUi()
    }

    private fun rebuildUi() {
        stage?.clear()

        val root = Table()
        root.setFillParent(true)
        root.defaults().pad(12f)
        stage?.addActor(root)

        val title = Label(LocalizationManager.tr("ui.test.title"), skin, "title")
        val subtitle = Label(LocalizationManager.tr("ui.test.subtitle"), skin, "muted")
        subtitle.setWrap(true)
        subtitle.setAlignment(Align.topLeft)

        val growthTestButton = TextButton(LocalizationManager.tr("ui.test.plantGrowth"), skin)
        growthTestButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.startScreen(PlantGrowthTestScreen(game))
            }
        })

        val backButton = TextButton(LocalizationManager.tr("ui.button.back"), skin)
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                finish()
            }
        })

        val card = Table()
        card.background = skin.getDrawable("panel")
        card.defaults().pad(8f)
        card.add(Label(LocalizationManager.tr("ui.test.plantGrowth"), skin, "section")).left().width(420f).row()
        card.add(Label(LocalizationManager.tr("ui.test.plantGrowthDesc"), skin, "muted")).width(420f).left().padBottom(12f).row()
        card.add(growthTestButton).width(320f).height(56f).left()

        root.add(title).left().row()
        root.add(subtitle).width(520f).left().row()
        root.add(card).padTop(10f).left().row()
        root.add(backButton).width(220f).height(48f).left().padTop(16f)

        Gdx.input.inputProcessor = stage
    }

    private fun createSkin(): Skin {
        val skin = Skin()
        val font = FontManager.baseFont
        skin.add("default-font", font)
        skin.add("panel", ColorDrawable(Color(0.09f, 0.12f, 0.12f, 0.95f), 14f), Drawable::class.java)
        skin.add("button-up", ColorDrawable(Color(0.13f, 0.18f, 0.20f, 1f), 8f), Drawable::class.java)
        skin.add("button-down", ColorDrawable(Color(0.19f, 0.28f, 0.31f, 1f), 8f), Drawable::class.java)
        skin.add("button-over", ColorDrawable(Color(0.16f, 0.24f, 0.27f, 1f), 8f), Drawable::class.java)

        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("title", Label.LabelStyle(font, Color(0.72f, 0.95f, 0.79f, 1f)))
        skin.add("section", Label.LabelStyle(font, Color(0.91f, 0.95f, 0.93f, 1f)))
        skin.add("muted", Label.LabelStyle(font, Color(0.70f, 0.77f, 0.75f, 1f)))

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
        Gdx.gl.glClearColor(0.04f, 0.06f, 0.07f, 1f)
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
        if (skin.has("default", TextButtonStyle::class.java)) {
            skin.remove("default", TextButtonStyle::class.java)
        }
        skin.dispose()
    }
}
