package person.wangchen11.gdx.helper

import com.badlogic.gdx.math.Vector2

class LinearProgress(val from: Vector2, val to: Vector2) {
    val delta = Vector2(to).sub(from)

    fun apply(out: Vector2, progress: Float) {
        out.x = from.x + delta.x * progress
        out.y = from.y + delta.y * progress
    }

    fun getProgress(progress: Float): Vector2 {
        return Vector2().also {
            apply(it, progress)
        }
    }
}