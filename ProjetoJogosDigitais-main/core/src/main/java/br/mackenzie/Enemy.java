package br.mackenzie;

final class Enemy {
    static final float SIZE = 32f;

    private float x;
    private final float y;
    private final float spawnTime;
    private float animationTime;

    Enemy(float x, float y, float spawnTime) {
        this.x = x;
        this.y = y;
        this.spawnTime = spawnTime;
    }

    void update(float delta, float playerX) {
        float playerCenter = playerX + Player.WIDTH / 2f;
        float targetX = playerCenter - SIZE / 2f;
        x += Math.signum(targetX - x) * 50f * delta;
        animationTime += delta;
    }

    boolean isOutside(float worldWidth) {
        return x + SIZE < 0f || x > worldWidth + SIZE;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    float getAnimationTime() {
        return animationTime;
    }

    float getSpawnTime() {
        return spawnTime;
    }
}
