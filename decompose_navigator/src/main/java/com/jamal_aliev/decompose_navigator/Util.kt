package com.jamal_aliev.decompose_navigator

import kotlin.random.Random.Default.nextLong

inline fun randomId(): Long = nextLong(Long.MIN_VALUE, Long.MAX_VALUE)
