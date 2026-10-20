package com.serwylo.lexica.view;

import org.junit.Test;
import static org.junit.Assert.*;

public class LexicaLogoTest {
    @Test public void identifiesTheAdaptation() {
        assertArrayEquals(new String[]{"WIGO", "PALAVRAS"}, LexicaLogo.TITLE_ROWS);
    }

    @Test public void rowsFitPortraitLandscapeAndSmallContainers() {
        for (int[] dimensions : new int[][]{{320, 480}, {480, 240}, {375, 600}, {1080, 1400}, {200, 100}}) {
            int padding = 4;
            int size = LexicaLogo.titleTileSize(dimensions[0], dimensions[1], padding);
            assertTrue(size > 0);
            assertTrue(8 * size + 9 * padding <= dimensions[0]);
            assertTrue(2 * size + 3 * padding <= dimensions[1]);
        }
    }

    @Test public void emptyOrTooSmallContainerDoesNotProduceNegativeTiles() {
        assertEquals(0, LexicaLogo.titleTileSize(0, 0, 4));
        assertEquals(0, LexicaLogo.titleTileSize(20, 20, 4));
    }
}
