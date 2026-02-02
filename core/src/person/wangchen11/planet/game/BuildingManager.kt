package person.wangchen11.planet.game

import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.BuildingModel

/**
 * 建筑管理器
 */
object BuildingManager {
    private val buildings = mutableListOf<BuildingInstance>()

    /**
     * 初始化建筑管理器
     */
    fun initialize() {
        buildings.clear()
    }

    /**
     * 重置建筑管理器
     */
    fun reset() {
        buildings.clear()
    }

    /**
     * 创建建筑
     */
    fun createBuilding(buildingId: String, x: Int, y: Int): BuildingInstance? {
        val model = MetadataManager.getBuilding(buildingId) ?: return null

        // 检查资源是否足够
        if (!ResourceManager.consumeResource(model.cost)) {
            return null
        }

        // 创建建筑实例
        val building = BuildingInstance(model, x, y)
        buildings.add(building)

        return building
    }

    /**
     * 移除建筑
     */
    fun removeBuilding(building: BuildingInstance) {
        buildings.remove(building)
    }

    /**
     * 获取所有建筑
     */
    fun getAllBuildings(): List<BuildingInstance> {
        return buildings
    }

    /**
     * 获取指定位置的建筑
     */
    fun getBuildingAt(x: Int, y: Int): BuildingInstance? {
        return buildings.find { it.x == x && it.y == y }
    }

    /**
     * 更新建筑
     */
    fun update(delta: Float) {
        buildings.forEach { it.update(delta) }
    }

    /**
     * 建筑实例
     */
    data class BuildingInstance(
        val model: BuildingModel,
        val x: Int,
        val y: Int,
        var health: Int = 100,
        var isConstructing: Boolean = true,
        var constructionProgress: Float = 0f
    ) {
        /**
         * 更新建筑状态
         */
        fun update(delta: Float) {
            if (isConstructing) {
                constructionProgress += delta
                if (constructionProgress >= model.buildTime) {
                    isConstructing = false
                }
            }
        }

        /**
         * 检查建筑是否已完成建造
         */
        fun isCompleted(): Boolean {
            return !isConstructing
        }
    }
}
