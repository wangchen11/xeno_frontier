package person.wangchen11.gdx.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import person.wangchen11.gdx.game.screen.BaseScreen
import java.lang.IllegalArgumentException

abstract class BaseGame: Game() {
    private val screenStack = ArrayList<BaseScreen>()

    fun startScreen(screen: BaseScreen) {
        if (screenStack.indexOf(screen) >= 0) {
            throw IllegalArgumentException("screen already in screenStack")
        }

        screenStack.add(screen)
        setScreen(screen)
    }

    fun finishScreen(screen: BaseScreen) {
        val find = screenStack.remove(screen)
        if (find) {
            setScreen(screenStack.lastOrNull())
            screen.dispose()
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.render()
    }
}