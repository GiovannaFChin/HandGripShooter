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
import java.util.HashMap;
import java.util.Map;

public class Main extends ApplicationAdapter {
    private static final boolean LIGHT_SPIKE = true;
    private static final boolean DEBUG_LIGHTS = false;

    private static final String SERIAL_PORT = "COM6";
    private static final String PAUSE_UI_FOLDER = "menu_ui/pause/";
    private static final String IN_GAME_UI_FOLDER = "menu_ui/in-game/";
    private static final float CONTROLS_FADE_DURATION = 3f;
    private static final String[] PAUSE_UI_ASSETS = {
        "botao_continuar_normal.png", "botao_continuar_selecionado.png",
        "botao_menu_principal_normal.png", "botao_menu_principal_selecionado.png",
        "botao_reiniciar_normal.png", "botao_reiniciar_selecionado.png",
        "overlay_escurecer.png", "painel_pausa.png", "seta_cursor_direita.png",
        "tecla_esc.png", "tecla_m.png", "tecla_r.png", "titulo_pausado.png"
    };
    private static final String[] IN_GAME_UI_ASSETS = {
        "barra_forca_marcadores.png", "barra_forca_moldura.png",
        "barra_forca_preenchimento.png", "coracao_cheio.png", "coracao_vazio.png",
        "icone_apertos.png", "icone_fase_manha.png", "icone_fase_noite.png",
        "icone_fase_tarde.png", "icone_pontos.png", "painel_apertos.png",
        "painel_controles.png", "painel_fase.png", "painel_pontos.png",
        "rotulo_apertos.png", "rotulo_fase.png", "rotulo_forca.png",
        "rotulo_mover.png", "rotulo_pausar.png", "rotulo_pontos.png", "rotulo_tiro.png",
        "tecla_baixo.png", "tecla_cima.png", "tecla_direita.png",
        "tecla_espaco.png"
    };

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
    private final Map<String, Texture> uiTextures = new HashMap<>();
    private boolean controlsFadeStarted;
    private float controlsFadeElapsed;

    private enum State { MENU, PLAYING, PAUSED, GAME_OVER }
    private State state = State.MENU;

    private static final float MENU_OPTION_X = 20f;
    private static final float MENU_OPTION_Y = 69f;
    private static final float MENU_OPTION_GAP = 4f;

    private static final Color PANEL  = Color.valueOf("2A0E3D");
    private static final Color ORANGE = Color.valueOf("EF6A3E");
    private static final Color CREAM  = Color.valueOf("FFF3E0");
    private static final Color YELLOW = Color.valueOf("F7E3A8");
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
        float pauseButtonX = (GameWorld.WORLD_WIDTH - 148f) / 2f;
        btnResume  = new Rectangle(pauseButtonX, 94f, 148f, 22f);
        btnRestart = new Rectangle(pauseButtonX, 66f, 148f, 22f);
        btnToMenu  = new Rectangle(pauseButtonX, 38f, 148f, 22f);

