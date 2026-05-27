package com.example.picpuzzleslider20_rys_jrm;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private LinearLayout layoutImageControls;
    private Button btnToggleMode;

    private int currentBoardSize;

    // Image feature
    private ImageTileManager imageTileManager = null;
    private Uri currentImageUri = null;
    private boolean showingImage = true; // true = image tiles, false = numbers

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsElapsed = 0;
    private boolean timerRunning = false;
    private boolean gamePaused = false;
    private boolean gameOver = false;

    // Photo picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        gridBoard           = findViewById(R.id.grid_board);
        tvMoveCount         = findViewById(R.id.tv_move_count);
        tvTimer             = findViewById(R.id.tv_timer);
        layoutImageControls = findViewById(R.id.layout_image_controls);
        btnToggleMode       = findViewById(R.id.btn_toggle_mode);

        // Button listeners
        findViewById(R.id.btn_new_game).setOnClickListener(v -> showDifficultyDialog());
        findViewById(R.id.btn_choose_image).setOnClickListener(v -> openImagePicker());
        btnToggleMode.setOnClickListener(v -> toggleDisplayMode());
        findViewById(R.id.btn_view_image).setOnClickListener(v -> showImagePreviewDialog());

        timerHandler = new Handler(Looper.getMainLooper());

        // Register photo picker (no permission needed for photo picker on API 21+)
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) onImagePicked(uri);
                }
        );

        // Boot up
        currentBoardSize = getIntent().getIntExtra(EXTRA_BOARD_SIZE, DEFAULT_BOARD_SIZE);
        board = new GameBoard(currentBoardSize);
        tileButtons = new Button[currentBoardSize][currentBoardSize];
        buildGrid();
        startNewGame(currentBoardSize);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerRunning) {
            gamePaused = true;  // remember we were mid-game
        }
        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gamePaused && !gameOver) {
            gamePaused = false;
            startTimer();
        }
    }

    // -------------------------------------------------------------------------
    // Image picking
    // -------------------------------------------------------------------------

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Called when the user has selected an image.
     * Slices it for the current board size and re-renders.
     */
    private void onImagePicked(Uri uri) {
        currentImageUri = uri;
        imageTileManager = new ImageTileManager(this, uri, currentBoardSize);

        if (imageTileManager.isLoaded()) {
            showingImage = true;
            layoutImageControls.setVisibility(View.VISIBLE);
            btnToggleMode.setText(getString(R.string.show_numbers));
            renderBoard();
        } else {
            Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show();
            imageTileManager = null;
        }
    }

    /**
     * When the board size changes we need to re-slice the same image.
     */
    private void resliceImageForCurrentSize() {
        if (currentImageUri == null) return;
        imageTileManager = new ImageTileManager(this, currentImageUri, currentBoardSize);
        if (!imageTileManager.isLoaded()) {
            imageTileManager = null;
            layoutImageControls.setVisibility(View.GONE);
        }
    }

    // -------------------------------------------------------------------------
    // Toggle number / image mode
    // -------------------------------------------------------------------------

    private void toggleDisplayMode() {
        showingImage = !showingImage;
        btnToggleMode.setText(showingImage
                ? getString(R.string.show_numbers)
                : getString(R.string.show_image));
        renderBoard();
    }

    // -------------------------------------------------------------------------
    // Image preview dialog
    // -------------------------------------------------------------------------

    private void showImagePreviewDialog() {
        if (currentImageUri == null) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_preview);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams lp =
                    dialog.getWindow().getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setAttributes(lp);
        }

        ImageView iv = dialog.findViewById(R.id.iv_preview);
        iv.setImageURI(currentImageUri);

        dialog.findViewById(R.id.tv_preview_close).setOnClickListener(v -> dialog.dismiss());
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // -------------------------------------------------------------------------
    // Game flow
    // -------------------------------------------------------------------------

    private void startNewGame(int size) {
        gameOver   = false;
        gamePaused = false;

        if (size != currentBoardSize) {
            currentBoardSize = size;
            board = new GameBoard(currentBoardSize);
            tileButtons = new Button[currentBoardSize][currentBoardSize];
            buildGrid();
            resliceImageForCurrentSize();
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
                params.width = 0;
                params.height = 0;
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
        Button btn   = tileButtons[row][col];
        int value    = board.getTile(row, col);
        boolean useImage = showingImage && imageTileManager != null;

        if (value == 0) {
            // Blank tile — always dark, no content
            btn.setText("");
            btn.setBackground(getDrawable(R.drawable.bg_blank_tile));
            btn.setContentDescription(getString(R.string.blank_tile_description));
            btn.setClickable(false);
            return;
        }

        btn.setClickable(true);
        btn.setContentDescription(getString(R.string.tile_description, value));

        if (useImage) {
            Bitmap piece = imageTileManager.getTileBitmap(value);
            if (piece != null) {
                btn.setText("");
                // Use BitmapDrawable as background so the image fills the tile
                BitmapDrawable drawable = new BitmapDrawable(getResources(), piece);
                drawable.setFilterBitmap(true);
                btn.setBackground(drawable);
            } else {
                // Fallback to number if bitmap missing
                applyNumberTile(btn, value);
            }
        } else {
            applyNumberTile(btn, value);
        }
    }

    private void applyNumberTile(Button btn, int value) {
        btn.setText(String.valueOf(value));
        btn.setTextAppearance(R.style.TextAppearance_TileNumber);
        btn.setBackground(getDrawable(R.drawable.bg_tile));
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
    // Stats
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
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
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

        ((TextView) dialog.findViewById(R.id.tv_dialog_moves))
                .setText(String.valueOf(board.getMoveCount()));
        ((TextView) dialog.findViewById(R.id.tv_dialog_time))
                .setText(formatTime(secondsElapsed));

        dialog.findViewById(R.id.btn_play_again).setOnClickListener(v -> {
            dialog.dismiss();
            showDifficultyDialog();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
