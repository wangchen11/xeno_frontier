package person.wangchen11.gdx.assets

import person.wangchen11.gdx.assets.theme.GameTheme
import person.wangchen11.gdx.assets.theme.ITheme

object ThemeManager {
    val theme: ITheme by lazy {
        GameTheme()
    }
}
