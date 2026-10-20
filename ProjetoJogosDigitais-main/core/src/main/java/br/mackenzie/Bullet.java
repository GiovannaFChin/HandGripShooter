package br.mackenzie;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

final class Bullet {
    private final float y;
    private final float width;
    private final float height;
    private final float speed;
    private final int direction;
    private final int level;
    private float x;

    Bullet(float x, float y, float width, float height, float speed, int direction, int level) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.direction = direction;
        this.level = level;
    }

    void update(float delta) {
        x += direction * speed * delta;
    }

    boolean isOutside(float worldWidth) {
        return x > worldWidth + 10f || x + width < -10f;
    }

    boolean overlaps(Enemy enemy) {
        return x < enemy.getX() + 32f
            && x + width > enemy.getX()
            && y < enemy.getY() + 32f
            && y + height > enemy.getY();
    }

    void render(ShapeRenderer renderer) {
        if (level == 1) {
            renderer.setColor(1f, 0.9f, 0.3f, 1f);
        } else if (level == 2) {
            renderer.setColor(0.3f, 0.9f, 1f, 1f);
        } else {
            renderer.setColor(1f, 0.4f, 0.3f, 1f);
        }
        renderer.rect(x, y, width, height);
    }
}
