package person.wangchen11.gdx.assets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.random.Random

/**
 * 图形管理器
 * 负责创建和管理游戏中的图形元素
 */
object GraphicsManager {
    private val sprites = mutableMapOf<String, Sprite>()

    /**
     * 初始化图形管理器
     */
    fun initialize() {
        createBasicSprites()
        createTerrainSprites()
    }

    /**
     * 创建基本的精灵
     */
    private fun createBasicSprites() {
        // 建筑精灵
        createBuildingSprite("basic_farm", Color.GREEN)
        createBuildingSprite("advanced_farm", Color.LIME)
        createBuildingSprite("super_farm", Color.YELLOW)
        createBuildingSprite("basic_mine", Color.GRAY)
        createBuildingSprite("advanced_mine", Color.DARK_GRAY)
        createBuildingSprite("super_mine", Color.BLACK)
        createBuildingSprite("laser_turret", Color.RED)
        createBuildingSprite("machine_gun_turret", Color.ORANGE)
        createBuildingSprite("rocket_launcher", Color.BROWN)
        createBuildingSprite("wall", Color.GRAY)
        createBuildingSprite("command_center", Color.BLUE)
        createBuildingSprite("resource_depot", Color.YELLOW)
        createBuildingSprite("medical_bay", Color.CYAN)
        createBuildingSprite("communication_tower", Color.PURPLE)
        createBuildingSprite("trading_post", Color.GOLD)

        // 敌人精灵
        createEnemySprite("basic_enemy", Color.RED)
        createEnemySprite("fast_enemy", Color.PINK)
        createEnemySprite("strong_enemy", Color.PURPLE)
        createEnemySprite("ranged_enemy", Color.CYAN)
        createEnemySprite("flying_enemy", Color.WHITE)
        createEnemySprite("exploding_enemy", Color.ORANGE)
        createEnemySprite("poison_enemy", Color.GREEN)
        createEnemySprite("stealth_enemy", Color.GRAY)

        // 作物精灵
        createCropSprite("basic_crop", Color.GREEN)
        createCropSprite("energy_crop", Color.YELLOW)
        createCropSprite("metal_crop", Color.GRAY)
        createCropSprite("defense_crop", Color.RED)
        createCropSprite("healing_crop", Color.BLUE)
    }

    private fun createTerrainSprites() {
        createSolidSprite("terrain_fill", Color.WHITE)
        createTerrainVariants("plain", Color(0.56f, 0.78f, 0.33f, 1f))
        createTerrainVariants("forest", Color(0.13f, 0.49f, 0.15f, 1f))
        createTerrainVariants("desert", Color(0.88f, 0.76f, 0.45f, 1f))
        createTerrainVariants("mountain", Color(0.56f, 0.58f, 0.61f, 1f))
        createTerrainVariants("swamp", Color(0.23f, 0.41f, 0.28f, 1f))
        createTerrainVariants("lava", Color(0.73f, 0.18f, 0.02f, 1f))
        createTerrainBlendMasks()
    }

