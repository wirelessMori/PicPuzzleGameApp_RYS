package com.example.picpuzzleslider20_rys_jrm;

import java.util.Random;

public class GameBoard {

    private int size;        // e.g. 3 for a 3x3 grid
    private int[][] grid;
    private int blankRow;
    private int blankCol;
    private int moveCount;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GameBoard(int size) {
        this.size = size;
        this.grid = new int[size][size];
        this.moveCount = 0;
        initSolvedState();
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Fills the grid in the solved order:
     *   1  2  3
     *   4  5  6
     *   7  8  0   <- 0 is the blank
     */
    private void initSolvedState() {
        int value = 1;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (row == size - 1 && col == size - 1) {
                    grid[row][col] = 0;   // blank tile
                    blankRow = row;
                    blankCol = col;
                } else {
                    grid[row][col] = value++;
                }
            }
        }
    }

    /**
     * Shuffles the board by performing a large number of random *legal* moves
     * starting from the solved state. This guarantees the puzzle is always
     * solvable (any sequence of legal moves from a solved state is solvable).
     */
    public void shuffle() {
        initSolvedState();
        moveCount = 0;

        Random rand = new Random();
        int totalMoves = size * size * 100; // enough to randomise well

        int[] dr = {-1, 1, 0, 0};   // up, down, left, right relative to blank
        int[] dc = {0, 0, -1, 1};

        int lastDir = -1; // avoid immediately undoing the previous move

        for (int i = 0; i < totalMoves; i++) {
            // Collect valid neighbour directions for the blank
            java.util.List<Integer> validDirs = new java.util.ArrayList<>();
            for (int d = 0; d < 4; d++) {
                if (d == opposite(lastDir)) continue; // skip undo direction
                int nr = blankRow + dr[d];
                int nc = blankCol + dc[d];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                    validDirs.add(d);
                }
            }

            int chosen = validDirs.get(rand.nextInt(validDirs.size()));
            int nr = blankRow + dr[chosen];
            int nc = blankCol + dc[chosen];

            // Swap blank with the chosen neighbour
            grid[blankRow][blankCol] = grid[nr][nc];
            grid[nr][nc] = 0;
            blankRow = nr;
            blankCol = nc;
            lastDir = chosen;
        }
    }

    /** Returns the direction index opposite to d (so we can avoid undoing moves). */
    private int opposite(int d) {
        if (d == 0) return 1;
        if (d == 1) return 0;
        if (d == 2) return 3;
        if (d == 3) return 2;
        return -1;
    }

    // -------------------------------------------------------------------------
    // Move logic
    // -------------------------------------------------------------------------

    /**
     * Returns true if the tile at (row, col) is directly adjacent
     * (up/down/left/right) to the blank tile.
     */
    public boolean isAdjacentToBlank(int row, int col) {
        if (grid[row][col] == 0) return false; // can't move the blank itself
        int rowDiff = Math.abs(row - blankRow);
        int colDiff = Math.abs(col - blankCol);
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
    }

    /**
     * Moves the tile at (row, col) into the blank space.
     * Returns true if the move was valid and performed, false otherwise.
     */
    public boolean moveTile(int row, int col) {
        if (!isAdjacentToBlank(row, col)) return false;

        grid[blankRow][blankCol] = grid[row][col];
        grid[row][col] = 0;
        blankRow = row;
        blankCol = col;
        moveCount++;
        return true;
    }

    // -------------------------------------------------------------------------
    // Win detection
    // -------------------------------------------------------------------------

    /**
     * Returns true when every tile is in its goal position:
     *   1  2  3
     *   4  5  6
     *   7  8  0
     */
    public boolean isSolved() {
        int expected = 1;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                boolean isLastCell = (row == size - 1 && col == size - 1);
                if (isLastCell) {
                    if (grid[row][col] != 0) return false;
                } else {
                    if (grid[row][col] != expected) return false;
                    expected++;
                }
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Returns the tile value at (row, col). 0 means blank. */
    public int getTile(int row, int col) {
        return grid[row][col];
    }

    public int getSize() {
        return size;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int getBlankRow() {
        return blankRow;
    }

    public int getBlankCol() {
        return blankCol;
    }

    // -------------------------------------------------------------------------
    // Debug helper
    // -------------------------------------------------------------------------

    /**
     * Prints the current grid to logcat / stdout — handy during development.
     *
     * Example output for a 3×3:
     *   [ 1][ 2][ 3]
     *   [ 4][ 5][ 6]
     *   [ 7][ 8][  ]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (grid[row][col] == 0) {
                    sb.append("[  ]");
                } else {
                    sb.append(String.format("[%2d]", grid[row][col]));
                }
            }
            sb.append("\n");
        }
        sb.append("Moves: ").append(moveCount);
        return sb.toString();
    }
}