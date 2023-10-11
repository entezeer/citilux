package com.example.citilux.extensions

fun <T> Set<T>.addOrReplace(element: T, isEqual: (T, T) -> Boolean) = map {
    if (isEqual(it, element))
        element
    else it
}.plusElement(element).toSet()