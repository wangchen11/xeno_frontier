package person.wangchen11.gdx.assets.font

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData

class BitmapFontDataWrapper(private val base: BitmapFontData): BitmapFontData() {
    init {
        lineHeight = base.lineHeight
        spaceXadvance = base.spaceXadvance
        xHeight = base.xHeight
        capHeight = base.capHeight
        ascent = base.ascent
        descent = base.descent
        down = base.down
        padLeft = base.padLeft
        padRight = base.padRight
        padTop = base.padTop
        padBottom = base.padBottom
        scaleX = base.scaleX
        scaleY = base.scaleY
    }

    override fun getGlyph(ch: Char): BitmapFont.Glyph? {
        return base.getGlyph(ch)
    }
}