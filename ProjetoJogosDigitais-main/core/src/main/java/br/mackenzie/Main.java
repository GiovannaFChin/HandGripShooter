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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class Main extends ApplicationAdapter {
    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GameWorld gameWorld;
    private GameController gameController;

    private enum State { MENU, PLAYING, GAME_OVER }
    private State state = State.MENU;

    private static final Color PANEL  = Color.valueOf("2A0E3D");
    private static final Color ORANGE = Color.valueOf("EF6A3E");
    private static final Color CREAM  = Color.valueOf("FFF3E0");
    private static final Color YELLOW = Color.valueOf("F7E3A8");
    private static final Color HEART_FULL  = Color.valueOf("E5384F");
    private static final Color HEART_EMPTY = Color.valueOf("4A2A5E");

    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touch = new Vector3();
    private Rectangle btnStart, btnExit;
    private boolean hoverStart, hoverExit;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.position.set(GameWorld.WORLD_WIDTH / 2f, GameWorld.WORLD_HEIGHT / 2f, 0f);
        viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        float bw = 130f, bh = 22f, cx = GameWorld.WORLD_WIDTH / 2f;
        btnStart = new Rectangle(cx - bw / 2f, GameWorld.WORLD_HEIGHT * 0.45f, bw, bh);
        btnExit  = new Rectangle(cx - bw / 2f, GameWorld.WORLD_HEIGHT * 0.45f - 30f, bw, bh);

        gameWorld = new GameWorld();
        gameController = new GameController(gameWorld, "COM3");
        gameController.start();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        if (state != State.PLAYING) handleMenuInput();

        if (state == State.PLAYING) {
            gameController.update(delta);
            if (gameWorld.getPlayer().isDead()) state = State.GAME_OVER;
        }
        camera.update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        gameWorld.renderBackground(batch);
        if (state != State.MENU) gameWorld.renderActors(batch);
        if (state != State.MENU) gameWorld.renderBullets(batch);
        batch.end();

        if (state != State.MENU) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            drawForceBar(gameController.getForce());
            drawHearts();
            shapeRenderer.end();

            if (state == State.PLAYING) {
                batch.begin();
                font.draw(batch, "ESP32 - forca", 10f, 161f);
                font.draw(batch, "Setas: mover  |  Espaco: tiro  |  1-3: testar", 10f, 12f);
                batch.end();
            }
        }

        if (state != State.PLAYING) drawMenuScreen();
    }

    private void drawForceBar(int force) {
        float fraction = MathUtils.clamp(force / 4095f, 0f, 1f);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(10f, 166f, 120f, 5f);
        shapeRenderer.setColor(1f, 0.5f, 0f, 1f);
        shapeRenderer.rect(10f, 166f, 120f * fraction, 5f);
    }

    private void startGame() {
        gameWorld.reset();
        state = State.PLAYING;
    }

    private void handleMenuInput() {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);
        hoverStart = btnStart.contains(touch.x, touch.y);
        hoverExit = btnExit.contains(touch.x, touch.y);
        boolean click = Gdx.input.justTouched();

        if ((click && hoverStart) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startGame();
        } else if ((click && hoverExit) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == State.GAME_OVER) state = State.MENU;
            else Gdx.app.exit();
        }
    }

    private void drawHeart(float cx, float cy, float s, boolean full) {
        shapeRenderer.setColor(full ? HEART_FULL : HEART_EMPTY);
        shapeRenderer.circle(cx - s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
        shapeRenderer.circle(cx + s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
        shapeRenderer.triangle(cx - s, cy + s * 0.15f, cx + s, cy + s * 0.15f, cx, cy - s);
    }

    private void drawHearts() {
        int lives = gameWorld.getPlayer().getLives();
        for (int i = 0; i < Player.MAX_LIVES; i++) {
            drawHeart(GameWorld.WORLD_WIDTH - 12f - i * 14f, GameWorld.WORLD_HEIGHT - 12f, 4f, i < lives);
        }
    }

    /** Retângulo preenchido com cantos arredondados (raio r). */
    private void roundedRect(float x, float y, float w, float h, float r) {
        shapeRenderer.rect(x + r, y, w - 2f * r, h);                 // centro
        shapeRenderer.rect(x, y + r, r, h - 2f * r);                 // lado esquerdo
        shapeRenderer.rect(x + w - r, y + r, r, h - 2f * r);         // lado direito
        shapeRenderer.circle(x + r, y + r, r, 16);                   // cantos
        shapeRenderer.circle(x + w - r, y + r, r, 16);
        shapeRenderer.circle(x + r, y + h - r, r, 16);
        shapeRenderer.circle(x + w - r, y + h - r, r, 16);
    }

    private void drawButton(Rectangle r, boolean hover) {
        float radius = r.height / 2f;   // formato de pílula (bem arredondado)

        // borda laranja
        shapeRenderer.setColor(ORANGE);
        roundedRect(r.x - 1f, r.y - 1f, r.width + 2f, r.height + 2f, radius + 1f);

        // preenchimento
        shapeRenderer.setColor(hover ? ORANGE : PANEL);
        roundedRect(r.x, r.y, r.width, r.height, radius);
    }

    private void drawButtonLabel(Rectangle r, String text, boolean hover) {
        font.setColor(hover ? PANEL : CREAM);
        layout.setText(font, text);
        font.draw(batch, text, r.x + (r.width - layout.width) / 2f, r.y + (r.height + layout.height) / 2f);
    }

    private void drawCentered(String text, float y, Color color) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, (GameWorld.WORLD_WIDTH - layout.width) / 2f, y);
    }

    private void drawMenuScreen() {
        boolean over = state == State.GAME_OVER;
        float w = GameWorld.WORLD_WIDTH, h = GameWorld.WORLD_HEIGHT;

        // Película escura sobre o fundo do jogo + botões
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.10f, 0.03f, 0.18f, 0.70f);
        shapeRenderer.rect(0f, 0f, w, h);
        drawButton(btnStart, hoverStart);
        drawButton(btnExit, hoverExit);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(2f);
        if (over) {
            drawCentered("GAME OVER", h * 0.78f, ORANGE);
        } else {
            float gap = 6f;
            layout.setText(font, "HandGrip");
            float w1 = layout.width;
            layout.setText(font, "Shooter");
            float x0 = (w - (w1 + gap + layout.width)) / 2f;
            font.setColor(CREAM);
            font.draw(batch, "HandGrip", x0, h * 0.78f);
            font.setColor(YELLOW);
            font.draw(batch, "Shooter", x0 + w1 + gap, h * 0.78f);
        }
        font.getData().setScale(1f);


        drawButtonLabel(btnStart, over ? "Jogar novamente" : "Jogar", hoverStart);
        drawButtonLabel(btnExit, over ? "Menu" : "Sair", hoverExit);

        font.setColor(Color.WHITE);
        batch.end();
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