        loadUiTextures(PAUSE_UI_FOLDER, PAUSE_UI_ASSETS);
        loadUiTextures(IN_GAME_UI_FOLDER, IN_GAME_UI_ASSETS);
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
            drawHud();
        }

        if (state == State.PAUSED) drawPauseScreen();
        else if (state != State.PLAYING) drawMenuScreen();
    }

    private void loadUiTextures(String folder, String[] assets) {
        for (String asset : assets) {
            String path = folder + asset;
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            uiTextures.put(path, texture);
        }
    }

    private void startGame() {
        gameWorld.reset();
        controlsFadeStarted = false;
        controlsFadeElapsed = 0f;
        state = State.PLAYING;
    }

    private Texture loadMenuTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private Texture uiTexture(String path) {
        Texture texture = uiTextures.get(path);
        if (texture == null) {
            throw new IllegalStateException("UI texture was not loaded: " + path);
        }
        return texture;
    }

    private void drawUiTexture(String path, float x, float y) {
        batch.draw(uiTexture(path), x, y);
    }

    private void drawUiTexture(String path, float x, float y, float width, float height) {
        batch.draw(uiTexture(path), x, y, width, height);
    }

    private void updatePlaying(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            state = State.PAUSED;
            return;
        }
        float previousPlayerX = gameWorld.getPlayer().getX();
        float previousPlayerY = gameWorld.getPlayer().getY();
        gameController.update(delta);
        if (!controlsFadeStarted
            && (gameWorld.getPlayer().getX() != previousPlayerX
                || gameWorld.getPlayer().getY() != previousPlayerY)) {
            controlsFadeStarted = true;
        } else if (controlsFadeStarted) {
            controlsFadeElapsed = Math.min(CONTROLS_FADE_DURATION, controlsFadeElapsed + delta);
        }
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
        boolean selectedResume = hoverResume || (!hoverRestart && !hoverToMenu);
        boolean selectedRestart = hoverRestart;
        boolean selectedToMenu = hoverToMenu;
        batch.begin();
        drawUiTexture(PAUSE_UI_FOLDER + "overlay_escurecer.png", 0f, 0f);
        drawUiTexture(PAUSE_UI_FOLDER + "painel_pausa.png", 62f, 24.5f);
        drawUiTexture(PAUSE_UI_FOLDER + "titulo_pausado.png", 96.5f, 130f);

        drawUiTexture(
            PAUSE_UI_FOLDER + (selectedResume
                ? "botao_continuar_selecionado.png"
                : "botao_continuar_normal.png"),
            btnResume.x, btnResume.y
        );
        drawUiTexture(PAUSE_UI_FOLDER + "tecla_esc.png", btnResume.x + 117f, btnResume.y + 3.5f);

        drawUiTexture(
            PAUSE_UI_FOLDER + (hoverRestart
                ? "botao_reiniciar_selecionado.png"
                : "botao_reiniciar_normal.png"),
            btnRestart.x, btnRestart.y
        );
        drawUiTexture(PAUSE_UI_FOLDER + "tecla_r.png", btnRestart.x + 127f, btnRestart.y + 3.5f);

        drawUiTexture(
            PAUSE_UI_FOLDER + (hoverToMenu
                ? "botao_menu_principal_selecionado.png"
                : "botao_menu_principal_normal.png"),
            btnToMenu.x, btnToMenu.y
        );
        drawUiTexture(PAUSE_UI_FOLDER + "tecla_m.png", btnToMenu.x + 127f, btnToMenu.y + 3.5f);
        if (selectedResume) {
            drawUiTexture(PAUSE_UI_FOLDER + "seta_cursor_direita.png", btnResume.x - 13f, btnResume.y + 4.5f);
        } else if (selectedRestart) {
            drawUiTexture(PAUSE_UI_FOLDER + "seta_cursor_direita.png", btnRestart.x - 13f, btnRestart.y + 4.5f);
        } else if (selectedToMenu) {
            drawUiTexture(PAUSE_UI_FOLDER + "seta_cursor_direita.png", btnToMenu.x - 13f, btnToMenu.y + 4.5f);
        }
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

    private void drawHud() {
        GameStats stats = gameWorld.getStats();
        ParallaxBackground.Period period = gameWorld.getCycleState().getPeriod();
        int phase = period.ordinal() + 1;
        String phaseIcon;
        switch (period) {
            case AFTERNOON:
                phaseIcon = "icone_fase_tarde.png";
                break;
            case NIGHT:
                phaseIcon = "icone_fase_noite.png";
                break;
            default:
                phaseIcon = "icone_fase_manha.png";
                break;
        }

        batch.begin();
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_forca.png", 8f, 168f);
        drawUiTexture(IN_GAME_UI_FOLDER + "barra_forca_moldura.png", 7f, 154f);
        Texture forceFill = uiTexture(IN_GAME_UI_FOLDER + "barra_forca_preenchimento.png");
        int fillWidth = Math.round(forceFill.getWidth()
            * MathUtils.clamp(gameController.getForce() / 4095f, 0f, 1f));
        if (fillWidth > 0) {
            batch.draw(forceFill, 10f, 157f, 0, 0, fillWidth, forceFill.getHeight());
        }
        drawUiTexture(IN_GAME_UI_FOLDER + "barra_forca_marcadores.png", 10f, 157f);

        drawHudPanel("painel_fase.png", 6f, 137f, 47f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + phaseIcon, 8f, 137f, 14f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_fase.png", 24f, 140.5f, 19f, 7f);

        drawHudPanel("painel_apertos.png", 54.5f, 137f, 63f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + "icone_apertos.png", 57f, 137f, 14f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_apertos.png", 68f, 139.5f, 25f, 6f);

        drawHudPanel("painel_pontos.png", 118.75f, 137f, 84f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + "icone_pontos.png", 122f, 139f, 10f, 10f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_pontos.png", 135f, 139.5f, 28f, 7f);

        font.getData().setScale(0.65f);
        drawHudValue(6f, 47f, 137f, String.valueOf(phase));
        drawHudValue(54.5f, 63f, 137f, String.valueOf(stats.getSqueezes(phase - 1)));
        drawHudValue(118.75f, 84f, 137f, String.valueOf(stats.getTotalScore()));
        font.getData().setScale(1f);

        drawHearts();
        float controlsAlpha = 1f - controlsFadeElapsed / CONTROLS_FADE_DURATION;
        if (state != State.GAME_OVER && controlsAlpha > 0f) {
            batch.setColor(1f, 1f, 1f, controlsAlpha);
            drawControls();
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void drawHudPanel(String asset, float x, float y, float width, float height) {
        drawUiTexture(IN_GAME_UI_FOLDER + asset, x, y, width, height);
    }

    private void drawHudValue(float panelX, float panelWidth, float panelY, String value) {
        layout.setText(font, value);
        float x = panelX + panelWidth - 3f - layout.width;
        font.setColor(CREAM);
        font.draw(batch, value, x, panelY + 11f);
        font.setColor(Color.WHITE);
    }

    private void drawHearts() {
        int hp = gameWorld.getPlayer().getHp();
        Texture fullHeart = uiTexture(IN_GAME_UI_FOLDER + "coracao_cheio.png");
        Texture emptyHeart = uiTexture(IN_GAME_UI_FOLDER + "coracao_vazio.png");
        for (int i = 0; i < Player.MAX_LIVES; i++) {
            float x = GameWorld.WORLD_WIDTH - 19f - (Player.MAX_LIVES - 1 - i) * 17f;
            float y = GameWorld.WORLD_HEIGHT - 21f;
            int heartHp = hp - i * 2;
            if (heartHp >= 2) {
                batch.draw(fullHeart, x, y, 14f, 14f);
            } else if (heartHp == 1) {
                batch.draw(emptyHeart, x, y, 14f, 14f);
                batch.draw(fullHeart, x, y, 7f, 14f, 0, 0,
                    fullHeart.getWidth() / 2, fullHeart.getHeight(), false, false);
            } else {
                batch.draw(emptyHeart, x, y, 14f, 14f);
            }
        }
    }

    private void drawControls() {
        drawUiTexture(IN_GAME_UI_FOLDER + "painel_controles.png", 20f, 2f);
        Texture rightArrowKey = uiTexture(IN_GAME_UI_FOLDER + "tecla_direita.png");
        batch.draw(rightArrowKey, 46f, 5.5f, -15f, 15f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_cima.png", 47f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_baixo.png", 63f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_direita.png", 79f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_mover.png", 94f, 8.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_espaco.png", 138f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_tiro.png", 184f, 8.5f);
        drawUiTexture(PAUSE_UI_FOLDER + "tecla_esc.png", 221f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_pausar.png", 251f, 8.5f);
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
        for (Texture texture : uiTextures.values()) {
            texture.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
