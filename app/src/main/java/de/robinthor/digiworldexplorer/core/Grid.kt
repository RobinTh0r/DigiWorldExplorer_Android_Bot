package de.robinthor.digiworldexplorer.core

data class BoardBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    init {
        require(right > left && bottom > top) { "Board bounds must have a positive size" }
    }

    fun cellCenter(cell: Cell): ScreenPoint {
        require(cell.row in 0 until GRID_SIZE && cell.col in 0 until GRID_SIZE)
        val x = left + ((cell.col + 0.5) * (right - left) / GRID_SIZE).toInt()
        val y = top + ((cell.row + 0.5) * (bottom - top) / GRID_SIZE).toInt()
        return ScreenPoint(x, y)
    }
}

data class Cell(val row: Int, val col: Int)
data class ScreenPoint(val x: Int, val y: Int)

const val GRID_SIZE = 5
