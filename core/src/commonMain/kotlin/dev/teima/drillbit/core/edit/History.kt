package dev.teima.drillbit.core.edit

/** Undo / redo over immutable values. Pushing a new value clears the redo stack. */
data class History<T>(
    val present: T,
    private val past: List<T> = emptyList(),
    private val future: List<T> = emptyList(),
    private val limit: Int = DEFAULT_LIMIT,
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun push(value: T): History<T> {
        if (value == present) return this
        return copy(present = value, past = (past + present).takeLast(limit), future = emptyList())
    }

    fun undo(): History<T> {
        if (!canUndo) return this
        return copy(present = past.last(), past = past.dropLast(1), future = listOf(present) + future)
    }

    fun redo(): History<T> {
        if (!canRedo) return this
        return copy(present = future.first(), past = past + present, future = future.drop(1))
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}
