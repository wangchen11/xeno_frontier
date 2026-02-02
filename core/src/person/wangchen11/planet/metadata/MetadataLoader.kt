package person.wangchen11.planet.metadata

import com.badlogic.gdx.Gdx
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

/**
 * 元数据加载器
 */
object MetadataLoader {
    private val gson = Gson()
    private val metadataCache = ConcurrentHashMap<String, Any>()

    /**
     * 加载建筑元数据
     */
    fun loadBuildingMetadata(): Map<String, BuildingModel> {
        val key = "buildings"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, BuildingModel>
        }

        val file = Gdx.files.internal("metadata/buildings.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, BuildingModel>>() {}.type
        val buildings = gson.fromJson<Map<String, BuildingModel>>(jsonString, type)

        metadataCache[key] = buildings
        return buildings
    }

    /**
     * 加载敌人元数据
     */
    fun loadEnemyMetadata(): Map<String, EnemyModel> {
        val key = "enemies"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, EnemyModel>
        }

        val file = Gdx.files.internal("metadata/enemies.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, EnemyModel>>() {}.type
        val enemies = gson.fromJson<Map<String, EnemyModel>>(jsonString, type)

        metadataCache[key] = enemies
        return enemies
    }

    /**
     * 加载作物元数据
     */
    fun loadCropMetadata(): Map<String, CropModel> {
        val key = "crops"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, CropModel>
        }

        val file = Gdx.files.internal("metadata/crops.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, CropModel>>() {}.type
        val crops = gson.fromJson<Map<String, CropModel>>(jsonString, type)

        metadataCache[key] = crops
        return crops
    }

    /**
     * 加载资源元数据
     */
    fun loadResourceMetadata(): Map<String, ResourceModel> {
        val key = "resources"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, ResourceModel>
        }

        val file = Gdx.files.internal("metadata/resources.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, ResourceModel>>() {}.type
        val resources = gson.fromJson<Map<String, ResourceModel>>(jsonString, type)

        metadataCache[key] = resources
        return resources
    }

    /**
     * 加载科技元数据
     */
    fun loadTechMetadata(): Map<String, TechModel> {
        val key = "techs"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, TechModel>
        }

        val file = Gdx.files.internal("metadata/techs.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, TechModel>>() {}.type
        val techs = gson.fromJson<Map<String, TechModel>>(jsonString, type)

        metadataCache[key] = techs
        return techs
    }

    /**
     * 加载科技分支元数据
     */
    fun loadTechBranchMetadata(): Map<String, TechBranchModel> {
        val key = "techBranches"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, TechBranchModel>
        }

        val file = Gdx.files.internal("metadata/techBranches.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, TechBranchModel>>() {}.type
        val techBranches = gson.fromJson<Map<String, TechBranchModel>>(jsonString, type)

        metadataCache[key] = techBranches
        return techBranches
    }

    /**
     * 加载地形元数据
     */
    fun loadTerrainMetadata(): Map<String, TerrainModel> {
        val key = "terrains"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, TerrainModel>
        }

        val file = Gdx.files.internal("metadata/terrains.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, TerrainModel>>() {}.type
        val terrains = gson.fromJson<Map<String, TerrainModel>>(jsonString, type)

        metadataCache[key] = terrains
        return terrains
    }

    /**
     * 加载地形物件元数据
     */
    fun loadTerrainObjectMetadata(): Map<String, TerrainObjectModel> {
        val key = "terrainObjects"
        if (metadataCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return metadataCache[key] as Map<String, TerrainObjectModel>
        }

        val file = Gdx.files.internal("metadata/terrainObjects.json")
        val jsonString = file.readString()

        // 使用Gson解析JSON
        val type = object : TypeToken<Map<String, TerrainObjectModel>>() {}.type
        val terrainObjects = gson.fromJson<Map<String, TerrainObjectModel>>(jsonString, type)

        metadataCache[key] = terrainObjects
        return terrainObjects
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        metadataCache.clear()
    }
}
