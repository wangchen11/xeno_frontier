package person.wangchen11.planet.game

import com.badlogic.gdx.graphics.Color
import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.TerrainModel
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

object MapManager {
    const val MAP_WIDTH = 160
    const val MAP_HEIGHT = 100
    private const val SAFE_ZONE_RADIUS = 8
    private const val SMOOTHING_PASSES = 3

    private val terrainGrid = Array(MAP_HEIGHT) { arrayOfNulls<TerrainModel>(MAP_WIDTH) }
    private val gatherableResources = Array(MAP_HEIGHT) { IntArray(MAP_WIDTH) }
    private val terrainVariantGrid = Array(MAP_HEIGHT) { IntArray(MAP_WIDTH) }
    private val random = Random(0xB4B2BEEF.toInt())

    private val terrainColors = mapOf(
        "plain" to Color(0.56f, 0.78f, 0.33f, 1f),
        "forest" to Color(0.10f, 0.55f, 0.11f, 1f),
        "desert" to Color(0.93f, 0.81f, 0.45f, 1f),
        "mountain" to Color(0.68f, 0.68f, 0.68f, 1f),
        "swamp" to Color(0.14f, 0.47f, 0.27f, 1f),
        "lava" to Color(0.83f, 0.25f, 0.03f, 1f)
    )

    private val terrainPriorities = mapOf(
        "plain" to 1,
        "desert" to 2,
        "forest" to 3,
        "swamp" to 4,
        "mountain" to 5,
        "lava" to 6
    )

