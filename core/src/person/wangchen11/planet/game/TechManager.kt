package person.wangchen11.planet.game

import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.TechModel

/**
 * 科技管理器
 */
object TechManager {
    private val defaultUnlockedBuildings = setOf("basic_farm", "basic_mine", "machine_gun_turret", "wall", "command_center")
    private val defaultUnlockedCrops = setOf("basic_crop", "energy_crop")
    private val researchedTechs = mutableSetOf<String>()
    private var researchPoints = 0

    /**
     * 初始化科技管理器
     */
    fun initialize() {
        researchedTechs.clear()
        researchPoints = 0

        // 初始解锁基础科技
        researchedTechs.add("basic_tech")
    }

    /**
     * 重置科技管理器
     */
    fun reset() {
        researchedTechs.clear()
        researchPoints = 0
    }

    /**
     * 增加研究点
     */
    fun addResearchPoints(amount: Int) {
        researchPoints += amount
    }

    /**
     * 获取研究点
     */
    fun getResearchPoints(): Int {
        return researchPoints
    }

    /**
     * 研究科技
     */
    fun researchTech(techId: String): Boolean {
        val tech = MetadataManager.getTech(techId) ?: return false

        // 检查是否已经研究过
        if (isTechResearched(techId)) {
            return false
        }

        // 检查前置科技
        tech.prerequisites?.forEach { prerequisite ->
            if (!isTechResearched(prerequisite)) {
                return false
            }
        }

        // 检查研究点是否足够
        if (researchPoints < tech.cost) {
            return false
        }

        // 消耗研究点
        researchPoints -= tech.cost

        // 标记为已研究
        researchedTechs.add(techId)

        return true
    }

    /**
     * 检查科技是否已研究
     */
    fun isTechResearched(techId: String): Boolean {
        return researchedTechs.contains(techId)
    }

    /**
     * 获取所有已研究的科技
     */
    fun getResearchedTechs(): Set<String> {
        return researchedTechs
    }

    /**
     * 获取可研究的科技
     */
    fun getAvailableTechs(): List<TechModel> {
        return MetadataManager.getAllTechs().values.filter { tech ->
            !isTechResearched(tech.id) &&
                    tech.prerequisites?.all { isTechResearched(it) } ?: true
        }
    }

    fun isBuildingUnlocked(buildingId: String): Boolean {
        if (buildingId in defaultUnlockedBuildings) return true
        return researchedTechs
            .asSequence()
            .mapNotNull { MetadataManager.getTech(it) }
            .flatMap { it.unlocks.orEmpty().asSequence() }
            .any { it == buildingId }
    }

    fun isCropUnlocked(cropId: String): Boolean {
        if (cropId in defaultUnlockedCrops) return true
        return researchedTechs
            .asSequence()
            .mapNotNull { MetadataManager.getTech(it) }
            .flatMap { it.unlocks.orEmpty().asSequence() }
            .any { it == cropId }
    }
}
