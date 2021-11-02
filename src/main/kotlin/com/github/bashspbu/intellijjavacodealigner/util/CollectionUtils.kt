package com.github.bashspbu.intellijjavacodealigner.util

import java.util.Comparator


internal fun <T: Comparable<T>> Iterable<T>.isSorted() =
        zipWithNext { a, b -> a <= b }.all { it }

internal fun <T> Iterable<T>.isSortedWith(comparator: Comparator<T>) =
        zipWithNext { a, b -> comparator.compare(a, b) <= 0 }.all { it }