package person.wangchen11.planet.game

import person.wangchen11.planet.metadata.MetadataManager
import person.wangchen11.planet.metadata.EnemyModel

/**
 * 敌人管理器
 */
object EnemyManager {
    private val enemies = mutableListOf<EnemyInstance>()
    private var waveCount = 0
    private var waveInterval = 30f // 波次间隔（秒）
    private var waveTimer = 0f

    /**
     * 初始化敌人管理器
     */
    fun initialize() {
        enemies.clear()
        waveCount = 0
        waveTimer = 0f
    }

    /**
     * 重置敌人管理器
     */
    fun reset() {
        enemies.clear()
        waveCount = 0
        waveTimer = 0f
    }

    /**
     * 生成敌人
     */
    fun spawnEnemy(enemyId: String, x: Float, y: Float): EnemyInstance? {
        val model = MetadataManager.getEnemy(enemyId) ?: return null

        val enemy = EnemyInstance(model, x, y)
        enemies.add(enemy)

        return enemy
    }

    /**
     * 生成敌人波次
     */
    fun spawnWave() {
        waveCount++
        val enemyCount = 5 + waveCount * 2

        for (i in 0 until enemyCount) {
            val enemyId = when {
                waveCount < 5 -> "basic_enemy"
                waveCount < 10 -> "fast_enemy"
                waveCount < 15 -> "tank_enemy"
                else -> "ranged_enemy"
            }

            val x = (Math.random() * 800).toFloat()
            val y = -50f

            spawnEnemy(enemyId, x, y)
        }
    }

    /**
     * 移除敌人
     */
    fun removeEnemy(enemy: EnemyInstance) {
        enemies.remove(enemy)

        // 处理敌人掉落
        enemy.model.drops.forEach { drop ->
            if (Math.random() < drop.chance) {
                ResourceManager.addResource(drop.itemId, drop.quantity)
            }
        }
    }

    /**
     * 获取所有敌人
     */
    fun getAllEnemies(): List<EnemyInstance> {
        return enemies
    }

    /**
     * 更新敌人
     */
    fun update(delta: Float) {
        // 更新波次计时器
        waveTimer += delta
        if (waveTimer >= waveInterval) {
            spawnWave()
            waveTimer = 0f
        }

        // 更新敌人状态
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            enemy.update(delta)

            // 检查敌人是否死亡
            if (enemy.health <= 0) {
                // 处理敌人掉落
                enemy.model.drops.forEach { drop ->
                    if (Math.random() < drop.chance) {
                        ResourceManager.addResource(drop.itemId, drop.quantity)
                    }
                }
                iterator.remove()
            }

            // 检查敌人是否到达基地
            if (enemy.y > 600) {
                iterator.remove()
                // 这里可以添加基地受到伤害的逻辑
            }
        }
    }

    /**
     * 敌人实例
     */
    data class EnemyInstance(
        val model: EnemyModel,
        var x: Float,
        var y: Float,
        var health: Int,
        var speed: Float,
        var damage: Int
    ) {
        constructor(model: EnemyModel, x: Float, y: Float) : this(
            model,
            x,
            y,
            model.health,
            model.speed,
            model.damage
        )

        /**
         * 更新敌人状态
         */
        fun update(delta: Float) {
            // 敌人向基地移动
            y += speed * delta
        }

        /**
         * 受到伤害
         */
        fun takeDamage(amount: Int) {
            health -= amount
        }
    }
}
