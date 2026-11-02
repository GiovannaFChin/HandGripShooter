package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

final class GameWorld {
    static final float WORLD_WIDTH = 320f;
    static final float WORLD_HEIGHT = 180f;

    static final float ACTOR_MIN_Y = 2f;
    static final float ACTOR_MAX_Y = ACTOR_MIN_Y + 20f;

    private static final float ENEMY_SPAWN_X = WORLD_WIDTH + 8f;
    private static final float MIN_ENEMY_INTERVAL = 1f;
    private static final float MAX_ENEMY_INTERVAL = 2f;

    private final Player player;
    private final Texture enemyTexture;
    private final TextureRegion[][] enemyFrames;
    private final TextureRegion[][] mirroredEnemyFrames;
    private final Bullet.Assets bulletAssets;
    private final ParallaxBackground background;
    private final GameStats stats = new GameStats();
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Bullet.Effect> bulletEffects = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private float fireCooldown;
    private float enemySpawnTimer = randomEnemyInterval();

    private final Rectangle enemyBounds = new Rectangle();

    GameWorld() {
        player = new Player("player/shoot/movement/Dog_shoot.png");
        enemyTexture = new Texture(Gdx.files.internal("enemies/pink_j/jelly_run.png"));
        enemyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        enemyFrames = SpriteSheet.split(enemyTexture, "enemies/pink_j/jelly_run.png", 2, 3);
        mirroredEnemyFrames = SpriteSheet.mirror(enemyFrames);
        bulletAssets = new Bullet.Assets();
        background = new ParallaxBackground();
        spawnEnemy();
    }

    private void spawnEnemy() {
        float spawnY = MathUtils.random(ACTOR_MIN_Y, ACTOR_MAX_Y);
        enemies.add(new Enemy(ENEMY_SPAWN_X, spawnY, background.getElapsed()));
    }

    private static float randomEnemyInterval() {
        return MathUtils.random(MIN_ENEMY_INTERVAL, MAX_ENEMY_INTERVAL);
    }

    private void updateBulletEffects(float delta) {
        for (int i = bulletEffects.size - 1; i >= 0; i--) {
            Bullet.Effect effect = bulletEffects.get(i);
            effect.update(delta);
            if (effect.isFinished()) {
                bulletEffects.removeIndex(i);
            }
        }
    }

    void update(float delta, float horizontal, float vertical, boolean firing) {
        stats.update(delta);   // tempo de jogo da fase (a pausa não conta)
        player.update(delta, horizontal, vertical, firing);
        ParallaxBackground.Period previousPeriod = background.getCycleState().getPeriod();
        background.update(delta);
        ParallaxBackground.Period currentPeriod = background.getCycleState().getPeriod();
        if (currentPeriod != previousPeriod) {
            stats.nextPhase();
        }

        fireCooldown -= delta;
        if (firing && fireCooldown <= 0f) {
            shoot(1);
            fireCooldown = 0.25f;
        }

        enemySpawnTimer -= delta;
        if (enemySpawnTimer <= 0f) {
            spawnEnemy();
            enemySpawnTimer = randomEnemyInterval();
        }

        updateBullets(delta);
        updateEnemies(delta);
        removeHitEnemies();
        updateBulletEffects(delta);
        checkPlayerCollisions();
    }

    /** Aperto válido vindo do hand grip: conta o aperto e dispara. Os pontos vêm só ao acertar. */
    void registerSqueeze(int level) {
        stats.registerSqueeze();
        shoot(level);
    }

    void shoot(int level) {
        Bullet.Strength strength = Bullet.Strength.fromValue(level);
        if (strength == null) {
            return;
        }

        int direction = player.getFacing();
        float centerY = getMuzzleY();
        float muzzleX = getMuzzleX();
        bulletEffects.add(new Bullet.Effect(
            strength, Bullet.EffectType.MUZZLE_FLASH, muzzleX, centerY, direction
        ));

        for (int i = 0; i < strength.getProjectileCount(); i++) {
            float offset = (i - (strength.getProjectileCount() - 1) / 2f) * strength.getSpread();
            addBullet(centerY + offset, strength, direction);
        }
    }

    float getMuzzleX() {
        return player.getFacing() > 0
            ? player.getX() + Player.WIDTH + 2f
            : player.getX() - 2f;
    }

    float getMuzzleY() {
        return player.getY() + Player.HEIGHT * 0.62f;
    }

    int getPlayerFacing() {
        return player.getFacing();
    }

