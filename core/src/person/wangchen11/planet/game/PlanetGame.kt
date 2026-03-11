package person.wangchen11.planet.game

import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.planet.game.screen.MainMenuScreen
import person.wangchen11.planet.game.screen.TerrainTransitionTestScreen
import person.wangchen11.planet.i18n.LocalizationManager

class PlanetGame(
    private val launchMode: LaunchMode = LaunchMode.NORMAL
) : BaseGame() {
    override fun create() {
        LocalizationManager.initialize()
        when (launchMode) {
            LaunchMode.NORMAL -> startScreen(MainMenuScreen(this))
            LaunchMode.TERRAIN_TRANSITION_TEST -> startScreen(TerrainTransitionTestScreen(this))
        }
    }

    enum class LaunchMode {
        NORMAL,
        TERRAIN_TRANSITION_TEST
    }
}
