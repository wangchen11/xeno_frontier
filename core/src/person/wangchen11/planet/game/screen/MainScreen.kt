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
import person.wangchen11.planet.game.EnemyManager
import person.wangchen11.planet.game.GameManager
import person.wangchen11.planet.game.MainScreenConfig
import person.wangchen11.planet.game.MapManager
import person.wangchen11.planet.game.ResourceManager
import person.wangchen11.planet.game.TechManager
import person.wangchen11.planet.i18n.LocalizationManager
import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.MetadataText
import kotlin.math.max

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
        currentSkin.add("button-up", ColorDrawable(Color(0.13f, 0.18f, 0.20f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-down", ColorDrawable(Color(0.19f, 0.28f, 0.31f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-over", ColorDrawable(Color(0.16f, 0.24f, 0.27f, 1f), 8f), Drawable::class.java)
        currentSkin.add("button-disabled", ColorDrawable(Color(0.10f, 0.12f, 0.13f, 0.92f), 8f), Drawable::class.java)
        currentSkin.add("scroll-bg", ColorDrawable(Color(0.04f, 0.05f, 0.06f, 0.55f), 6f), Drawable::class.java)
        currentSkin.add("scroll-knob", ColorDrawable(Color(0.38f, 0.61f, 0.52f, 0.95f), 6f), Drawable::class.java)

        currentSkin.add("default", Label.LabelStyle(font, Color.WHITE))

        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = font
        buttonStyle.fontColor = Color.WHITE
        buttonStyle.disabledFontColor = Color(0.55f, 0.57f, 0.58f, 1f)
        buttonStyle.up = currentSkin.getDrawable("button-up")
        buttonStyle.down = currentSkin.getDrawable("button-down")
        buttonStyle.over = currentSkin.getDrawable("button-over")
        buttonStyle.disabled = currentSkin.getDrawable("button-disabled")
        currentSkin.add("default", buttonStyle)

        val windowStyle = Window.WindowStyle()
        windowStyle.titleFont = font
        windowStyle.titleFontColor = Color(0.72f, 0.95f, 0.79f, 1f)
        windowStyle.background = currentSkin.getDrawable("panel")
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
        top.background = skin.getDrawable("panel")
        top.defaults().pad(8f)
        resourceLabel = Label("", skin)
        statusLabel = Label("", skin)
        top.add(resourceLabel).expandX().left()
        top.add(statusLabel).right()
        root.add(top).expandX().fillX().row()

        objectiveLabel = Label("", skin)
        objectiveLabel.setWrap(true)
        objectiveLabel.color = Color(0.72f, 0.95f, 0.79f, 1f)
        root.add(objectiveLabel).expandX().fillX().pad(4f, 8f, 0f, 8f).row()

        val bottom = Table()
        bottom.background = skin.getDrawable("panel")
        bottom.defaults().pad(6f)
        bottom.add(menuButton(LocalizationManager.tr("ui.button.build")) { showBuildDialog() })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.plant")) { showCropDialog() })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.gather")) { actionMode = ActionMode.GATHER })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.harvest")) { actionMode = ActionMode.HARVEST })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.inspect")) { actionMode = ActionMode.INSPECT })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.tech")) { showTechDialog() })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.wave")) { EnemyManager.spawnWave() })
        bottom.add(menuButton(LocalizationManager.tr("ui.button.language")) {
            LocalizationManager.toggleLanguage()
            rebuildHud()
        })

        val bottomWrap = Table()
        bottomWrap.setFillParent(true)
        bottomWrap.bottom()
        bottomWrap.add(bottom).pad(12f)
        stage?.addActor(bottomWrap)

        val side = Table()
        side.background = skin.getDrawable("panel")
        side.defaults().pad(8f).left()
        selectionLabel = Label("", skin)
        selectionLabel.setAlignment(Align.topLeft)
        selectionLabel.setWrap(true)
        hintLabel = Label("", skin)
        hintLabel.setWrap(true)
        toastLabel = Label("", skin)
        toastLabel.setWrap(true)
        toastLabel.color = Color.GOLD
        side.add(Label(LocalizationManager.tr("ui.section.sectorIntel"), skin)).left().row()
        side.add(selectionLabel).width(250f).row()
        side.add(Label(LocalizationManager.tr("ui.section.currentAction"), skin)).left().padTop(8f).row()
        side.add(hintLabel).width(250f).row()
        side.add(Label(LocalizationManager.tr("ui.section.log"), skin)).left().padTop(8f).row()
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
        handleKeyboard(delta)
        handleMouseWorld()
        GameManager.update(delta)
        updateToast(delta)

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        batch.begin()
        drawTerrain()
        drawSelectionOutline()
        drawBuildings()
        drawCrops()
        drawEnemies()
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

                var overlayTerrainId: String? = null
                var overlayPriority = basePriority
                val neighbors = listOf(
                    MapManager.getTerrainAt(x, y + 1),
                    MapManager.getTerrainAt(x + 1, y),
                    MapManager.getTerrainAt(x, y - 1),
                    MapManager.getTerrainAt(x - 1, y),
                    MapManager.getTerrainAt(x - 1, y + 1),
                    MapManager.getTerrainAt(x + 1, y + 1),
                    MapManager.getTerrainAt(x + 1, y - 1),
                    MapManager.getTerrainAt(x - 1, y - 1)
                )
                neighbors.forEach { neighbor ->
                    if (neighbor == null) return@forEach
                    val priority = MapManager.getTerrainPriority(neighbor.id)
                    if (priority > overlayPriority) {
                        overlayPriority = priority
                        overlayTerrainId = neighbor.id
                    }
                }

                val targetTerrainId = overlayTerrainId ?: continue
                var mask = 0
                if (shouldBlendTo(x, y + 1, basePriority, targetTerrainId)) mask = mask or 1
                if (shouldBlendTo(x + 1, y, basePriority, targetTerrainId)) mask = mask or 2
                if (shouldBlendTo(x, y - 1, basePriority, targetTerrainId)) mask = mask or 4
                if (shouldBlendTo(x - 1, y, basePriority, targetTerrainId)) mask = mask or 8
                if (shouldBlendTo(x - 1, y + 1, basePriority, targetTerrainId)) mask = mask or 16
                if (shouldBlendTo(x + 1, y + 1, basePriority, targetTerrainId)) mask = mask or 32
                if (shouldBlendTo(x + 1, y - 1, basePriority, targetTerrainId)) mask = mask or 64
                if (shouldBlendTo(x - 1, y - 1, basePriority, targetTerrainId)) mask = mask or 128
                if (mask == 0) continue

                val maskSprite = GraphicsManager.getSprite("terrain_mask_$mask") ?: continue
                val color = MapManager.getTerrainColor(targetTerrainId)
                maskSprite.setColor(color.r, color.g, color.b, 0.96f)
                maskSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                maskSprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                maskSprite.draw(batch)
                maskSprite.setColor(Color.WHITE)
            }
        }
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
            val sprite = GraphicsManager.getSprite(crop.model.id) ?: return@forEach
            val scale = 0.55f + crop.getGrowthPercentage() * 0.65f
            val size = 16f * scale
            sprite.setSize(size, size)
            sprite.setPosition(
                crop.x * MainScreenConfig.TILE_SIZE + (MainScreenConfig.TILE_SIZE - size) / 2f,
                crop.y * MainScreenConfig.TILE_SIZE + (MainScreenConfig.TILE_SIZE - size) / 2f
            )
            sprite.color.a = 0.6f + crop.getGrowthPercentage() * 0.4f
            sprite.draw(batch)
            sprite.color.a = 1f
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
            LocalizationManager.format(
                "ui.status.summary",
                GameManager.day,
                GameManager.colonyHp,
                EnemyManager.getWaveCount(),
                EnemyManager.getSecondsUntilNextWave().toInt(),
                TechManager.getResearchPoints()
            )
        )
        objectiveLabel.setText(GameManager.getObjectiveSummary())

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
        if (skin.has("default", TextButtonStyle::class.java)) {
            skin.remove("default", TextButtonStyle::class.java)
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
