package person.wangchen11.planet.game

/**
 * 资源管理器
 */
object ResourceManager {
    private val resources = mutableMapOf<String, Int>()

    /**
     * 初始化资源
     */
    fun initialize() {
        // 初始资源
        resources["wood"] = 100
        resources["stone"] = 100
        resources["metal"] = 50
        resources["energy"] = 50
        resources["food"] = 50
    }

    /**
     * 重置资源
     */
    fun reset() {
        resources.clear()
    }

    /**
     * 获取资源数量
     */
    fun getResource(resourceId: String): Int {
        return resources.getOrDefault(resourceId, 0)
    }

    /**
     * 增加资源
     */
    fun addResource(resourceId: String, amount: Int) {
        resources[resourceId] = getResource(resourceId) + amount
    }

    /**
     * 消耗资源
     */
    fun consumeResource(cost: Map<String, Int>): Boolean {
        // 检查是否有足够的资源
        for ((resourceId, amount) in cost) {
            if (getResource(resourceId) < amount) {
                return false
            }
        }

        // 消耗资源
        for ((resourceId, amount) in cost) {
            resources[resourceId] = getResource(resourceId) - amount
        }

        return true
    }

    /**
     * 消耗指定数量的资源
     */
    fun consumeResource(resourceId: String, amount: Int): Boolean {
        if (getResource(resourceId) < amount) {
            return false
        }
        resources[resourceId] = getResource(resourceId) - amount
        return true
    }

    /**
     * 获取所有资源
     */
    fun getAllResources(): Map<String, Int> {
        return resources
    }

    /**
     * 更新资源（例如生产建筑产生的资源）
     */
    fun update(delta: Float) {
        // 这里可以添加资源自动增长的逻辑
        // 例如，根据建筑的生产效率增加资源
    }
}
