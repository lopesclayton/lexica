/*
 *  Copyright (C) 2008-2009 Rev. Johnny Healey <rev.null@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.serwylo.lexica.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.serwylo.lexica.R;

import java.util.Random;

public class LexicaLogo extends View {

    private enum BoxColor {BACKGROUND, MAIN}

    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Qu", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private Picture cached;
    private final ThemeProperties theme;
    static final String[] TITLE_ROWS = {"WIGO", "PALAVRAS"};

    public LexicaLogo(Context context) {
        this(context, null);
    }

    public LexicaLogo(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.lexicaViewStyle);
    }

    public LexicaLogo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        setupSoftwareCanvas();
        cached = null;

        theme = new ThemeProperties(context, attrs, defStyle);
        setContentDescription(context.getString(R.string.app_name));
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /**
     * Newer versions of android don't allow Picture's to be drawn to hardware accelerated canvases.
     * It seems that as of some time in the past, the default is to use hardware acceleration
     * for canvases. This sets the view to use a software canvas.
     */
    private void setupSoftwareCanvas() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void drawTile(Canvas canvas, Paint p, String letter, BoxColor color, int x, int y, int size, float offset) {

        ThemeProperties.TileProperties tileColours = color == BoxColor.BACKGROUND ? theme.home.bgTile : theme.home.fgTile;
        p.setColor(tileColours.backgroundColour);
        canvas.drawRect(x, y, x + size, y + size, p);

        p.setColor(tileColours.borderColour);
        p.setStrokeWidth(tileColours.borderWidth);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawRect(x, y, x + size, y + size, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(tileColours.foregroundColour);
        p.setTypeface(Fonts.get().getSansSerifCondensed());
        float textSize = size * 0.8f;
        p.setTextSize(textSize);
        canvas.drawText(letter, x + size / 2f, y + (size / 2f) - offset, p);

    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (cached == null) {
            int height = getHeight();
            int width = getWidth();
            cached = new Picture();
            drawOnCanvas(cached.beginRecording(width, height));
            cached.endRecording();
        }

        cached.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cached = null;
    }

    static int titleTileSize(int width, int height, int padding) {
        return Math.max(0, Math.min((width - 9 * padding) / 8, (height - 3 * padding) / 2));
    }

    private void drawOnCanvas(Canvas canvas) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setTypeface(Typeface.MONOSPACE);
        p.setTextAlign(Paint.Align.CENTER);

        int paddingSize = getResources().getDimensionPixelSize(R.dimen.homeScreenLetterPadding);
        int height = getHeight();
        int width = getWidth();

        int size = Math.min(height, width) / 8;
        if (size <= 0) return;
        p.setTextSize(size * 8f / 10f);

        // Find vertical center offset
        Rect textBounds = new Rect();
        p.getTextBounds("A", 0, 1, textBounds);
        float offset = textBounds.exactCenterY();

        Random rng = new Random();

        for (int i = 0; i < 20; i++) {
            String l = LETTERS[rng.nextInt(LETTERS.length)];
            int x = rng.nextInt(Math.max(1, width - size - 10)) + 5;
            int y = rng.nextInt(Math.max(1, height - size - 10)) + 5;

            drawTile(canvas, p, l, BoxColor.BACKGROUND, x, y, size, offset);
        }

        size = titleTileSize(width, height, paddingSize);
        if (size <= 0) return;
        p.setTextSize(size * 8f / 10f);

        // Find vertical center offset
        p.getTextBounds("A", 0, 1, textBounds);
        offset = textBounds.exactCenterY();

        int y = (height - 2 * size - paddingSize) / 2;
        for (String row : TITLE_ROWS) {
            int rowWidth = row.length() * size + (row.length() - 1) * paddingSize;
            int x = (width - rowWidth) / 2;
            for (int i = 0; i < row.length(); i++) {
                drawTile(canvas, p, row.substring(i, i + 1), BoxColor.MAIN, x, y, size, offset);
                x += size + paddingSize;
            }
            y += size + paddingSize;
        }

    }

}

