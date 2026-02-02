package person.wangchen11.gdx.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.scenes.scene2d.Stage
import person.wangchen11.gdx.game.BaseGame

abstract class BaseScreen(val game: BaseGame): ScreenAdapter() {
    var renderTime: Float = 0f
    var stage: Stage? = null

    fun finish() {
        game.finishScreen(this)
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = stage
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        stage?.viewport?.update(width, height, true)
    }

    override fun render(delta: Float) {
        renderTime += delta
        super.render(delta)
        stage?.let {
            it.act()
            it.draw()
        }
    }

    override fun dispose() {
        stage?.dispose()
        super.dispose()
    }
}