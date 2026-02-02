package person.wangchen11.gdx.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import person.wangchen11.gdx.assets.font.BitmapFontDataWrapper

object FontManager {
    private val BASE_FONT_SIZE: Int = 32.sp.toInt()

    private val fontGenerator: FreeTypeFontGenerator by lazy {
        FreeTypeFontGenerator.setMaxTextureSize(4096)
        FreeTypeFontGenerator(Gdx.files.internal("fonts/font.ttf"))
    }

    val texts: String by lazy {
        FreeTypeFontGenerator.DEFAULT_CHARS + Gdx.files.internal("fonts/texts.txt").readString("UTF-8")
    }

    val baseFont by lazy {
        newFont(BASE_FONT_SIZE)
    }

    private val fontCache = HashMap<Int, BitmapFont>()

    fun getFont(fontSize: Float): BitmapFont {
        return getFont(fontSize.toInt())
    }

    fun getFont(fontSize: Int): BitmapFont {
        if (fontSize == BASE_FONT_SIZE) {
            return baseFont
        }
        fontCache[fontSize]?.let {
            return it
        }
        return buildFont(fontSize).also {
            fontCache[fontSize] = it
        }
    }

    private fun buildFont(fontSize: Int): BitmapFont {
        return BitmapFont(BitmapFontDataWrapper(baseFont.data), baseFont.regions, true).also {
            it.data.setScale(fontSize / BASE_FONT_SIZE.toFloat())
        }
    }

    private fun newFont(fontSize: Int): BitmapFont {
        return fontGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = fontSize
            incremental = true
            characters = texts
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
        })
    }
}