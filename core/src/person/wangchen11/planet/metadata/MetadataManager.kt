package person.wangchen11.planet.metadata

/**
 * 元数据管理器
 */
object MetadataManager {
    private var buildingMetadata: Map<String, BuildingModel>? = null
    private var enemyMetadata: Map<String, EnemyModel>? = null
    private var cropMetadata: Map<String, CropModel>? = null
    private var resourceMetadata: Map<String, ResourceModel>? = null
    private var techMetadata: Map<String, TechModel>? = null
    private var techBranchMetadata: Map<String, TechBranchModel>? = null
    private var terrainMetadata: Map<String, TerrainModel>? = null
    private var terrainObjectMetadata: Map<String, TerrainObjectModel>? = null

    /**
     * 初始化元数据
     */
    fun initialize() {
        buildingMetadata = MetadataLoader.loadBuildingMetadata()
        enemyMetadata = MetadataLoader.loadEnemyMetadata()
        cropMetadata = MetadataLoader.loadCropMetadata()
        resourceMetadata = MetadataLoader.loadResourceMetadata()
        techMetadata = MetadataLoader.loadTechMetadata()
        techBranchMetadata = MetadataLoader.loadTechBranchMetadata()
        terrainMetadata = MetadataLoader.loadTerrainMetadata()
        terrainObjectMetadata = MetadataLoader.loadTerrainObjectMetadata()
    }

    /**
     * 重新加载元数据
     */
    fun reload() {
        MetadataLoader.clearCache()
        initialize()
    }

    /**
     * 获取建筑元数据
     */
    fun getBuilding(buildingId: String): BuildingModel? {
        if (buildingMetadata == null) {
            buildingMetadata = MetadataLoader.loadBuildingMetadata()
        }
        return buildingMetadata?.get(buildingId)
    }

    /**
     * 获取所有建筑元数据
     */
    fun getAllBuildings(): Map<String, BuildingModel> {
        if (buildingMetadata == null) {
            buildingMetadata = MetadataLoader.loadBuildingMetadata()
        }
        return buildingMetadata ?: emptyMap()
    }

    /**
     * 获取敌人元数据
     */
    fun getEnemy(enemyId: String): EnemyModel? {
        if (enemyMetadata == null) {
            enemyMetadata = MetadataLoader.loadEnemyMetadata()
        }
        return enemyMetadata?.get(enemyId)
    }

    /**
     * 获取所有敌人元数据
     */
    fun getAllEnemies(): Map<String, EnemyModel> {
        if (enemyMetadata == null) {
            enemyMetadata = MetadataLoader.loadEnemyMetadata()
        }
        return enemyMetadata ?: emptyMap()
    }

    /**
     * 获取作物元数据
     */
    fun getCrop(cropId: String): CropModel? {
        if (cropMetadata == null) {
            cropMetadata = MetadataLoader.loadCropMetadata()
        }
        return cropMetadata?.get(cropId)
    }

    /**
     * 获取所有作物元数据
     */
    fun getAllCrops(): Map<String, CropModel> {
        if (cropMetadata == null) {
            cropMetadata = MetadataLoader.loadCropMetadata()
        }
        return cropMetadata ?: emptyMap()
    }

    /**
     * 获取资源元数据
     */
    fun getResource(resourceId: String): ResourceModel? {
        if (resourceMetadata == null) {
            resourceMetadata = MetadataLoader.loadResourceMetadata()
        }
        return resourceMetadata?.get(resourceId)
    }

    /**
     * 获取所有资源元数据
     */
    fun getAllResources(): Map<String, ResourceModel> {
        if (resourceMetadata == null) {
            resourceMetadata = MetadataLoader.loadResourceMetadata()
        }
        return resourceMetadata ?: emptyMap()
    }

    /**
     * 获取科技元数据
     */
    fun getTech(techId: String): TechModel? {
        if (techMetadata == null) {
            techMetadata = MetadataLoader.loadTechMetadata()
        }
        return techMetadata?.get(techId)
    }

    /**
     * 获取所有科技元数据
     */
    fun getAllTechs(): Map<String, TechModel> {
        if (techMetadata == null) {
            techMetadata = MetadataLoader.loadTechMetadata()
        }
        return techMetadata ?: emptyMap()
    }

    /**
     * 获取科技分支元数据
     */
    fun getTechBranch(branchId: String): TechBranchModel? {
        if (techBranchMetadata == null) {
            techBranchMetadata = MetadataLoader.loadTechBranchMetadata()
        }
        return techBranchMetadata?.get(branchId)
    }

    /**
     * 获取所有科技分支元数据
     */
    fun getAllTechBranches(): Map<String, TechBranchModel> {
        if (techBranchMetadata == null) {
            techBranchMetadata = MetadataLoader.loadTechBranchMetadata()
        }
        return techBranchMetadata ?: emptyMap()
    }

    /**
     * 获取地形元数据
     */
    fun getTerrain(terrainId: String): TerrainModel? {
        if (terrainMetadata == null) {
            terrainMetadata = MetadataLoader.loadTerrainMetadata()
        }
        return terrainMetadata?.get(terrainId)
    }

    /**
     * 获取所有地形元数据
     */
    fun getAllTerrains(): Map<String, TerrainModel> {
        if (terrainMetadata == null) {
            terrainMetadata = MetadataLoader.loadTerrainMetadata()
        }
        return terrainMetadata ?: emptyMap()
    }

    /**
     * 获取地形物件元数据
     */
    fun getTerrainObject(objectId: String): TerrainObjectModel? {
        if (terrainObjectMetadata == null) {
            terrainObjectMetadata = MetadataLoader.loadTerrainObjectMetadata()
        }
        return terrainObjectMetadata?.get(objectId)
    }

    /**
     * 获取所有地形物件元数据
     */
    fun getAllTerrainObjects(): Map<String, TerrainObjectModel> {
        if (terrainObjectMetadata == null) {
            terrainObjectMetadata = MetadataLoader.loadTerrainObjectMetadata()
        }
        return terrainObjectMetadata ?: emptyMap()
    }
}
