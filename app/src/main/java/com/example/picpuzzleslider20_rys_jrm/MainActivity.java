package com.example.picpuzzleslider20_rys_jrm;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    public static final String EXTRA_BOARD_SIZE = "board_size";
    private static final int DEFAULT_BOARD_SIZE = 3;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private GameBoard board;
    private Button[][] tileButtons;

    private GridLayout gridBoard;
    private TextView tvMoveCount;
    private TextView tvTimer;

    private int currentBoardSize;

    // Timer state
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsElapsed = 0;
    private boolean timerRunning = false;
    private boolean gameOver = false;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridBoard   = findViewById(R.id.grid_board);
        tvMoveCount = findViewById(R.id.tv_move_count);
        tvTimer     = findViewById(R.id.tv_timer);

        Button btnNewGame = findViewById(R.id.btn_new_game);
        btnNewGame.setOnClickListener(v -> showDifficultyDialog());

        timerHandler = new Handler(Looper.getMainLooper());

        // Read board size passed from MenuActivity (fallback to 3x3)
        currentBoardSize = getIntent().getIntExtra(EXTRA_BOARD_SIZE, DEFAULT_BOARD_SIZE);

        board = new GameBoard(currentBoardSize);
        tileButtons = new Button[currentBoardSize][currentBoardSize];
        buildGrid();
        startNewGame(currentBoardSize);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timerRunning && !gameOver) startTimer();
    }

    // -------------------------------------------------------------------------
    // Game flow
    // -------------------------------------------------------------------------

    /**
     * Starts a fresh game at the given board size.
     * Rebuilds the grid only if the size has changed.
     */
    private void startNewGame(int size) {
        gameOver = false;

        if (size != currentBoardSize) {
            currentBoardSize = size;
            board = new GameBoard(currentBoardSize);
            tileButtons = new Button[currentBoardSize][currentBoardSize];
            buildGrid();
        } else {
            board = new GameBoard(currentBoardSize);
        }

        board.shuffle();
        resetTimer();
        updateMoveCounter();
        renderBoard();
        startTimer();
    }

    // -------------------------------------------------------------------------
    // Difficulty dialog
    // -------------------------------------------------------------------------

    private void showDifficultyDialog() {
        stopTimer();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_difficulty);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams lp =
                    dialog.getWindow().getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(lp);
        }

        Button btnNormal  = dialog.findViewById(R.id.btn_diff_normal);
        Button btnHard    = dialog.findViewById(R.id.btn_diff_hard);
        Button btnExpert  = dialog.findViewById(R.id.btn_diff_expert);
        TextView tvCancel = dialog.findViewById(R.id.tv_cancel);

        btnNormal.setOnClickListener(v -> { dialog.dismiss(); startNewGame(3); });
        btnHard.setOnClickListener(v   -> { dialog.dismiss(); startNewGame(4); });
        btnExpert.setOnClickListener(v -> { dialog.dismiss(); startNewGame(5); });
        tvCancel.setOnClickListener(v  -> {
            dialog.dismiss();
            if (!gameOver) startTimer();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // -------------------------------------------------------------------------
    // Grid construction
    // -------------------------------------------------------------------------

    private void buildGrid() {
        gridBoard.removeAllViews();
        gridBoard.setRowCount(currentBoardSize);
        gridBoard.setColumnCount(currentBoardSize);

        for (int row = 0; row < currentBoardSize; row++) {
            for (int col = 0; col < currentBoardSize; col++) {
                Button btn = new Button(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(row, 1, GridLayout.FILL, 1f),
                        GridLayout.spec(col, 1, GridLayout.FILL, 1f)
                );
                params.width = 0;   // ← required for column weights to work
                params.height = 0;  // ← required for row weights to work
                params.setMargins(5, 5, 5, 5);
                btn.setLayoutParams(params);
                btn.setStateListAnimator(null);
                btn.setElevation(0);

                final int r = row;
                final int c = col;
                tileButtons[row][col] = btn;
                btn.setOnClickListener(v -> onTileClicked(r, c));

                gridBoard.addView(btn);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void renderBoard() {
        for (int row = 0; row < currentBoardSize; row++) {
            for (int col = 0; col < currentBoardSize; col++) {
                updateTileView(row, col);
            }
        }
    }

    private void updateTileView(int row, int col) {
        Button btn = tileButtons[row][col];
        int value  = board.getTile(row, col);

        if (value == 0) {
            btn.setText("");
            btn.setBackground(getDrawable(R.drawable.bg_blank_tile));
            btn.setContentDescription(getString(R.string.blank_tile_description));
            btn.setClickable(false);
        } else {
            btn.setText(String.valueOf(value));
            btn.setTextAppearance(R.style.TextAppearance_TileNumber);
            btn.setBackground(getDrawable(R.drawable.bg_tile));
            btn.setContentDescription(getString(R.string.tile_description, value));
            btn.setClickable(true);
        }
    }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    private void onTileClicked(int row, int col) {
        if (board.getTile(row, col) == 0) return;

        int prevBlankRow = board.getBlankRow();
        int prevBlankCol = board.getBlankCol();

        if (board.moveTile(row, col)) {
            animateTileMove(row, col, prevBlankRow, prevBlankCol);
            updateTileView(row, col);
            updateTileView(prevBlankRow, prevBlankCol);
            updateMoveCounter();

            if (board.isSolved()) {
                gameOver = true;
                stopTimer();
                timerHandler.postDelayed(this::showWinDialog, 400);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private void animateTileMove(int fromRow, int fromCol, int toRow, int toCol) {
        Button destination = tileButtons[toRow][toCol];
        destination.setScaleX(0.85f);
        destination.setScaleY(0.85f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(destination, "scaleX", 0.85f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(destination, "scaleY", 0.85f, 1f);
        scaleX.setDuration(180);
        scaleY.setDuration(180);
        scaleX.setInterpolator(new OvershootInterpolator(2f));
        scaleY.setInterpolator(new OvershootInterpolator(2f));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    // -------------------------------------------------------------------------
    // Stats UI
    // -------------------------------------------------------------------------

    private void updateMoveCounter() {
        tvMoveCount.setText(String.valueOf(board.getMoveCount()));
    }

    // -------------------------------------------------------------------------
    // Timer
    // -------------------------------------------------------------------------

    private void startTimer() {
        timerRunning = true;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!timerRunning) return;
                secondsElapsed++;
                tvTimer.setText(formatTime(secondsElapsed));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerRunning = false;
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resetTimer() {
        stopTimer();
        secondsElapsed = 0;
        tvTimer.setText(getString(R.string.timer_default));
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // -------------------------------------------------------------------------
    // Win dialog
    // -------------------------------------------------------------------------

    private void showWinDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_win);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams lp =
                    dialog.getWindow().getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(lp);
        }

        TextView tvMoves = dialog.findViewById(R.id.tv_dialog_moves);
        TextView tvTime  = dialog.findViewById(R.id.tv_dialog_time);
        tvMoves.setText(String.valueOf(board.getMoveCount()));
        tvTime.setText(formatTime(secondsElapsed));

        Button btnPlayAgain = dialog.findViewById(R.id.btn_play_again);
        btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            showDifficultyDialog();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
