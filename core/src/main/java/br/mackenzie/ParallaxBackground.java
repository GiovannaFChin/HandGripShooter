package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

final class ParallaxBackground {
    private static final String[] LAYER_NAMES = {
        "00_ceu", "01_nuvens", "02_colinas", "03_arvores",
        "04_fazenda_longe", "05_fazenda_perto", "06_chao_e_props", "07_primeiro_plano"
    };
    private static final float SCROLL_SPEED = 200f;

    private final Array<Layer> layers = new Array<>();

    ParallaxBackground() {
        JsonValue speeds = new JsonReader().parse(Gdx.files.internal("background/velocidades.json"));
        for (int i = 0; i < LAYER_NAMES.length; i++) {
            Texture texture = new Texture(Gdx.files.internal("background/" + LAYER_NAMES[i] + ".png"));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            layers.add(new Layer(texture, speeds.getFloat(LAYER_NAMES[i])));
        }
    }

    void update(float delta) {
        for (Layer layer : layers) {
            layer.update(delta);
        }
    }

    void render(SpriteBatch batch) {
        for (Layer layer : layers) {
            layer.render(batch);
        }
    }

    void dispose() {
        for (Layer layer : layers) {
            layer.texture.dispose();
        }
    }

    private static final class Layer {
        private final Texture texture;
        private final float speedFactor;
        private final float width;
        private float offset;

        private Layer(Texture texture, float speedFactor) {
            this.texture = texture;
            this.speedFactor = speedFactor;
            this.width = texture.getWidth() * GameWorld.WORLD_HEIGHT / texture.getHeight();
        }

        private void update(float delta) {
            if (speedFactor > 0f && width > 0f) {
                offset = (offset + SCROLL_SPEED * speedFactor * delta) % width;
            }
        }

        private void render(SpriteBatch batch) {
            for (float x = -offset; x < GameWorld.WORLD_WIDTH; x += width) {
                batch.draw(texture, x, 0f, width, GameWorld.WORLD_HEIGHT);
            }
        }
    }
    void reset() {
        for (Layer layer : layers) {
            layer.offset = 0f;
        }
    }
}
