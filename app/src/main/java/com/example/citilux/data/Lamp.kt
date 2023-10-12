package com.example.citilux.data

import android.graphics.Color

object Lamp {
    const val LAMP_VOLUME_MAX = 31

    // compat pattern codes
    // currently only for usage in patterns
    object Mode {
        const val NORMAL = 80
        const val WARMTH = 89
        const val RAINBOW = 83
        const val CANDLE = 90
        const val JUMP = 97
        const val DISCO = 81
    }

    object Colors {
        val RGB_BLACK = Color.BLACK // rgb color not set
        val RGB_NIGHT_BLUE = Color.rgb(0, 0, 5) // night mode blue

        const val COLOR_MAX = 255 // max value for r/g/b/ color
        const val COLOR_THRESHOLD_UP = 250
    }

    object Light {
        const val LED_THRESHOLD_UP = 250
        const val LED_THRESHOLD_DOWN = 6 // minimal white/yellow leds light level
        const val LED_MAX = 255 // max value for r/g/b/ color

        // max brightness at 50% warmth
        const val BALANCE_LED_MAX = 128

        const val BRIGHTNESS_THRESHOLD_UP = 0.95
        const val BRIGHTNESS_THRESHOLD_DOWN = 0.05
        const val BRIGHTNESS_MAX = 1.0
    }
}