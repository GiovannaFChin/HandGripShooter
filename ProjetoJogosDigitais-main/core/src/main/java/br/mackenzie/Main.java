package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main extends ApplicationAdapter {
    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GameWorld gameWorld;
    private GameController gameController;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.position.set(GameWorld.WORLD_WIDTH / 2f, GameWorld.WORLD_HEIGHT / 2f, 0f);
        viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        gameWorld = new GameWorld();
        gameController = new GameController(gameWorld, "COM3");
        gameController.start();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        gameController.update(delta);
        camera.update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        gameWorld.renderBackground(batch);
        gameWorld.renderActors(batch);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        gameWorld.renderBullets(shapeRenderer);
        drawForceBar(gameController.getForce());
        shapeRenderer.end();

        batch.begin();
        font.draw(batch, "ESP32 - forca", 10f, 161f);
        font.draw(batch, "Setas: mover  |  Espaco: tiro  |  1-3: testar", 10f, 12f);
        batch.end();
    }

    private void drawForceBar(int force) {
        float fraction = MathUtils.clamp(force / 4095f, 0f, 1f);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(10f, 166f, 120f, 5f);
        shapeRenderer.setColor(1f, 0.5f, 0f, 1f);
        shapeRenderer.rect(10f, 166f, 120f * fraction, 5f);
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (gameController != null) {
            gameController.dispose();
        }
        if (gameWorld != null) {
            gameWorld.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
