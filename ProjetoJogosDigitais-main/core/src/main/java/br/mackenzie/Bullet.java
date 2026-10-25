package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

final class Bullet {
    private static final float PROJECTILE_FRAME_DURATION = 0.08f;
    private static final float EFFECT_FRAME_DURATION = 0.07f;

    enum Strength {
        SMALL(
            1,
            "bullet_shoot/projetil_pequeno_spritesheet.png",
            "bullet_shoot/clarao_cano_pequeno_spritesheet.png",
            "bullet_shoot/impact/impacto_pequeno_spritesheet.png",
            4,
            3,
            5,
            220f,
            1,
            0f,
            8f,
            3f,
            12f,
            5f,
            14f,
            10f,
            24f,
            24f
        ),
        MEDIUM(
            2,
            "bullet_shoot/projetil_medio_spritesheet.png",
            "bullet_shoot/clarao_cano_medio_spritesheet.png",
            "bullet_shoot/impact/impacto_medio_spritesheet.png",
            3,
            3,
            5,
            240f,
            2,
            5f,
            10f,
            4f,
            18f,
            7f,
            20f,
            14f,
            34f,
            34f
        ),
        LARGE(
            3,
            "bullet_shoot/projetil_grande_spritesheet.png",
            "bullet_shoot/clarao_cano_grande_spritesheet.png",
            "bullet_shoot/impact/impacto_grande_spritesheet.png",
            3,
            3,
            5,
            260f,
            3,
            8f,
            10f,
            4f,
            24f,
            9f,
            30f,
            20f,
            50f,
            50f
        );

        private final int value;
        private final String projectilePath;
        private final String muzzleFlashPath;
        private final String impactPath;
        private final int projectileFrames;
        private final int muzzleFlashFrames;
        private final int impactFrames;
        private final float speed;
        private final int projectileCount;
        private final float spread;
        private final float hitboxWidth;
        private final float hitboxHeight;
        private final float projectileWidth;
        private final float projectileHeight;
        private final float muzzleFlashWidth;
        private final float muzzleFlashHeight;
        private final float impactWidth;
        private final float impactHeight;

        Strength(
            int value,
            String projectilePath,
            String muzzleFlashPath,
            String impactPath,
            int projectileFrames,
            int muzzleFlashFrames,
            int impactFrames,
            float speed,
            int projectileCount,
            float spread,
            float hitboxWidth,
            float hitboxHeight,
            float projectileWidth,
            float projectileHeight,
            float muzzleFlashWidth,
            float muzzleFlashHeight,
            float impactWidth,
            float impactHeight
        ) {
            this.value = value;
            this.projectilePath = projectilePath;
            this.muzzleFlashPath = muzzleFlashPath;
            this.impactPath = impactPath;
            this.projectileFrames = projectileFrames;
            this.muzzleFlashFrames = muzzleFlashFrames;
            this.impactFrames = impactFrames;
            this.speed = speed;
            this.projectileCount = projectileCount;
            this.spread = spread;
            this.hitboxWidth = hitboxWidth;
            this.hitboxHeight = hitboxHeight;
            this.projectileWidth = projectileWidth;
            this.projectileHeight = projectileHeight;
            this.muzzleFlashWidth = muzzleFlashWidth;
            this.muzzleFlashHeight = muzzleFlashHeight;
            this.impactWidth = impactWidth;
            this.impactHeight = impactHeight;
        }

        int getProjectileCount() {
            return projectileCount;
        }

        float getSpread() {
            return spread;
        }

        float getHitboxWidth() {
            return hitboxWidth;
        }

        float getHitboxHeight() {
            return hitboxHeight;
        }

        static Strength fromValue(int value) {
            for (Strength strength : values()) {
                if (strength.value == value) {
                    return strength;
                }
            }
            return null;
        }
    }

    enum EffectType {
        MUZZLE_FLASH,
        IMPACT
    }

    static final class Assets implements Disposable {
        private final Texture[] projectileTextures = new Texture[Strength.values().length];
        private final Texture[] muzzleFlashTextures = new Texture[Strength.values().length];
        private final Texture[] impactTextures = new Texture[Strength.values().length];
        private final TextureRegion[][][] projectileFrames = new TextureRegion[Strength.values().length][][];
        private final TextureRegion[][][] mirroredProjectileFrames =
            new TextureRegion[Strength.values().length][][];
        private final TextureRegion[][][] muzzleFlashFrames = new TextureRegion[Strength.values().length][][];
        private final TextureRegion[][][] mirroredMuzzleFlashFrames =
            new TextureRegion[Strength.values().length][][];
        private final TextureRegion[][][] impactFrames = new TextureRegion[Strength.values().length][][];
        private final TextureRegion[][][] mirroredImpactFrames = new TextureRegion[Strength.values().length][][];

