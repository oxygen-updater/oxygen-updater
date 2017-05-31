package com.arjanvlek.oxygenupdater.internal;

public class ThreeTuple<F, S, T> {

    private F first;
    private S second;
    private T third;

    public static <F, S, T> ThreeTuple<F, S, T> create(F first, S second, T third) {
        ThreeTuple<F, S, T> instance = new ThreeTuple<>();
        instance.first = first;
        instance.second = second;
        instance.third = third;
        return instance;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public T getThird() {
        return third;
    }
}
