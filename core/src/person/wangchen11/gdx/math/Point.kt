package person.wangchen11.gdx.math


class Point(
    @JvmField
    var x: Int = 0,
    @JvmField
    var y: Int = 0
) {
    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun set(point: Point) {
        this.x = point.x
        this.y = point.y
    }
}