    private fun createSolidSprite(id: String, color: Color) {
        val pixmap = Pixmap(2, 2, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        sprites[id] = Sprite(texture)
    }

    private fun createTerrainVariants(id: String, baseColor: Color) {
        repeat(3) { variant ->
            val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            val rng = Random(id.hashCode() * 31 + variant * 997)
            pixmap.setColor(baseColor)
            pixmap.fill()

            drawTerrainNoise(pixmap, id, baseColor, rng)

            val texture = Texture(pixmap)
            pixmap.dispose()
            sprites["terrain_${id}_$variant"] = Sprite(texture)
        }
    }

    private fun drawTerrainNoise(pixmap: Pixmap, id: String, baseColor: Color, rng: Random) {
        repeat(180) {
            val x = rng.nextInt(32)
            val y = rng.nextInt(32)
            val shade = 0.82f + rng.nextFloat() * 0.26f
            pixmap.setColor(
                (baseColor.r * shade).coerceIn(0f, 1f),
                (baseColor.g * shade).coerceIn(0f, 1f),
                (baseColor.b * shade).coerceIn(0f, 1f),
                1f
            )
            pixmap.drawPixel(x, y)
        }

        when (id) {
            "plain" -> repeat(20) {
                pixmap.setColor(0.72f, 0.90f, 0.46f, 0.45f)
                val x = rng.nextInt(28)
                val y = rng.nextInt(28)
                pixmap.drawLine(x, y, x + rng.nextInt(4), y + rng.nextInt(3))
            }
            "forest" -> repeat(16) {
                pixmap.setColor(0.07f, 0.28f, 0.09f, 0.55f)
                pixmap.fillCircle(rng.nextInt(32), rng.nextInt(32), 1 + rng.nextInt(2))
            }
            "desert" -> repeat(14) {
                pixmap.setColor(0.97f, 0.88f, 0.60f, 0.45f)
                val y = rng.nextInt(32)
                pixmap.drawLine(0, y, 31, (y + rng.nextInt(3) - 1).coerceIn(0, 31))
            }
            "mountain" -> repeat(14) {
                pixmap.setColor(0.35f, 0.37f, 0.40f, 0.65f)
                pixmap.fillCircle(rng.nextInt(32), rng.nextInt(32), 1 + rng.nextInt(3))
            }
            "swamp" -> repeat(12) {
                pixmap.setColor(0.13f, 0.18f, 0.12f, 0.55f)
                pixmap.fillCircle(rng.nextInt(32), rng.nextInt(32), 2 + rng.nextInt(3))
            }
            "lava" -> repeat(18) {
                pixmap.setColor(0.98f, 0.62f, 0.12f, 0.75f)
                val x = rng.nextInt(30)
                val y = rng.nextInt(30)
                pixmap.drawLine(x, y, x + rng.nextInt(3), y + 1 + rng.nextInt(3))
            }
        }
    }

    private fun createTerrainBlendMasks() {
        for (mask in 1..255) {
            val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            for (x in 0 until 32) {
                for (y in 0 until 32) {
                    val nx = x / 31f
                    val ny = y / 31f
                    val alpha = roundedMaskAlpha(mask, nx, ny)
                    pixmap.setColor(1f, 1f, 1f, alpha)
                    pixmap.drawPixel(x, y)
                }
            }
            val texture = Texture(pixmap)
            pixmap.dispose()
            sprites["terrain_mask_$mask"] = Sprite(texture)
        }
    }

    private fun roundedMaskAlpha(mask: Int, nx: Float, ny: Float): Float {
        val north = has(mask, 1)
        val east = has(mask, 2)
        val south = has(mask, 4)
        val west = has(mask, 8)
        val northwest = has(mask, 16)
        val northeast = has(mask, 32)
        val southeast = has(mask, 64)
        val southwest = has(mask, 128)

        val nw = if (north || west || northwest) 1f else 0f
        val ne = if (north || east || northeast) 1f else 0f
        val se = if (south || east || southeast) 1f else 0f
        val sw = if (south || west || southwest) 1f else 0f

        val top = lerp(nw, ne, nx)
        val bottom = lerp(sw, se, nx)
        val field = lerp(top, bottom, ny)
        return smoothStep(0.42f, 0.58f, field)
    }

    private fun has(mask: Int, bit: Int): Boolean = mask and bit != 0

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        if (edge0 == edge1) return if (value >= edge1) 1f else 0f
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * 创建建筑精灵
     */
    private fun createBuildingSprite(id: String, color: Color) {
        val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
        
        // 绘制建筑主体
        pixmap.setColor(color)
        pixmap.fill()
        
        // 绘制边框
        pixmap.setColor(Color.BLACK)
        pixmap.drawRectangle(0, 0, 31, 31)
        
        // 根据建筑类型添加不同的细节
        when (id) {
            "basic_farm" -> {
                // 农场：添加作物图标
                pixmap.setColor(Color.GREEN)
                pixmap.fillRectangle(8, 16, 4, 12)
                pixmap.fillRectangle(16, 16, 4, 12)
                pixmap.fillRectangle(24, 16, 4, 12)
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(10, 12, 3)
                pixmap.fillCircle(18, 12, 3)
                pixmap.fillCircle(26, 12, 3)
            }
            "advanced_farm" -> {
                // 高级农场：添加更多作物
                pixmap.setColor(Color.GREEN)
                pixmap.fillRectangle(6, 14, 4, 14)
                pixmap.fillRectangle(14, 14, 4, 14)
                pixmap.fillRectangle(22, 14, 4, 14)
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(8, 10, 3)
                pixmap.fillCircle(16, 10, 3)
                pixmap.fillCircle(24, 10, 3)
                pixmap.setColor(Color.ORANGE)
                pixmap.fillCircle(8, 4, 2)
                pixmap.fillCircle(16, 4, 2)
                pixmap.fillCircle(24, 4, 2)
            }
            "super_farm" -> {
                // 超级农场：添加温室效果
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(4, 4, 24, 20)
                pixmap.setColor(Color.GREEN)
                pixmap.fillRectangle(8, 16, 4, 8)
                pixmap.fillRectangle(16, 16, 4, 8)
                pixmap.fillRectangle(24, 16, 4, 8)
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(10, 12, 4)
                pixmap.fillCircle(18, 12, 4)
                pixmap.fillCircle(26, 12, 4)
                // 温室框架
                pixmap.setColor(Color.WHITE)
                pixmap.drawLine(4, 4, 28, 4)
                pixmap.drawLine(4, 12, 28, 12)
                pixmap.drawLine(4, 20, 28, 20)
                pixmap.drawLine(12, 4, 12, 20)
                pixmap.drawLine(20, 4, 20, 20)
            }
            "basic_mine" -> {
                // 矿场：添加矿井和矿石
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillRectangle(8, 16, 16, 12)
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(12, 8, 8, 8)
                pixmap.setColor(Color.GOLD)
                pixmap.fillCircle(10, 20, 2)
                pixmap.fillCircle(14, 20, 2)
                pixmap.fillCircle(18, 20, 2)
                pixmap.fillCircle(22, 20, 2)
            }
            "laser_turret" -> {
                // 激光炮塔：添加炮管和底座
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillCircle(16, 16, 8)
                pixmap.setColor(Color.RED)
                pixmap.fillRectangle(14, 4, 4, 12)
                pixmap.setColor(Color.ORANGE)
                pixmap.fillRectangle(12, 2, 8, 2)
            }
            "machine_gun_turret" -> {
                // 机枪炮塔：添加机枪和底座
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillCircle(16, 16, 8)
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(14, 4, 4, 12)
                pixmap.setColor(Color.BLACK)
                pixmap.fillRectangle(12, 2, 8, 2)
                // 机枪枪管
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(15, 0, 2, 2)
            }
            "command_center" -> {
                // 指挥中心：添加天线和窗户
                pixmap.setColor(Color.BLUE)
                pixmap.fill()
                pixmap.setColor(Color(0.5f, 0.5f, 1.0f, 1.0f)) // 浅蓝色
                pixmap.fillRectangle(8, 8, 6, 6)
                pixmap.fillRectangle(18, 8, 6, 6)
                pixmap.fillRectangle(8, 18, 6, 6)
                pixmap.fillRectangle(18, 18, 6, 6)
                // 天线
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(15, 4, 2, 4)
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillCircle(16, 2, 2)
            }
            "advanced_mine" -> {
                // 高级矿场：添加更多矿井和矿石
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fill()
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(8, 16, 16, 12)
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(12, 8, 8, 8)
                // 更多矿石
                pixmap.setColor(Color.GOLD)
                pixmap.fillCircle(10, 20, 2)
                pixmap.fillCircle(14, 20, 2)
                pixmap.fillCircle(18, 20, 2)
                pixmap.fillCircle(22, 20, 2)
                pixmap.fillCircle(12, 16, 2)
                pixmap.fillCircle(20, 16, 2)
            }
            "super_mine" -> {
                // 超级矿场：添加大型矿井和传送带
                pixmap.setColor(Color.BLACK)
                pixmap.fill()
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillRectangle(6, 14, 20, 14)
                // 大型矿井
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(10, 6, 12, 10)
                // 传送带
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(4, 24, 24, 4)
                // 矿石
                pixmap.setColor(Color.GOLD)
                pixmap.fillCircle(10, 26, 2)
                pixmap.fillCircle(16, 26, 2)
                pixmap.fillCircle(22, 26, 2)
                pixmap.fillCircle(10, 20, 2)
                pixmap.fillCircle(16, 20, 2)
                pixmap.fillCircle(22, 20, 2)
            }
            "rocket_launcher" -> {
                // 火箭发射器：添加火箭和底座
                pixmap.setColor(Color.BROWN)
                pixmap.fill()
                // 底座
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillRectangle(8, 16, 16, 8)
                // 火箭
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(14, 8, 4, 8)
                // 火箭头部
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillTriangle(12, 8, 18, 8, 15, 4)
            }
            "wall" -> {
                // 防御墙：简单的墙体
                pixmap.setColor(Color.GRAY)
                pixmap.fill()
                // 墙体纹理
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.drawLine(0, 16, 31, 16)
                pixmap.drawLine(16, 0, 16, 31)
            }
            "resource_depot" -> {
                // 资源仓库：添加存储箱
                pixmap.setColor(Color.YELLOW)
                pixmap.fill()
                // 存储箱
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillRectangle(8, 8, 8, 8)
                pixmap.fillRectangle(16, 8, 8, 8)
                pixmap.fillRectangle(8, 16, 8, 8)
                pixmap.fillRectangle(16, 16, 8, 8)
            }
            "medical_bay" -> {
                // 医疗舱：添加医疗标志
                pixmap.setColor(Color.CYAN)
                pixmap.fill()
                // 医疗十字标志
                pixmap.setColor(Color.WHITE)
                pixmap.fillRectangle(14, 8, 4, 16)
                pixmap.fillRectangle(8, 14, 16, 4)
            }
            "communication_tower" -> {
                // 通讯塔：添加高天线
                pixmap.setColor(Color.PURPLE)
                pixmap.fill()
                // 塔基
                pixmap.setColor(Color.DARK_GRAY)
                pixmap.fillRectangle(12, 16, 8, 12)
                // 天线
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(15, 8, 2, 8)
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillCircle(16, 6, 2)
                // 天线横杆
                pixmap.drawLine(10, 10, 22, 10)
            }
            "trading_post" -> {
                // 交易站：添加交易标志
                pixmap.setColor(Color.GOLD)
                pixmap.fill()
                // 交易标志
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(16, 16, 8)
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(16, 16, 4)
            }
        }
        
        val texture = Texture(pixmap)
        pixmap.dispose()
        val sprite = Sprite(texture)
        sprites[id] = sprite
    }

    /**
     * 创建敌人精灵
     */
    private fun createEnemySprite(id: String, color: Color) {
        val pixmap = Pixmap(24, 24, Pixmap.Format.RGBA8888)
        
        // 绘制敌人主体
        pixmap.setColor(color)
        pixmap.fillCircle(12, 12, 10)
        
        // 绘制黑色边框
        pixmap.setColor(Color.BLACK)
        pixmap.drawCircle(12, 12, 10)
        
        // 根据敌人类型添加不同的细节
        when (id) {
            "basic_enemy" -> {
                // 基本敌人：添加眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
                // 嘴巴
                pixmap.drawLine(9, 14, 15, 14)
            }
            "fast_enemy" -> {
                // 快速敌人：添加翅膀
                pixmap.setColor(Color.PINK)
                pixmap.fillCircle(12, 12, 8)
                pixmap.setColor(Color(1.0f, 0.75f, 0.8f, 1.0f)) // 浅粉色
                // 翅膀
                pixmap.fillTriangle(4, 12, 12, 4, 12, 12)
                pixmap.fillTriangle(20, 12, 12, 4, 12, 12)
                // 眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
            }
            "strong_enemy" -> {
                // 强壮敌人：添加盔甲
                pixmap.setColor(Color.PURPLE)
                pixmap.fillCircle(12, 12, 10)
                pixmap.setColor(Color(0.5f, 0.0f, 0.5f, 1.0f)) // 深紫色
                // 盔甲
                pixmap.fillTriangle(8, 8, 16, 8, 12, 4)
                pixmap.fillRectangle(8, 12, 8, 4)
                // 眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.RED)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
                // 嘴巴
                pixmap.drawLine(8, 14, 16, 14)
                pixmap.drawLine(10, 16, 14, 16)
            }
            "ranged_enemy" -> {
                // 远程敌人：添加武器
                pixmap.setColor(Color.CYAN)
                pixmap.fillCircle(12, 12, 9)
                // 武器
                pixmap.setColor(Color.GRAY)
                pixmap.fillRectangle(18, 10, 4, 4)
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillRectangle(22, 11, 2, 2)
                // 眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(13, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(13, 10, 1)
            }
            "flying_enemy" -> {
                // 飞行敌人：添加翅膀
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(12, 12, 8)
                // 翅膀
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillTriangle(4, 12, 12, 4, 12, 12)
                pixmap.fillTriangle(20, 12, 12, 4, 12, 12)
                // 眼睛
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
            }
            "exploding_enemy" -> {
                // 自爆敌人：添加爆炸标志
                pixmap.setColor(Color.ORANGE)
                pixmap.fillCircle(12, 12, 9)
                // 爆炸标志
                pixmap.setColor(Color.RED)
                pixmap.fillCircle(12, 12, 5)
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(12, 12, 3)
                // 眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
            }
            "poison_enemy" -> {
                // 毒雾敌人：添加毒雾效果
                pixmap.setColor(Color.GREEN)
                pixmap.fillCircle(12, 12, 9)
                // 毒雾效果
                pixmap.setColor(Color(0.5f, 1.0f, 0.5f, 1.0f)) // 浅绿色
                pixmap.fillCircle(8, 8, 3)
                pixmap.fillCircle(16, 8, 3)
                pixmap.fillCircle(12, 16, 3)
                // 眼睛
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
            }
            "stealth_enemy" -> {
                // 隐形敌人：半透明效果
                pixmap.setColor(Color.GRAY)
                pixmap.fillCircle(12, 12, 9)
                // 半透明效果（通过纹理模拟）
                pixmap.setColor(Color(0.5f, 0.5f, 0.5f, 0.5f))
                pixmap.fillCircle(12, 12, 7)
                // 眼睛
                pixmap.setColor(Color.WHITE)
                pixmap.fillCircle(9, 10, 2)
                pixmap.fillCircle(15, 10, 2)
                pixmap.setColor(Color.BLACK)
                pixmap.fillCircle(9, 10, 1)
                pixmap.fillCircle(15, 10, 1)
            }
        }
        
        val texture = Texture(pixmap)
        pixmap.dispose()
        val sprite = Sprite(texture)
        sprites[id] = sprite
    }

    /**
     * 创建作物精灵
     */
    private fun createCropSprite(id: String, color: Color) {
        val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
        
        // 绘制作物
        when (id) {
            "basic_crop" -> {
                // 基本作物：小麦
                pixmap.setColor(Color.GREEN)
                // 茎
                pixmap.fillRectangle(7, 8, 2, 6)
                // 叶子
                pixmap.fillTriangle(4, 10, 7, 8, 7, 12)
                pixmap.fillTriangle(9, 8, 12, 10, 9, 12)
                // 麦穗
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(8, 6, 3)
                pixmap.fillCircle(6, 4, 2)
                pixmap.fillCircle(10, 4, 2)
            }
            "energy_crop" -> {
                // 能量作物：向日葵
                pixmap.setColor(Color.GREEN)
                // 茎
                pixmap.fillRectangle(7, 8, 2, 6)
                // 叶子
                pixmap.fillTriangle(4, 10, 7, 8, 7, 12)
                pixmap.fillTriangle(9, 8, 12, 10, 9, 12)
                // 花盘
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(8, 6, 3)
                // 花籽
                pixmap.setColor(Color(0.6f, 0.4f, 0.2f, 1.0f)) // 棕色
                pixmap.fillCircle(8, 6, 1)
            }
            "metal_crop" -> {
                // 金属作物：矿石植物
                pixmap.setColor(Color.GRAY)
                // 茎
                pixmap.fillRectangle(7, 8, 2, 6)
                // 叶子
                pixmap.setColor(Color.LIGHT_GRAY)
                pixmap.fillTriangle(4, 10, 7, 8, 7, 12)
                pixmap.fillTriangle(9, 8, 12, 10, 9, 12)
                // 矿石
                pixmap.setColor(Color.GOLD)
                pixmap.fillCircle(8, 6, 3)
                // 矿石斑点
                pixmap.setColor(Color.YELLOW)
                pixmap.fillCircle(7, 5, 1)
                pixmap.fillCircle(9, 5, 1)
                pixmap.fillCircle(8, 7, 1)
            }
            "defense_crop" -> {
                // 防御作物：刺球
                pixmap.setColor(Color.GREEN)
                // 茎
                pixmap.fillRectangle(7, 10, 2, 4)
                // 刺球
                pixmap.setColor(Color.RED)
                pixmap.fillCircle(8, 8, 4)
                // 刺
                pixmap.setColor(Color.GREEN)
                pixmap.drawLine(8, 4, 8, 2)
                pixmap.drawLine(12, 8, 14, 8)
                pixmap.drawLine(8, 12, 8, 14)
                pixmap.drawLine(4, 8, 2, 8)
                pixmap.drawLine(4, 4, 2, 2)
                pixmap.drawLine(12, 4, 14, 2)
                pixmap.drawLine(4, 12, 2, 14)
                pixmap.drawLine(12, 12, 14, 14)
            }
            "healing_crop" -> {
                // 治疗作物：蓝色草药
                pixmap.setColor(Color.GREEN)
                // 茎
                pixmap.fillRectangle(7, 8, 2, 6)
                // 叶子
                pixmap.setColor(Color(0.5f, 1.0f, 0.5f, 1.0f)) // 浅绿色
                pixmap.fillTriangle(4, 10, 7, 8, 7, 12)
                pixmap.fillTriangle(9, 8, 12, 10, 9, 12)
                // 花朵
                pixmap.setColor(Color.BLUE)
                pixmap.fillCircle(8, 6, 3)
                // 花瓣
                pixmap.setColor(Color(0.5f, 0.5f, 1.0f, 1.0f)) // 浅蓝色
                pixmap.fillCircle(6, 5, 1)
                pixmap.fillCircle(10, 5, 1)
                pixmap.fillCircle(8, 8, 1)
                pixmap.fillCircle(8, 4, 1)
            }
        }
        
        val texture = Texture(pixmap)
        pixmap.dispose()
        val sprite = Sprite(texture)
        sprites[id] = sprite
    }

    /**
     * 获取精灵
     */
    fun getSprite(id: String): Sprite? {
        return sprites[id]
    }

    /**
     * 绘制网格
     */
    fun drawGrid(batch: SpriteBatch, width: Int, height: Int, cellSize: Int) {
        val gridSprite = getOrCreateGridSprite()
        for (x in 0 until width step cellSize) {
            for (y in 0 until height step cellSize) {
                gridSprite.setPosition(x.toFloat(), y.toFloat())
                gridSprite.draw(batch)
            }
        }
    }

    /**
     * 获取或创建网格精灵
     */
    private fun getOrCreateGridSprite(): Sprite {
        return sprites.getOrPut("grid") {
            val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.DARK_GRAY)
            pixmap.drawLine(0, 0, 31, 0)
            pixmap.drawLine(0, 0, 0, 31)
            val texture = Texture(pixmap)
            pixmap.dispose()
            Sprite(texture)
        }
    }

    /**
     * 释放所有资源
     */
    fun dispose() {
        sprites.values.forEach { it.texture.dispose() }
        sprites.clear()
    }
}
