package uy.kohesive.injekt

/** Minimal Injekt host so extension APKs that call Injekt.get() do not crash. */
object Injekt {
    // public so public inline get() can access it
    val map = mutableMapOf<String, Any>()

    fun <T : Any> addSingleton(instance: T) {
        map[instance::class.java.name] = instance
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> get(): T {
        return (map[T::class.java.name] as? T)
            ?: throw IllegalStateException("Injekt: ${T::class.java.name} not registered")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(clazz: Class<T>): T? = map[clazz.name] as? T
}
