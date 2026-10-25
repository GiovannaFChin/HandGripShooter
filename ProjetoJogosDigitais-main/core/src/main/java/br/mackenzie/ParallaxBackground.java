package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

final class ParallaxBackground {
    enum Period { MORNING, AFTERNOON, NIGHT }

    static final class CycleState {
        private Period period = Period.MORNING;
        private Period nextPeriod = Period.MORNING;
        private float fadeProgress;

        Period getPeriod() {
            return period;
        }

        Period getNextPeriod() {
            return nextPeriod;
        }

        float getFadeProgress() {
            return fadeProgress;
        }

        private void set(Period period, Period nextPeriod, float fadeProgress) {
            this.period = period;
            this.nextPeriod = nextPeriod;
            this.fadeProgress = fadeProgress;
        }
    }

    private static final String[] LAYER_NAMES = {
        "00_ceu", "01_nuvens_longe", "02_nuvens_perto",
        "03_colinas_longe", "04_colinas_perto", "05_floresta_longe",
        "06_floresta_perto", "07_neblina_fundo", "08_cidade_longe",
        "09_vila_perto", "10_chao_e_props", "11_neblina_chao",
        "12_primeiro_plano_longe", "13_primeiro_plano_perto"
    };
    private static final float MORNING_DURATION = 20f;
    private static final float AFTERNOON_DURATION = 20f;
    private static final float TRANSITION_DURATION = 1f;
    private static final float SCROLL_SPEED = 200f;

    private final Scene morning;
    private final Scene afternoon;
    private final Scene night;
    private final CycleState renderCycleState = new CycleState();
    private float elapsed;

    ParallaxBackground() {
        JsonValue speeds = new JsonReader().parse(Gdx.files.internal("background/velocidades.json"));
        morning = new Scene("background/manha/", speeds);
        afternoon = new Scene("background/tarde/", speeds);
        night = new Scene("background/noite/", speeds);
    }

    void update(float delta) {
        elapsed += delta;
        morning.update(delta);
        afternoon.update(delta);
        night.update(delta);
    }

    void render(SpriteBatch batch) {
        CycleState cycleState = getCycleState();
        if (cycleState.period != cycleState.nextPeriod) {
            Scene outgoing = getScene(cycleState.period);
            Scene incoming = getScene(cycleState.nextPeriod);
            renderTransition(batch, outgoing, incoming, cycleState.fadeProgress);
        } else {
            getScene(cycleState.period).render(batch);
        }
    }

    CycleState getCycleState() {
        float morningTransitionStart = MORNING_DURATION - TRANSITION_DURATION;
        float nightTransitionStart = MORNING_DURATION + AFTERNOON_DURATION - TRANSITION_DURATION;

        if (elapsed < morningTransitionStart) {
            renderCycleState.set(Period.MORNING, Period.MORNING, 0f);
        } else if (elapsed < MORNING_DURATION) {
            renderCycleState.set(
                Period.MORNING,
                Period.AFTERNOON,
                (elapsed - morningTransitionStart) / TRANSITION_DURATION
            );
        } else if (elapsed < nightTransitionStart) {
            renderCycleState.set(Period.AFTERNOON, Period.AFTERNOON, 0f);
        } else if (elapsed < MORNING_DURATION + AFTERNOON_DURATION) {
            renderCycleState.set(
                Period.AFTERNOON,
                Period.NIGHT,
                (elapsed - nightTransitionStart) / TRANSITION_DURATION
            );
        } else {
            renderCycleState.set(Period.NIGHT, Period.NIGHT, 0f);
        }
        return renderCycleState;
    }

    float getLayerOffset(String layerName) {
        return morning.getLayerOffset(layerName);
    }

    float getLayerWidth(String layerName) {
        return morning.getLayerWidth(layerName);
    }

    private Scene getScene(Period period) {
        switch (period) {
            case MORNING:
                return morning;
            case AFTERNOON:
                return afternoon;
            case NIGHT:
                return night;
            default:
                throw new IllegalArgumentException("Unknown day period: " + period);
        }
    }

    private void renderTransition(SpriteBatch batch, Scene outgoing, Scene incoming, float progress) {
        outgoing.render(batch);

        Color originalColor = new Color(batch.getColor());
        batch.setColor(
            originalColor.r,
            originalColor.g,
            originalColor.b,
            originalColor.a * progress
        );
        incoming.render(batch);
        batch.setColor(originalColor);
    }

    void dispose() {
        morning.dispose();
        afternoon.dispose();
        night.dispose();
    }

    void reset() {
        elapsed = 0f;
        morning.reset();
        afternoon.reset();
        night.reset();
    }

    float getElapsed() {
        return elapsed;
    }

    float getEnemyAlpha(float spawnTime) {
        float alpha = getEnemyAlpha(spawnTime, MORNING_DURATION - TRANSITION_DURATION);
        return Math.min(alpha, getEnemyAlpha(
            spawnTime,
            MORNING_DURATION + AFTERNOON_DURATION - TRANSITION_DURATION
        ));
    }

    private float getEnemyAlpha(float spawnTime, float transitionStart) {
        if (spawnTime >= transitionStart || elapsed <= transitionStart) {
            return 1f;
        }
        if (elapsed >= transitionStart + TRANSITION_DURATION) {
            return 0f;
        }
        return 1f - (elapsed - transitionStart) / TRANSITION_DURATION;
    }

    private static final class Scene {
        private final Array<Layer> layers = new Array<>();

        private Scene(String path, JsonValue speeds) {
            for (String layerName : LAYER_NAMES) {
                Texture texture = new Texture(Gdx.files.internal(path + layerName + ".png"));
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                layers.add(new Layer(texture, speeds.getFloat(layerName)));
            }
        }

        private void update(float delta) {
            for (Layer layer : layers) {
                layer.update(delta);
            }
        }

        private void render(SpriteBatch batch) {
            for (Layer layer : layers) {
                layer.render(batch);
            }
        }

        private void dispose() {
            for (Layer layer : layers) {
                layer.texture.dispose();
            }
        }

        private void reset() {
            for (Layer layer : layers) {
                layer.offset = 0f;
            }
        }

        private float getLayerOffset(String layerName) {
            return getLayer(layerName).offset;
        }

        private float getLayerWidth(String layerName) {
            return getLayer(layerName).width;
        }

        private Layer getLayer(String layerName) {
            for (int i = 0; i < LAYER_NAMES.length; i++) {
                if (LAYER_NAMES[i].equals(layerName)) {
                    return layers.get(i);
                }
            }
            throw new IllegalArgumentException("Unknown background layer: " + layerName);
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
}
