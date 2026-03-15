package person.wangchen11.planet.game.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import person.wangchen11.gdx.assets.GraphicsManager
import person.wangchen11.planet.game.BuildingManager
import person.wangchen11.planet.game.CropManager
import person.wangchen11.planet.game.CropManager.PlantPhase
import person.wangchen11.planet.game.EnemyManager
import person.wangchen11.planet.game.MainScreenConfig
import person.wangchen11.planet.game.MapManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object WorldSceneRenderer {
    const val ENABLE_TERRAIN_CHUNK_CACHE = true
    private const val TILE_PADDING = 2
    private const val TERRAIN_CHUNK_SIZE = 16
    private const val TERRAIN_CHUNK_BORDER = 1
    private val terrainChunkMatrix = Matrix4()
    private val terrainCacheBatch = SpriteBatch()
    private val terrainChunks = mutableMapOf<Pair<Int, Int>, TerrainChunk>()
    private var terrainCacheReady = false
    private var cachedMapWidth = -1
    private var cachedMapHeight = -1

    private data class TerrainChunk(
        val chunkX: Int,
        val chunkY: Int,
        val drawWidth: Int,
        val drawHeight: Int,
        val frameBuffer: FrameBuffer,
        val region: TextureRegion
    )

    data class ViewBounds(
        val minTileX: Int,
        val minTileY: Int,
        val maxTileX: Int,
        val maxTileY: Int
    ) {
        fun containsTile(tileX: Int, tileY: Int): Boolean {
            return tileX in minTileX..maxTileX && tileY in minTileY..maxTileY
        }

        fun intersectsTiles(tileX: Int, tileY: Int, width: Int, height: Int): Boolean {
            val endX = tileX + width - 1
            val endY = tileY + height - 1
            return endX >= minTileX && tileX <= maxTileX && endY >= minTileY && tileY <= maxTileY
        }

        companion object {
            fun fromWorldRect(minX: Float, minY: Float, maxX: Float, maxY: Float): ViewBounds {
                val tileSize = MainScreenConfig.TILE_SIZE
                val minTileX = ((minX / tileSize).toInt() - TILE_PADDING).coerceIn(0, MapManager.MAP_WIDTH - 1)
                val minTileY = ((minY / tileSize).toInt() - TILE_PADDING).coerceIn(0, MapManager.MAP_HEIGHT - 1)
                val maxTileX = ((maxX / tileSize).toInt() + TILE_PADDING).coerceIn(0, MapManager.MAP_WIDTH - 1)
                val maxTileY = ((maxY / tileSize).toInt() + TILE_PADDING).coerceIn(0, MapManager.MAP_HEIGHT - 1)
                return ViewBounds(minTileX, minTileY, maxTileX, maxTileY)
            }
        }
    }

    fun invalidateTerrainCache() {
        terrainChunks.values.forEach { it.frameBuffer.dispose() }
        terrainChunks.clear()
        terrainCacheReady = false
        cachedMapWidth = -1
        cachedMapHeight = -1
    }

    fun prepareTerrainCache() {
        if (!ENABLE_TERRAIN_CHUNK_CACHE) {
            invalidateTerrainCache()
            return
        }
        if (terrainCacheReady &&
            cachedMapWidth == MapManager.MAP_WIDTH &&
            cachedMapHeight == MapManager.MAP_HEIGHT
        ) {
            return
        }

        invalidateTerrainCache()
        cachedMapWidth = MapManager.MAP_WIDTH
        cachedMapHeight = MapManager.MAP_HEIGHT

        val chunkCountX = (MapManager.MAP_WIDTH + TERRAIN_CHUNK_SIZE - 1) / TERRAIN_CHUNK_SIZE
        val chunkCountY = (MapManager.MAP_HEIGHT + TERRAIN_CHUNK_SIZE - 1) / TERRAIN_CHUNK_SIZE

        for (chunkY in 0 until chunkCountY) {
            for (chunkX in 0 until chunkCountX) {
                val tileStartX = chunkX * TERRAIN_CHUNK_SIZE
                val tileStartY = chunkY * TERRAIN_CHUNK_SIZE
                val tilesWide = minOf(TERRAIN_CHUNK_SIZE, MapManager.MAP_WIDTH - tileStartX)
                val tilesHigh = minOf(TERRAIN_CHUNK_SIZE, MapManager.MAP_HEIGHT - tileStartY)
                val borderPixels = (TERRAIN_CHUNK_BORDER * MainScreenConfig.TILE_SIZE).toInt()
                val pixelWidth = (tilesWide * MainScreenConfig.TILE_SIZE).toInt().coerceAtLeast(1) + borderPixels * 2
                val pixelHeight = (tilesHigh * MainScreenConfig.TILE_SIZE).toInt().coerceAtLeast(1) + borderPixels * 2
                val frameBuffer = FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, pixelWidth, pixelHeight, false)
                frameBuffer.colorBufferTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
                frameBuffer.colorBufferTexture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
                frameBuffer.begin()
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
                terrainChunkMatrix.setToOrtho2D(0f, 0f, pixelWidth.toFloat(), pixelHeight.toFloat())
                terrainCacheBatch.projectionMatrix = terrainChunkMatrix
                terrainCacheBatch.begin()
                val renderStartX = (tileStartX - TERRAIN_CHUNK_BORDER).coerceAtLeast(0)
                val renderStartY = (tileStartY - TERRAIN_CHUNK_BORDER).coerceAtLeast(0)
                val renderEndX = (tileStartX + tilesWide - 1 + TERRAIN_CHUNK_BORDER).coerceAtMost(MapManager.MAP_WIDTH - 1)
                val renderEndY = (tileStartY + tilesHigh - 1 + TERRAIN_CHUNK_BORDER).coerceAtMost(MapManager.MAP_HEIGHT - 1)
                val renderTilesWide = renderEndX - renderStartX + 1
                val renderTilesHigh = renderEndY - renderStartY + 1
                val cacheOriginX = tileStartX - TERRAIN_CHUNK_BORDER
                val cacheOriginY = tileStartY - TERRAIN_CHUNK_BORDER
                drawTerrainTiles(terrainCacheBatch, renderStartX, renderStartY, renderTilesWide, renderTilesHigh, includeGatherMarkers = false, originX = cacheOriginX, originY = cacheOriginY)
                drawTerrainTransitionTiles(terrainCacheBatch, renderStartX, renderStartY, renderTilesWide, renderTilesHigh, originX = cacheOriginX, originY = cacheOriginY)
                terrainCacheBatch.end()
                frameBuffer.end()
                val regionWidth = (tilesWide * MainScreenConfig.TILE_SIZE).toInt()
                val regionHeight = (tilesHigh * MainScreenConfig.TILE_SIZE).toInt()
                val region = TextureRegion(
                    frameBuffer.colorBufferTexture,
                    borderPixels,
                    borderPixels,
                    regionWidth,
                    regionHeight
                )
                region.flip(false, true)
                terrainChunks[chunkX to chunkY] = TerrainChunk(
                    chunkX,
                    chunkY,
                    regionWidth,
                    regionHeight,
                    frameBuffer,
                    region
                )
            }
        }

        terrainCacheReady = true
    }

    fun drawWorld(batch: SpriteBatch, viewBounds: ViewBounds) {
        drawTerrain(batch, viewBounds)
        drawBuildings(batch, viewBounds)
        drawCrops(batch, viewBounds)
        drawEnemies(batch, viewBounds)
    }

    fun drawSelectionOutline(batch: SpriteBatch, tileX: Int, tileY: Int) {
        if (tileX !in 0 until MapManager.MAP_WIDTH || tileY !in 0 until MapManager.MAP_HEIGHT) return
        val sprite = GraphicsManager.getSprite("grid") ?: return
        sprite.setColor(Color(1f, 1f, 1f, 0.3f))
        sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
        sprite.setPosition(tileX * MainScreenConfig.TILE_SIZE, tileY * MainScreenConfig.TILE_SIZE)
        sprite.draw(batch)
        sprite.setColor(Color.WHITE)
    }

    private fun drawTerrain(batch: SpriteBatch, viewBounds: ViewBounds) {
        if (ENABLE_TERRAIN_CHUNK_CACHE) {
            if (!terrainCacheReady) {
                prepareTerrainCache()
            }

            val chunkMinX = (viewBounds.minTileX / TERRAIN_CHUNK_SIZE).coerceAtLeast(0)
            val chunkMinY = (viewBounds.minTileY / TERRAIN_CHUNK_SIZE).coerceAtLeast(0)
            val chunkMaxX = (viewBounds.maxTileX / TERRAIN_CHUNK_SIZE).coerceAtLeast(chunkMinX)
            val chunkMaxY = (viewBounds.maxTileY / TERRAIN_CHUNK_SIZE).coerceAtLeast(chunkMinY)

            batch.flush()
            batch.disableBlending()
            for (chunkY in chunkMinY..chunkMaxY) {
                for (chunkX in chunkMinX..chunkMaxX) {
                    val chunk = terrainChunks[chunkX to chunkY] ?: continue
                    batch.draw(
                        chunk.region,
                        chunk.chunkX * TERRAIN_CHUNK_SIZE * MainScreenConfig.TILE_SIZE,
                        chunk.chunkY * TERRAIN_CHUNK_SIZE * MainScreenConfig.TILE_SIZE,
                        chunk.drawWidth.toFloat(),
                        chunk.drawHeight.toFloat()
                    )
                }
            }
            batch.flush()
            batch.enableBlending()
            drawGatherMarkers(batch, viewBounds)
            return
        }

        drawTerrainTiles(
            batch,
            viewBounds.minTileX,
            viewBounds.minTileY,
            viewBounds.maxTileX - viewBounds.minTileX + 1,
            viewBounds.maxTileY - viewBounds.minTileY + 1,
            includeGatherMarkers = false,
            originX = 0,
            originY = 0
        )
        drawTerrainTransitionTiles(
            batch,
            viewBounds.minTileX,
            viewBounds.minTileY,
            viewBounds.maxTileX - viewBounds.minTileX + 1,
            viewBounds.maxTileY - viewBounds.minTileY + 1,
            originX = 0,
            originY = 0
        )
        drawGatherMarkers(batch, viewBounds)
    }

    private fun drawTerrainTiles(batch: SpriteBatch, startX: Int, startY: Int, width: Int, height: Int, includeGatherMarkers: Boolean, originX: Int, originY: Int) {
        val fallbackSprite = GraphicsManager.getSprite("grid")
        val fillSprite = GraphicsManager.getSprite("terrain_fill")
        val endX = (startX + width - 1).coerceAtMost(MapManager.MAP_WIDTH - 1)
        val endY = (startY + height - 1).coerceAtMost(MapManager.MAP_HEIGHT - 1)
        for (y in startY..endY) {
            for (x in startX..endX) {
                val terrain = MapManager.getTerrainAt(x, y) ?: continue
                val worldX = (x - originX) * MainScreenConfig.TILE_SIZE
                val worldY = (y - originY) * MainScreenConfig.TILE_SIZE
                if (fillSprite != null) {
                    val color = MapManager.getTerrainColor(terrain.id)
                    fillSprite.setColor(color)
                    fillSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    fillSprite.setPosition(worldX, worldY)
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
                    sprite.setPosition(worldX, worldY)
                    sprite.draw(batch)
                    sprite.setColor(Color.WHITE)
                }

                if (includeGatherMarkers && MapManager.getGatherAmountAt(x, y) > 0 && BuildingManager.getBuildingAt(x, y) == null) {
                    val marker = fillSprite ?: sprite
                    if (marker != null) {
                        marker.setColor(1f, 1f, 1f, 0.06f)
                        marker.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                        marker.setPosition(worldX, worldY)
                        marker.draw(batch)
                        marker.setColor(Color.WHITE)
                    }
                }
            }
        }
    }

    private fun drawTerrainTransitionTiles(batch: SpriteBatch, startX: Int, startY: Int, width: Int, height: Int, originX: Int, originY: Int) {
        val endX = (startX + width - 1).coerceAtMost(MapManager.MAP_WIDTH - 1)
        val endY = (startY + height - 1).coerceAtMost(MapManager.MAP_HEIGHT - 1)
        for (y in startY..endY) {
            for (x in startX..endX) {
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
                    maskSprite.setColor(color.r, color.g, color.b, 1f)
                    maskSprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                    maskSprite.setPosition((x - originX) * MainScreenConfig.TILE_SIZE, (y - originY) * MainScreenConfig.TILE_SIZE)
                    maskSprite.draw(batch)
                    maskSprite.setColor(Color.WHITE)
                }
            }
        }
    }

    private fun drawGatherMarkers(batch: SpriteBatch, viewBounds: ViewBounds) {
        val fillSprite = GraphicsManager.getSprite("terrain_fill")
        val fallbackSprite = GraphicsManager.getSprite("grid")
        for (y in viewBounds.minTileY..viewBounds.maxTileY) {
            for (x in viewBounds.minTileX..viewBounds.maxTileX) {
                if (MapManager.getGatherAmountAt(x, y) <= 0 || BuildingManager.getBuildingAt(x, y) != null) {
                    continue
                }
                val terrain = MapManager.getTerrainAt(x, y) ?: continue
                val marker = fillSprite ?: GraphicsManager.getSprite("terrain_${terrain.id}_${MapManager.getTerrainVariantAt(x, y)}") ?: fallbackSprite ?: continue
                marker.setColor(1f, 1f, 1f, 0.06f)
                marker.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
                marker.setPosition(x * MainScreenConfig.TILE_SIZE, y * MainScreenConfig.TILE_SIZE)
                marker.draw(batch)
                marker.setColor(Color.WHITE)
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

    private fun drawBuildings(batch: SpriteBatch, viewBounds: ViewBounds) {
        BuildingManager.getAllBuildings().forEach { building ->
            if (!viewBounds.intersectsTiles(building.x, building.y, building.model.width, building.model.height)) {
                return@forEach
            }
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

    private fun drawCrops(batch: SpriteBatch, viewBounds: ViewBounds) {
        CropManager.getAllCrops().forEach { crop ->
            if (!viewBounds.containsTile(crop.x, crop.y)) {
                return@forEach
            }
            drawProceduralCrop(batch, crop)
        }
    }

    private fun drawProceduralCrop(batch: SpriteBatch, crop: CropManager.CropInstance) {
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
            drawPlantCircle(batch, fill, centerX, tileY + tileSize * 0.17f, tileSize * 0.10f, stemColor)
            return
        }

        val midX = centerX + tileSize * profile.stemLean * 0.45f
        val midY = baseY + stemHeight * 0.55f
        drawPlantStem(batch, fill, centerX, baseY, midX, midY, stemWidth, stemColor)
        drawPlantStem(batch, fill, midX, midY, topX, topY, stemWidth * 0.88f, stemColor)

        when (crop.model.category) {
            "energy" -> drawSunBloomCrop(batch, fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, bloomColor, fruitColor)
            "material" -> drawFerronCrop(batch, fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, fruitColor)
            "defense" -> drawSpineCrop(batch, fill, crop, profile, baseY, topX, topY, stemHeight, stemColor, bloomColor)
            "medical" -> drawMedCrop(batch, fill, crop, profile, centerX, baseY, topX, topY, stemHeight, leafColor, bloomColor, fruitColor)
            else -> drawGrainCrop(batch, fill, crop, profile, centerX, baseY, topX, topY, stemHeight, stemColor, leafColor, bloomColor, fruitColor)
        }
    }

    private fun drawGrainCrop(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, profile: CropManager.PlantVisualProfile, centerX: Float, baseY: Float, topX: Float, topY: Float, stemHeight: Float, stemColor: Color, leafColor: Color, bloomColor: Color, fruitColor: Color) {
        repeat(profile.leafCount) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / (profile.leafCount - 1f)
            val side = if (index % 2 == 0) -1f else 1f
            val leafY = baseY + stemHeight * (0.22f + t * 0.58f)
            val anchorX = centerX + (topX - centerX) * (0.18f + t * 0.58f)
            val leafSize = MainScreenConfig.TILE_SIZE * (0.11f + (1f - abs(t - 0.45f)) * 0.12f)
            val tipX = anchorX + side * leafSize * (1.1f + t * 0.25f)
            val tipY = leafY + leafSize * (0.15f - t * 0.08f)
            drawLeaf(batch, fill, anchorX, leafY, tipX, tipY, leafSize, if (profile.phase == PlantPhase.DECAY) stemColor else leafColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawFlower(batch, fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.13f, bloomColor, stemColor, profile.flowerCount.coerceAtLeast(1))
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            repeat(profile.fruitCount.coerceAtLeast(2)) { index ->
                val side = if (index % 2 == 0) -1f else 1f
                val y = topY - MainScreenConfig.TILE_SIZE * (0.03f + index * 0.03f)
                drawPlantCircle(batch, fill, topX + side * MainScreenConfig.TILE_SIZE * 0.08f, y, MainScreenConfig.TILE_SIZE * 0.045f, fruitColor)
            }
        }
        drawDispersalSeeds(batch, fill, crop, topX, topY)
    }

    private fun drawSunBloomCrop(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, profile: CropManager.PlantVisualProfile, centerX: Float, baseY: Float, topX: Float, topY: Float, stemHeight: Float, leafColor: Color, bloomColor: Color, fruitColor: Color) {
        repeat(profile.leafCount.coerceAtMost(4)) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val level = index / 2f
            val anchorY = baseY + stemHeight * (0.26f + level * 0.22f)
            val anchorX = centerX + (topX - centerX) * (0.20f + level * 0.14f)
            val size = MainScreenConfig.TILE_SIZE * (0.18f + level * 0.04f)
            val tipX = anchorX + side * size * 1.55f
            val tipY = anchorY - size * (0.08f + level * 0.10f)
            drawSunflowerLeaf(batch, fill, anchorX, anchorY, tipX, tipY, size, leafColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawSunflowerHead(batch, fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.19f, bloomColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            drawPlantCircle(batch, fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.095f, Color(0.32f, 0.20f, 0.08f, 1f))
            repeat(profile.fruitCount.coerceAtLeast(2)) { index ->
                val angle = (-0.8f + index * 0.45f)
                val seedX = topX + kotlin.math.cos(angle) * MainScreenConfig.TILE_SIZE * 0.07f
                val seedY = topY + kotlin.math.sin(angle) * MainScreenConfig.TILE_SIZE * 0.06f
                drawPlantCircle(batch, fill, seedX, seedY, MainScreenConfig.TILE_SIZE * 0.014f, fruitColor)
            }
        }
        drawDispersalSeeds(batch, fill, crop, topX, topY)
    }

    private fun drawFerronCrop(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, profile: CropManager.PlantVisualProfile, centerX: Float, baseY: Float, topX: Float, topY: Float, stemHeight: Float, leafColor: Color, fruitColor: Color) {
        repeat(profile.leafCount.coerceAtMost(5)) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / profile.leafCount.toFloat()
            val side = if (index % 2 == 0) -1f else 1f
            val anchorY = baseY + stemHeight * (0.24f + t * 0.54f)
            val anchorX = centerX + (topX - centerX) * (0.20f + t * 0.38f)
            val size = MainScreenConfig.TILE_SIZE * (0.10f + t * 0.06f)
            drawCrystalLeaf(batch, fill, anchorX, anchorY, anchorX + side * size * 1.05f, anchorY + size * 0.05f, size, leafColor)
        }
        repeat(profile.fruitCount.coerceAtLeast(2)) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val crystalX = topX + side * MainScreenConfig.TILE_SIZE * (0.05f + index * 0.05f)
            val crystalY = topY - MainScreenConfig.TILE_SIZE * (0.02f + index * 0.04f)
            drawCrystalLeaf(batch, fill, crystalX, crystalY - MainScreenConfig.TILE_SIZE * 0.04f, crystalX, crystalY + MainScreenConfig.TILE_SIZE * 0.04f, MainScreenConfig.TILE_SIZE * 0.11f, fruitColor)
        }
        drawDispersalSeeds(batch, fill, crop, topX, topY)
    }

    private fun drawSpineCrop(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, profile: CropManager.PlantVisualProfile, baseY: Float, topX: Float, topY: Float, stemHeight: Float, stemColor: Color, bloomColor: Color) {
        val bulbY = baseY + stemHeight * 0.68f
        val bulbRadius = MainScreenConfig.TILE_SIZE * (0.11f + profile.heightFactor * 0.08f)
        drawPlantCircle(batch, fill, topX, bulbY, bulbRadius, bloomColor)
        repeat(8) { index ->
            val angle = (Math.PI * 2.0 * index / 8.0).toFloat()
            val spikeBaseX = topX + cos(angle) * bulbRadius * 0.55f
            val spikeBaseY = bulbY + sin(angle) * bulbRadius * 0.55f
            val spikeTipX = topX + cos(angle) * bulbRadius * 1.45f
            val spikeTipY = bulbY + sin(angle) * bulbRadius * 1.45f
            drawPlantStem(batch, fill, spikeBaseX, spikeBaseY, spikeTipX, spikeTipY, MainScreenConfig.TILE_SIZE * 0.018f, stemColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawPlantCircle(batch, fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.06f, Color(0.98f, 0.82f, 0.52f, 1f))
        }
        drawDispersalSeeds(batch, fill, crop, topX, topY)
    }

    private fun drawMedCrop(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, profile: CropManager.PlantVisualProfile, centerX: Float, baseY: Float, topX: Float, topY: Float, stemHeight: Float, leafColor: Color, bloomColor: Color, fruitColor: Color) {
        repeat(profile.leafCount.coerceAtMost(6)) { index ->
            val t = if (profile.leafCount <= 1) 0.5f else index / (profile.leafCount - 1f)
            val side = if (index % 2 == 0) -1f else 1f
            val anchorY = baseY + stemHeight * (0.18f + t * 0.60f)
            val anchorX = centerX + (topX - centerX) * (0.16f + t * 0.44f)
            val size = MainScreenConfig.TILE_SIZE * (0.13f + (1f - abs(t - 0.5f)) * 0.05f)
            drawRoundedLeaf(batch, fill, anchorX, anchorY, anchorX + side * size * 0.95f, anchorY + size * 0.12f, size, leafColor)
        }
        if (profile.phase.ordinal >= PlantPhase.FLOWERING.ordinal) {
            drawFlower(batch, fill, topX, topY, MainScreenConfig.TILE_SIZE * 0.14f, bloomColor, fruitColor, 4 + profile.flowerCount)
        }
        if (profile.phase.ordinal >= PlantPhase.FRUITING.ordinal) {
            drawPlantCircle(batch, fill, topX, topY - MainScreenConfig.TILE_SIZE * 0.02f, MainScreenConfig.TILE_SIZE * 0.05f, fruitColor)
        }
        drawDispersalSeeds(batch, fill, crop, topX, topY)
    }

    private fun drawDispersalSeeds(batch: SpriteBatch, fill: Sprite, crop: CropManager.CropInstance, topX: Float, topY: Float) {
        if (crop.getLifePhase() != PlantPhase.DISPERSAL) return
        repeat(3) { index ->
            val drift = ((crop.lifecycleTime * 1.5f + index) % 1f) * MainScreenConfig.TILE_SIZE * 0.26f
            drawPlantCircle(batch, fill, topX + drift, topY + MainScreenConfig.TILE_SIZE * 0.05f + index * MainScreenConfig.TILE_SIZE * 0.04f, MainScreenConfig.TILE_SIZE * 0.035f, Color(0.96f, 0.92f, 0.72f, 0.85f))
        }
    }

    private fun drawPlantStem(batch: SpriteBatch, sprite: Sprite, startX: Float, startY: Float, endX: Float, endY: Float, thickness: Float, color: Color) {
        val segments = 8
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = startX + (endX - startX) * t
            val y = startY + (endY - startY) * t
            drawPlantCircle(batch, sprite, x, y, thickness * (0.62f - t * 0.12f), color)
        }
    }

    private fun drawLeaf(batch: SpriteBatch, sprite: Sprite, baseX: Float, baseY: Float, tipX: Float, tipY: Float, size: Float, color: Color) {
        val segments = 6
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * sin(t * Math.PI).toFloat() * 0.42f
            drawPlantCircle(batch, sprite, x, y, width.coerceAtLeast(size * 0.06f), color)
        }
    }

    private fun drawRoundedLeaf(batch: SpriteBatch, sprite: Sprite, baseX: Float, baseY: Float, tipX: Float, tipY: Float, size: Float, color: Color) {
        val segments = 7
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * sin(t * Math.PI).toFloat() * 0.52f
            drawPlantCircle(batch, sprite, x, y, width.coerceAtLeast(size * 0.08f), color)
        }
    }

    private fun drawCrystalLeaf(batch: SpriteBatch, sprite: Sprite, baseX: Float, baseY: Float, tipX: Float, tipY: Float, size: Float, color: Color) {
        val segments = 5
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val width = size * (1f - abs(t - 0.5f) * 2f) * 0.34f
            drawPlantRect(batch, sprite, x - width, y - size * 0.05f, width * 2f, size * 0.10f, color)
        }
    }

    private fun drawFlower(batch: SpriteBatch, sprite: Sprite, centerX: Float, centerY: Float, radius: Float, petalColor: Color, coreColor: Color, petalCount: Int) {
        val petals = (4 + petalCount).coerceIn(5, 8)
        repeat(petals) { index ->
            val angle = (Math.PI * 2.0 * index / petals).toFloat()
            val px = centerX + cos(angle) * radius * 0.82f
            val py = centerY + sin(angle) * radius * 0.82f
            drawPlantCircle(batch, sprite, px, py, radius * 0.46f, petalColor)
        }
        drawPlantCircle(batch, sprite, centerX, centerY, radius * 0.42f, Color(0.47f, 0.30f, 0.16f, 1f))
        drawPlantCircle(batch, sprite, centerX, centerY, radius * 0.22f, coreColor)
    }

    private fun drawSunflowerHead(batch: SpriteBatch, sprite: Sprite, centerX: Float, centerY: Float, radius: Float, petalColor: Color) {
        val outerPetals = 18
        repeat(outerPetals) { index ->
            val angle = (Math.PI * 2.0 * index / outerPetals).toFloat()
            val baseX = centerX + cos(angle) * radius * 0.52f
            val baseY = centerY + sin(angle) * radius * 0.52f
            val tipX = centerX + cos(angle) * radius * 1.34f
            val tipY = centerY + sin(angle) * radius * 1.34f
            drawSunflowerPetal(batch, sprite, baseX, baseY, tipX, tipY, radius * 0.42f, petalColor)
        }
        val innerPetals = 12
        repeat(innerPetals) { index ->
            val angle = (Math.PI * 2.0 * index / innerPetals + 0.13).toFloat()
            val baseX = centerX + cos(angle) * radius * 0.36f
            val baseY = centerY + sin(angle) * radius * 0.36f
            val tipX = centerX + cos(angle) * radius * 1.02f
            val tipY = centerY + sin(angle) * radius * 1.02f
            drawSunflowerPetal(batch, sprite, baseX, baseY, tipX, tipY, radius * 0.28f, Color(1.0f, 0.80f, 0.18f, 1f))
        }
        drawPlantCircle(batch, sprite, centerX, centerY, radius * 0.62f, Color(0.35f, 0.22f, 0.08f, 1f))
        drawPlantCircle(batch, sprite, centerX, centerY, radius * 0.44f, Color(0.24f, 0.15f, 0.05f, 1f))
        repeat(18) { index ->
            val angle = (Math.PI * 2.0 * index / 18.0).toFloat()
            val seedX = centerX + cos(angle) * radius * 0.23f
            val seedY = centerY + sin(angle) * radius * 0.23f
            drawPlantCircle(batch, sprite, seedX, seedY, radius * 0.05f, Color(0.48f, 0.32f, 0.12f, 1f))
        }
    }

    private fun drawSunflowerPetal(batch: SpriteBatch, sprite: Sprite, baseX: Float, baseY: Float, tipX: Float, tipY: Float, width: Float, color: Color) {
        val segments = 7
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t
            val size = width * sin(t * Math.PI).toFloat() * (0.55f - t * 0.12f)
            drawPlantCircle(batch, sprite, x, y, size.coerceAtLeast(width * 0.10f), color)
        }
    }

    private fun drawSunflowerLeaf(batch: SpriteBatch, sprite: Sprite, baseX: Float, baseY: Float, tipX: Float, tipY: Float, size: Float, color: Color) {
        val segments = 8
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val curve = sin(t * Math.PI).toFloat() * size * 0.16f
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (tipY - baseY) * t - curve
            val width = size * sin(t * Math.PI).toFloat() * 0.62f
            drawPlantCircle(batch, sprite, x, y, width.coerceAtLeast(size * 0.08f), color)
        }
    }

    private fun drawPlantCircle(batch: SpriteBatch, sprite: Sprite, centerX: Float, centerY: Float, radius: Float, color: Color) {
        if (radius <= 0f) return
        val step = (radius * 0.55f).coerceAtLeast(1.5f)
        var offsetY = -radius
        while (offsetY <= radius) {
            val width = kotlin.math.sqrt((radius * radius - offsetY * offsetY).coerceAtLeast(0f))
            drawPlantRect(batch, sprite, centerX - width, centerY + offsetY - step * 0.5f, width * 2f, step, color)
            offsetY += step
        }
    }

    private fun drawPlantRect(batch: SpriteBatch, sprite: Sprite, x: Float, y: Float, width: Float, height: Float, color: Color) {
        sprite.setColor(color)
        sprite.setSize(width, height)
        sprite.setPosition(x, y)
        sprite.draw(batch)
        sprite.setColor(Color.WHITE)
    }

    private fun drawEnemies(batch: SpriteBatch, viewBounds: ViewBounds) {
        EnemyManager.getAllEnemies().forEach { enemy ->
            val tileX = (enemy.x / MainScreenConfig.TILE_SIZE).toInt()
            val tileY = (enemy.y / MainScreenConfig.TILE_SIZE).toInt()
            if (!viewBounds.containsTile(tileX, tileY)) {
                return@forEach
            }
            val sprite = GraphicsManager.getSprite(enemy.model.id) ?: return@forEach
            sprite.setSize(MainScreenConfig.TILE_SIZE, MainScreenConfig.TILE_SIZE)
            sprite.setPosition(enemy.x - MainScreenConfig.TILE_SIZE / 2f, enemy.y - MainScreenConfig.TILE_SIZE / 2f)
            sprite.draw(batch)
        }
    }
}