    fun initialize() {
        val terrains = MetadataManager.getAllTerrains()
        if (terrains.isEmpty()) return

        val elevation = buildNoiseMap(scale = 22f, octaves = 4, persistence = 0.55f)
        val moisture = buildNoiseMap(scale = 18f, octaves = 4, persistence = 0.58f)
        val heat = buildNoiseMap(scale = 26f, octaves = 3, persistence = 0.52f)
        val terrainIds = Array(MAP_HEIGHT) { Array(MAP_WIDTH) { "plain" } }

        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                terrainIds[y][x] = classifyTerrain(
                    elevation = applyRadialFalloff(elevation[y][x], x, y),
                    moisture = moisture[y][x],
                    heat = heat[y][x]
                )
            }
        }

        smoothTerrainIds(terrainIds)

        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                val terrainId = terrainIds[y][x]
                val terrain = terrains[terrainId] ?: terrains["plain"] ?: terrains.values.first()
                terrainGrid[y][x] = terrain
                terrainVariantGrid[y][x] = terrainVariantFor(x, y, terrain.id)
                gatherableResources[y][x] = gatherSeedFor(terrain.id)
            }
        }

        val safeTerrain = terrains["plain"] ?: terrains.values.first()
        val centerX = MAP_WIDTH / 2
        val centerY = MAP_HEIGHT / 2
        for (y in (centerY - SAFE_ZONE_RADIUS)..(centerY + SAFE_ZONE_RADIUS)) {
            for (x in (centerX - SAFE_ZONE_RADIUS)..(centerX + SAFE_ZONE_RADIUS)) {
                if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) continue
                terrainGrid[y][x] = safeTerrain
                terrainVariantGrid[y][x] = terrainVariantFor(x, y, safeTerrain.id)
                gatherableResources[y][x] = 2 + random.nextInt(2)
            }
        }
    }

    private fun smoothTerrainIds(terrainIds: Array<Array<String>>) {
        repeat(SMOOTHING_PASSES) {
            val next = Array(MAP_HEIGHT) { y -> terrainIds[y].clone() }
            for (y in 0 until MAP_HEIGHT) {
                for (x in 0 until MAP_WIDTH) {
                    if (isInsideSafeZone(x, y)) {
                        next[y][x] = "plain"
                        continue
                    }

                    val weights = linkedMapOf<String, Float>()
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx !in 0 until MAP_WIDTH || ny !in 0 until MAP_HEIGHT) continue
                            val terrainId = terrainIds[ny][nx]
                            val distanceWeight = when {
                                dx == 0 && dy == 0 -> 3f
                                dx == 0 || dy == 0 -> 1.35f
                                else -> 0.85f
                            }
                            weights[terrainId] = weights.getOrDefault(terrainId, 0f) + distanceWeight
                        }
                    }

                    val current = terrainIds[y][x]
                    val best = weights.maxByOrNull { entry ->
                        entry.value + terrainPriorityBias(entry.key, current)
                    }?.key ?: current

                    next[y][x] = when {
                        current == "lava" && best != "lava" && weights.getOrDefault("lava", 0f) > 2.6f -> "lava"
                        current == "mountain" && best == "plain" && weights.getOrDefault("mountain", 0f) > 2.2f -> "mountain"
                        else -> best
                    }
                }
            }

            for (y in 0 until MAP_HEIGHT) {
                for (x in 0 until MAP_WIDTH) {
                    terrainIds[y][x] = next[y][x]
                }
            }
        }
    }

    private fun terrainPriorityBias(candidate: String, current: String): Float {
        if (candidate == current) return 0.45f
        return when (candidate) {
            "lava" -> 0.12f
            "mountain" -> 0.08f
            "swamp" -> 0.05f
            else -> 0f
        }
    }

    private fun isInsideSafeZone(x: Int, y: Int): Boolean {
        val centerX = MAP_WIDTH / 2
        val centerY = MAP_HEIGHT / 2
        return x in (centerX - SAFE_ZONE_RADIUS)..(centerX + SAFE_ZONE_RADIUS) &&
            y in (centerY - SAFE_ZONE_RADIUS)..(centerY + SAFE_ZONE_RADIUS)
    }

    private fun gatherSeedFor(terrainId: String): Int {
        return when (terrainId) {
            "forest" -> 5 + random.nextInt(4)
            "mountain" -> 6 + random.nextInt(5)
            "desert" -> 4 + random.nextInt(3)
            "swamp" -> 3 + random.nextInt(3)
            "lava" -> 4 + random.nextInt(4)
            else -> 3 + random.nextInt(3)
        }
    }

    private fun buildNoiseMap(scale: Float, octaves: Int, persistence: Float): Array<FloatArray> {
        return Array(MAP_HEIGHT) { y ->
            FloatArray(MAP_WIDTH) { x ->
                var amplitude = 1f
                var frequency = 1f
                var total = 0f
                var amplitudeSum = 0f
                repeat(octaves) {
                    total += valueNoise(x / scale * frequency, y / scale * frequency) * amplitude
                    amplitudeSum += amplitude
                    amplitude *= persistence
                    frequency *= 2f
                }
                (total / amplitudeSum).coerceIn(0f, 1f)
            }
        }
    }

    private fun valueNoise(x: Float, y: Float): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = x0 + 1
        val y1 = y0 + 1
        val sx = smoothStep(x - x0)
        val sy = smoothStep(y - y0)

        val n00 = randomAt(x0, y0)
        val n10 = randomAt(x1, y0)
        val n01 = randomAt(x0, y1)
        val n11 = randomAt(x1, y1)

        val ix0 = lerp(n00, n10, sx)
        val ix1 = lerp(n01, n11, sx)
        return lerp(ix0, ix1, sy)
    }

    private fun randomAt(x: Int, y: Int): Float {
        var hash = x * 374761393 + y * 668265263 + 0x9E3779B9.toInt()
        hash = (hash xor (hash ushr 13)) * 1274126177
        hash = hash xor (hash ushr 16)
        return ((hash and Int.MAX_VALUE) / Int.MAX_VALUE.toFloat()).coerceIn(0f, 1f)
    }

    private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun applyRadialFalloff(elevation: Float, x: Int, y: Int): Float {
        val nx = (x / (MAP_WIDTH - 1f)) * 2f - 1f
        val ny = (y / (MAP_HEIGHT - 1f)) * 2f - 1f
        val distance = sqrt(nx * nx + ny * ny).coerceAtMost(1.4f)
        val edgeBias = ((distance - 0.45f) / 0.95f).coerceIn(0f, 1f)
        return (elevation * (1f - edgeBias * 0.35f)).coerceIn(0f, 1f)
    }

    private fun classifyTerrain(elevation: Float, moisture: Float, heat: Float): String {
        if (elevation > 0.82f && heat > 0.62f) return "lava"
        if (elevation > 0.74f) return "mountain"
        if (elevation < 0.30f && moisture > 0.67f) return "swamp"
        if (moisture < 0.24f && heat > 0.52f) return "desert"
        if (moisture > 0.56f) return "forest"
        return "plain"
    }

    private fun terrainVariantFor(x: Int, y: Int, terrainId: String): Int {
        val base = randomAt(x + terrainId.hashCode(), y - terrainId.hashCode())
        return (base * 3f).toInt().coerceIn(0, 2)
    }

    fun getTerrainAt(x: Int, y: Int): TerrainModel? {
        if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) return null
        return terrainGrid[y][x]
    }

    fun getTerrainVariantAt(x: Int, y: Int): Int {
        if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) return 0
        return terrainVariantGrid[y][x]
    }

    fun getTerrainColor(terrainId: String?): Color {
        return terrainColors[terrainId] ?: Color.SKY
    }

    fun getTerrainPriority(terrainId: String?): Int {
        return terrainPriorities[terrainId] ?: 0
    }

    fun getLegend(): List<TerrainModel> {
        return MetadataManager.getAllTerrains().values.sortedBy { it.name }
    }

    fun getBuildableTiles(): Int {
        var count = 0
        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                if (terrainGrid[y][x]?.buildable == true) {
                    count++
                }
            }
        }
        return count
    }

    fun getTerrainDistribution(): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                val id = terrainGrid[y][x]?.id ?: "unknown"
                distribution[id] = distribution.getOrDefault(id, 0) + 1
            }
        }
        return distribution
    }

    fun getTotalTiles(): Int = MAP_WIDTH * MAP_HEIGHT

    fun getGatherAmountAt(x: Int, y: Int): Int {
        if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) return 0
        return gatherableResources[y][x]
    }

    fun gatherAt(x: Int, y: Int): Map<String, Int> {
        if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) return emptyMap()
        val remaining = gatherableResources[y][x]
        if (remaining <= 0) return emptyMap()

        val terrain = terrainGrid[y][x] ?: return emptyMap()
        val resourceId = when (terrain.id) {
            "forest" -> "wood"
            "mountain" -> if (remaining % 2 == 0) "metal" else "stone"
            "desert" -> "stone"
            "swamp" -> "wood"
            "lava" -> "metal"
            else -> if (remaining % 2 == 0) "stone" else "wood"
        }
        val amount = minOf(3, remaining)
        gatherableResources[y][x] = (remaining - amount).coerceAtLeast(0)
        return mapOf(resourceId to amount)
    }
}
