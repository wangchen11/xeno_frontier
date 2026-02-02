package person.wangchen11.planet.metadata

/**
 * 基础元数据模型接口
 */
interface MetadataModel {
    val id: String
    val name: String
    val description: String
}

/**
 * 建筑元数据模型
 */
data class BuildingModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val type: String,
    val category: String,
    val cost: Map<String, Int>,
    val effects: Map<String, Any>,
    val upgradePath: List<String>? = null,
    val buildTime: Float = 1f,
    val width: Int = 1,
    val height: Int = 1
) : MetadataModel

/**
 * 敌人元数据模型
 */
data class EnemyModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val type: String,
    val category: String,
    val health: Int,
    val damage: Int,
    val speed: Float,
    val attackRange: Float,
    val attackType: String,
    val drops: List<DropItem>,
    val specialAbilities: List<String>? = null
) : MetadataModel

/**
 * 掉落物品模型
 */
data class DropItem(
    val itemId: String,
    val quantity: Int,
    val chance: Float
)

/**
 * 作物元数据模型
 */
data class CropModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val type: String,
    val category: String,
    val growthTime: Float,
    val yield: Int,
    val sellPrice: Int,
    val effects: Map<String, Any>? = null,
    val requiredConditions: Map<String, Any>? = null
) : MetadataModel

/**
 * 资源元数据模型
 */
data class ResourceModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val type: String,
    val category: String,
    val obtainMethods: List<String>,
    val uses: List<String>
) : MetadataModel

/**
 * 科技元数据模型
 */
data class TechModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val branch: String,
    val effects: Map<String, Any>,
    val prerequisites: List<String>? = null,
    val cost: Int,
    val unlocks: List<String>? = null
) : MetadataModel

/**
 * 科技分支元数据模型
 */
data class TechBranchModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val techIds: List<String>
) : MetadataModel

/**
 * 地形元数据模型
 */
data class TerrainModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val resourceDistribution: Map<String, Float>,
    val environmentalEffects: Map<String, Any>,
    val movementCost: Float,
    val buildable: Boolean
) : MetadataModel

/**
 * 地形物件元数据模型
 */
data class TerrainObjectModel(
    override val id: String,
    override val name: String,
    override val description: String,
    val type: String,
    val effects: Map<String, Any>,
    val interactable: Boolean,
    val interactionMethod: String? = null
) : MetadataModel
