package com.example.picpuzzleslider20_rys_jrm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

/**
 * Slices a source bitmap into (size x size) tile bitmaps.
 *
 * Tile numbering matches the GameBoard convention:
 *   tile 1  = top-left piece
 *   tile N² = bottom-right piece  (this is the blank slot, never displayed)
 *
 * Usage:
 *   ImageTileManager mgr = new ImageTileManager(context, uri, 3);
 *   Bitmap piece = mgr.getTileBitmap(tileValue);  // tileValue 1..N²-1
 */
public class ImageTileManager {

    private final Bitmap[] tiles;   // index 0 unused; tiles[1] = tile #1, etc.
    private final int size;
    private boolean loaded = false;

    public ImageTileManager(Context context, Uri imageUri, int boardSize) {
        this.size = boardSize;
        int total = boardSize * boardSize;
        tiles = new Bitmap[total + 1]; // 1-indexed

        try {
            Bitmap source = loadAndCropSquare(context, imageUri);
            if (source != null) {
                sliceIntoTiles(source, boardSize);
                loaded = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Returns the bitmap for the given tile value (1-indexed, matching GameBoard).
     * Returns null if not loaded or value is out of range.
     */
    public Bitmap getTileBitmap(int tileValue) {
        if (!loaded || tileValue < 1 || tileValue >= tiles.length) return null;
        return tiles[tileValue];
    }

    public int getSize() {
        return size;
    }

    // -------------------------------------------------------------------------
    // Image loading
    // -------------------------------------------------------------------------

    /**
     * Loads the image from URI, scales it down if needed (max 1024px),
     * then crops to a square from the centre.
     */
    private Bitmap loadAndCropSquare(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;

        // Decode with sub-sampling to avoid OOM on large photos
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, opts);
        inputStream.close();

        int maxDim = 1024;
        int sampleSize = 1;
        while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2;
        }

        inputStream = context.getContentResolver().openInputStream(uri);
        opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize;
        Bitmap raw = BitmapFactory.decodeStream(inputStream, null, opts);
        inputStream.close();

        if (raw == null) return null;

        // Crop to square (centre crop)
        int w = raw.getWidth();
        int h = raw.getHeight();
        int dim = Math.min(w, h);
        int x = (w - dim) / 2;
        int y = (h - dim) / 2;
        Bitmap squared = Bitmap.createBitmap(raw, x, y, dim, dim);
        if (squared != raw) raw.recycle();
        return squared;
    }

    // -------------------------------------------------------------------------
    // Slicing
    // -------------------------------------------------------------------------

    /**
     * Cuts the square bitmap into boardSize*boardSize equal pieces.
     * Piece numbering goes left-to-right, top-to-bottom (same as tile values).
     */
    private void sliceIntoTiles(Bitmap source, int boardSize) {
        int dim = source.getWidth(); // already square
        int tileSize = dim / boardSize;

        int tileValue = 1;
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                int srcX = col * tileSize;
                int srcY = row * tileSize;
                tiles[tileValue] = Bitmap.createBitmap(source, srcX, srcY, tileSize, tileSize);
                tileValue++;
            }
        }
        source.recycle();
    }
}
