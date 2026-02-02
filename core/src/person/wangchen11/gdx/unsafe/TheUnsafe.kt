package person.wangchen11.gdx.unsafe

import sun.misc.Unsafe

object TheUnsafe {
    val unsafe = loadUnsafe()

    private fun loadUnsafe() : Unsafe {
        val theUnsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafeField.isAccessible = true
        return theUnsafeField.get(null) as Unsafe
    }
}

val unsafe: Unsafe = TheUnsafe.unsafe