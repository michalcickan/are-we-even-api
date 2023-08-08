package eu.helpers

class StringHelpers {
    companion object {
        fun generateRandomUUID(): String {
            val characters = ('a'..'f') + ('0'..'9')
            val segments = listOf(8, 4, 4, 4, 12)

            return segments.map { segmentLength ->
                (1..segmentLength)
                    .map { characters.random() }
                    .joinToString("")
            }.joinToString("-")
        }
    }
}
