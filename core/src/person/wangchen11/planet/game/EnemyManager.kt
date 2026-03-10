package person.wangchen11.planet.game

import person.wangchen11.gdx.assets.SoundManager
import person.wangchen11.planet.metadata.EnemyModel
import person.wangchen11.planet.metadata.MetadataManager
import kotlin.math.hypot
import kotlin.random.Random

object EnemyManager {
    private val enemies = mutableListOf<EnemyInstance>()
    private val random = Random(20260310)

    private var waveCount = 0
    private var waveInterval = 25f
    private var waveTimer = waveInterval

    fun initialize() {
        enemies.clear()
        waveCount = 0
        waveTimer = waveInterval
    }

    fun reset() {
        enemies.clear()
        waveCount = 0
        waveTimer = waveInterval
    }

    fun spawnEnemy(enemyId: String, x: Float, y: Float): EnemyInstance? {
        val model = MetadataManager.getEnemy(enemyId) ?: return null
        val enemy = EnemyInstance(model, x, y, model.health, model.speed, model.damage)
        enemies.add(enemy)
        return enemy
    }

    fun spawnWave() {
        waveCount += 1
        val enemyCount = 3 + waveCount * 2
        repeat(enemyCount) { index ->
            val enemyId = chooseEnemyForWave()
            val laneX = if (index % 2 == 0) 1f else MapManager.MAP_WIDTH - 2f
            val spawnX = (laneX + random.nextFloat() * 4f) * MainScreenConfig.TILE_SIZE
            val spawnY = (random.nextFloat() * 2f + 1f) * MainScreenConfig.TILE_SIZE
            spawnEnemy(enemyId, spawnX, spawnY)
        }
        if (waveCount % 5 == 0) {
            spawnEnemy(
                "boss_enemy",
                (MapManager.MAP_WIDTH / 2f) * MainScreenConfig.TILE_SIZE,
                MainScreenConfig.TILE_SIZE
            )
        }
    }

    private fun chooseEnemyForWave(): String {
        return when {
            waveCount < 3 -> "basic_enemy"
            waveCount < 5 -> if (random.nextBoolean()) "basic_enemy" else "fast_enemy"
            waveCount < 7 -> listOf("fast_enemy", "strong_enemy", "ranged_enemy").random(random)
            waveCount < 10 -> listOf("strong_enemy", "ranged_enemy", "poison_enemy").random(random)
            else -> listOf("strong_enemy", "flying_enemy", "exploding_enemy", "poison_enemy", "stealth_enemy").random(random)
        }
    }

    fun removeEnemy(enemy: EnemyInstance) {
        if (!enemies.remove(enemy)) return
        enemy.model.drops.forEach { drop ->
            if (random.nextFloat() <= drop.chance) {
                ResourceManager.addResource(drop.itemId, drop.quantity)
            }
        }
        TechManager.addResearchPoints(1)
    }

    fun getAllEnemies(): List<EnemyInstance> = enemies

    fun getWaveCount(): Int = waveCount

    fun getEnemyCount(): Int = enemies.size

    fun getSecondsUntilNextWave(): Float = waveTimer

    fun update(delta: Float) {
        waveTimer -= delta
        if (waveTimer <= 0f) {
            spawnWave()
            waveTimer = waveInterval
        }

        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            enemy.update(delta)

            if (enemy.health <= 0) {
                SoundManager.playSound("enemy_death", 0.3f)
                iterator.remove()
                enemy.model.drops.forEach { drop ->
                    if (random.nextFloat() <= drop.chance) {
                        ResourceManager.addResource(drop.itemId, drop.quantity)
                    }
                }
                TechManager.addResearchPoints(1)
                continue
            }

            if (enemy.reachedColony) {
                GameManager.damageColony(enemy.damage)
                iterator.remove()
            }
        }
    }

    data class EnemyInstance(
        val model: EnemyModel,
        var x: Float,
        var y: Float,
        var health: Int,
        var speed: Float,
        var damage: Int,
        var reachedColony: Boolean = false,
        private var attackCooldown: Float = 0f
    ) {
        fun update(delta: Float) {
            attackCooldown = (attackCooldown - delta).coerceAtLeast(0f)

            val target = BuildingManager.getAllBuildings()
                .filter { it.isCompleted() }
                .minByOrNull { building ->
                    val centerX = (building.x + building.model.width / 2f) * MainScreenConfig.TILE_SIZE
                    val centerY = (building.y + building.model.height / 2f) * MainScreenConfig.TILE_SIZE
                    hypot(x - centerX, y - centerY)
                }

            if (target != null) {
                val centerX = (target.x + target.model.width / 2f) * MainScreenConfig.TILE_SIZE
                val centerY = (target.y + target.model.height / 2f) * MainScreenConfig.TILE_SIZE
                val distance = hypot(x - centerX, y - centerY)
                if (distance <= model.attackRange + MainScreenConfig.TILE_SIZE * 0.5f) {
                    attack(target)
                } else {
                    moveTowards(centerX, centerY, delta)
                }
                return
            }

            val colonyX = (MapManager.MAP_WIDTH / 2f) * MainScreenConfig.TILE_SIZE
            val colonyY = (MapManager.MAP_HEIGHT / 2f) * MainScreenConfig.TILE_SIZE
            if (hypot(x - colonyX, y - colonyY) <= MainScreenConfig.TILE_SIZE) {
                reachedColony = true
            } else {
                moveTowards(colonyX, colonyY, delta)
            }
        }

        private fun attack(target: BuildingManager.BuildingInstance) {
            if (attackCooldown > 0f) return
            target.health -= damage
            if (target.health <= 0) {
                BuildingManager.removeBuilding(target)
            }
            attackCooldown = 1f
            SoundManager.playSound("attack", 0.25f)
        }

        private fun moveTowards(targetX: Float, targetY: Float, delta: Float) {
            val dx = targetX - x
            val dy = targetY - y
            val distance = hypot(dx, dy)
            if (distance <= 0.001f) return
            val step = speed * delta
            x += dx / distance * step
            y += dy / distance * step
        }

        fun takeDamage(amount: Int) {
            health -= amount
        }
    }
}
