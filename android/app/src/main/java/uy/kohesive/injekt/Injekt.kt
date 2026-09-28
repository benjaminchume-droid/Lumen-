package uy.kohesive.injekt

/** Minimal Injekt host so extension APKs that call Injekt.get() do not crash. */
object Injekt {
    private val map = mutableMapOf<String, Any>()

    fun <T : Any> addSingleton(instance: T) {
        map[instance::class.java.name] = instance
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: Class<T>): T {
        return (map[clazz.name] as? T)
            ?: throw IllegalStateException("Injekt: ${clazz.name} not registered")
    }

    inline fun <reified T : Any> get(): T = get(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(clazz: Class<T>): T? = map[clazz.name] as? T
}
