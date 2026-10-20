package br.mackenzie;

final class Enemy {
    private float x;
    private final float y;
    private float animationTime;

    Enemy(float x, float y) {
        this.x = x;
        this.y = y;
    }

    void update(float delta, float playerX) {
        x += Math.signum(playerX - x) * 35f * delta;
        animationTime += delta;
    }

    boolean isOutside(float worldWidth) {
        return x + 32f < 0f || x > worldWidth;
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
}
