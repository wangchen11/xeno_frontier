package person.wangchen11.planet.game

import person.wangchen11.gdx.assets.SoundManager
import person.wangchen11.planet.metadata.CropModel
import person.wangchen11.planet.metadata.MetadataManager
import kotlin.math.hypot

object CropManager {
    private val crops = mutableListOf<CropInstance>()

    fun initialize() {
        crops.clear()
    }

    fun reset() {
        crops.clear()
    }

    fun plantCrop(cropId: String, x: Int, y: Int): CropInstance? {
        val model = MetadataManager.getCrop(cropId) ?: return null
        if (!TechManager.isCropUnlocked(cropId)) return null
        val terrain = MapManager.getTerrainAt(x, y) ?: return null
        if (!terrain.buildable) return null
        if (getCropAt(x, y) != null || BuildingManager.getBuildingAt(x, y) != null) return null
        if (!ResourceManager.consumeResource(mapOf("energy" to 3))) return null

        val crop = CropInstance(model, x, y)
        crops.add(crop)
        return crop
    }

    fun harvestCrop(crop: CropInstance): Boolean {
        if (!crop.isReady()) return false

        when (crop.model.category) {
            "food" -> ResourceManager.addResource("food", crop.model.yield)
            "energy" -> ResourceManager.addResource("energy", crop.model.yield)
            "material" -> ResourceManager.addResource("metal", crop.model.yield)
            "defense" -> {
                ResourceManager.addResource("food", crop.model.yield)
                TechManager.addResearchPoints(1)
            }
            "medical" -> {
                ResourceManager.addResource("food", crop.model.yield)
                GameManager.healColony(2)
            }
        }

        SoundManager.playSound("harvest", 0.35f)
        crops.remove(crop)
        return true
    }

    fun removeCrop(crop: CropInstance) {
        crops.remove(crop)
    }

    fun getAllCrops(): List<CropInstance> = crops

    fun getCropAt(x: Int, y: Int): CropInstance? = crops.find { it.x == x && it.y == y }

    fun update(delta: Float) {
        crops.forEach { it.update(delta) }
    }

    data class CropInstance(
        val model: CropModel,
        val x: Int,
        val y: Int,
        var growthProgress: Float = 0f
    ) {
        fun update(delta: Float) {
            var growthSpeed = 1f
            val tileCenterX = (x + 0.5f) * MainScreenConfig.TILE_SIZE
            val tileCenterY = (y + 0.5f) * MainScreenConfig.TILE_SIZE

            BuildingManager.getAllBuildings()
                .filter { it.isCompleted() }
                .forEach { building ->
                    val buildingCenterX = (building.x + building.model.width / 2f) * MainScreenConfig.TILE_SIZE
                    val buildingCenterY = (building.y + building.model.height / 2f) * MainScreenConfig.TILE_SIZE
                    if (hypot(tileCenterX - buildingCenterX, tileCenterY - buildingCenterY) <= MainScreenConfig.TILE_SIZE * 4f) {
                        when (building.model.id) {
                            "basic_farm" -> growthSpeed += 0.20f
                            "advanced_farm" -> growthSpeed += 0.45f
                            "super_farm" -> growthSpeed += 0.80f
                        }
                    }
                }

            growthProgress += delta * growthSpeed
        }

        fun isReady(): Boolean = growthProgress >= model.growthTime

        fun getGrowthPercentage(): Float = (growthProgress / model.growthTime).coerceIn(0f, 1f)
    }
}
