package com.example.ui.models

data class Coordinate(val row: Int, val col: Int)

object LudoBoardHelper {
    
    // 52 cells of the standard Ludo track
    val trackCells = listOf(
        Coordinate(6, 0), Coordinate(6, 1), Coordinate(6, 2), Coordinate(6, 3), Coordinate(6, 4), Coordinate(6, 5), // 0-5 Left arm top
        Coordinate(5, 6), Coordinate(4, 6), Coordinate(3, 6), Coordinate(2, 6), Coordinate(1, 6), Coordinate(0, 6), // 6-11 Top arm left
        Coordinate(0, 7), // 12 Top arm top end
        Coordinate(0, 8), Coordinate(1, 8), Coordinate(2, 8), Coordinate(3, 8), Coordinate(4, 8), Coordinate(5, 8), // 13-18 Top arm right
        Coordinate(6, 9), Coordinate(6, 10), Coordinate(6, 11), Coordinate(6, 12), Coordinate(6, 13), Coordinate(6, 14), // 19-24 Right arm top
        Coordinate(7, 14), // 25 Right arm right end
        Coordinate(8, 14), Coordinate(8, 13), Coordinate(8, 12), Coordinate(8, 11), Coordinate(8, 10), Coordinate(8, 9), // 26-31 Right arm bottom
        Coordinate(9, 8), Coordinate(10, 8), Coordinate(11, 8), Coordinate(12, 8), Coordinate(13, 8), Coordinate(14, 8), // 32-37 Bottom arm right
        Coordinate(14, 7), // 38 Bottom arm bottom end
        Coordinate(14, 6), Coordinate(13, 6), Coordinate(12, 6), Coordinate(11, 6), Coordinate(10, 6), Coordinate(9, 6), // 39-44 Bottom arm left
        Coordinate(8, 5), Coordinate(8, 4), Coordinate(8, 3), Coordinate(8, 2), Coordinate(8, 1), Coordinate(8, 0), // 45-50 Left arm bottom
        Coordinate(7, 0) // 51 Left arm left end
    )

    // Symmetrical Safe Zones where captures are forbidden
    val safeIndices = setOf(
        49, // Red start cell (8,1)
        10, // Green start cell (1,6)
        23, // Yellow start cell (6,13)
        36, // Blue start cell (13,8)
        0,  // Left arm cell (6,0)
        13, // Top arm cell (0,8)
        26, // Right arm cell (8,14)
        39  // Bottom arm cell (14,6)
    )

    // Precalculated Coordinate-based set for rapid safe-cell check
    val safeCoordinates: Set<Coordinate> = safeIndices.map { trackCells[it] }.toSet()

    // Starting track cell indices for each color
    fun getStartTrackIndex(color: LudoColor): Int = when (color) {
        LudoColor.RED -> 49
        LudoColor.GREEN -> 10
        LudoColor.YELLOW -> 23
        LudoColor.BLUE -> 36
    }

    // Checking preceding tracker cell before turning to home
    fun getHomeEntryLimitIndex(color: LudoColor): Int = when (color) {
        LudoColor.RED -> 48
        LudoColor.GREEN -> 9
        LudoColor.YELLOW -> 22
        LudoColor.BLUE -> 35
    }

    // Default yard placements for the 4 tokens
    fun getYardPlacement(color: LudoColor, tokenId: Int): Coordinate {
        return when (color) {
            LudoColor.RED -> when (tokenId) {
                0 -> Coordinate(10, 2)
                1 -> Coordinate(10, 3)
                2 -> Coordinate(11, 2)
                else -> Coordinate(11, 3)
            }
            LudoColor.GREEN -> when (tokenId) {
                0 -> Coordinate(2, 2)
                1 -> Coordinate(2, 3)
                2 -> Coordinate(3, 2)
                else -> Coordinate(3, 3)
            }
            LudoColor.YELLOW -> when (tokenId) {
                0 -> Coordinate(2, 11)
                1 -> Coordinate(2, 12)
                2 -> Coordinate(3, 11)
                else -> Coordinate(3, 12)
            }
            LudoColor.BLUE -> when (tokenId) {
                0 -> Coordinate(10, 11)
                1 -> Coordinate(10, 12)
                2 -> Coordinate(11, 11)
                else -> Coordinate(11, 12)
            }
        }
    }

    // Home path coordinates mappings (relativePosition range 52 to 56)
    fun getHomePathCoordinate(color: LudoColor, relativePosition: Int): Coordinate {
        val homeStep = relativePosition - 52 // ranges from 0 to 4
        return when (color) {
            LudoColor.RED -> Coordinate(7, 1 + homeStep)
            LudoColor.GREEN -> Coordinate(1 + homeStep, 7)
            LudoColor.YELLOW -> Coordinate(7, 13 - homeStep)
            LudoColor.BLUE -> Coordinate(13 - homeStep, 7)
        }
    }

    // Center home triangles coordinates mappings (relativePosition 57)
    fun getHomeCenterCoordinate(color: LudoColor): Coordinate {
        return when (color) {
            LudoColor.RED -> Coordinate(7, 6)
            LudoColor.GREEN -> Coordinate(6, 7)
            LudoColor.YELLOW -> Coordinate(7, 8)
            LudoColor.BLUE -> Coordinate(8, 7)
        }
    }

    // Complete Board Coordinate Resolver
    fun getCoordinate(token: LudoToken): Coordinate {
        return when {
            token.isHomeYard -> getYardPlacement(token.color, token.id)
            token.isCompleted -> getHomeCenterCoordinate(token.color)
            token.isInHomePath -> getHomePathCoordinate(token.color, token.relativePosition)
            else -> {
                val startIdx = getStartTrackIndex(token.color)
                // Track relativePosition starts at 1
                val trackIdx = (startIdx + token.relativePosition - 1) % 52
                trackCells[trackIdx]
            }
        }
    }
}
