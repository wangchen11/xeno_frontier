package person.wangchen11.planet.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import person.wangchen11.gdx.game.BaseGame
import person.wangchen11.gdx.game.screen.BaseScreen
import person.wangchen11.planet.game.GameManager
import person.wangchen11.planet.game.ResourceManager
import person.wangchen11.planet.game.BuildingManager
import person.wangchen11.planet.game.EnemyManager
import person.wangchen11.planet.game.CropManager
import person.wangchen11.planet.game.TechManager

/**
 * 主游戏屏幕
 */
class MainScreen(game: BaseGame) : BaseScreen(game) {
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    init {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    }

    override fun show() {
        super.show()
        GameManager.initialize()
    }

    override fun render(delta: Float) {
        // 清除屏幕
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // 更新游戏
        GameManager.update(delta)

        // 处理输入
        handleInput()

        // 渲染游戏
        batch.begin()
        batch.projectionMatrix = camera.combined

        // 渲染建筑
        BuildingManager.getAllBuildings().forEach { building ->
            font.draw(batch, building.model.name, building.x * 32f, Gdx.graphics.height - building.y * 32f)
        }

        // 渲染敌人
        EnemyManager.getAllEnemies().forEach { enemy ->
            font.draw(batch, "E", enemy.x, Gdx.graphics.height - enemy.y)
        }

        // 渲染作物
        CropManager.getAllCrops().forEach { crop ->
            font.draw(batch, "C", crop.x * 32f, Gdx.graphics.height - crop.y * 32f)
        }

        // 渲染UI
        renderUI()

        batch.end()

        super.render(delta)
    }

    /**
     * 处理输入
     */
    private fun handleInput() {
        // 键盘输入
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            // 建造模式
            BuildingManager.createBuilding("basic_farm", 10, 10)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            // 生成敌人
            EnemyManager.spawnWave()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            // 种植作物
            CropManager.plantCrop("basic_crop", 5, 5)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            // 增加科技点
            TechManager.addResearchPoints(10)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            // 增加资源
            ResourceManager.addResource("wood", 100)
            ResourceManager.addResource("stone", 100)
            ResourceManager.addResource("metal", 50)
            ResourceManager.addResource("energy", 50)
        }

        // 鼠标输入
        if (Gdx.input.justTouched()) {
            val x = Gdx.input.x / 32
            val y = (Gdx.graphics.height - Gdx.input.y) / 32

            // 点击建造
            BuildingManager.createBuilding("basic_farm", x, y)
        }
    }

    /**
     * 渲染UI
     */
    private fun renderUI() {
        val resources = ResourceManager.getAllResources()
        var y = Gdx.graphics.height - 20f

        font.draw(batch, "Resources:", 20f, y)
        y -= 20f

        resources.forEach { (resource, amount) ->
            font.draw(batch, "$resource: $amount", 20f, y)
            y -= 20f
        }

        font.draw(batch, "Research Points: ${TechManager.getResearchPoints()}", 20f, y)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        super.dispose()
    }
}