        Assets() {
            for (Strength strength : Strength.values()) {
                int index = strength.ordinal();
                projectileTextures[index] = loadTexture(strength.projectilePath);
                muzzleFlashTextures[index] = loadTexture(strength.muzzleFlashPath);
                impactTextures[index] = loadTexture(strength.impactPath);

                projectileFrames[index] = split(
                    projectileTextures[index], strength.projectilePath, strength.projectileFrames
                );
                mirroredProjectileFrames[index] = SpriteSheet.mirror(projectileFrames[index]);
                muzzleFlashFrames[index] = split(
                    muzzleFlashTextures[index], strength.muzzleFlashPath, strength.muzzleFlashFrames
                );
                mirroredMuzzleFlashFrames[index] = SpriteSheet.mirror(muzzleFlashFrames[index]);
                impactFrames[index] = split(impactTextures[index], strength.impactPath, strength.impactFrames);
                mirroredImpactFrames[index] = SpriteSheet.mirror(impactFrames[index]);
            }
        }

        private static Texture loadTexture(String path) {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return texture;
        }

        private static TextureRegion[][] split(Texture texture, String path, int columns) {
            return SpriteSheet.split(texture, path, columns, 1);
        }

        private TextureRegion projectileFrame(Strength strength, float elapsed, int direction) {
            int index = strength.ordinal();
            TextureRegion[][] frames = direction < 0
                ? mirroredProjectileFrames[index]
                : projectileFrames[index];
            return frames[0][frameIndex(elapsed, frames[0].length, PROJECTILE_FRAME_DURATION)];
        }

        private TextureRegion effectFrame(EffectType type, Strength strength, float elapsed, int direction) {
            int index = strength.ordinal();
            TextureRegion[][] frames;
            TextureRegion[][] mirroredFrames;
            if (type == EffectType.MUZZLE_FLASH) {
                frames = muzzleFlashFrames[index];
                mirroredFrames = mirroredMuzzleFlashFrames[index];
            } else {
                frames = impactFrames[index];
                mirroredFrames = mirroredImpactFrames[index];
            }
            TextureRegion[][] orientedFrames = direction < 0 ? mirroredFrames : frames;
            return orientedFrames[0][frameIndex(elapsed, orientedFrames[0].length, EFFECT_FRAME_DURATION)];
        }

        @Override
        public void dispose() {
            for (Texture texture : projectileTextures) {
                if (texture != null) texture.dispose();
            }
            for (Texture texture : muzzleFlashTextures) {
                if (texture != null) texture.dispose();
            }
            for (Texture texture : impactTextures) {
                if (texture != null) texture.dispose();
            }
        }
    }

    static final class Effect {
        private final Strength strength;
        private final EffectType type;
        private final float x;
        private final float y;
        private final int direction;
        private float elapsed;
        private boolean impactLightClaimed;

        Effect(Strength strength, EffectType type, float x, float y, int direction) {
            this.strength = strength;
            this.type = type;
            this.x = x;
            this.y = y;
            this.direction = direction;
        }

        void update(float delta) {
            elapsed += delta;
        }

        boolean isFinished() {
            int frames = type == EffectType.MUZZLE_FLASH
                ? strength.muzzleFlashFrames
                : strength.impactFrames;
            return elapsed >= frames * EFFECT_FRAME_DURATION;
        }

        EffectType getType() {
            return type;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }

        float getElapsed() {
            return elapsed;
        }

        boolean isImpactLightClaimed() {
            return impactLightClaimed;
        }

        void claimImpactLight() {
            impactLightClaimed = true;
        }

        void render(SpriteBatch batch, Assets assets) {
            float width = type == EffectType.MUZZLE_FLASH ? strength.muzzleFlashWidth : strength.impactWidth;
            float height = type == EffectType.MUZZLE_FLASH ? strength.muzzleFlashHeight : strength.impactHeight;
            TextureRegion frame = assets.effectFrame(type, strength, elapsed, direction);
            batch.draw(frame, x - width / 2f, y - height / 2f, width, height);
        }
    }

    private final Strength strength;
    private final int direction;
    private float x;
    private float y;
    private float elapsed;

    Bullet(float x, float y, Strength strength, int direction) {
        this.x = x;
        this.y = y;
        this.strength = strength;
        this.direction = direction;
    }

    void update(float delta) {
        x += direction * strength.speed * delta;
        elapsed += delta;
    }

    boolean isOutside(float worldWidth) {
        return x > worldWidth + 10f || x + strength.hitboxWidth < -10f;
    }

    boolean overlaps(Enemy enemy) {
        return x < enemy.getX() + Enemy.SIZE
            && x + strength.hitboxWidth > enemy.getX()
            && y < enemy.getY() + Enemy.SIZE
            && y + strength.hitboxHeight > enemy.getY();
    }

    void render(SpriteBatch batch, Assets assets) {
        TextureRegion frame = assets.projectileFrame(strength, elapsed, direction);
        batch.draw(
            frame,
            x + (strength.hitboxWidth - strength.projectileWidth) / 2f,
            y + (strength.hitboxHeight - strength.projectileHeight) / 2f,
            strength.projectileWidth,
            strength.projectileHeight
        );
    }

    Strength getStrength() {
        return strength;
    }

    int getDirection() {
        return direction;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    float getElapsed() {
        return elapsed;
    }

    float getCenterX() {
        return x + strength.hitboxWidth / 2f;
    }

    float getCenterY() {
        return y + strength.hitboxHeight / 2f;
    }

    private static int frameIndex(float elapsed, int frameCount, float frameDuration) {
        return Math.min((int) (elapsed / frameDuration), frameCount - 1);
    }
}
