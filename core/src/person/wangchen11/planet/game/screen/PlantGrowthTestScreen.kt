package person.wangchen11.planet.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import person.wangchen11.gdx.assets.FontManager
import person.wangchen11.gdx.assets.GraphicsManager
import person.wangchen11.gdx.drawable.ColorDrawable
import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.gdx.game.screen.BaseScreen
import person.wangchen11.planet.game.BuildingManager
import person.wangchen11.planet.game.CropManager
import person.wangchen11.planet.game.CropManager.PlantPhase
import person.wangchen11.planet.game.MainScreenConfig
import person.wangchen11.planet.game.MapManager
import person.wangchen11.planet.game.ResourceManager
import person.wangchen11.planet.game.TechManager
import person.wangchen11.planet.i18n.LocalizationManager
import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.MetadataText
import kotlin.math.max

class PlantGrowthTestScreen(game: BaseGame) : BaseScreen(game) {
    private val batch = SpriteBatch()
    private val camera = OrthographicCamera()
    private lateinit var skin: Skin
    private lateinit var inputMultiplexer: InputMultiplexer
    private lateinit var headerLabel: Label
    private lateinit var fpsLabel: Label
    private lateinit var detailLabel: Label
    private lateinit var selectionLabel: Label
    private var selectedTileX = -1
    private var selectedTileY = -1
    private var pendingScrollAmount = 0f
    private var selectedCropId = "basic_crop"

