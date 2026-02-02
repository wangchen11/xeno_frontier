package person.wangchen11.gdx.assets.theme

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import person.wangchen11.gdx.assets.FontManager
import person.wangchen11.gdx.assets.dp
import person.wangchen11.gdx.assets.sp
import person.wangchen11.gdx.drawable.ModernDrawable

class GameTheme: ITheme {
    override val fontS: BitmapFont = FontManager.getFont(16.sp)
    override val fontM: BitmapFont = FontManager.getFont(18.sp)
    override val fontL: BitmapFont = FontManager.getFont(24.sp)
    override val fontXL: BitmapFont = FontManager.getFont(32.sp)
    override val fontXXL: BitmapFont = FontManager.getFont(48.sp)

    // Modern color constants
    private val primaryColor = Color.valueOf("#4A90E2")
    private val primaryDark = Color.valueOf("#357ABD")
    private val surfaceColor = Color.valueOf("#1E1E24")
    private val surfaceLight = Color.valueOf("#2A2A32")

    override val labelStyleContext: Label.LabelStyle = Label.LabelStyle().apply {
        background = ModernDrawable(
            backgroundColor = Color(0f, 0f, 0f, 0.6f),
            cornerRadius = 4.dp
        )
        font = fontS
        fontColor = Color.WHITE
    }

    override val textButtonStyle: TextButton.TextButtonStyle = TextButton.TextButtonStyle().apply {
        up = ModernDrawable(
            backgroundColor = primaryColor,
            cornerRadius = 8.dp,
            borderColor = Color(1f, 1f, 1f, 0.2f),
            borderWidth = 1.dp
        ).apply {
            setPadding(4.dp, 12.dp, 4.dp, 12.dp)
        }
        down = ModernDrawable(
            backgroundColor = primaryDark,
            cornerRadius = 8.dp
        ).apply {
            setPadding(4.dp, 12.dp, 4.dp, 12.dp)
        }
        font = fontM
        fontColor = Color.WHITE
        downFontColor = Color.LIGHT_GRAY
    }

    override val textFieldStyle: TextField.TextFieldStyle = TextField.TextFieldStyle().apply {
        background = ModernDrawable(
            backgroundColor = surfaceLight,
            cornerRadius = 6.dp,
            borderColor = Color(1f, 1f, 1f, 0.1f),
            borderWidth = 1.dp
        )
        focusedBackground = ModernDrawable(
            backgroundColor = surfaceLight,
            cornerRadius = 6.dp,
            borderColor = primaryColor,
            borderWidth = 1.5.dp
        )
        cursor = ModernDrawable(backgroundColor = Color.WHITE).apply {
            minWidth = 2.dp
        }
        selection = ModernDrawable(backgroundColor = primaryColor.cpy().apply { a = 0.5f })
        font = fontS
        fontColor = Color.WHITE
    }

    override fun createTextButton(text: String?): TextButton {
        return TextButton(text, textButtonStyle).apply {
            setTransform(true)
            setOrigin(Align.center)
            pad(10.dp, 16.dp, 10.dp, 16.dp)
        }
    }
}
