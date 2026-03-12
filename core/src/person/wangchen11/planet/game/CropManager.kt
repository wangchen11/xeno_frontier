package person.wangchen11.planet.game

import person.wangchen11.gdx.assets.SoundManager
import person.wangchen11.planet.metadata.CropModel
import person.wangchen11.planet.metadata.MetadataManager
import kotlin.math.hypot
import kotlin.random.Random

object CropManager {
    private val crops = mutableListOf<CropInstance>()
    private val spawnRandom = Random(19451)

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
        val additions = mutableListOf<CropInstance>()
        val removals = mutableListOf<CropInstance>()
        crops.toList().forEach { crop ->
            val result = crop.update(delta)
            additions += result.newCrops
            if (result.shouldRemove) {
                removals += crop
            }
        }
        removals.forEach(crops::remove)
        additions.forEach { newCrop ->
            if (getCropAt(newCrop.x, newCrop.y) == null && BuildingManager.getBuildingAt(newCrop.x, newCrop.y) == null) {
                crops.add(newCrop)
            }
        }
    }

    data class CropInstance(
        val model: CropModel,
        val x: Int,
        val y: Int,
        var growthProgress: Float = 0f,
        var lifecycleTime: Float = 0f,
        var reproductionCooldown: Float = 8f,
        var offspringCount: Int = 0,
        val visualSeed: Int = ((x * 73856093) xor (y * 19349663) xor model.id.hashCode()),
        var decayTimer: Float = 0f
    ) {
        fun update(delta: Float): UpdateResult {
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
            lifecycleTime += delta
            reproductionCooldown -= delta

            val newCrops = mutableListOf<CropInstance>()
            if (canReproduce() && reproductionCooldown <= 0f) {
                trySpread()?.let(newCrops::add)
                reproductionCooldown = 7f + spawnRandom.nextFloat() * 5f
            }

            if (getLifePhase() == PlantPhase.DECAY) {
                decayTimer += delta
            }
            return UpdateResult(
                newCrops = newCrops,
                shouldRemove = decayTimer >= 14f
            )
        }

        fun isReady(): Boolean = getLifePhase().ordinal >= PlantPhase.MATURE.ordinal

        fun getGrowthPercentage(): Float = (growthProgress / model.growthTime).coerceIn(0f, 1f)

        fun getLifePhase(): PlantPhase {
            val normalizedAge = getNormalizedLifecycleAge()
            return when {
                normalizedAge < 0.08f -> PlantPhase.SEED
                normalizedAge < 0.20f -> PlantPhase.SPROUT
                normalizedAge < 0.42f -> PlantPhase.JUVENILE
                normalizedAge < 0.62f -> PlantPhase.MATURE
                normalizedAge < 0.78f -> PlantPhase.FLOWERING
                normalizedAge < 1.0f -> PlantPhase.FRUITING
                normalizedAge < 1.28f -> PlantPhase.DISPERSAL
                else -> PlantPhase.DECAY
            }
        }

        fun getNormalizedLifecycleAge(): Float {
            val baseDuration = model.growthTime.coerceAtLeast(1f)
            return lifecycleTime / baseDuration
        }

        fun getVisualProfile(): PlantVisualProfile {
            val phase = getLifePhase()
            val phaseProgress = getPhaseProgress(phase)
            val seedNoise = Random(visualSeed)
            val heightFactor = when (phase) {
                PlantPhase.SEED -> 0.08f
                PlantPhase.SPROUT -> 0.18f + phaseProgress * 0.18f
                PlantPhase.JUVENILE -> 0.36f + phaseProgress * 0.24f
                PlantPhase.MATURE -> 0.62f + phaseProgress * 0.14f
                PlantPhase.FLOWERING -> 0.76f + phaseProgress * 0.08f
                PlantPhase.FRUITING -> 0.82f + phaseProgress * 0.05f
                PlantPhase.DISPERSAL -> 0.87f - phaseProgress * 0.08f
                PlantPhase.DECAY -> 0.68f - phaseProgress * 0.34f
            }.coerceIn(0.04f, 1f)

            val leafCount = when (phase) {
                PlantPhase.SEED -> 0
                PlantPhase.SPROUT -> 2
                PlantPhase.JUVENILE -> 4 + (phaseProgress * 2f).toInt()
                PlantPhase.MATURE -> 6
                PlantPhase.FLOWERING -> 6
                PlantPhase.FRUITING -> 5
                PlantPhase.DISPERSAL -> 4
                PlantPhase.DECAY -> 2
            }
            val flowerCount = when (phase) {
                PlantPhase.FLOWERING -> 1 + (phaseProgress * 2f).toInt()
                PlantPhase.FRUITING -> 2
                else -> 0
            }
            val fruitCount = when (phase) {
                PlantPhase.FRUITING -> 1 + (phaseProgress * 3f).toInt()
                PlantPhase.DISPERSAL -> 3
                PlantPhase.DECAY -> 1
                else -> 0
            }
            val sway = ((lifecycleTime * 1.8f) + seedNoise.nextFloat() * 6.28f)
            return PlantVisualProfile(
                phase = phase,
                heightFactor = heightFactor,
                stemLean = kotlin.math.sin(sway) * 0.10f,
                leafCount = leafCount,
                flowerCount = flowerCount,
                fruitCount = fruitCount,
                vitality = when (phase) {
                    PlantPhase.DECAY -> (1f - phaseProgress * 0.8f).coerceAtLeast(0.2f)
                    else -> 0.65f + getGrowthPercentage() * 0.35f
                }
            )
        }

        private fun canReproduce(): Boolean {
            return getLifePhase() == PlantPhase.DISPERSAL && offspringCount < 2
        }

        private fun trySpread(): CropInstance? {
            val offsets = listOf(
                0 to 1, 1 to 0, 0 to -1, -1 to 0,
                1 to 1, 1 to -1, -1 to 1, -1 to -1
            ).shuffled(spawnRandom)
            for ((dx, dy) in offsets) {
                val targetX = x + dx
                val targetY = y + dy
                if (targetX !in 0 until MapManager.MAP_WIDTH || targetY !in 0 until MapManager.MAP_HEIGHT) continue
                val terrain = MapManager.getTerrainAt(targetX, targetY) ?: continue
                if (!terrain.buildable) continue
                if (getCropAt(targetX, targetY) != null || BuildingManager.getBuildingAt(targetX, targetY) != null) continue
                offspringCount += 1
                return CropInstance(
                    model = model,
                    x = targetX,
                    y = targetY,
                    growthProgress = model.growthTime * 0.05f,
                    lifecycleTime = model.growthTime * 0.05f,
                    reproductionCooldown = 10f + spawnRandom.nextFloat() * 3f,
                    visualSeed = ((targetX * 73856093) xor (targetY * 19349663) xor model.id.hashCode() xor spawnRandom.nextInt())
                )
            }
            return null
        }

        private fun getPhaseProgress(phase: PlantPhase): Float {
            val age = getNormalizedLifecycleAge()
            val (start, end) = when (phase) {
                PlantPhase.SEED -> 0f to 0.08f
                PlantPhase.SPROUT -> 0.08f to 0.20f
                PlantPhase.JUVENILE -> 0.20f to 0.42f
                PlantPhase.MATURE -> 0.42f to 0.62f
                PlantPhase.FLOWERING -> 0.62f to 0.78f
                PlantPhase.FRUITING -> 0.78f to 1.0f
                PlantPhase.DISPERSAL -> 1.0f to 1.28f
                PlantPhase.DECAY -> 1.28f to 1.55f
            }
            return ((age - start) / (end - start)).coerceIn(0f, 1f)
        }
    }

    data class UpdateResult(
        val newCrops: List<CropInstance> = emptyList(),
        val shouldRemove: Boolean = false
    )

    data class PlantVisualProfile(
        val phase: PlantPhase,
        val heightFactor: Float,
        val stemLean: Float,
        val leafCount: Int,
        val flowerCount: Int,
        val fruitCount: Int,
        val vitality: Float
    )

    enum class PlantPhase {
        SEED,
        SPROUT,
        JUVENILE,
        MATURE,
        FLOWERING,
        FRUITING,
        DISPERSAL,
        DECAY
    }
}
