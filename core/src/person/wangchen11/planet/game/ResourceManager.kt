package person.wangchen11.planet.game

object ResourceManager {
    private val resources = mutableMapOf<String, Int>()
    private val fractionalProduction = mutableMapOf<String, Float>()

    fun initialize() {
        resources.clear()
        resources["wood"] = 160
        resources["stone"] = 120
        resources["metal"] = 80
        resources["energy"] = 60
        resources["food"] = 80
        fractionalProduction.clear()
    }

    fun reset() {
        resources.clear()
        fractionalProduction.clear()
    }

    fun getResource(resourceId: String): Int = resources.getOrDefault(resourceId, 0)

    fun addResource(resourceId: String, amount: Int) {
        resources[resourceId] = getResource(resourceId) + amount
    }

    fun consumeResource(cost: Map<String, Int>): Boolean {
        if (cost.any { getResource(it.key) < it.value }) {
            return false
        }
        cost.forEach { (resourceId, amount) ->
            resources[resourceId] = getResource(resourceId) - amount
        }
        return true
    }

    fun getAllResources(): Map<String, Int> = resources

    fun update(delta: Float) {
        BuildingManager.getAllBuildings()
            .filter { it.isCompleted() }
            .forEach { building ->
                when (building.model.id) {
                    "basic_farm" -> produce("food", 1.2f * delta)
                    "advanced_farm" -> produce("food", 2.2f * delta)
                    "super_farm" -> produce("food", 3.8f * delta)
                    "basic_mine" -> {
                        produce("stone", 1.0f * delta)
                        produce("metal", 0.4f * delta)
                    }
                    "advanced_mine" -> {
                        produce("stone", 1.6f * delta)
                        produce("metal", 0.9f * delta)
                    }
                    "super_mine" -> {
                        produce("stone", 2.4f * delta)
                        produce("metal", 1.4f * delta)
                    }
                    "command_center" -> produce("energy", 0.5f * delta)
                    "communication_tower" -> produce("energy", 0.2f * delta)
                }
            }
    }

    private fun produce(resourceId: String, amount: Float) {
        val total = fractionalProduction.getOrDefault(resourceId, 0f) + amount
        val whole = total.toInt()
        fractionalProduction[resourceId] = total - whole
        if (whole > 0) {
            addResource(resourceId, whole)
        }
    }
}
