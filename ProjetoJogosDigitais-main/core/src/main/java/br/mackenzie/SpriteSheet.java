package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

final class SpriteSheet {
    private SpriteSheet() {
    }

    static TextureRegion[][] split(Texture texture, String assetPath, int columns, int rows) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(assetPath));
        try {
            if (pixmap.getWidth() % columns != 0 || pixmap.getHeight() % rows != 0
                || pixmap.getWidth() != texture.getWidth() || pixmap.getHeight() != texture.getHeight()) {
                throw new IllegalArgumentException("Dimensao invalida para a folha de sprites: " + assetPath);
            }

            int frameWidth = pixmap.getWidth() / columns;
            int frameHeight = pixmap.getHeight() / rows;
            TextureRegion[][] frames = new TextureRegion[rows][columns];
            for (int row = 0; row < rows; row++) {
                int frameTop = row * frameHeight;
                for (int column = 0; column < columns; column++) {
                    int frameLeft = column * frameWidth;
                    int[] bounds = findVisibleBounds(
                        pixmap, frameLeft, frameTop, frameWidth, frameHeight, assetPath, row, column
                    );
                    int regionBottom = texture.getHeight() - frameTop - bounds[1] - bounds[3];
                    frames[row][column] = new TextureRegion(
                        texture,
                        frameLeft + bounds[0],
                        regionBottom,
                        bounds[2],
                        bounds[3]
                    );
                }
            }
            return frames;
        } finally {
            pixmap.dispose();
        }
    }

    private static int[] findVisibleBounds(
        Pixmap pixmap,
        int frameLeft,
        int frameTop,
        int frameWidth,
        int frameHeight,
        String assetPath,
        int row,
        int column
    ) {
        boolean[] visited = new boolean[frameWidth * frameHeight];
        int[] queue = new int[visited.length];
        int largestComponent = 0;
        int[] bounds = null;

        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                int start = y * frameWidth + x;
                if (visited[start] || (pixmap.getPixel(frameLeft + x, frameTop + y) & 0xff) == 0) {
                    continue;
                }

                int head = 0;
                int tail = 0;
                int minX = x;
                int minY = y;
                int maxX = x;
                int maxY = y;
                visited[start] = true;
                queue[tail++] = start;

                while (head < tail) {
                    int pixel = queue[head++];
                    int pixelX = pixel % frameWidth;
                    int pixelY = pixel / frameWidth;
                    minX = Math.min(minX, pixelX);
                    minY = Math.min(minY, pixelY);
                    maxX = Math.max(maxX, pixelX);
                    maxY = Math.max(maxY, pixelY);

                    for (int neighborY = Math.max(0, pixelY - 1);
                         neighborY <= Math.min(frameHeight - 1, pixelY + 1);
                         neighborY++) {
                        for (int neighborX = Math.max(0, pixelX - 1);
                             neighborX <= Math.min(frameWidth - 1, pixelX + 1);
                             neighborX++) {
                            int neighbor = neighborY * frameWidth + neighborX;
                            if (!visited[neighbor]
                                && (pixmap.getPixel(frameLeft + neighborX, frameTop + neighborY) & 0xff) != 0) {
                                visited[neighbor] = true;
                                queue[tail++] = neighbor;
                            }
                        }
                    }
                }

                if (tail > largestComponent) {
                    largestComponent = tail;
                    bounds = new int[] {minX, minY, maxX - minX + 1, maxY - minY + 1};
                }
            }
        }

        if (bounds == null) {
            throw new IllegalArgumentException(
                "Quadro vazio na folha de sprites: " + assetPath + " (" + row + ", " + column + ")"
            );
        }
        return bounds;
    }

    static TextureRegion[][] mirror(TextureRegion[][] frames) {
        TextureRegion[][] mirrored = new TextureRegion[frames.length][];
        for (int row = 0; row < frames.length; row++) {
            mirrored[row] = new TextureRegion[frames[row].length];
            for (int column = 0; column < frames[row].length; column++) {
                mirrored[row][column] = orient(frames[row][column], true);
            }
        }
        return mirrored;
    }

    static TextureRegion orient(TextureRegion frame, boolean flipHorizontal) {
        TextureRegion oriented = new TextureRegion(frame);
        if (flipHorizontal) {
            oriented.flip(true, false);
        }
        return oriented;
    }
}
