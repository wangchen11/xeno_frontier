package person.wangchen11.planet.game

import person.wangchen11.gdx.assets.SoundManager
import person.wangchen11.planet.i18n.LocalizationManager
import person.wangchen11.planet.metadata.MetadataManager

object GameManager {
    private const val START_COLONY_HP = 100
    private const val DAY_LENGTH_SECONDS = 90f

    var isInitialized = false
        private set

    var colonyHp = START_COLONY_HP
        private set

    var elapsedTime = 0f
        private set

    var day = 1
        private set

    var isGameOver = false
        private set

    fun initialize() {
        if (isInitialized) return

        MetadataManager.initialize()
        MapManager.initialize()
        SoundManager.initialize()

        ResourceManager.initialize()
        BuildingManager.initialize()
        EnemyManager.initialize()
        CropManager.initialize()
        TechManager.initialize()

        ensureStartingCommandCenter()

        colonyHp = START_COLONY_HP
        elapsedTime = 0f
        day = 1
        isGameOver = false
        isInitialized = true

        SoundManager.playMusic("background")
    }

    private fun ensureStartingCommandCenter() {
        val preferredX = MapManager.MAP_WIDTH / 2 - 1
        val preferredY = MapManager.MAP_HEIGHT / 2 - 1
        if (BuildingManager.createFreeBuilding("command_center", preferredX, preferredY) != null) {
            return
        }

        for (radius in 1..8) {
            for (y in (preferredY - radius)..(preferredY + radius)) {
                for (x in (preferredX - radius)..(preferredX + radius)) {
                    if (BuildingManager.createFreeBuilding("command_center", x, y) != null) {
                        return
                    }
                }
            }
        }
    }

    fun update(delta: Float) {
        if (!isInitialized || isGameOver) return

        elapsedTime += delta
        day = 1 + (elapsedTime / DAY_LENGTH_SECONDS).toInt()

        BuildingManager.update(delta)
        CropManager.update(delta)
        EnemyManager.update(delta)
        ResourceManager.update(delta)

        if (colonyHp <= 0 || !BuildingManager.hasCommandCenter()) {
            colonyHp = 0
            isGameOver = true
        }
    }

    fun damageColony(amount: Int) {
        if (isGameOver) return
        colonyHp = (colonyHp - amount).coerceAtLeast(0)
        if (colonyHp == 0) {
            isGameOver = true
        }
    }

    fun healColony(amount: Int) {
        if (isGameOver) return
        colonyHp = (colonyHp + amount).coerceAtMost(START_COLONY_HP)
    }

    fun getObjectiveSummary(): String {
        val surviveReady = if (day >= 4) LocalizationManager.tr("ui.objective.done") else LocalizationManager.tr("ui.objective.pending")
        val waveReady = if (EnemyManager.getWaveCount() >= 3) LocalizationManager.tr("ui.objective.done") else LocalizationManager.tr("ui.objective.pending")
        val techReady = if (TechManager.getResearchedTechs().size >= 4) LocalizationManager.tr("ui.objective.done") else LocalizationManager.tr("ui.objective.pending")
        return LocalizationManager.format("ui.objectives", surviveReady, waveReady, techReady)
    }

    fun hasWon(): Boolean {
        return !isGameOver &&
            day >= 4 &&
            EnemyManager.getWaveCount() >= 3 &&
            TechManager.getResearchedTechs().size >= 4
    }

    fun reset() {
        ResourceManager.reset()
        BuildingManager.reset()
        EnemyManager.reset()
        CropManager.reset()
        TechManager.reset()
        SoundManager.stopMusic()

        colonyHp = START_COLONY_HP
        elapsedTime = 0f
        day = 1
        isGameOver = false
        isInitialized = false
    }
}
