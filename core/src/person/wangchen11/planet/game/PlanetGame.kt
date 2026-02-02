package person.wangchen11.planet.game

import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.planet.game.screen.MainScreen

/**
 * 星球游戏类
 */
class PlanetGame : BaseGame() {
    override fun create() {
        startScreen(MainScreen(this))
    }
}
