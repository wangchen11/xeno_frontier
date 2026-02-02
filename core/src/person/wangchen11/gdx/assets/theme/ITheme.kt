package person.wangchen11.gdx.assets.theme

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import org.w3c.dom.Text

interface ITheme {
    val fontS: BitmapFont
    val fontM: BitmapFont
    val fontL: BitmapFont
    val fontXL: BitmapFont
    val fontXXL: BitmapFont

    val labelStyleContext: LabelStyle
    val textButtonStyle: TextButtonStyle
    val textFieldStyle: TextFieldStyle

    fun createTextButton(text: String? = null): TextButton
}