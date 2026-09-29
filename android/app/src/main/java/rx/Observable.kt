package rx

/**
 * Minimal RxJava1-compatible Observable so Keiyoushi extensions that return
 * rx.Observable can be invoked without shipping full RxJava.
 * Supports the subset extensions actually call: just(), error(), map, blockingFirst.
 */
class Observable<T> private constructor(
    private val producer: () -> T
) {
    fun <R> map(fn: (T) -> R): Observable<R> = Observable { fn(producer()) }

    fun blockingFirst(): T = producer()

    fun toBlocking(): BlockingObservable<T> = BlockingObservable(this)

    fun subscribe(
        onNext: (T) -> Unit,
        onError: (Throwable) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        try {
            onNext(producer())
            onCompleted()
        } catch (t: Throwable) {
            onError(t)
        }
    }

    companion object {
        fun <T> just(value: T): Observable<T> = Observable { value }

        fun <T> from(iterable: Iterable<T>): Observable<T> = Observable {
            iterable.firstOrNull() ?: throw NoSuchElementException("empty")
        }

        fun <T> error(t: Throwable): Observable<T> = Observable { throw t }

        fun <T> fromCallable(callable: () -> T): Observable<T> = Observable(callable)
    }
}

class BlockingObservable<T>(private val source: Observable<T>) {
    fun first(): T = source.blockingFirst()
    fun single(): T = source.blockingFirst()
}