    private void addBullet(float centerY, Bullet.Strength strength, int direction) {
        float width = strength.getHitboxWidth();
        float x = direction > 0
            ? player.getX() + Player.WIDTH
            : player.getX() - width;
        float y = centerY - strength.getHitboxHeight() / 2f;
        bullets.add(new Bullet(x, y, strength, direction));
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
            if (enemy.isOutside(WORLD_WIDTH) || background.getEnemyAlpha(enemy.getSpawnTime()) <= 0f) {
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
                    bulletEffects.add(new Bullet.Effect(
                        bullet.getStrength(),
                        Bullet.EffectType.IMPACT,
                        enemy.getX() + Enemy.SIZE / 2f,
                        enemy.getY() + Enemy.SIZE / 2f,
                        bullet.getDirection()
                    ));
                    // Pontos só quando o tiro acerta um inimigo.
                    // ordinal() + 1 assume que o enum Strength está na ordem leve, moderado, forte.
                    stats.registerHit(bullet.getStrength().ordinal() + 1);
                    bullets.removeIndex(bulletIndex);
                    enemies.removeIndex(enemyIndex);
                    break;
                }
            }
        }
    }

    Player getPlayer() {
        return player;
    }

    GameStats getStats() {
        return stats;
    }

    ParallaxBackground.CycleState getCycleState() {
        return background.getCycleState();
    }

    float getElapsed() {
        return background.getElapsed();
    }

    float getLayerOffset(String layerName) {
        return background.getLayerOffset(layerName);
    }

    float getLayerWidth(String layerName) {
        return background.getLayerWidth(layerName);
    }

    int getBulletCount() {
        return bullets.size;
    }

    Bullet getBullet(int index) {
        return bullets.get(index);
    }

    boolean containsBullet(Bullet target) {
        for (int i = 0; i < bullets.size; i++) {
            if (bullets.get(i) == target) {
                return true;
            }
        }
        return false;
    }

    int getBulletEffectCount() {
        return bulletEffects.size;
    }

    Bullet.Effect getBulletEffect(int index) {
        return bulletEffects.get(index);
    }

    int getEnemyCount() {
        return enemies.size;
    }

    Enemy getEnemy(int index) {
        return enemies.get(index);
    }

    float getEnemyRenderedWidth(int index) {
        Enemy enemy = enemies.get(index);
        int frameIndex = (int) (enemy.getAnimationTime() * 8f) % 6;
        TextureRegion frame = enemyFrames[frameIndex / 2][frameIndex % 2];
        return Enemy.SIZE * frame.getRegionWidth() / frame.getRegionHeight();
    }

    /** Chame quando o jogador cumprir a meta da fase. */
    void advancePhase() {
        stats.nextPhase();
    }

    /** Dano por inimigo, em pontos de vida: 1 = meio coração, 2 = coração inteiro. */
    private int enemyDamage() {
        return stats.getPhaseNumber() == 1 ? 1 : 2;
    }

    /** Inimigo que encosta no player some e tira vida. */
    private void checkPlayerCollisions() {
        Rectangle playerBounds = player.getBounds();
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemyBounds.set(enemy.getX() + 3f, enemy.getY(), Enemy.SIZE - 6f, Enemy.SIZE);
            if (playerBounds.overlaps(enemyBounds)) {
                player.hit(enemyDamage());
                enemies.removeIndex(i);
            }
        }
    }

    /** Reinicia a partida (usado ao clicar em Jogar, Jogar novamente ou Reiniciar). */
    void reset() {
        player.reset();
        background.reset();
        stats.reset();
        bullets.clear();
        bulletEffects.clear();
        enemies.clear();
        spawnEnemy();
        enemySpawnTimer = randomEnemyInterval();
        fireCooldown = 0f;
    }

    void renderBackground(SpriteBatch batch) {
        background.render(batch);
    }

    void renderActors(SpriteBatch batch) {
        player.render(batch);
        Color originalColor = new Color(batch.getColor());
        for (Enemy enemy : enemies) {
            int frameIndex = (int) (enemy.getAnimationTime() * 8f) % 6;
            boolean playerIsLeft = player.getX() + Player.WIDTH / 2f
                < enemy.getX() + Enemy.SIZE / 2f;
            TextureRegion frame = (playerIsLeft ? enemyFrames : mirroredEnemyFrames)
                [frameIndex / 2][frameIndex % 2];
            float drawWidth = Enemy.SIZE * frame.getRegionWidth() / frame.getRegionHeight();
            batch.setColor(
                originalColor.r,
                originalColor.g,
                originalColor.b,
                originalColor.a * background.getEnemyAlpha(enemy.getSpawnTime())
            );
            batch.draw(
                frame,
                enemy.getX() + (Enemy.SIZE - drawWidth) / 2f,
                enemy.getY(),
                drawWidth,
                Enemy.SIZE
            );
        }
        batch.setColor(originalColor);
    }

    void renderBullets(SpriteBatch batch) {
        for (Bullet.Effect effect : bulletEffects) {
            effect.render(batch, bulletAssets);
        }
        for (Bullet bullet : bullets) {
            bullet.render(batch, bulletAssets);
        }
    }

    void dispose() {
        background.dispose();
        player.dispose();
        enemyTexture.dispose();
        bulletAssets.dispose();
    }
}