    override fun show() {
        super.show()
        GraphicsManager.initialize()
        MetadataManager.initialize()
        MapManager.initialize()
        WorldSceneRenderer.invalidateTerrainCache()
        ResourceManager.reset()
        BuildingManager.reset()
        CropManager.reset()
        TechManager.reset()
        TechManager.initialize()
        TechManager.addResearchPoints(999)
        TechManager.researchTech("advanced_farming")
        TechManager.researchTech("hydroponics")

        seedTestField()

        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(worldWidth() / 2f, worldHeight() / 2f, 0f)
        camera.zoom = 0.7f
        camera.update()

        skin = createSkin()
        stage = Stage(ScreenViewport())
        rebuildHud()

        inputMultiplexer = InputMultiplexer(
            object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (button != Input.Buttons.LEFT || isPointerOverUi(screenX.toFloat(), screenY.toFloat())) {
                        return false
                    }
                    plantSelectedCrop(screenX, screenY)
                    return true
                }

                override fun scrolled(amountX: Float, amountY: Float): Boolean {
                    pendingScrollAmount += amountY
                    return true
                }
            },
            stage
        )
        Gdx.input.inputProcessor = inputMultiplexer
    }

    private fun seedTestField() {
        ResourceManager.addResource("energy", 1000)
        ResourceManager.addResource("food", 1000)
        ResourceManager.addResource("wood", 1000)
        ResourceManager.addResource("stone", 1000)
        ResourceManager.addResource("metal", 1000)

        val crops = MetadataManager.getAllCrops().values.sortedBy { it.id }
        val startX = MapManager.MAP_WIDTH / 2 - 4
        val startY = MapManager.MAP_HEIGHT / 2 - 3
        var index = 0
        for (crop in crops) {
            for (copy in 0 until 2) {
                CropManager.plantCrop(crop.id, startX + (index % 4) * 2, startY + (index / 4) * 2)
                index += 1
            }
        }
    }

    private fun createSkin(): Skin {
        val currentSkin = Skin()
        val font = FontManager.baseFont
        currentSkin.add("default-font", font)
        currentSkin.add("panel", ColorDrawable(Color(0.05f, 0.07f, 0.08f, 0.88f), 8f), Drawable::class.java)
        currentSkin.add("panel-strong", ColorDrawable(Color(0.09f, 0.12f, 0.12f, 0.95f), 14f), Drawable::class.java)
        currentSkin.add("button-up", ColorDrawable(Color(0.13f, 0.18f, 0.20f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-down", ColorDrawable(Color(0.19f, 0.28f, 0.31f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-over", ColorDrawable(Color(0.16f, 0.24f, 0.27f, 1f), 8f), Drawable::class.java)
        currentSkin.add("default", Label.LabelStyle(font, Color.WHITE))
        currentSkin.add("title", Label.LabelStyle(font, Color(0.72f, 0.95f, 0.79f, 1f)))
        currentSkin.add("muted", Label.LabelStyle(font, Color(0.70f, 0.77f, 0.75f, 1f)))
        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = font
        buttonStyle.fontColor = Color.WHITE
        buttonStyle.up = currentSkin.getDrawable("button-up")
        buttonStyle.down = currentSkin.getDrawable("button-down")
        buttonStyle.over = currentSkin.getDrawable("button-over")
        currentSkin.add("default", buttonStyle)
        val windowStyle = Window.WindowStyle()
        windowStyle.titleFont = font
        windowStyle.titleFontColor = Color(0.72f, 0.95f, 0.79f, 1f)
        windowStyle.background = currentSkin.getDrawable("panel-strong")
        currentSkin.add("default", windowStyle)
        return currentSkin
    }

    private fun rebuildHud() {
        stage?.clear()
        val root = Table()
        root.setFillParent(true)
        root.top()
        stage?.addActor(root)

        val top = Table()
        top.background = skin.getDrawable("panel-strong")
        top.defaults().pad(10f)
        headerLabel = Label(LocalizationManager.tr("ui.test.plantGrowthObjective"), skin, "title")
        headerLabel.setWrap(true)
        fpsLabel = Label("", skin, "muted")
        val headerBlock = Table()
        headerBlock.defaults().left().padBottom(4f)
        headerBlock.add(headerLabel).expandX().fillX().left().row()
        headerBlock.add(fpsLabel).left()
        top.add(headerBlock).expandX().fillX().left()
        top.add(menuButton(LocalizationManager.tr("ui.button.back")) { finish() }).width(180f).height(42f)
        root.add(top).expandX().fillX().pad(10f).row()

        val picker = Table()
        picker.background = skin.getDrawable("panel")
        picker.defaults().pad(6f)
        picker.add(Label(LocalizationManager.tr("ui.test.selectedCrop"), skin)).left().row()
        selectionLabel = Label("", skin, "muted")
        picker.add(selectionLabel).width(240f).left().padBottom(8f).row()

        val cropButtons = Table()
        cropButtons.defaults().pad(4f)
        MetadataManager.getAllCrops().values.sortedBy { it.id }.forEachIndexed { index, crop ->
            cropButtons.add(menuButton(MetadataText.cropName(crop)) {
                selectedCropId = crop.id
                updateSelectionLabel()
            }).width(118f).height(38f)
            if (index % 2 == 1) {
                cropButtons.row()
            }
        }
        picker.add(cropButtons).left()
        root.add(picker).left().pad(0f, 10f, 0f, 10f).row()

        val side = Table()
        side.background = skin.getDrawable("panel")
        side.defaults().pad(8f).left()
        detailLabel = Label("", skin, "muted")
        detailLabel.setWrap(true)
        detailLabel.setAlignment(Align.topLeft)
        side.add(Label(LocalizationManager.tr("ui.test.plantGrowth"), skin)).left().row()
        side.add(detailLabel).width(260f)

        val sideWrap = Table()
        sideWrap.setFillParent(true)
        sideWrap.right().top()
        sideWrap.add(side).width(290f).pad(12f)
        stage?.addActor(sideWrap)
        updateSelectionLabel()
    }

    private fun menuButton(text: String, action: () -> Unit): TextButton {
        val button = TextButton(text, skin)
        button.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                action()
            }
        })
        return button
    }

    override fun render(delta: Float) {
        handleInput(delta)
        updateSelection()
        CropManager.update(delta)
        ResourceManager.update(delta)

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined
        batch.begin()
        WorldSceneRenderer.drawWorld(batch, currentViewBounds())
        WorldSceneRenderer.drawSelectionOutline(batch, selectedTileX, selectedTileY)
        batch.end()

        updateDetails()
        fpsLabel.setText(LocalizationManager.format("ui.status.fps", Gdx.graphics.framesPerSecond))
        super.render(delta)
    }

    private fun handleInput(delta: Float) {
        val moveSpeed = 520f * delta * camera.zoom
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.position.x -= moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.position.x += moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.position.y += moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.position.y -= moveSpeed
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) finish()
        if (pendingScrollAmount != 0f) {
            camera.zoom = (camera.zoom + pendingScrollAmount * 0.08f).coerceIn(0.55f, 2.2f)
            pendingScrollAmount = 0f
        }
        clampCamera()
    }

    private fun plantSelectedCrop(screenX: Int, screenY: Int) {
        val world = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        val tileX = (world.x / MainScreenConfig.TILE_SIZE).toInt()
        val tileY = (world.y / MainScreenConfig.TILE_SIZE).toInt()
        if (tileX !in 0 until MapManager.MAP_WIDTH || tileY !in 0 until MapManager.MAP_HEIGHT) return
        val crop = MetadataManager.getCrop(selectedCropId) ?: return
        if (CropManager.plantCrop(selectedCropId, tileX, tileY) != null) {
            headerLabel.setText(LocalizationManager.format("toast.planted", MetadataText.cropName(crop)))
        } else {
            headerLabel.setText(LocalizationManager.format("toast.cannotPlant", MetadataText.cropName(crop)))
        }
    }

    private fun updateSelection() {
        val world = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        selectedTileX = (world.x / MainScreenConfig.TILE_SIZE).toInt()
        selectedTileY = (world.y / MainScreenConfig.TILE_SIZE).toInt()
    }

    private fun updateDetails() {
        val crop = CropManager.getCropAt(selectedTileX, selectedTileY)
        detailLabel.setText(
            buildString {
                appendLine(LocalizationManager.format("ui.tile.header", selectedTileX, selectedTileY))
                crop?.let {
                    appendLine(MetadataText.cropName(it.model))
                    appendLine(LocalizationManager.format("ui.tile.crop", MetadataText.cropName(it.model), (it.getGrowthPercentage() * 100).toInt()))
                    appendLine(LocalizationManager.format("ui.tile.cropPhase", plantPhaseText(it.getLifePhase())))
                    appendLine(LocalizationManager.format("ui.tile.cropState", plantStateText(it)))
                } ?: append(LocalizationManager.tr("ui.test.noCrop"))
            }
        )
    }

    private fun updateSelectionLabel() {
        val crop = MetadataManager.getCrop(selectedCropId)
        selectionLabel.setText(
            if (crop == null) {
                LocalizationManager.tr("ui.test.noCrop")
            } else {
                LocalizationManager.format("ui.test.selectedCropValue", MetadataText.cropName(crop))
            }
        )
    }

    private fun currentViewBounds(): WorldSceneRenderer.ViewBounds {
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f
        return WorldSceneRenderer.ViewBounds.fromWorldRect(
            camera.position.x - halfWidth,
            camera.position.y - halfHeight,
            camera.position.x + halfWidth,
            camera.position.y + halfHeight
        )
    }

    private fun plantPhaseText(phase: PlantPhase): String = when (phase) {
        PlantPhase.SEED -> LocalizationManager.tr("ui.plantPhase.seed")
        PlantPhase.SPROUT -> LocalizationManager.tr("ui.plantPhase.sprout")
        PlantPhase.JUVENILE -> LocalizationManager.tr("ui.plantPhase.juvenile")
        PlantPhase.MATURE -> LocalizationManager.tr("ui.plantPhase.mature")
        PlantPhase.FLOWERING -> LocalizationManager.tr("ui.plantPhase.flowering")
        PlantPhase.FRUITING -> LocalizationManager.tr("ui.plantPhase.fruiting")
        PlantPhase.DISPERSAL -> LocalizationManager.tr("ui.plantPhase.dispersal")
        PlantPhase.DECAY -> LocalizationManager.tr("ui.plantPhase.decay")
    }

    private fun plantStateText(crop: CropManager.CropInstance): String = when (crop.getLifePhase()) {
        PlantPhase.DISPERSAL -> LocalizationManager.format("ui.plantState.spread", crop.reproductionCooldown.coerceAtLeast(0f).toInt())
        PlantPhase.DECAY -> LocalizationManager.format("ui.plantState.decay", (14f - crop.decayTimer).coerceAtLeast(0f).toInt())
        else -> LocalizationManager.format("ui.plantState.growth", ((1f - crop.getGrowthPercentage()).coerceAtLeast(0f) * crop.model.growthTime).toInt())
    }

    private fun clampCamera() {
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f
        val worldWidth = worldWidth()
        val worldHeight = worldHeight()
        camera.position.x = if (worldWidth <= halfWidth * 2f) worldWidth / 2f else camera.position.x.coerceIn(halfWidth, max(halfWidth, worldWidth - halfWidth))
        camera.position.y = if (worldHeight <= halfHeight * 2f) worldHeight / 2f else camera.position.y.coerceIn(halfHeight, max(halfHeight, worldHeight - halfHeight))
    }

    private fun worldWidth(): Float = MapManager.MAP_WIDTH * MainScreenConfig.TILE_SIZE
    private fun worldHeight(): Float = MapManager.MAP_HEIGHT * MainScreenConfig.TILE_SIZE

    private fun isPointerOverUi(screenX: Float, screenY: Float): Boolean {
        return stage?.hit(screenX, Gdx.graphics.height - screenY, true) != null
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        clampCamera()
    }

    override fun dispose() {
        batch.dispose()
        if (::skin.isInitialized) disposeSkinSafely()
        super.dispose()
    }

    private fun disposeSkinSafely() {
        if (skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)) skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
        if (skin.has("default", LabelStyle::class.java)) skin.remove("default", LabelStyle::class.java)
        if (skin.has("title", LabelStyle::class.java)) skin.remove("title", LabelStyle::class.java)
        if (skin.has("muted", LabelStyle::class.java)) skin.remove("muted", LabelStyle::class.java)
        if (skin.has("default", TextButtonStyle::class.java)) skin.remove("default", TextButtonStyle::class.java)
        if (skin.has("default", WindowStyle::class.java)) skin.remove("default", WindowStyle::class.java)
        skin.dispose()
    }
}
