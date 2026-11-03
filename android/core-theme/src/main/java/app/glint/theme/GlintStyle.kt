package app.glint.theme

import kotlinx.serialization.Serializable

/** The three looks a user can switch between. One codebase, three renderers. */
@Serializable
enum class StyleKind { Bento, Ink, Liquid }

/**
 * Colour and shape tokens for one style. ARGB ints so the same values feed
 * Compose (launcher), Glance (widgets) and Canvas (baked glass).
 */
data class GlintPalette(
    val kind: StyleKind,
    val name: String,
    val background: Int,
    /** Wallpaper gradient stops for Liquid; solid background for others. */
    val wallpaper: List<Int>,
    val surface: Int,
    /** Tile colours used in order by Bento: clock, weather, battery, agenda, steps. */
    val tiles: List<Int>,
    val onTile: Int,
    val onBackground: Int,
    val onBackgroundDim: Int,
    val accent: Int,
    val cornerDp: Float,
    /** Liquid only: glass fill opacity and blur radius for the launcher. */
    val glassOpacity: Float = 0f,
    val blurDp: Float = 0f,
    val serifClock: Boolean = false,
)

object GlintStyles {
    val Bento = GlintPalette(
        kind = StyleKind.Bento, name = "Bento",
        background = 0xFF0F1013.toInt(), wallpaper = listOf(0xFF0F1013.toInt()),
        surface = 0xFF1D1F25.toInt(),
        tiles = listOf(0xFFFFB59E, 0xFFA9C8FF, 0xFFB5E8C8, 0xFFD2BDFF, 0xFFF2DCA6).map { it.toInt() },
        onTile = 0xFF121316.toInt(), onBackground = 0xFFE9EAEE.toInt(), onBackgroundDim = 0xFF9AA0AB.toInt(),
        accent = 0xFFFFB59E.toInt(), cornerDp = 20f,
    )
    val Ink = GlintPalette(
        kind = StyleKind.Ink, name = "Ink",
        background = 0xFFEEF0F3.toInt(), wallpaper = listOf(0xFFEEF0F3.toInt()),
        surface = 0xFFFFFFFF.toInt(), tiles = emptyList(),
        onTile = 0xFF111418.toInt(), onBackground = 0xFF111418.toInt(), onBackgroundDim = 0xFF5D6470.toInt(),
        accent = 0xFF2F5BFF.toInt(), cornerDp = 0f, serifClock = true,
    )
    val Liquid = GlintPalette(
        kind = StyleKind.Liquid, name = "Liquid",
        background = 0xFFF3C9E0.toInt(),
        wallpaper = listOf(0xFFFF9A6B, 0xFF6B8CFF, 0xFF2FD3B4, 0xFFF3C9E0).map { it.toInt() },
        surface = 0xFFFFFFFF.toInt(), tiles = emptyList(),
        onTile = 0xFF14161B.toInt(), onBackground = 0xFF14161B.toInt(), onBackgroundDim = 0xFF3B3F4A.toInt(),
        accent = 0xFF14161B.toInt(), cornerDp = 26f, glassOpacity = 0.42f, blurDp = 26f,
    )

    val all = listOf(Bento, Ink, Liquid)
    fun of(kind: StyleKind) = when (kind) {
        StyleKind.Bento -> Bento
        StyleKind.Ink -> Ink
        StyleKind.Liquid -> Liquid
    }
}
