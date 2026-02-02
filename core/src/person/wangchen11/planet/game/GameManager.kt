package person.wangchen11.planet.game

import person.wangchen11.planet.metadata.MetadataManager

/**
 * 游戏管理器
 */
object GameManager {
    var isInitialized = false
        private set

    /**
     * 初始化游戏
     */
    fun initialize() {
        if (isInitialized) return

        // 初始化元数据
        MetadataManager.initialize()

        // 初始化各个子系统
        ResourceManager.initialize()
        BuildingManager.initialize()
        EnemyManager.initialize()
        CropManager.initialize()
        TechManager.initialize()

        isInitialized = true
    }

    /**
     * 更新游戏状态
     */
    fun update(delta: Float) {
        if (!isInitialized) return

        BuildingManager.update(delta)
        EnemyManager.update(delta)
        CropManager.update(delta)
        ResourceManager.update(delta)
    }

    /**
     * 重置游戏
     */
    fun reset() {
        ResourceManager.reset()
        BuildingManager.reset()
        EnemyManager.reset()
        CropManager.reset()
        TechManager.reset()

        isInitialized = false
    }
}
