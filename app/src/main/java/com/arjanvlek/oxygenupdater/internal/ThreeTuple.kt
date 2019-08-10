package com.arjanvlek.oxygenupdater.internal

class ThreeTuple<F, S, T> {

    var first: F? = null
        private set
    var second: S? = null
        private set
    var third: T? = null
        private set

    companion object {
        fun <F, S, T> create(first: F, second: S, third: T): ThreeTuple<F, S, T> {
            val instance = ThreeTuple<F, S, T>()
            instance.first = first
            instance.second = second
            instance.third = third
            return instance
        }
    }
}
