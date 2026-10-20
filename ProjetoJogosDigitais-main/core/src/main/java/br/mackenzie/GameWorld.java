package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

final class GameWorld {
    static final float WORLD_WIDTH = 320f;
    static final float WORLD_HEIGHT = 180f;

    private static final float PLAYER_GROUND_Y = 18f;
    private static final float ENEMY_SPAWN_X = WORLD_WIDTH + 8f;
    private static final float ENEMY_INTERVAL = 2f;

    private final Player player;
    private final Texture enemyTexture;
    private final TextureRegion[][] enemyFrames;
    private final TextureRegion[][] mirroredEnemyFrames;
    private final ParallaxBackground background;
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private float fireCooldown;
    private float enemySpawnTimer = ENEMY_INTERVAL;

    GameWorld() {
        player = new Player("player/shoot/movement/Dog_shoot.png");
        enemyTexture = new Texture(Gdx.files.internal("enemies/pink_j/jelly_run.png"));
        enemyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        enemyFrames = SpriteSheet.split(enemyTexture, "enemies/pink_j/jelly_run.png", 2, 3);
        mirroredEnemyFrames = SpriteSheet.mirror(enemyFrames);
        background = new ParallaxBackground();
        enemies.add(new Enemy(ENEMY_SPAWN_X, PLAYER_GROUND_Y));
    }

    void update(float delta, float horizontal, float vertical, boolean firing) {
        player.update(delta, horizontal, vertical, firing);
        background.update(delta);

        fireCooldown -= delta;
        if (firing && fireCooldown <= 0f) {
            shoot(1);
            fireCooldown = 0.25f;
        }

        enemySpawnTimer -= delta;
        if (enemySpawnTimer <= 0f) {
            enemies.add(new Enemy(ENEMY_SPAWN_X, PLAYER_GROUND_Y));
            enemySpawnTimer = ENEMY_INTERVAL;
        }

        updateBullets(delta);
        updateEnemies(delta);
        removeHitEnemies();
    }

    void shoot(int level) {
        if (level < 1 || level > 3) {
            return;
        }

        float centerY = player.getY() + Player.HEIGHT * 0.62f;
        if (level == 1) {
            addBullet(centerY, 0f, 8f, 3f, 220f, level);
        } else if (level == 2) {
            addBullet(centerY - 5f, 0f, 10f, 4f, 240f, level);
            addBullet(centerY + 5f, 0f, 10f, 4f, 240f, level);
        } else {
            addBullet(centerY - 8f, 0f, 10f, 4f, 260f, level);
            addBullet(centerY, 0f, 10f, 4f, 260f, level);
            addBullet(centerY + 8f, 0f, 10f, 4f, 260f, level);
        }
    }

    private void addBullet(float y, float offset, float width, float height, float speed, int level) {
        int direction = player.getFacing();
        float x = direction > 0
            ? player.getX() + Player.WIDTH + offset
            : player.getX() - width - offset;
        bullets.add(new Bullet(x, y, width, height, speed, direction, level));
    }

    private void updateBullets(float delta) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(delta);
            if (bullet.isOutside(WORLD_WIDTH)) {
                bullets.removeIndex(i);
            }
        }
    }

    private void updateEnemies(float delta) {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(delta, player.getX());
            if (enemy.isOutside(WORLD_WIDTH)) {
                enemies.removeIndex(i);
            }
        }
    }

    private void removeHitEnemies() {
        for (int bulletIndex = bullets.size - 1; bulletIndex >= 0; bulletIndex--) {
            Bullet bullet = bullets.get(bulletIndex);
            for (int enemyIndex = enemies.size - 1; enemyIndex >= 0; enemyIndex--) {
                Enemy enemy = enemies.get(enemyIndex);
                if (bullet.overlaps(enemy)) {
                    bullets.removeIndex(bulletIndex);
                    enemies.removeIndex(enemyIndex);
                    break;
                }
            }
        }
    }

    void renderBackground(SpriteBatch batch) {
        background.render(batch);
    }

    void renderActors(SpriteBatch batch) {
        player.render(batch);
        for (Enemy enemy : enemies) {
            int frameIndex = (int) (enemy.getAnimationTime() * 8f) % 6;
            boolean playerIsLeft = player.getX() + Player.WIDTH / 2f
                < enemy.getX() + Enemy.SIZE / 2f;
            TextureRegion frame = (playerIsLeft ? enemyFrames : mirroredEnemyFrames)
                [frameIndex / 2][frameIndex % 2];
            float drawWidth = Enemy.SIZE * frame.getRegionWidth() / frame.getRegionHeight();
            batch.draw(
                frame,
                enemy.getX() + (Enemy.SIZE - drawWidth) / 2f,
                enemy.getY(),
                drawWidth,
                Enemy.SIZE
            );
        }
    }

    void renderBullets(ShapeRenderer renderer) {
        for (Bullet bullet : bullets) {
            bullet.render(renderer);
        }
    }

    void dispose() {
        background.dispose();
        player.dispose();
        enemyTexture.dispose();
    }
}
