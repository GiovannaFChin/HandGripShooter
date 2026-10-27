package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
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
    private static final boolean LIGHT_SPIKE = true;
    private static final boolean DEBUG_LIGHTS = false;

    private static final String SERIAL_PORT = "COM6";

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private FrameBuffer sceneFrameBuffer;
    private LightingManager lightingManager;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GameWorld gameWorld;
    private GameController gameController;
    private Texture menuBackground;
    private Texture menuLogo;
    private Texture menuNewGame;
    private Texture menuNewGameSelected;
    private Texture menuExit;
    private Texture menuExitSelected;

    private enum State { MENU, PLAYING, PAUSED, GAME_OVER }
    private State state = State.MENU;

    private static final float MENU_OPTION_X = 20f;
    private static final float MENU_OPTION_Y = 69f;
    private static final float MENU_OPTION_GAP = 4f;

    private static final Color PANEL  = Color.valueOf("2A0E3D");
    private static final Color ORANGE = Color.valueOf("EF6A3E");
    private static final Color CREAM  = Color.valueOf("FFF3E0");
    private static final Color YELLOW = Color.valueOf("F7E3A8");
    private static final Color HEART_FULL  = Color.valueOf("E5384F");
    private static final Color HEART_EMPTY = Color.valueOf("4A2A5E");

    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touch = new Vector3();
    private Rectangle btnStart, btnExit;
    private Rectangle btnResume, btnRestart, btnToMenu;
    private final Rectangle menuNewGameBounds = new Rectangle(
        MENU_OPTION_X, MENU_OPTION_Y, 66f, 10f
    );
    private final Rectangle menuExitBounds = new Rectangle(
        MENU_OPTION_X, MENU_OPTION_Y - (10f + MENU_OPTION_GAP), 36f, 10f
    );
    private boolean hoverResume, hoverRestart, hoverToMenu;
    private boolean hoverStart, hoverExit;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.position.set(GameWorld.WORLD_WIDTH / 2f, GameWorld.WORLD_HEIGHT / 2f, 0f);
        viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        if (LIGHT_SPIKE) {
            sceneFrameBuffer = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                (int) GameWorld.WORLD_WIDTH,
                (int) GameWorld.WORLD_HEIGHT,
                false
            );
            sceneFrameBuffer.getColorBufferTexture().setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            );
            lightingManager = new LightingManager(camera, batch, viewport);
            lightingManager.init();
        }
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        menuBackground = loadMenuTexture("menu_ui/fundo_inicial.png");
        menuLogo = loadMenuTexture("menu_ui/logo_completa.png");
        menuNewGame = loadMenuTexture("menu_ui/opcao_novo_jogo_normal.png");
        menuNewGameSelected = loadMenuTexture("menu_ui/opcao_novo_jogo_selecionada.png");
        menuExit = loadMenuTexture("menu_ui/opcao_sair_normal.png");
        menuExitSelected = loadMenuTexture("menu_ui/opcao_sair_selecionada.png");

        float bw = 130f, bh = 22f, cx = GameWorld.WORLD_WIDTH / 2f;
        btnStart = new Rectangle(cx - bw / 2f, GameWorld.WORLD_HEIGHT * 0.45f, bw, bh);
        btnExit  = new Rectangle(cx - bw / 2f, GameWorld.WORLD_HEIGHT * 0.45f - 30f, bw, bh);
        float py = GameWorld.WORLD_HEIGHT * 0.52f;
        btnResume  = new Rectangle(cx - bw / 2f, py,        bw, bh);
        btnRestart = new Rectangle(cx - bw / 2f, py - 30f,  bw, bh);
        btnToMenu  = new Rectangle(cx - bw / 2f, py - 60f,  bw, bh);

        gameWorld = new GameWorld();
        gameController = new GameController(gameWorld, SERIAL_PORT);
        gameController.start();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        if (state == State.PLAYING) {
            updatePlaying(delta);
        } else {
            gameController.clearPendingShots();
            handleMenuInput();
        }
        camera.update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);
        if (state == State.MENU) {
            drawMenuScreen();
            return;
        }

        if (LIGHT_SPIKE) {
            lightingManager.updateFog(
                gameWorld.getElapsed(),
                gameWorld.getCycleState(),
                gameWorld.getLayerOffset("10_chao_e_props")
            );
            sceneFrameBuffer.begin();
            ScreenUtils.clear(0f, 0f, 0f, 1f);
            batch.begin();
            gameWorld.renderBackground(batch);
            lightingManager.renderFog();
            if (state != State.MENU) {
                lightingManager.renderShadows(gameWorld, gameWorld.getCycleState());
            }
            if (state != State.MENU) gameWorld.renderActors(batch);
            if (state != State.MENU) gameWorld.renderBullets(batch);
            batch.end();
            sceneFrameBuffer.end();

            lightingManager.update(
                delta,
                gameWorld.getCycleState(),
                gameWorld.getElapsed(),
                gameWorld.getLayerOffset("10_chao_e_props"),
                gameWorld.getLayerWidth("10_chao_e_props"),
                gameWorld.getLayerOffset("09_vila_perto"),
                gameWorld.getLayerWidth("09_vila_perto"),
                DEBUG_LIGHTS,
                gameWorld
            );
            lightingManager.render(sceneFrameBuffer.getColorBufferTexture());
            batch.setShader(null);
        } else {
            batch.begin();
            gameWorld.renderBackground(batch);
            if (state != State.MENU) gameWorld.renderActors(batch);
            if (state != State.MENU) gameWorld.renderBullets(batch);
            batch.end();
        }

        if (LIGHT_SPIKE && (DEBUG_LIGHTS || LightingManager.DEBUG_MUZZLE
            || LightingManager.DEBUG_IMPACT || LightingManager.DEBUG_SHADOW || LightingManager.DEBUG_FOG)) {
            viewport.apply();
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(
                LightingManager.DEBUG_SHADOW || LightingManager.DEBUG_FOG
                    ? ShapeRenderer.ShapeType.Line
                    : ShapeRenderer.ShapeType.Filled
            );
            if (DEBUG_LIGHTS || LightingManager.DEBUG_MUZZLE || LightingManager.DEBUG_IMPACT) {
                lightingManager.renderDebug(shapeRenderer);
            }
            if (LightingManager.DEBUG_SHADOW) {
                lightingManager.renderShadowDebug(gameWorld, gameWorld.getCycleState(), shapeRenderer);
            }
            if (LightingManager.DEBUG_FOG) {
                lightingManager.renderFogDebug(shapeRenderer);
            }
            shapeRenderer.end();
        }

        if (state != State.MENU) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            drawForceBar(gameController.getForce());
            drawHearts();
            shapeRenderer.end();

            if (state == State.PLAYING) {
                batch.begin();
                GameStats stats = gameWorld.getStats();
                int p = stats.getPhaseNumber();
                font.draw(batch, "Fase " + p + "  Apertos: " + stats.getSqueezes(p - 1)
                    + "  Pontos: " + stats.getTotalScore(), 10f, 147f);
                font.draw(batch, "Setas: mover  |  Espaco: tiro  |  ESC: pausar", 10f, 12f);
                batch.end();
            }
        }

        if (state == State.PAUSED) drawPauseScreen();
        else if (state != State.PLAYING) drawMenuScreen();
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

    private Texture loadMenuTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private void updatePlaying(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            state = State.PAUSED;
            return;
        }
        gameController.update(delta);
        if (gameWorld.getPlayer().isDead()) state = State.GAME_OVER;
    }

    @Override
    public void pause() {
        if (state == State.PLAYING) state = State.PAUSED;
    }

    private void drawSummary(float top) {
        GameStats s = gameWorld.getStats();
        float[] col = {55f, 105f, 170f, 235f};
        float lineH = 12f;

        font.setColor(YELLOW);
        font.draw(batch, "Fase", col[0], top);
        font.draw(batch, "Apertos", col[1], top);
        font.draw(batch, "Pontos", col[2], top);
        font.draw(batch, "Aper./min", col[3], top);

        font.setColor(CREAM);
        for (int i = 0; i < s.getPhaseNumber(); i++) {
            float y = top - lineH * (i + 1);
            font.draw(batch, String.valueOf(i + 1), col[0], y);
            font.draw(batch, String.valueOf(s.getSqueezes(i)), col[1], y);
            font.draw(batch, String.valueOf(s.getScore(i)), col[2], y);
            font.draw(batch, String.format("%.1f", s.getSqueezesPerMinute(i)), col[3], y);
        }

        float yTotal = top - lineH * (s.getPhaseNumber() + 1);
        font.setColor(ORANGE);
        font.draw(batch, "Total", col[0], yTotal);
        font.draw(batch, String.valueOf(s.getTotalSqueezes()), col[1], yTotal);
        font.draw(batch, String.valueOf(s.getTotalScore()), col[2], yTotal);
        font.draw(batch, String.format("%.1f", s.getTotalSqueezesPerMinute()), col[3], yTotal);
    }

    private void drawPauseScreen() {
        float w = GameWorld.WORLD_WIDTH, h = GameWorld.WORLD_HEIGHT;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.10f, 0.03f, 0.18f, 0.70f);
        shapeRenderer.rect(0f, 0f, w, h);
        drawButton(btnResume, hoverResume);
        drawButton(btnRestart, hoverRestart);
        drawButton(btnToMenu, hoverToMenu);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(2f);
        drawCentered("PAUSADO", h * 0.82f, ORANGE);
        font.getData().setScale(1f);
        drawButtonLabel(btnResume, "Continuar", hoverResume);
        drawButtonLabel(btnRestart, "Reiniciar", hoverRestart);
        drawButtonLabel(btnToMenu, "Menu principal", hoverToMenu);
        drawCentered("ESC: continuar  |  R: reiniciar  |  M: menu", 12f, CREAM);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void layoutMenuButtons() {
        float w = GameWorld.WORLD_WIDTH, h = GameWorld.WORLD_HEIGHT;
        float bw = 130f, bh = 22f, cx = w / 2f;
        if (state == State.GAME_OVER) {
            btnStart.set(cx - bw - 5f, 8f, bw, bh);
            btnExit.set(cx + 5f, 8f, bw, bh);
        } else {
            btnStart.set(cx - bw / 2f, h * 0.45f, bw, bh);
            btnExit.set(cx - bw / 2f, h * 0.45f - 30f, bw, bh);
        }
    }

    private void handleMenuInput() {
        layoutMenuButtons();
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);
        if (state == State.MENU) {
            hoverStart = menuNewGameBounds.contains(touch.x, touch.y);
            hoverExit = menuExitBounds.contains(touch.x, touch.y);
            boolean click = Gdx.input.justTouched();

            if ((click && hoverStart) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                startGame();
            } else if ((click && hoverExit) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
            }
            return;
        }

        hoverStart = btnStart.contains(touch.x, touch.y);
        hoverExit = btnExit.contains(touch.x, touch.y);
        boolean click = Gdx.input.justTouched();

        if (state == State.PAUSED) {
            hoverResume  = btnResume.contains(touch.x, touch.y);
            hoverRestart = btnRestart.contains(touch.x, touch.y);
            hoverToMenu  = btnToMenu.contains(touch.x, touch.y);

            if ((click && hoverResume) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                state = State.PLAYING;
            } else if ((click && hoverRestart) || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                startGame();
            } else if ((click && hoverToMenu) || Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                state = State.MENU;
            }
            return;
        }

        if ((click && hoverStart) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startGame();
        } else if ((click && hoverExit) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == State.GAME_OVER) state = State.MENU;
            else Gdx.app.exit();
        }
    }

    private void drawHeart(float cx, float cy, float s, int part) {
        shapeRenderer.setColor(HEART_EMPTY);
        shapeRenderer.circle(cx - s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
        shapeRenderer.circle(cx + s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
        shapeRenderer.triangle(cx - s, cy + s * 0.15f, cx + s, cy + s * 0.15f, cx, cy - s);

        shapeRenderer.setColor(HEART_FULL);
        if (part >= 2) {
            shapeRenderer.circle(cx - s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
            shapeRenderer.circle(cx + s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
            shapeRenderer.triangle(cx - s, cy + s * 0.15f, cx + s, cy + s * 0.15f, cx, cy - s);
        } else if (part == 1) {
            shapeRenderer.circle(cx - s * 0.5f, cy + s * 0.3f, s * 0.55f, 16);
            shapeRenderer.triangle(cx - s, cy + s * 0.15f, cx, cy + s * 0.15f, cx, cy - s);
        }
    }

    private void drawHearts() {
        int hp = gameWorld.getPlayer().getHp();
        float s = 7f;
        float spacing = 2f * s + 5f;
        float cy = GameWorld.WORLD_HEIGHT - 11f;

        for (int i = 0; i < Player.MAX_LIVES; i++) {
            float cx = GameWorld.WORLD_WIDTH - 12f - (Player.MAX_LIVES - 1 - i) * spacing;
            drawHeart(cx, cy, s, hp - i * 2);
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
        if (state == State.MENU) {
            float optionY = MENU_OPTION_Y;
            batch.begin();
            batch.draw(menuBackground, 0f, 0f, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
            batch.draw(menuLogo, 7f, 104f);
            batch.draw(
                hoverStart ? menuNewGameSelected : menuNewGame,
                MENU_OPTION_X,
                optionY
            );
            batch.draw(
                hoverExit ? menuExitSelected : menuExit,
                MENU_OPTION_X,
                optionY - (10f + MENU_OPTION_GAP)
            );
            batch.end();
            return;
        }

        boolean over = state == State.GAME_OVER;
        float w = GameWorld.WORLD_WIDTH, h = GameWorld.WORLD_HEIGHT;

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
            drawCentered("GAME OVER", h * 0.90f, ORANGE);
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

        if (over) drawSummary(h * 0.70f);

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
        if (lightingManager != null) {
            lightingManager.dispose();
        }
        if (sceneFrameBuffer != null) {
            sceneFrameBuffer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (menuBackground != null) {
            menuBackground.dispose();
        }
        if (menuLogo != null) {
            menuLogo.dispose();
        }
        if (menuNewGame != null) {
            menuNewGame.dispose();
        }
        if (menuNewGameSelected != null) {
            menuNewGameSelected.dispose();
        }
        if (menuExit != null) {
            menuExit.dispose();
        }
        if (menuExitSelected != null) {
            menuExitSelected.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
