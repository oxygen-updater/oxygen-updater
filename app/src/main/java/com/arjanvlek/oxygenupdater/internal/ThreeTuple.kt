package com.arjanvlek.oxygenupdater.internal

data class ThreeTuple<F, S, T>(val first: F, val second: S, val third: T) {

    companion object {
        fun <F, S, T> create(first: F, second: S, third: T): ThreeTuple<F, S, T> {
            return ThreeTuple(first, second, third)
        }
    }
}
