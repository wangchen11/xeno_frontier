package person.wangchen11.gdx.helper

fun Float.lerp(target: Float, alpha: Float): Float {
    return alpha * (target - this)
}