package person.wangchen11.planet.game

import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.planet.game.screen.MainMenuScreen
import person.wangchen11.planet.i18n.LocalizationManager

class PlanetGame : BaseGame() {
    override fun create() {
        LocalizationManager.initialize()
        startScreen(MainMenuScreen(this))
    }
}
