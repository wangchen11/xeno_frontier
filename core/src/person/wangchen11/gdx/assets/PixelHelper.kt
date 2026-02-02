package person.wangchen11.gdx.assets

import com.badlogic.gdx.Gdx

object PixelHelper {
    private const val TAG = "PixelHelper"

    var density: Float = 1f
    var textScale: Float = 1f

    init {
        Gdx.app.log(TAG, "density: Gdx.graphics.density:${Gdx.graphics.density}")
        density = Gdx.graphics.density
    }
}

val Number.dp: Float
    get() = this.toFloat() * PixelHelper.density

val Number.sp: Float
    get() = this.toFloat() * PixelHelper.density * PixelHelper.textScale