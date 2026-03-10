package person.wangchen11.planet.game

import person.wangchen11.gdx.assets.SoundManager
import person.wangchen11.planet.metadata.BuildingModel
import person.wangchen11.planet.metadata.MetadataManager
import kotlin.math.hypot

object BuildingManager {
    private val buildings = mutableListOf<BuildingInstance>()

    fun initialize() {
        buildings.clear()
    }

    fun reset() {
        buildings.clear()
    }

    fun createBuilding(buildingId: String, x: Int, y: Int): BuildingInstance? {
        val model = MetadataManager.getBuilding(buildingId) ?: return null
        if (!TechManager.isBuildingUnlocked(buildingId)) return null
        if (!canPlace(model, x, y)) return null
        if (!ResourceManager.consumeResource(model.cost)) return null
        return place(model, x, y)
    }

    fun createFreeBuilding(buildingId: String, x: Int, y: Int): BuildingInstance? {
        val model = MetadataManager.getBuilding(buildingId) ?: return null
        if (!canPlace(model, x, y)) return null
        return place(model, x, y)
    }

    private fun place(model: BuildingModel, x: Int, y: Int): BuildingInstance {
        val maxHealth = (model.effects["health"] as? Number)?.toInt() ?: 100
        val building = BuildingInstance(model, x, y, maxHealth = maxHealth, health = maxHealth)
        buildings.add(building)
        SoundManager.playSound("build", 0.4f)
        return building
    }

    fun canPlace(model: BuildingModel, x: Int, y: Int): Boolean {
        for (offsetY in 0 until model.height) {
            for (offsetX in 0 until model.width) {
                val tileX = x + offsetX
                val tileY = y + offsetY
                val terrain = MapManager.getTerrainAt(tileX, tileY) ?: return false
                if (!terrain.buildable) return false
                if (getBuildingAt(tileX, tileY) != null) return false
                if (CropManager.getCropAt(tileX, tileY) != null) return false
            }
        }
        return true
    }

    fun removeBuilding(building: BuildingInstance) {
        buildings.remove(building)
    }

    fun getAllBuildings(): List<BuildingInstance> = buildings

    fun getBuildingAt(x: Int, y: Int): BuildingInstance? {
        return buildings.find { building ->
            x in building.x until building.x + building.model.width &&
                y in building.y until building.y + building.model.height
        }
    }

    fun hasCommandCenter(): Boolean = buildings.any { it.model.id == "command_center" }

    fun update(delta: Float) {
        buildings.toList().forEach { it.update(delta) }
    }

    data class BuildingInstance(
        val model: BuildingModel,
        val x: Int,
        val y: Int,
        val maxHealth: Int,
        var health: Int,
        var isConstructing: Boolean = true,
        var constructionProgress: Float = 0f,
        private var attackCooldown: Float = 0f
    ) {
        fun update(delta: Float) {
            if (isConstructing) {
                constructionProgress += delta
                if (constructionProgress >= model.buildTime) {
                    isConstructing = false
                }
                return
            }

            attackCooldown = (attackCooldown - delta).coerceAtLeast(0f)
            if (model.type == "defense") {
                attackEnemies()
            }
        }

        private fun attackEnemies() {
            if (attackCooldown > 0f) return

            val attackRange = (model.effects["range"] as? Number)?.toFloat() ?: 120f
            val damage = (model.effects["damage"] as? Number)?.toInt() ?: 12
            val fireRate = (model.effects["fire_rate"] as? Number)?.toFloat() ?: 1f
            val centerX = (x + model.width / 2f) * MainScreenConfig.TILE_SIZE
            val centerY = (y + model.height / 2f) * MainScreenConfig.TILE_SIZE

            val target = EnemyManager.getAllEnemies()
                .filter { enemy ->
                    hypot(enemy.x - centerX, enemy.y - centerY) <= attackRange
                }
                .minByOrNull { enemy ->
                    hypot(enemy.x - centerX, enemy.y - centerY)
                } ?: return

            target.takeDamage(damage)
            attackCooldown = if (fireRate <= 0f) 1f else 1f / fireRate
        }

        fun isCompleted(): Boolean = !isConstructing
    }
}
