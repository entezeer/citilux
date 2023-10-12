package com.example.citilux.data

import android.graphics.Color
import android.os.Parcelable
import com.example.citilux.extensions.brightnessNormalized
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

interface LightParameters {
    val rgb: Int // RGB color at brightness
    val white: Int // White light at brightness, normalized
    val yellow: Int // Yellow light at brightness, normalized
    val mode: Int // Preset int value
    var uiMode: Int // Same as mode; used for delayed pattern changes

    // Sic; lamp receives RBG, not RGB
    val rbgHex: Int
        get() {
            return (Color.red(rgb) shl 16) or // as 3rd byte
                    (Color.blue(rgb) shl 8) or // as 2nd byte
                    (Color.green(rgb)) // as 1st byte
        }
    val lightHex: Int
        get() {
            return (yellow shl 16) or // as 3rd byte
                    (white shl 8) or // as 2nd byte
                    mode // as 1st byte
        }

    companion object {
        fun fromHex(rbgaHex: Int, lightHex: Int): LightParameters {
            // rgbaHex: Color in [0xRRGGBB]
            val red = (rbgaHex shr 16) and 0xff // 3rd byte
            val blue = (rbgaHex shr 8) and 0xff // 2nd byte
            val green = rbgaHex and 0xff // 1st byte

            val rgbColor = Color.rgb(red, green, blue)

            // lightHex: White light [0xYYWWMM]
            val yellow = lightHex shr 16 and 0xff // 3rd byte
            val white = lightHex shr 8 and 0xff // 2nd byte
            val mode = lightHex and 0xff // 1st byte

            return RawLight(rgbColor, white, yellow, mode)
        }

        fun isOn(params: LightParameters) =
            !(params.rgb == Color.BLACK && params.yellow == 0 && params.white == 0 && params.mode == Lamp.Mode.NORMAL)

        val RANDOM_COLOR: LightParameters
            get() {
                val rgb = Color.rgb(
                    Random.nextInt(255),
                    Random.nextInt(255),
                    Random.nextInt(255)
                )
                return RawLight(rgb, 0, 0, Lamp.Mode.NORMAL)
            }

        val OFF: LightParameters
            get() = RawLight(Color.BLACK, 0, 0, Lamp.Mode.NORMAL)
    }
}

// For getting value from lamp
@Parcelize
data class RawLight(
    override val rgb: Int,
    override val white: Int,
    override val yellow: Int,
    override val mode: Int
) : LightParameters, Parcelable {
    @IgnoredOnParcel
    override var uiMode: Int = mode

    companion object {
        fun from(lightParameters: LightParameters) = RawLight(
            lightParameters.rgb,
            lightParameters.white,
            lightParameters.yellow,
            lightParameters.mode
        )
    }
}

// For sending values to lamp
abstract class AbsoluteLight : LightParameters {
    abstract val rgbAbsolute: Int // RGB (0-255, 0-255, 0-255)
    abstract val whiteAbsolute: Int // White light, absolute (0-255)
    abstract val yellowAbsolute: Int // Yellow light. absolute (0-255)
    abstract val brightnessFraction: Double // Brightness (0.0 - 1.0)

    val brightness
        get() = brightnessFraction.brightnessNormalized

    override val rgb
        get() = Color.rgb(
            (Color.red(rgbAbsolute) * brightness).toInt(),//.ledNormalized,
            (Color.green(rgbAbsolute) * brightness).toInt(),//.ledNormalized,
            (Color.blue(rgbAbsolute) * brightness).toInt()//.ledNormalized
        )
    override val white
        get() = (whiteAbsolute * brightness).toInt()//.ledNormalized
    override val yellow
        get() = (yellowAbsolute * brightness).toInt()//.ledNormalized

    companion object {
        fun calculate(raw: LightParameters): AbsoluteLight {
            val rgbMax = maxOf(
                Color.red(raw.rgb),
                Color.green(raw.rgb),
                Color.blue(raw.rgb)
            )
            val brightness = run {
                if (rgbMax != 0)
                    rgbMax.toDouble() / Lamp.Colors.COLOR_MAX
                else
                    (raw.white + raw.yellow).toDouble() / Lamp.Light.LED_MAX
            }
            return object : AbsoluteLight() {
                override val brightnessFraction = brightness

                override val rgbAbsolute: Int = Color.rgb(
                    (Color.red(raw.rgb) / brightness).toInt(),
                    (Color.green(raw.rgb) / brightness).toInt(),
                    (Color.blue(raw.rgb) / brightness).toInt()
                )
                override val whiteAbsolute: Int = (raw.white / brightness).toInt()
                    .coerceAtMost(Lamp.Light.LED_MAX)
                override val yellowAbsolute: Int = (raw.yellow / brightness).toInt()
                    .coerceAtMost(Lamp.Light.LED_MAX)

                override val mode: Int = raw.mode
                override var uiMode: Int = mode
            }
        }
    }
}

data class ColorLight(
    override val rgbAbsolute: Int,
    override val brightnessFraction: Double,
) : AbsoluteLight() {
    override val whiteAbsolute: Int = 0
    override val yellowAbsolute: Int = 0
    override val white: Int = 0
    override val yellow: Int = 0
    override val mode: Int = Lamp.Mode.NORMAL
    override var uiMode: Int = mode
}

data class WhiteWarmthLight(
    val warmthAbsolute: Int,
    override val brightnessFraction: Double
) : AbsoluteLight() {
    override val yellowAbsolute: Int
        get() = warmthAbsolute
    override val whiteAbsolute: Int
        get() = Lamp.Light.LED_MAX - yellowAbsolute
    override val rgbAbsolute: Int = Lamp.Colors.RGB_BLACK
    override val rgb: Int = Lamp.Colors.RGB_BLACK
    override val mode: Int = Lamp.Mode.NORMAL
    override var uiMode: Int = mode
}

