package person.wangchen11.planet.game

import com.badlogic.gdx.graphics.Color
import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.TerrainModel
import kotlin.random.Random

/** 地图数据管理 */
object MapManager {
    const val MAP_WIDTH = 40
    const val MAP_HEIGHT = 25
    private const val SAFE_ZONE_RADIUS = 4

    private val terrainGrid = Array(MAP_HEIGHT) { arrayOfNulls<TerrainModel>(MAP_WIDTH) }
    private val gatherableResources = Array(MAP_HEIGHT) { IntArray(MAP_WIDTH) }
    private val random = Random(0xB4B2BEEF.toInt())

    private val terrainWeights = listOf(
        "plain" to 35,
        "forest" to 20,
        "desert" to 15,
        "mountain" to 10,
        "swamp" to 10,
        "lava" to 5
    )

    private val terrainColors = mapOf(
        "plain" to Color(0.56f, 0.78f, 0.33f, 1f),
        "forest" to Color(0.10f, 0.55f, 0.11f, 1f),
        "desert" to Color(0.93f, 0.81f, 0.45f, 1f),
        "mountain" to Color(0.68f, 0.68f, 0.68f, 1f),
        "swamp" to Color(0.14f, 0.47f, 0.27f, 1f),
        "lava" to Color(0.83f, 0.25f, 0.03f, 1f)
    )

    private val totalWeight = terrainWeights.sumOf { it.second }

    /** 生成地图 */
    fun initialize() {
        val terrains = MetadataManager.getAllTerrains()
        if (terrains.isEmpty()) {
            return
        }

        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                val terrainId = pickTerrainId()
                val terrain = terrains[terrainId] ?: terrains["plain"]
                terrainGrid[y][x] = terrain
                gatherableResources[y][x] = when (terrain?.id) {
                    "forest" -> 5 + random.nextInt(4)
                    "mountain" -> 6 + random.nextInt(5)
                    "desert" -> 4 + random.nextInt(3)
                    "swamp" -> 3 + random.nextInt(3)
                    "lava" -> 4 + random.nextInt(4)
                    else -> 3 + random.nextInt(3)
                }
            }
        }

        // Reserve a buildable crash-site zone in the center so the opening base can always spawn.
        val safeTerrain = terrains["plain"] ?: terrains.values.first()
        val centerX = MAP_WIDTH / 2
        val centerY = MAP_HEIGHT / 2
        for (y in (centerY - SAFE_ZONE_RADIUS)..(centerY + SAFE_ZONE_RADIUS)) {
            for (x in (centerX - SAFE_ZONE_RADIUS)..(centerX + SAFE_ZONE_RADIUS)) {
                if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) continue
                terrainGrid[y][x] = safeTerrain
                gatherableResources[y][x] = 2 + random.nextInt(2)
            }
        }
    }

    private fun pickTerrainId(): String {
        var roll = random.nextInt(totalWeight)
        for ((terrainId, weight) in terrainWeights) {
            if (roll < weight) {
                return terrainId
            }
            roll -= weight
        }
        return terrainWeights.first().first
    }

    fun getTerrainAt(x: Int, y: Int): TerrainModel? {
        if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) {
            return null
        }
        return terrainGrid[y][x]
    }

    fun getTerrainColor(terrainId: String?): Color {
        return terrainColors[terrainId] ?: Color.SKY
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
