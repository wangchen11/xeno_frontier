package person.wangchen11.planet.game

import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.CropModel

/**
 * 作物管理器
 */
object CropManager {
    private val crops = mutableListOf<CropInstance>()

    /**
     * 初始化作物管理器
     */
    fun initialize() {
        crops.clear()
    }

    /**
     * 重置作物管理器
     */
    fun reset() {
        crops.clear()
    }

    /**
     * 种植作物
     */
    fun plantCrop(cropId: String, x: Int, y: Int): CropInstance? {
        val model = MetadataManager.getCrop(cropId) ?: return null

        // 检查是否已经有作物
        if (getCropAt(x, y) != null) {
            return null
        }

        // 检查资源是否足够
        val cost = mapOf("energy" to 5)
        if (!ResourceManager.consumeResource(cost)) {
            return null
        }

        val crop = CropInstance(model, x, y)
        crops.add(crop)

        return crop
    }

    /**
     * 收获作物
     */
    fun harvestCrop(crop: CropInstance): Boolean {
        if (!crop.isReady()) {
            return false
        }

        // 收获作物，添加资源
        ResourceManager.addResource("food", crop.model.yield)

        // 移除作物
        crops.remove(crop)

        return true
    }

    /**
     * 移除作物
     */
    fun removeCrop(crop: CropInstance) {
        crops.remove(crop)
    }

    /**
     * 获取所有作物
     */
    fun getAllCrops(): List<CropInstance> {
        return crops
    }

    /**
     * 获取指定位置的作物
     */
    fun getCropAt(x: Int, y: Int): CropInstance? {
        return crops.find { it.x == x && it.y == y }
    }

    /**
     * 更新作物
     */
    fun update(delta: Float) {
        crops.forEach { it.update(delta) }
    }

    /**
     * 作物实例
     */
    data class CropInstance(
        val model: CropModel,
        val x: Int,
        val y: Int,
        var growthProgress: Float = 0f
    ) {
        /**
         * 更新作物状态
         */
        fun update(delta: Float) {
            growthProgress += delta
        }

        /**
         * 检查作物是否成熟
         */
        fun isReady(): Boolean {
            return growthProgress >= model.growthTime
        }

        /**
         * 获取生长百分比
         */
        fun getGrowthPercentage(): Float {
            return minOf(1f, growthProgress / model.growthTime)
        }
    }

    private fun minOf(a: Float, b: Float): Float {
        return if (a < b) a else b
    }
}
