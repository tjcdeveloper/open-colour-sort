package uk.co.tjcdeveloper.opencoloursort.game

/**
 * The twelve game colours. Keys match the design handoff's level encodings
 * (each tube is a string of colour keys, bottom -> top).
 *
 * The first six are the classic set; the remaining six only appear in hard
 * mode. The soft ("zen") palette is only specified for the classic six —
 * hard-mode colours fall back to their vivid value.
 */
enum class GameColour(
    val key: Char,
    val vivid: Long,
    val soft: Long,
    val symbol: String,
) {
    RED('r', 0xFFDD4A44, 0xFFC26862, "●"),
    ORANGE('o', 0xFFEE8A2F, 0xFFC28F62, "▲"),
    YELLOW('y', 0xFFE9C23A, 0xFFC2AB62, "■"),
    GREEN('g', 0xFF57AB4A, 0xFF74A862, "◆"),
    BLUE('b', 0xFF3F7FD8, 0xFF6285C2, "✚"),
    PURPLE('p', 0xFF9061D0, 0xFF8F74B8, "★"),
    TEAL('t', 0xFF2FAE9F, 0xFF2FAE9F, "♥"),
    PINK('k', 0xFFD95F9E, 0xFFD95F9E, "✖"),
    LIME('l', 0xFF9DBD3C, 0xFF9DBD3C, "◐"),
    SKY('c', 0xFF4FB6E2, 0xFF4FB6E2, "☾"),
    BROWN('m', 0xFFA5673F, 0xFFA5673F, "⬟"),
    GREY_VIOLET('v', 0xFF8B8FA8, 0xFF8B8FA8, "☀"),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }
        fun fromKey(key: Char): GameColour =
            byKey[key] ?: throw IllegalArgumentException("Unknown colour key '$key'")
    }
}
