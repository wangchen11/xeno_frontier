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
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle
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
import person.wangchen11.planet.game.EnemyManager
import person.wangchen11.planet.game.GameManager
import person.wangchen11.planet.game.MainScreenConfig
import person.wangchen11.planet.game.MapManager
import person.wangchen11.planet.game.ResourceManager
import person.wangchen11.planet.game.TechManager
import person.wangchen11.planet.i18n.LocalizationManager
import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.MetadataText
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class MainScreen(game: BaseGame) : BaseScreen(game) {
    private val batch = SpriteBatch()
    private val camera = OrthographicCamera()
    private lateinit var skin: Skin
    private lateinit var inputMultiplexer: InputMultiplexer

    private lateinit var resourceLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var objectiveLabel: Label
    private lateinit var selectionLabel: Label
    private lateinit var hintLabel: Label
    private lateinit var toastLabel: Label
    private lateinit var modeLabel: Label

    private var selectedBuildingId = "basic_farm"
    private var selectedCropId = "basic_crop"
    private var actionMode = ActionMode.INSPECT
    private var selectedTileX = -1
    private var selectedTileY = -1
    private var pendingScrollAmount = 0f
    private var gameOverDialogShown = false
    private var winDialogShown = false
    private var toastTimer = 0f
    private var pointerDownOnWorld = false
    private var activePointer = -1
    private var lastDragScreenX = 0
    private var lastDragScreenY = 0
    private var dragDistance = 0f
    private var isDraggingCamera = false

    override fun show() {
        super.show()
        GraphicsManager.initialize()
        GameManager.initialize()
        WorldSceneRenderer.invalidateTerrainCache()

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
                    pointerDownOnWorld = true
                    activePointer = pointer
                    lastDragScreenX = screenX
                    lastDragScreenY = screenY
                    dragDistance = 0f
                    isDraggingCamera = false
                    return true
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (!pointerDownOnWorld || pointer != activePointer) {
                        return false
                    }
                    val dx = screenX - lastDragScreenX
                    val dy = screenY - lastDragScreenY
                    dragDistance += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                    if (dragDistance > 6f) {
                        isDraggingCamera = true
                    }
                    if (isDraggingCamera) {
                        camera.position.x -= dx * camera.zoom
                        camera.position.y += dy * camera.zoom
                        clampCamera()
                    }
                    lastDragScreenX = screenX
                    lastDragScreenY = screenY
                    return true
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (button != Input.Buttons.LEFT || pointer != activePointer) {
                        return false
                    }
                    val shouldTriggerAction = pointerDownOnWorld && !isDraggingCamera && !isPointerOverUi(screenX.toFloat(), screenY.toFloat())
                    pointerDownOnWorld = false
                    activePointer = -1
                    if (shouldTriggerAction) {
                        performWorldAction(screenX, screenY)
                    }
                    isDraggingCamera = false
                    dragDistance = 0f
                    return shouldTriggerAction
                }

                override fun scrolled(amountX: Float, amountY: Float): Boolean {
                    pendingScrollAmount += amountY
                    return true
                }
            },
            stage
        )
        Gdx.input.inputProcessor = inputMultiplexer
        showToast(LocalizationManager.tr("toast.welcome"))
    }

    private fun createSkin(): Skin {
        val currentSkin = Skin()
        val font = FontManager.baseFont
        currentSkin.add("default-font", font)
        currentSkin.add("panel", ColorDrawable(Color(0.05f, 0.07f, 0.08f, 0.88f), 8f), Drawable::class.java)
        currentSkin.add("panel-soft", ColorDrawable(Color(0.08f, 0.10f, 0.11f, 0.78f), 12f), Drawable::class.java)
        currentSkin.add("panel-strong", ColorDrawable(Color(0.09f, 0.12f, 0.12f, 0.95f), 14f), Drawable::class.java)
        currentSkin.add("button-up", ColorDrawable(Color(0.13f, 0.18f, 0.20f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-down", ColorDrawable(Color(0.19f, 0.28f, 0.31f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-over", ColorDrawable(Color(0.16f, 0.24f, 0.27f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-disabled", ColorDrawable(Color(0.10f, 0.12f, 0.13f, 0.92f), 8f), Drawable::class.java)
        currentSkin.add("button-primary-up", ColorDrawable(Color(0.18f, 0.33f, 0.28f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-primary-down", ColorDrawable(Color(0.24f, 0.46f, 0.39f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-primary-over", ColorDrawable(Color(0.21f, 0.39f, 0.34f, 1f), 8f), Drawable::class.java)
        currentSkin.add("scroll-bg", ColorDrawable(Color(0.04f, 0.05f, 0.06f, 0.55f), 6f), Drawable::class.java)
        currentSkin.add("scroll-knob", ColorDrawable(Color(0.38f, 0.61f, 0.52f, 0.95f), 6f), Drawable::class.java)

        currentSkin.add("default", Label.LabelStyle(font, Color.WHITE))
        currentSkin.add("muted", Label.LabelStyle(font, Color(0.71f, 0.78f, 0.77f, 1f)))
        currentSkin.add("accent", Label.LabelStyle(font, Color(0.72f, 0.95f, 0.79f, 1f)))
        currentSkin.add("section", Label.LabelStyle(font, Color(0.91f, 0.95f, 0.93f, 1f)))

        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = font
        buttonStyle.fontColor = Color.WHITE
        buttonStyle.disabledFontColor = Color(0.55f, 0.57f, 0.58f, 1f)
        buttonStyle.up = currentSkin.getDrawable("button-up")
        buttonStyle.down = currentSkin.getDrawable("button-down")
        buttonStyle.over = currentSkin.getDrawable("button-over")
        buttonStyle.disabled = currentSkin.getDrawable("button-disabled")
        currentSkin.add("default", buttonStyle)

        val primaryButtonStyle = TextButton.TextButtonStyle(buttonStyle)
        primaryButtonStyle.up = currentSkin.getDrawable("button-primary-up")
        primaryButtonStyle.down = currentSkin.getDrawable("button-primary-down")
        primaryButtonStyle.over = currentSkin.getDrawable("button-primary-over")
        currentSkin.add("primary", primaryButtonStyle)

        val windowStyle = Window.WindowStyle()
        windowStyle.titleFont = font
        windowStyle.titleFontColor = Color(0.72f, 0.95f, 0.79f, 1f)
        windowStyle.background = currentSkin.getDrawable("panel-strong")
        currentSkin.add("default", windowStyle)

        val scrollPaneStyle = ScrollPaneStyle()
        scrollPaneStyle.background = currentSkin.getDrawable("scroll-bg")
        scrollPaneStyle.vScrollKnob = currentSkin.getDrawable("scroll-knob")
        scrollPaneStyle.vScroll = currentSkin.getDrawable("scroll-bg")
        scrollPaneStyle.hScrollKnob = currentSkin.getDrawable("scroll-knob")
        scrollPaneStyle.hScroll = currentSkin.getDrawable("scroll-bg")
        currentSkin.add("default", scrollPaneStyle)

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
        resourceLabel = Label("", skin)
        statusLabel = Label("", skin)
        resourceLabel.setWrap(true)
        statusLabel.setAlignment(Align.right)

        val brandBlock = Table()
        brandBlock.defaults().left().padBottom(4f)
        brandBlock.add(Label(LocalizationManager.tr("ui.header.outpostStatus"), skin, "accent")).left().row()
        brandBlock.add(resourceLabel).width(640f).left()

        val telemetryBlock = Table()
        telemetryBlock.defaults().right().padBottom(4f)
        telemetryBlock.add(Label(LocalizationManager.tr("ui.header.fieldTelemetry"), skin, "muted")).right().row()
        telemetryBlock.add(statusLabel).right()

        top.add(brandBlock).expandX().fillX().left()
        top.add(telemetryBlock).right().minWidth(320f)
        root.add(top).expandX().fillX().pad(10f, 10f, 0f, 10f).row()

        objectiveLabel = Label("", skin)
        objectiveLabel.setWrap(true)
        objectiveLabel.color = Color(0.72f, 0.95f, 0.79f, 1f)
        objectiveLabel.setAlignment(Align.left)
        val objectiveCard = Table()
        objectiveCard.background = skin.getDrawable("panel-soft")
        objectiveCard.add(objectiveLabel).expandX().fillX().pad(8f, 12f, 8f, 12f)
        root.add(objectiveCard).expandX().fillX().pad(6f, 10f, 0f, 10f).padBottom(0f).row()

        val controlsCard = Table()
        controlsCard.background = skin.getDrawable("panel-strong")
        controlsCard.defaults().pad(6f)
        controlsCard.add(Label(LocalizationManager.tr("ui.header.controlDeck"), skin, "section")).colspan(4).left().row()

        modeLabel = Label("", skin, "muted")
        modeLabel.setWrap(true)
        controlsCard.add(modeLabel).colspan(4).width(520f).left().padBottom(10f).row()

        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.build")} [B]", "primary") { showBuildDialog() }).width(138f).height(48f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.plant")} [P]") { showCropDialog() }).width(138f).height(48f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.gather")} [G]") { actionMode = ActionMode.GATHER }).width(138f).height(48f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.harvest")} [H]") { actionMode = ActionMode.HARVEST }).width(138f).height(48f).row()
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.inspect")} [I]") { actionMode = ActionMode.INSPECT }).width(138f).height(44f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.tech")} [T]") { showTechDialog() }).width(138f).height(44f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.wave")} [E]") { EnemyManager.spawnWave() }).width(138f).height(44f)
        controlsCard.add(menuButton("${LocalizationManager.tr("ui.button.language")} [F2]") {
            LocalizationManager.toggleLanguage()
            rebuildHud()
        }).width(138f).height(44f)
        val bottomWrap = Table()
        bottomWrap.setFillParent(true)
        bottomWrap.bottom()
        bottomWrap.add(controlsCard).pad(12f)
        stage?.addActor(bottomWrap)

        val side = Table()
        side.background = skin.getDrawable("panel-strong")
        side.defaults().pad(8f).left()
        selectionLabel = Label("", skin)
        selectionLabel.setAlignment(Align.topLeft)
        selectionLabel.setWrap(true)
        hintLabel = Label("", skin)
        hintLabel.setWrap(true)
        toastLabel = Label("", skin)
        toastLabel.setWrap(true)
        toastLabel.color = Color.GOLD
        side.add(Label(LocalizationManager.tr("ui.section.sectorIntel"), skin, "section")).left().row()
        side.add(selectionLabel).width(250f).row()
        side.add(Label(LocalizationManager.tr("ui.section.currentAction"), skin, "section")).left().padTop(8f).row()
        side.add(hintLabel).width(250f).row()
        side.add(Label(LocalizationManager.tr("ui.section.log"), skin, "section")).left().padTop(8f).row()
        side.add(toastLabel).width(250f)

        val sideWrap = Table()
        sideWrap.setFillParent(true)
        sideWrap.right().top()
        sideWrap.add(side).width(280f).pad(12f)
        stage?.addActor(sideWrap)

        if (toastTimer > 0f) {
            toastLabel.setText(toastLabel.text)
        }
    }

    private fun menuButton(text: String, style: String = "default", action: () -> Unit): TextButton {
        val button = TextButton(text, skin, style)
        button.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                action()
            }
        })
        return button
    }

    override fun render(delta: Float) {
        handleKeyboard(delta)
        handleMouseWorld()
        GameManager.update(delta)
        updateToast(delta)

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        batch.begin()
        WorldSceneRenderer.drawWorld(batch, currentViewBounds())
        WorldSceneRenderer.drawSelectionOutline(batch, selectedTileX, selectedTileY)
        batch.end()

        updateHud()

        if (GameManager.isGameOver) {
            showGameOverDialog()
        } else if (GameManager.hasWon()) {
            showWinDialog()
        }

        super.render(delta)
    }

    private fun drawTerrain() {
        val fallbackSprite = GraphicsManager.getSprite("grid")
        val fillSprite = GraphicsManager.getSprite("terrain_fill")
        for (y in 0 until MapManager.MAP_HEIGHT) {
            for (x in 0 until MapManager.MAP_WIDTH) {
                val terrain = MapManager.getTerrainAt(x, y) ?: continue
                if (fillSprite != null) {
                    val color = MapManager.getTerrainColor(terrain.id)
                    fillSprite.setColor(color)
                    fillSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    fillSprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                    fillSprite.draw(batch)
                    fillSprite.setColor(Color.WHITE)
                }

                val variant = MapManager.getTerrainVariantAt(x, y)
                val sprite = GraphicsManager.getSprite("terrain_${terrain.id}_$variant") ?: fallbackSprite
                if (sprite != null) {
                    if (sprite === fallbackSprite) {
                        sprite.setColor(MapManager.getTerrainColor(terrain.id))
                    } else {
                        sprite.setColor(1f, 1f, 1f, 0.42f)
                    }
                    sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    sprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                    sprite.draw(batch)
                    sprite.setColor(Color.WHITE)
                }

                if (MapManager.getGatherAmountAt(x, y) > 0 && BuildingManager.getBuildingAt(x, y) == null) {
                    val marker = fillSprite ?: sprite
                    if (marker != null) {
                        marker.setColor(1f, 1f, 1f, 0.06f)
                        marker.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                        marker.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                        marker.draw(batch)
                        marker.setColor(Color.WHITE)
                    }
                }
            }
        }

        drawTerrainTransitions()
    }

    private fun drawTerrainTransitions() {
        for (y in 0 until MapManager.MAP_HEIGHT) {
            for (x in 0 until MapManager.MAP_WIDTH) {
                val terrain = MapManager.getTerrainAt(x, y) ?: continue
                val basePriority = MapManager.getTerrainPriority(terrain.id)
                collectOverlayTerrainIds(x, y, basePriority).forEach { targetTerrainId ->
                    var mask = 0
                    if (shouldBlendTo(x, y + 1, basePriority, targetTerrainId)) mask = mask or 1
                    if (shouldBlendTo(x + 1, y, basePriority, targetTerrainId)) mask = mask or 2
                    if (shouldBlendTo(x, y - 1, basePriority, targetTerrainId)) mask = mask or 4
                    if (shouldBlendTo(x - 1, y, basePriority, targetTerrainId)) mask = mask or 8
                    if (shouldBlendTo(x - 1, y + 1, basePriority, targetTerrainId)) mask = mask or 16
                    if (shouldBlendTo(x + 1, y + 1, basePriority, targetTerrainId)) mask = mask or 32
                    if (shouldBlendTo(x + 1, y - 1, basePriority, targetTerrainId)) mask = mask or 64
                    if (shouldBlendTo(x - 1, y - 1, basePriority, targetTerrainId)) mask = mask or 128
                    if (mask == 0) return@forEach

                    val maskSprite = GraphicsManager.getSprite("terrain_mask_$mask") ?: return@forEach
                    val color = MapManager.getTerrainColor(targetTerrainId)
                    maskSprite.setColor(color.r, color.g, color.b, 0.96f)
                    maskSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    maskSprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                    maskSprite.draw(batch)
                    maskSprite.setColor(Color.WHITE)
                }
            }
        }
    }

    private fun collectOverlayTerrainIds(tileX: Int, tileY: Int, basePriority: Int): List<String> {
        return listOfNotNull(
            MapManager.getTerrainAt(tileX, tileY + 1)?.id,
            MapManager.getTerrainAt(tileX + 1, tileY)?.id,
            MapManager.getTerrainAt(tileX, tileY - 1)?.id,
            MapManager.getTerrainAt(tileX - 1, tileY)?.id,
            MapManager.getTerrainAt(tileX - 1, tileY + 1)?.id,
            MapManager.getTerrainAt(tileX + 1, tileY + 1)?.id,
            MapManager.getTerrainAt(tileX + 1, tileY - 1)?.id,
            MapManager.getTerrainAt(tileX - 1, tileY - 1)?.id
        )
            .distinct()
            .filter { MapManager.getTerrainPriority(it) > basePriority }
            .sortedBy { MapManager.getTerrainPriority(it) }
    }

    private fun shouldBlendTo(tileX: Int, tileY: Int, basePriority: Int, targetTerrainId: String): Boolean {
        val neighbor = MapManager.getTerrainAt(tileX, tileY) ?: return false
        return neighbor.id == targetTerrainId && MapManager.getTerrainPriority(neighbor.id) > basePriority
    }

    private fun drawSelectionOutline() {
        if (selectedTileX !in 0 until MapManager.MAP_WIDTH || selectedTileY !in 0 until MapManager.MAP_HEIGHT) return
        val sprite = GraphicsManager.getSprite("grid") ?: return
        sprite.setColor(Color(1f, 1f, 1f, 0.3f))
        sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
        sprite.setPosition(selectedTileX * MainScreenConfig.TILE_SIZE, selectedTileY * MainScreenConfig.TILE_SIZE)
        sprite.draw(batch)
        sprite.setColor(Color.WHITE)
    }

    private fun drawBuildings() {
        BuildingManager.getAllBuildings().forEach { building ->
            val sprite = GraphicsManager.getSprite(building.model.id) ?: return@forEach
            sprite.setSize(
                building.model.width * MainScreenConfig.TILE_SIZE,
                building.model.height * MainScreenConfig.TILE_SIZE
            )
            sprite.setPosition(
                building.x * MainScreenConfig.TILE_SIZE,
                building.y * MainScreenConfig.TILE_SIZE
            )
            sprite.color.a = if (building.isConstructing) 0.55f else 1f
            sprite.draw(batch)
            sprite.color.a = 1f
        }
    }

    private fun drawCrops() {
        CropManager.getAllCrops().forEach { crop ->
            drawProceduralCrop(crop)
        }
    }

    private fun drawProceduralCrop(crop: CropManager.CropInstance) {
        val fill = GraphicsManager.getSprite("terrain_fill") ?: return
        val profile = crop.getVisualProfile()
        val tileSize = MainScreenConfig.TILE_SIZE
        val tileX = crop.x * tileSize
        val tileY = crop.y * tileSize
        val centerX = tileX + tileSize * 0.5f
        val baseY = tileY + tileSize * 0.18f
        val stemHeight = tileSize * (0.18f + profile.heightFactor * 0.60f)
        val stemWidth = tileSize * (0.06f + profile.vitality * 0.04f)
        val topX = centerX + tileSize * profile.stemLean
        val topY = baseY + stemHeight
        val phase = profile.phase

        val stemColor = when (phase) {
            PlantPhase.SEED -> Color(0.42f, 0.31f, 0.20f, 1f)
            PlantPhase.DECAY -> Color(0.52f, 0.42f, 0.23f, 1f)
            else -> Color(0.25f, 0.68f, 0.29f, 1f)
        }
        val leafColor = when (crop.model.category) {
            "energy" -> Color(0.41f, 0.82f, 0.34f, 1f)
            "material" -> Color(0.58f, 0.72f, 0.63f, 1f)
            "defense" -> Color(0.33f, 0.68f, 0.30f, 1f)
            "medical" -> Color(0.36f, 0.86f, 0.56f, 1f)
            else -> Color(0.36f, 0.78f, 0.31f, 1f)
        }
        val bloomColor = when (crop.model.category) {
            "energy" -> Color(0.98f, 0.78f, 0.16f, 1f)
            "material" -> Color(0.93f, 0.86f, 0.40f, 1f)
            "defense" -> Color(0.89f, 0.18f, 0.22f, 1f)
            "medical" -> Color(0.32f, 0.58f, 1.0f, 1f)
            else -> Color(0.96f, 0.86f, 0.35f, 1f)
        }
        val fruitColor = when (crop.model.category) {
            "energy" -> Color(1.0f, 0.90f, 0.35f, 1f)
            "material" -> Color(0.81f, 0.74f, 0.64f, 1f)
            "defense" -> Color(0.97f, 0.35f, 0.30f, 1f)
            "medical" -> Color(0.56f, 0.72f, 1.0f, 1f)
            else -> Color(0.95f, 0.74f, 0.27f, 1f)
        }

        if (phase == PlantPhase.SEED) {
            drawPlantCircle(fill, centerX, tileY + tileSize * 0.17f, tileSize * 0.10f, stemColor)
            return
        }

        val midX = centerX + tileSize * profile.stemLean * 0.45f
        val midY = baseY + stemHeight * 0.55f
        drawPlantStem(fill, centerX, baseY, midX, midY, stemWidth, stemColor)
        drawPlantStem(fill, midX, midY, topX, topY, stemWidth * 0.88f, stemColor)

        when (crop.model.category) {
            "energy" -> drawSunBloomCrop(fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, bloomColor, fruitColor)
            "material" -> drawFerronCrop(fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, fruitColor)
            "defense" -> drawSpineCrop(fill, crop, profile, baseY, topX, topY, stemHeight, stemColor, bloomColor)
            "medical" -> drawMedCrop(fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, bloomColor, fruitColor)
            else -> drawGrainCrop(fill, crop, profile, centerX, baseY, topX, topY, stemHeight, stemColor, leafColor, bloomColor, fruitColor)
        }
    }

    private fun drawGrainCrop(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        profile: CropManager.PlantVisualProfile,
        centerX: Float,
        baseY: Float,
        topX: Float,
        topY: Float,
        stemHeight: Float,
        stemColor: Color,
        leafColor: Color,
        bloomColor: Color,
        fruitColor: Color
    ) {
        repeat(profile.leafCount) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / (profile.leafCount - 1f)
            val side = if (index % 2 == 0) -1f else 1f
            val leafY = baseY + stemHeight * (0.22f + t * 0.58f)
            val anchorX = centerX + (topX - centerX) * (0.18f + t * 0.58f)
            val leafSize = MainScreenConfig.TILE_SIZE * (0.11f + (1f - abs(t - 0.45f)) * 0.12f)
            val tipX = anchorX + side * leafSize * (1.1f + t * 0.25f)
            val tipY = leafY + leafSize * (0.15f - t * 0.08f)
            drawLeaf(fill, anchorX, leafY, tipX, tipY, leafSize, if (profile.phase == PlantPhase.DECAY) stemColor else leafColor)
        }

        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawFlower(fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.13f, bloomColor, stemColor, profile.flowerCount.coerceAtLeast(1))
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            repeat(profile.fruitCount.coerceAtLeast(2)) { index ->
                val side = if (index % 2 == 0) -1f else 1f
                val y = topY - MainScreenConfig.TILE_SIZE * (0.03f + index * 0.03f)
                drawPlantCircle(fill, topX + side * MainScreenConfig.TILE_SIZE * 0.08f, y, MainScreenConfig.TILE_SIZE * 0.045f, fruitColor)
            }
        }
        drawDispersalSeeds(fill, crop, topX, topY)
    }

    private fun drawSunBloomCrop(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        profile: CropManager.PlantVisualProfile,
        centerX: Float,
        baseY: Float,
        topX: Float,
        topY: Float,
        stemHeight: Float,
        leafColor: Color,
        bloomColor: Color,
        fruitColor: Color
    ) {
        repeat(profile.leafCount.coerceAtMost(4)) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val level = index / 2f
            val anchorY = baseY + stemHeight * (0.28f + level * 0.22f)
            val anchorX = centerX + (topX - centerX) * (0.28f + level * 0.12f)
            val size = MainScreenConfig.TILE_SIZE * (0.15f + level * 0.03f)
            drawLeaf(fill, anchorX, anchorY, anchorX + side * size * 1.35f, anchorY + size * 0.18f, size, leafColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawFlower(fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.20f, bloomColor, fruitColor, 5 + profile.flowerCount)
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            drawPlantCircle(fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.09f, Color(0.44f, 0.27f, 0.12f, 1f))
        }
        drawDispersalSeeds(fill, crop, topX, topY)
    }

    private fun drawFerronCrop(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        profile: CropManager.PlantVisualProfile,
        centerX: Float,
        baseY: Float,
        topX: Float,
        topY: Float,
        stemHeight: Float,
        leafColor: Color,
        fruitColor: Color
    ) {
        repeat(profile.leafCount.coerceAtMost(5)) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / profile.leafCount.toFloat()
            val side = if (index % 2 == 0) -1f else 1f
            val anchorY = baseY + stemHeight * (0.24f + t * 0.54f)
            val anchorX = centerX + (topX - centerX) * (0.20f + t * 0.38f)
            val size = MainScreenConfig.TILE_SIZE * (0.10f + t * 0.06f)
            drawCrystalLeaf(fill, anchorX, anchorY, anchorX + side * size * 1.05f, anchorY + size * 0.05f, size, leafColor)
        }
        repeat(profile.fruitCount.coerceAtLeast(2)) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val crystalX = topX + side * MainScreenConfig.TILE_SIZE * (0.05f + index * 0.05f)
            val crystalY = topY - MainScreenConfig.TILE_SIZE * (0.02f + index * 0.04f)
            drawCrystalLeaf(fill, crystalX, crystalY - MainScreenConfig.TILE_SIZE * 0.04f, crystalX, crystalY + MainScreenConfig.TILE_SIZE * 0.04f, MainScreenConfig.TILE_SIZE * 0.11f, fruitColor)
        }
        drawDispersalSeeds(fill, crop, topX, topY)
    }

    private fun drawSpineCrop(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        profile: CropManager.PlantVisualProfile,
        baseY: Float,
        topX: Float,
        topY: Float,
        stemHeight: Float,
        stemColor: Color,
        bloomColor: Color
    ) {
        val bulbY = baseY + stemHeight * 0.68f
        val bulbRadius = MainScreenConfig.TILE_SIZE * (0.11f + profile.heightFactor * 0.08f)
        drawPlantCircle(fill, topX, bulbY, bulbRadius, bloomColor)
        repeat(8) { index ->
            val angle = (Math.PI * 2.0 * index / 8.0).toFloat()
            val spikeBaseX = topX + cos(angle) * bulbRadius * 0.55f
            val spikeBaseY = bulbY + sin(angle) * bulbRadius * 0.55f
            val spikeTipX = topX + cos(angle) * bulbRadius * 1.45f
            val spikeTipY = bulbY + sin(angle) * bulbRadius * 1.45f
            drawPlantStem(fill, spikeBaseX, spikeBaseY, spikeTipX, spikeTipY, MainScreenConfig.TILE_SIZE * 0.018f, stemColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawPlantCircle(fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.06f, Color(0.98f, 0.82f, 0.52f, 1f))
        }
        drawDispersalSeeds(fill, crop, topX, topY)
    }

    private fun drawMedCrop(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        profile: CropManager.PlantVisualProfile,
        centerX: Float,
        baseY: Float,
        topX: Float,
        topY: Float,
        stemHeight: Float,
        leafColor: Color,
        bloomColor: Color,
        fruitColor: Color
    ) {
        repeat(profile.leafCount.coerceAtMost(6)) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / (profile.leafCount - 1f)
            val side = if (index % 2 == 0) -1f else 1f
            val anchorY = baseY + stemHeight * (0.18f + t * 0.60f)
            val anchorX = centerX + (topX - centerX) * (0.16f + t * 0.44f)
            val size = MainScreenConfig.TILE_SIZE * (0.13f + (1f - abs(t - 0.5f)) * 0.05f)
            drawRoundedLeaf(fill, anchorX, anchorY, anchorX + side * size * 0.95f, anchorY + size * 0.12f, size, leafColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawFlower(fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.14f, bloomColor, fruitColor, 4 + profile.flowerCount)
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            drawPlantCircle(fill, topX, topY - MainScreenConfig.TILE_SIZE * 0.02f, MainScreenConfig.TILE_SIZE * 0.05f, fruitColor)
        }
        drawDispersalSeeds(fill, crop, topX, topY)
    }

    private fun drawDispersalSeeds(
        fill: com.badlogic.gdx.graphics.g2d.Sprite,
        crop: CropManager.CropInstance,
        topX: Float,
        topY: Float
    ) {
        if (crop.getLifePhase() != PlantPhase.DISPERSAL) return
        repeat(3) { index ->
            val drift = ((crop.lifecycleTime * 1.5f + index) % 1f) * MainScreenConfig.TILE_SIZE * 0.26f
            drawPlantCircle(
                fill,
                topX + drift,
                topY + MainScreenConfig.TILE_SIZE * 0.05f + index * MainScreenConfig.TILE_SIZE * 0.04f,
                MainScreenConfig.TILE_SIZE * 0.035f,
                Color(0.96f, 0.92f, 0.72f, 0.85f)
            )
        }
    }

    private fun plantPhaseText(phase: PlantPhase): String {
        return when (phase) {
            PlantPhase.SEED -> LocalizationManager.tr("ui.plantPhase.seed")
            PlantPhase.SPROUT -> LocalizationManager.tr("ui.plantPhase.sprout")
            PlantPhase.JUVENILE -> LocalizationManager.tr("ui.plantPhase.juvenile")
            PlantPhase.MATURE -> LocalizationManager.tr("ui.plantPhase.mature")
            PlantPhase.FLOWERING -> LocalizationManager.tr("ui.plantPhase.flowering")
            PlantPhase.FRUITING -> LocalizationManager.tr("ui.plantPhase.fruiting")
            PlantPhase.DISPERSAL -> LocalizationManager.tr("ui.plantPhase.dispersal")
            PlantPhase.DECAY -> LocalizationManager.tr("ui.plantPhase.decay")
        }
    }

    private fun plantStateText(crop: CropManager.CropInstance): String {
        return when (crop.getLifePhase()) {
            PlantPhase.DISPERSAL -> LocalizationManager.format("ui.plantState.spread", crop.reproductionCooldown.coerceAtLeast(0f).toInt())
            PlantPhase.DECAY -> LocalizationManager.format("ui.plantState.decay", (14f - crop.decayTimer).coerceAtLeast(0f).toInt())
            else -> LocalizationManager.format(
                "ui.plantState.growth",
                ((1f - crop.getGrowthPercentage()).coerceAtLeast(0f) * crop.model.growthTime).toInt()
            )
        }
    }

    private fun drawPlantRect(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color
    ) {
        sprite.setColor(color)
        sprite.setSize(width, height)
        sprite.setPosition(x, y)
        sprite.draw(batch)
        sprite.setColor(Color.WHITE)
    }

    private fun drawPlantStem(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        thickness: Float,
        color: Color
    ) {
        val segments = 8
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = startX + (endX - startX) * t
            val y = startY + (endY - startY) * t
            drawPlantCircle(sprite, x, y, thickness * (0.62f - t * 0.12f), color)
        }
    }

    private fun drawLeaf(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        baseX: Float,
        baseY: Float,
        tipX: Float,
        tipY: Float,
        size: Float,
        color: Color
    ) {
        val segments = 6
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * sin(t * Math.PI).toFloat() * 0.42f
            drawPlantCircle(sprite, x, y, width.coerceAtLeast(size * 0.06f), color)
        }
    }

    private fun drawRoundedLeaf(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        baseX: Float,
        baseY: Float,
        tipX: Float,
        tipY: Float,
        size: Float,
        color: Color
    ) {
        val segments = 7
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * sin(t * Math.PI).toFloat() * 0.52f
            drawPlantCircle(sprite, x, y, width.coerceAtLeast(size * 0.08f), color)
        }
    }

    private fun drawCrystalLeaf(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        baseX: Float,
        baseY: Float,
        tipX: Float,
        tipY: Float,
        size: Float,
        color: Color
    ) {
        val segments = 5
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * (1f - abs(t - 0.5f) * 2f) * 0.34f
            drawPlantRect(sprite, x - width, y - size * 0.05f, width * 2f, size * 0.10f, color)
        }
    }

    private fun drawFlower(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        centerX: Float,
        centerY: Float,
        radius: Float,
        petalColor: Color,
        coreColor: Color,
        petalCount: Int
    ) {
        val petals = (4 + petalCount).coerceIn(5, 8)
        repeat(petals) { index ->
            val angle = (Math.PI * 2.0 * index / petals).toFloat()
            val px = centerX + cos(angle) * radius * 0.82f
            val py = centerY + sin(angle) * radius * 0.82f
            drawPlantCircle(sprite, px, py, radius * 0.46f, petalColor)
        }
        drawPlantCircle(sprite, centerX, centerY, radius * 0.42f, Color(0.47f, 0.30f, 0.16f, 1f))
        drawPlantCircle(sprite, centerX, centerY, radius * 0.22f, coreColor)
    }

    private fun drawPlantCircle(
        sprite: com.badlogic.gdx.graphics.g2d.Sprite,
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Color
    ) {
        if (radius <= 0f) return
        val step = (radius * 0.55f).coerceAtLeast(1.5f)
        var offsetY = -radius
        while (offsetY <= radius) {
            val width = kotlin.math.sqrt((radius * radius - offsetY * offsetY).coerceAtLeast(0f))
            drawPlantRect(
                sprite,
                centerX - width,
                centerY + offsetY - step * 0.5f,
                width * 2f,
                step,
                color
            )
            offsetY += step
        }
    }

    private fun drawEnemies() {
        EnemyManager.getAllEnemies().forEach { enemy ->
            val sprite = GraphicsManager.getSprite(enemy.model.id) ?: return@forEach
            sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
            sprite.setPosition(enemy.x - MainScreenConfig.TILE_SIZE / 2f, enemy.y - MainScreenConfig.TILE_SIZE / 2f)
            sprite.draw(batch)
        }
    }

    private fun updateHud() {
        val wood = ResourceManager.getResource("wood")
        val stone = ResourceManager.getResource("stone")
        val metal = ResourceManager.getResource("metal")
        val energy = ResourceManager.getResource("energy")
        val food = ResourceManager.getResource("food")

        resourceLabel.setText(LocalizationManager.format("ui.status.resources", wood, stone, metal, energy, food))
        statusLabel.setText(
            "${LocalizationManager.format(
                "ui.status.summary",
                GameManager.day,
                GameManager.colonyHp,
                EnemyManager.getWaveCount(),
                EnemyManager.getSecondsUntilNextWave().toInt(),
                TechManager.getResearchPoints()
            )}   ${LocalizationManager.format("ui.status.fps", Gdx.graphics.framesPerSecond)}"
        )
        objectiveLabel.setText(GameManager.getObjectiveSummary())
        modeLabel.setText(
            when (actionMode) {
                ActionMode.INSPECT -> LocalizationManager.tr("ui.mode.inspect")
                ActionMode.BUILD -> LocalizationManager.tr("ui.mode.build")
                ActionMode.PLANT -> LocalizationManager.tr("ui.mode.plant")
                ActionMode.GATHER -> LocalizationManager.tr("ui.mode.gather")
                ActionMode.HARVEST -> LocalizationManager.tr("ui.mode.harvest")
            }
        )

        val terrain = MapManager.getTerrainAt(selectedTileX, selectedTileY)
        val building = BuildingManager.getBuildingAt(selectedTileX, selectedTileY)
        val crop = CropManager.getCropAt(selectedTileX, selectedTileY)
        val gatherAmount = if (selectedTileX >= 0 && selectedTileY >= 0) MapManager.getGatherAmountAt(selectedTileX, selectedTileY) else 0

        selectionLabel.setText(
            buildString {
                appendLine(LocalizationManager.format("ui.tile.header", selectedTileX, selectedTileY))
                if (terrain != null) {
                    appendLine(LocalizationManager.format("ui.tile.terrain", MetadataText.terrainName(terrain)))
                    appendLine(MetadataText.terrainDescription(terrain))
                }
                if (building != null) {
                    appendLine(
                        LocalizationManager.format(
                            "ui.tile.building",
                            MetadataText.buildingName(building.model),
                            building.health,
                            building.maxHealth
                        )
                    )
                }
                if (crop != null) {
                    appendLine(
                        LocalizationManager.format(
                            "ui.tile.crop",
                            MetadataText.cropName(crop.model),
                            (crop.getGrowthPercentage() * 100).toInt()
                        )
                    )
                    appendLine(LocalizationManager.format("ui.tile.cropPhase", plantPhaseText(crop.getLifePhase())))
                    appendLine(LocalizationManager.format("ui.tile.cropState", plantStateText(crop)))
                }
                appendLine(LocalizationManager.format("ui.tile.gatherable", gatherAmount))
                if (terrain != null && building == null && crop == null && gatherAmount == 0) {
                    append(LocalizationManager.tr("ui.tile.exhausted"))
                }
            }
        )

        hintLabel.setText(
            when (actionMode) {
                ActionMode.INSPECT -> LocalizationManager.tr("ui.action.inspect")
                ActionMode.BUILD -> LocalizationManager.format(
                    "ui.action.build",
                    MetadataText.buildingName(MetadataManager.getBuilding(selectedBuildingId) ?: return)
                )
                ActionMode.PLANT -> LocalizationManager.format(
                    "ui.action.plant",
                    MetadataText.cropName(MetadataManager.getCrop(selectedCropId) ?: return)
                )
                ActionMode.GATHER -> LocalizationManager.tr("ui.action.gather")
                ActionMode.HARVEST -> LocalizationManager.tr("ui.action.harvest")
            }
        )
    }

    private fun handleKeyboard(delta: Float) {
        val moveSpeed = 520f * delta * camera.zoom
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.position.x -= moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.position.x += moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.position.y += moveSpeed
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.position.y -= moveSpeed

        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) showBuildDialog()
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) showCropDialog()
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) actionMode = ActionMode.GATHER
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) actionMode = ActionMode.HARVEST
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) actionMode = ActionMode.INSPECT
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) EnemyManager.spawnWave()
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) showTechDialog()
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            LocalizationManager.toggleLanguage()
            rebuildHud()
        }

        if (pendingScrollAmount != 0f) {
            camera.zoom = (camera.zoom + pendingScrollAmount * 0.08f).coerceIn(0.55f, 2.2f)
            pendingScrollAmount = 0f
        }

        clampCamera()
    }

    private fun handleMouseWorld() {
        val world = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        selectedTileX = (world.x / MainScreenConfig.TILE_SIZE).toInt()
        selectedTileY = (world.y / MainScreenConfig.TILE_SIZE).toInt()
    }

    private fun performWorldAction(screenX: Int, screenY: Int) {
        val world = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        val tileX = (world.x / MainScreenConfig.TILE_SIZE).toInt()
        val tileY = (world.y / MainScreenConfig.TILE_SIZE).toInt()
        if (tileX !in 0 until MapManager.MAP_WIDTH || tileY !in 0 until MapManager.MAP_HEIGHT) return

        when (actionMode) {
            ActionMode.INSPECT -> Unit
            ActionMode.BUILD -> {
                val model = MetadataManager.getBuilding(selectedBuildingId) ?: return
                if (BuildingManager.createBuilding(selectedBuildingId, tileX, tileY) != null) {
                    showToast(LocalizationManager.format("toast.placed", MetadataText.buildingName(model)))
                } else {
                    showToast(LocalizationManager.format("toast.cannotPlace", MetadataText.buildingName(model)))
                }
            }
            ActionMode.PLANT -> {
                val model = MetadataManager.getCrop(selectedCropId) ?: return
                if (CropManager.plantCrop(selectedCropId, tileX, tileY) != null) {
                    showToast(LocalizationManager.format("toast.planted", MetadataText.cropName(model)))
                } else {
                    showToast(LocalizationManager.format("toast.cannotPlant", MetadataText.cropName(model)))
                }
            }
            ActionMode.GATHER -> {
                val result = MapManager.gatherAt(tileX, tileY)
                if (result.isEmpty()) {
                    showToast(LocalizationManager.tr("toast.nothingToGather"))
                } else {
                    result.forEach { (resourceId, amount) -> ResourceManager.addResource(resourceId, amount) }
                    val summary = result.entries.joinToString { entry ->
                        val resource = MetadataManager.getResource(entry.key)
                        val localizedName = if (resource != null) MetadataText.resourceName(resource) else entry.key
                        "${entry.value} $localizedName"
                    }
                    showToast(LocalizationManager.format("toast.gathered", summary))
                }
            }
            ActionMode.HARVEST -> {
                val crop = CropManager.getCropAt(tileX, tileY)
                if (crop != null && CropManager.harvestCrop(crop)) {
                    showToast(LocalizationManager.tr("toast.harvestComplete"))
                } else {
                    showToast(LocalizationManager.tr("toast.cropNotReady"))
                }
            }
        }
    }

    private fun isPointerOverUi(screenX: Float, screenY: Float): Boolean {
        return stage?.hit(screenX, Gdx.graphics.height - screenY, true) != null
    }

    private fun clampCamera() {
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f
        if (worldWidth() <= halfWidth * 2f) {
            camera.position.x = worldWidth() / 2f
        } else {
            camera.position.x = camera.position.x.coerceIn(halfWidth, max(halfWidth, worldWidth() - halfWidth))
        }
        if (worldHeight() <= halfHeight * 2f) {
            camera.position.y = worldHeight() / 2f
        } else {
            camera.position.y = camera.position.y.coerceIn(halfHeight, max(halfHeight, worldHeight() - halfHeight))
        }
    }

    private fun showBuildDialog() {
        val dialog = Dialog(LocalizationManager.tr("ui.dialog.build"), skin)
        val content = Table()
        content.defaults().pad(6f).width(240f).height(52f)
        MetadataManager.getAllBuildings().values
            .sortedBy { it.category + it.name }
            .forEach { building ->
                val unlocked = TechManager.isBuildingUnlocked(building.id)
                val buttonText = if (unlocked) {
                    "${MetadataText.buildingName(building)}  ${formatCost(building.cost)}"
                } else {
                    LocalizationManager.format("ui.build.locked", MetadataText.buildingName(building))
                }
                val button = TextButton(buttonText, skin)
                button.isDisabled = !unlocked
                button.addListener(object : ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        selectedBuildingId = building.id
                        actionMode = ActionMode.BUILD
                        dialog.hide()
                        showToast(LocalizationManager.format("toast.buildMode", MetadataText.buildingName(building)))
                    }
                })
                content.add(button).row()
            }
        dialog.contentTable.add(ScrollPane(content, skin)).width(500f).height(360f)
        dialog.button(LocalizationManager.tr("ui.button.close"))
        dialog.show(stage)
    }

    private fun showCropDialog() {
        val dialog = Dialog(LocalizationManager.tr("ui.dialog.crop"), skin)
        val content = Table()
        content.defaults().pad(6f).width(240f).height(52f)
        MetadataManager.getAllCrops().values.sortedBy { it.name }.forEach { crop ->
            val unlocked = TechManager.isCropUnlocked(crop.id)
            val text = if (unlocked) {
                LocalizationManager.format("ui.crop.item", MetadataText.cropName(crop), crop.growthTime.toInt())
            } else {
                LocalizationManager.format("ui.crop.locked", MetadataText.cropName(crop))
            }
            val button = TextButton(text, skin)
            button.isDisabled = !unlocked
            button.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    selectedCropId = crop.id
                    actionMode = ActionMode.PLANT
                    dialog.hide()
                    showToast(LocalizationManager.format("toast.plantMode", MetadataText.cropName(crop)))
                }
            })
            content.add(button).row()
        }
        dialog.contentTable.add(content).width(420f)
        dialog.button(LocalizationManager.tr("ui.button.close"))
        dialog.show(stage)
    }

    private fun showTechDialog() {
        val dialog = Dialog(LocalizationManager.tr("ui.dialog.tech"), skin)
        val content = Table()
        content.defaults().pad(6f).left()

        MetadataManager.getAllTechBranches().values
            .sortedBy { it.name }
            .forEach { branch ->
                content.add(Label(MetadataText.techBranchName(branch), skin)).left().padTop(10f).row()
                branch.techIds.forEach techLoop@{ techId ->
                    val tech = MetadataManager.getTech(techId) ?: return@techLoop
                    val available = TechManager.getAvailableTechs().any { it.id == tech.id }
                    val researched = TechManager.isTechResearched(tech.id)
                    val buttonText = when {
                        researched -> LocalizationManager.format("ui.tech.done", MetadataText.techName(tech))
                        available -> LocalizationManager.format("ui.tech.cost", MetadataText.techName(tech), tech.cost)
                        else -> LocalizationManager.format("ui.tech.locked", MetadataText.techName(tech))
                    }
                    val button = TextButton(buttonText, skin)
                    button.isDisabled = !available
                    button.addListener(object : ClickListener() {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                            if (TechManager.researchTech(tech.id)) {
                                showToast(LocalizationManager.format("toast.researched", MetadataText.techName(tech)))
                                dialog.hide()
                            }
                        }
                    })
                    content.add(button).width(340f).row()
                }
            }

        dialog.contentTable.add(ScrollPane(content, skin)).width(400f).height(360f)
        dialog.button(LocalizationManager.tr("ui.button.close"))
        dialog.show(stage)
    }

    private fun showGameOverDialog() {
        if (gameOverDialogShown) return
        gameOverDialogShown = true
        val dialog = Dialog(LocalizationManager.tr("ui.dialog.gameOver"), skin)
        dialog.text(LocalizationManager.tr("gameOver.text"))
        val restart = TextButton(LocalizationManager.tr("ui.button.restart"), skin)
        restart.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                GameManager.reset()
                game.startScreen(MainScreen(game))
            }
        })
        dialog.button(restart)
        dialog.show(stage)
    }

    private fun showWinDialog() {
        if (winDialogShown) return
        winDialogShown = true
        val dialog = Dialog(LocalizationManager.tr("ui.dialog.win"), skin)
        dialog.text(LocalizationManager.tr("win.text"))
        val restart = TextButton(LocalizationManager.tr("ui.button.playAgain"), skin)
        restart.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                GameManager.reset()
                game.startScreen(MainScreen(game))
            }
        })
        dialog.button(restart)
        dialog.show(stage)
    }

    private fun formatCost(cost: Map<String, Int>): String {
        return cost.entries.joinToString(" ") { "${it.key}:${it.value}" }
    }

    private fun showToast(text: String) {
        toastLabel.setText(text)
        toastTimer = 4f
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


    private fun updateToast(delta: Float) {
        if (toastTimer > 0f) {
            toastTimer -= delta
            if (toastTimer <= 0f) {
                toastLabel.setText("")
            }
        }
    }

    private fun worldWidth(): Float = MapManager.MAP_WIDTH * MainScreenConfig.TILE_SIZE

    private fun worldHeight(): Float = MapManager.MAP_HEIGHT * MainScreenConfig.TILE_SIZE

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        clampCamera()
    }

    override fun dispose() {
        batch.dispose()
        disposeSkinSafely()
        super.dispose()
    }

    private fun disposeSkinSafely() {
        if (!::skin.isInitialized) return
        if (skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)) {
            skin.remove("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
        }
        if (skin.has("default", LabelStyle::class.java)) {
            skin.remove("default", LabelStyle::class.java)
        }
        if (skin.has("muted", LabelStyle::class.java)) {
            skin.remove("muted", LabelStyle::class.java)
        }
        if (skin.has("accent", LabelStyle::class.java)) {
            skin.remove("accent", LabelStyle::class.java)
        }
        if (skin.has("section", LabelStyle::class.java)) {
            skin.remove("section", LabelStyle::class.java)
        }
        if (skin.has("default", TextButtonStyle::class.java)) {
            skin.remove("default", TextButtonStyle::class.java)
        }
        if (skin.has("primary", TextButtonStyle::class.java)) {
            skin.remove("primary", TextButtonStyle::class.java)
        }
        if (skin.has("default", WindowStyle::class.java)) {
            skin.remove("default", WindowStyle::class.java)
        }
        if (skin.has("default", ScrollPaneStyle::class.java)) {
            skin.remove("default", ScrollPaneStyle::class.java)
        }
        skin.dispose()
    }

    private enum class ActionMode {
        INSPECT,
        BUILD,
        PLANT,
        GATHER,
        HARVEST
    }
}
