package person.wangchen11.planet.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import person.wangchen11.gdx.assets.GraphicsManager
import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.gdx.game.screen.BaseScreen
import person.wangchen11.planet.game.MainScreenConfig
import person.wangchen11.planet.game.MapManager
import java.io.File

class TerrainTransitionTestScreen(
    game: BaseGame,
    private val maskOnlyMode: Boolean = false
) : BaseScreen(game) {
    private val batch = SpriteBatch()
    private val camera = OrthographicCamera()
    private val testWidth = 44
    private val testHeight = 28
    private val terrainIds = Array(testHeight) { Array(testWidth) { "plain" } }
    private val terrainVariants = Array(testHeight) { IntArray(testWidth) }
    private var screenshotCaptured = false
    private var elapsedTime = 0f

    override fun show() {
        super.show()
        GraphicsManager.initialize()
        buildTestPattern()

        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(testWorldWidth() / 2f, testWorldHeight() / 2f, 0f)
        camera.zoom = 1f
        camera.update()
    }

    override fun render(delta: Float) {
        elapsedTime += delta
        if (maskOnlyMode) {
            Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        } else {
            Gdx.gl.glClearColor(0.09f, 0.11f, 0.12f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        batch.begin()
        drawTerrain()
        batch.end()

        if (!screenshotCaptured && elapsedTime > 0.35f) {
            saveScreenshot()
            screenshotCaptured = true
            Gdx.app.exit()
        }
    }

    private fun buildTestPattern() {
        fillRect(0, 0, testWidth, testHeight, "plain")

        fillRect(2, 2, 12, 9, "forest")
        fillRect(7, 6, 3, 2, "plain") // inner notch
        fillRect(4, 10, 3, 5, "forest") // straight edge test
        fillRect(14, 3, 11, 8, "desert")
        fillRect(18, 7, 2, 5, "plain") // vertical cut
        fillRect(27, 2, 12, 11, "mountain")
        fillRect(31, 6, 4, 3, "plain") // concave bay
        fillRect(3, 16, 13, 8, "swamp")
        fillRect(8, 19, 2, 2, "plain")
        fillRect(19, 16, 11, 7, "forest")
        fillRect(21, 18, 2, 2, "mountain")
        fillRect(31, 16, 10, 8, "lava")
        fillRect(35, 18, 2, 3, "plain")

        // Single-tile spikes and corners that are easy to inspect on the screenshot.
        terrainIds[12][10] = "forest"
        terrainIds[12][11] = "plain"
        terrainIds[12][12] = "forest"
        terrainIds[20][26] = "desert"
        terrainIds[20][27] = "mountain"
        terrainIds[20][28] = "desert"
        terrainIds[24][34] = "lava"
        terrainIds[23][34] = "plain"
        terrainIds[24][35] = "plain"

        for (y in 0 until testHeight) {
            for (x in 0 until testWidth) {
                val id = terrainIds[y][x]
                terrainVariants[y][x] = terrainVariantFor(x, y, id)
            }
        }
    }

    private fun fillRect(x: Int, y: Int, width: Int, height: Int, terrainId: String) {
        for (ty in y until (y + height).coerceAtMost(testHeight)) {
            for (tx in x until (x + width).coerceAtMost(testWidth)) {
                terrainIds[ty][tx] = terrainId
            }
        }
    }

    private fun drawTerrain() {
        val fillSprite = GraphicsManager.getSprite("terrain_fill")
        val fallbackSprite = GraphicsManager.getSprite("grid")

        for (y in 0 until testHeight) {
            for (x in 0 until testWidth) {
                val terrainId = terrainIds[y][x]
                if (fillSprite != null) {
                    fillSprite.setColor(if (maskOnlyMode) Color(0.18f, 0.18f, 0.18f, 1f) else MapManager.getTerrainColor(terrainId))
                    fillSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    fillSprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                    fillSprite.draw(batch)
                    fillSprite.setColor(Color.WHITE)
                }

                if (maskOnlyMode) {
                    continue
                }

                val sprite = GraphicsManager.getSprite("terrain_${terrainId}_${terrainVariants[y][x]}") ?: fallbackSprite
                if (sprite != null) {
                    if (sprite === fallbackSprite) {
                        sprite.setColor(MapManager.getTerrainColor(terrainId))
                    } else {
                        sprite.setColor(1f, 1f, 1f, 0.42f)
                    }
                    sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    sprite.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                    sprite.draw(batch)
                    sprite.setColor(Color.WHITE)
                }
            }
        }

        for (y in 0 until testHeight) {
            for (x in 0 until testWidth) {
                val baseTerrainId = terrainIds[y][x]
                val basePriority = MapManager.getTerrainPriority(baseTerrainId)
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
                    if (maskOnlyMode) {
                        maskSprite.setColor(1f, 0.15f, 0.15f, 1f)
                    } else {
                        val color = MapManager.getTerrainColor(targetTerrainId)
                        maskSprite.setColor(color.r, color.g, color.b, 0.96f)
                    }
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
            terrainAt(tileX, tileY + 1),
            terrainAt(tileX + 1, tileY),
            terrainAt(tileX, tileY - 1),
            terrainAt(tileX - 1, tileY),
            terrainAt(tileX - 1, tileY + 1),
            terrainAt(tileX + 1, tileY + 1),
            terrainAt(tileX + 1, tileY - 1),
            terrainAt(tileX - 1, tileY - 1)
        )
            .distinct()
            .filter { MapManager.getTerrainPriority(it) > basePriority }
            .sortedBy { MapManager.getTerrainPriority(it) }
    }

    private fun terrainAt(x: Int, y: Int): String? {
        if (x !in 0 until testWidth || y !in 0 until testHeight) return null
        return terrainIds[y][x]
    }

    private fun shouldBlendTo(tileX: Int, tileY: Int, basePriority: Int, targetTerrainId: String): Boolean {
        val neighbor = terrainAt(tileX, tileY) ?: return false
        return neighbor == targetTerrainId && MapManager.getTerrainPriority(neighbor) > basePriority
    }

    private fun terrainVariantFor(x: Int, y: Int, terrainId: String): Int {
        val seed = (x * 92821 + y * 68917 + terrainId.hashCode()).toUInt().toInt()
        return (seed and 3) % 3
    }

    private fun saveScreenshot() {
        val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        val flipped = Pixmap(pixmap.width, pixmap.height, pixmap.format)
        for (y in 0 until pixmap.height) {
            for (x in 0 until pixmap.width) {
                flipped.drawPixel(x, pixmap.height - 1 - y, pixmap.getPixel(x, y))
            }
        }

        val outputDir = File(File(System.getProperty("user.dir")).parentFile, "debug-screenshots")
        outputDir.mkdirs()
        val outputFile = File(outputDir, if (maskOnlyMode) "terrain-transition-mask-test.png" else "terrain-transition-test.png")
        PixmapIO.writePNG(Gdx.files.absolute(outputFile.absolutePath), flipped)
        pixmap.dispose()
        flipped.dispose()
        Gdx.app.log("TerrainTransitionTest", "Saved screenshot: ${outputFile.absolutePath}")
    }

    private fun testWorldWidth(): Float = testWidth * MainScreenConfig.TILE_SIZE

    private fun testWorldHeight(): Float = testHeight * MainScreenConfig.TILE_SIZE

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}
