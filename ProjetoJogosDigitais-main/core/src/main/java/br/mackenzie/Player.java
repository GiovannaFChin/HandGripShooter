package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

final class Player {
    static final float WIDTH = 36f;
    static final float HEIGHT = 36f;

    private static final float SPEED = 100f;
    private static final float VERTICAL_SPEED = 80f;
    private static final float GROUND_Y = 18f;

    private final Texture texture;
    private final TextureRegion[][] frames;
    private final TextureRegion[][] mirroredFrames;
    private float x = 48f;
    private float y = GROUND_Y;
    private float animationTime;
    private int facing = 1;
    private boolean active;

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
        y = MathUtils.clamp(y, GROUND_Y, GameWorld.WORLD_HEIGHT - HEIGHT);
        if (horizontal != 0f) {
            facing = horizontal > 0f ? 1 : -1;
        }
        active = horizontal != 0f || vertical != 0f || firing;
        animationTime += delta;
    }

    void render(SpriteBatch batch) {
        int frameIndex = active ? (int) (animationTime * 12f) % 9 : 0;
        TextureRegion frame = facing > 0
            ? frames[frameIndex / 3][frameIndex % 3]
            : mirroredFrames[frameIndex / 3][frameIndex % 3];
        float drawWidth = HEIGHT * frame.getRegionWidth() / frame.getRegionHeight();
        batch.draw(frame, x + (WIDTH - drawWidth) / 2f, y, drawWidth, HEIGHT);
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
