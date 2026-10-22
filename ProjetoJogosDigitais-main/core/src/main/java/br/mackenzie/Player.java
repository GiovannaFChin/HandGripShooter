package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

final class Player {
    static final float WIDTH = 36f;
    static final float HEIGHT = 36f;

    private static final float SPEED = 100f;
    private static final float VERTICAL_SPEED = 80f;
    private static final float GROUND_Y = 2f;
    private static final float MAX_Y = GROUND_Y + 20f;
    private static final int[] MOVEMENT_FRAMES = {1, 2, 4, 5, 7, 8};

    private final Texture texture;
    private final TextureRegion[][] frames;
    private final TextureRegion[][] mirroredFrames;
    private float x = 48f;
    private float y = GROUND_Y;
    private float animationTime;
    private int facing = 1;
    private boolean moving;
    private boolean firing;

    static final int MAX_LIVES = 3;
    private static final float INVULNERABLE_TIME = 1.5f;

    private final Rectangle bounds = new Rectangle();
    private int lives = MAX_LIVES;
    private float invulnerableTimer;

    Player(String assetPath) {
        texture = new Texture(Gdx.files.internal(assetPath));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        frames = SpriteSheet.split(texture, assetPath, 3, 3);
        mirroredFrames = SpriteSheet.mirror(frames);
    }

    void update(float delta, float horizontal, float vertical, boolean firing) {
        x += horizontal * SPEED * delta;
        y += vertical * VERTICAL_SPEED * delta;
        x = MathUtils.clamp(x, 0f, GameWorld.WORLD_WIDTH - WIDTH);
        y = MathUtils.clamp(y, GROUND_Y, MAX_Y);
        if (horizontal != 0f) {
            facing = horizontal > 0f ? 1 : -1;
        }
        moving = horizontal != 0f || vertical != 0f;
        this.firing = firing;
        animationTime = moving ? animationTime + delta : 0f;

        if (invulnerableTimer > 0f) invulnerableTimer -= delta;

    }

    void render(SpriteBatch batch) {
        int frameIndex = firing
            ? 0
            : moving ? MOVEMENT_FRAMES[(int) (animationTime * 8f) % MOVEMENT_FRAMES.length] : 1;
        TextureRegion frame = facing > 0
            ? frames[frameIndex / 3][frameIndex % 3]
            : mirroredFrames[frameIndex / 3][frameIndex % 3];
        float drawWidth = HEIGHT * frame.getRegionWidth() / frame.getRegionHeight();
        batch.draw(frame, x + (WIDTH - drawWidth) / 2f, y, drawWidth, HEIGHT);

        if (invulnerableTimer > 0f && ((int) (invulnerableTimer * 12f)) % 2 == 0) return;
    }

    Rectangle getBounds() {
        return bounds.set(x + 8f, y, WIDTH - 16f, HEIGHT);
    }

    /** Retorna true se realmente perdeu vida (não estava invulnerável). */
    boolean hit() {
        if (invulnerableTimer > 0f || lives <= 0) return false;
        lives--;
        invulnerableTimer = INVULNERABLE_TIME;
        return true;
    }

    int getLives() { return lives; }

    boolean isDead() { return lives <= 0; }

    void reset() {
        x = 48f;
        y = GROUND_Y;
        lives = MAX_LIVES;
        invulnerableTimer = 0f;
        facing = 1;
        moving = false;
        firing = false;
        animationTime = 0f;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    int getFacing() {
        return facing;
    }

    void dispose() {
        texture.dispose();
    }
}
