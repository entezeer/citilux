package com.example.citilux.extensions

import android.graphics.Color
import com.example.citilux.data.Lamp

fun <T> Set<T>.addOrReplace(element: T, isEqual: (T, T) -> Boolean) = map {
    if (isEqual(it, element))
        element
    else it
}.plusElement(element).toSet()

val Int.ledNormalized: Int
    get() {
        return if (this >= Lamp.Light.LED_THRESHOLD_UP)
            Lamp.Light.LED_MAX
        else this.coerceAtLeast(Lamp.Light.LED_THRESHOLD_DOWN)
    }

val Double.brightnessNormalized: Double
    get() {
        return if (this >= Lamp.Light.BRIGHTNESS_THRESHOLD_UP)
            Lamp.Light.BRIGHTNESS_MAX
        else this.coerceAtLeast(Lamp.Light.BRIGHTNESS_THRESHOLD_DOWN)
    }

val Int.rgbNormalized: Int
    get() {
        return Color.rgb(
            Color.red(this).ledNormalized,
            Color.green(this).ledNormalized,
            Color.blue(this).ledNormalized
        )
    }