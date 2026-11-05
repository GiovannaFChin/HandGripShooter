package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.HashMap;
import java.util.Map;

public class Main extends ApplicationAdapter {



    private static final String SERIAL_PORT = "COM6";
    private static final float FINAL_PHASE_SECONDS = 50f;

    private static final boolean LIGHT_SPIKE = true;
    private static final boolean DEBUG_LIGHTS = false;
    private static final String LAYER_GROUND = "10_chao_e_props";
    private static final String LAYER_VILLAGE = "09_vila_perto";

    private static final String MENU_UI_FOLDER = "menu_ui/";
    private static final String PAUSE_UI_FOLDER = "menu_ui/pause/";
    private static final String IN_GAME_UI_FOLDER = "menu_ui/in-game/";

    private static final String[] MENU_UI_ASSETS = {
        "fundo_inicial.png", "logo_completa.png",
        "opcao_novo_jogo_normal.png", "opcao_novo_jogo_selecionada.png",
        "opcao_sair_normal.png", "opcao_sair_selecionada.png"
    };
    private static final String[] PAUSE_UI_ASSETS = {
        "botao_continuar_normal.png", "botao_continuar_selecionado.png",
        "botao_menu_principal_normal.png", "botao_menu_principal_selecionado.png",
        "botao_reiniciar_normal.png", "botao_reiniciar_selecionado.png",
        "overlay_escurecer.png", "painel_pausa.png", "seta_cursor_direita.png",
        "tecla_esc.png", "tecla_m.png", "tecla_r.png", "titulo_pausado.png"
    };

    private static final String TITLE_GAME_OVER = "titulo_game_over.png";
    private static final String TITLE_FINISHED = "titulo_fim_de_jogo.png";
    private static final String[] END_TITLE_ASSETS = {TITLE_GAME_OVER, TITLE_FINISHED};

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
    private static final String[] PHASE_ICONS = {
        "icone_fase_manha.png", "icone_fase_tarde.png", "icone_fase_noite.png"
    };

    private static final float MENU_OPTION_X = 20f;
    private static final float MENU_OPTION_Y = 69f;
    private static final float MENU_OPTION_GAP = 4f;

    private static final float PANEL_X = 62f;
    private static final float PANEL_Y = 24.5f;
    private static final float BUTTON_X = (GameWorld.WORLD_WIDTH - 148f) / 2f;
    private static final float END_TITLE_Y = 135f;
    private static final float SUMMARY_TOP = 130f;
    private static final float CONTROLS_FADE_DURATION = 3f;

    private static final Color ORANGE = Color.valueOf("EF6A3E");
    private static final Color CREAM  = Color.valueOf("FFF3E0");
    private static final Color YELLOW = Color.valueOf("F7E3A8");

    private enum State { MENU, PLAYING, PAUSED, GAME_OVER, FINISHED }

    private State state = State.MENU;

    private final Rectangle[] startButtons = {
        new Rectangle(MENU_OPTION_X, MENU_OPTION_Y, 66f, 10f),                          // Novo jogo
        new Rectangle(MENU_OPTION_X, MENU_OPTION_Y - (10f + MENU_OPTION_GAP), 36f, 10f) // Sair
    };
    private final Rectangle[] pauseButtons = {
        new Rectangle(BUTTON_X, 94f, 148f, 22f),   // Continuar
        new Rectangle(BUTTON_X, 66f, 148f, 22f),   // Reiniciar
        new Rectangle(BUTTON_X, 38f, 148f, 22f)    // Menu principal
    };
    private final Rectangle[] endButtons = {
        new Rectangle(BUTTON_X, 54f, 148f, 22f),   // Reiniciar
        new Rectangle(BUTTON_X, 28f, 148f, 22f)    // Menu principal
    };
    private int hovered = -1;

    private boolean controlsFadeStarted;
    private float controlsFadeElapsed;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private FrameBuffer sceneFrameBuffer;
    private LightingManager lightingManager;
    private final Map<String, Texture> uiTextures = new HashMap<>();
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touch = new Vector3();

    private GameWorld gameWorld;
    private GameController gameController;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.position.set(GameWorld.WORLD_WIDTH / 2f, GameWorld.WORLD_HEIGHT / 2f, 0f);
        viewport = new FitViewport(GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        if (LIGHT_SPIKE) {
            createLighting();
        }
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        loadUiTextures(MENU_UI_FOLDER, MENU_UI_ASSETS);
        loadUiTextures(PAUSE_UI_FOLDER, PAUSE_UI_ASSETS);
        loadUiTextures(IN_GAME_UI_FOLDER, IN_GAME_UI_ASSETS);
        loadOptionalUiTextures(PAUSE_UI_FOLDER, END_TITLE_ASSETS);

        gameWorld = new GameWorld();
        gameController = new GameController(gameWorld, SERIAL_PORT);
        gameController.start();
    }

    private void createLighting() {
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

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        if (state == State.PLAYING) {
            updatePlaying(delta);
        } else {
            gameController.clearPendingShots();
            updateMenuInput();
        }
        camera.update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.setProjectionMatrix(camera.combined);

        if (state == State.MENU) {
            drawStartMenu();
            return;
        }

        renderScene(delta);
        drawHud();

        if (state == State.PAUSED) {
            drawPauseScreen();
        } else if (isEndState()) {
            drawEndScreen();
        }
    }

    @Override
    public void pause() {
        if (state == State.PLAYING) state = State.PAUSED;
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (gameController != null) gameController.dispose();
        if (gameWorld != null) gameWorld.dispose();
        if (lightingManager != null) lightingManager.dispose();
        if (sceneFrameBuffer != null) sceneFrameBuffer.dispose();
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        for (Texture texture : uiTextures.values()) {
            texture.dispose();
        }
        if (batch != null) batch.dispose();
    }

    private void startGame() {
        gameWorld.reset();
        gameController.clearPendingShots();
        controlsFadeStarted = false;
        controlsFadeElapsed = 0f;
        state = State.PLAYING;
    }

    private void updatePlaying(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            state = State.PAUSED;
            return;
        }

        Player player = gameWorld.getPlayer();
        float previousX = player.getX();
        float previousY = player.getY();

        gameController.update(delta);

        updateControlsHint(delta, player.getX() != previousX || player.getY() != previousY);
        checkEndOfGame();
    }

    private void updateControlsHint(float delta, boolean playerMoved) {
        if (!controlsFadeStarted && playerMoved) {
            controlsFadeStarted = true;
        } else if (controlsFadeStarted) {
            controlsFadeElapsed = Math.min(CONTROLS_FADE_DURATION, controlsFadeElapsed + delta);
        }
    }

    private void checkEndOfGame() {
        if (gameWorld.getPlayer().isDead()) {
            state = State.GAME_OVER;
            return;
        }
        GameStats stats = gameWorld.getStats();
        int lastPhase = GameStats.PHASES - 1;
        if (stats.getPhaseNumber() == GameStats.PHASES
            && stats.getSeconds(lastPhase) >= FINAL_PHASE_SECONDS) {
            state = State.FINISHED;
        }
    }

    private boolean isEndState() {
        return state == State.GAME_OVER || state == State.FINISHED;
    }

    private void updateMenuInput() {
        updateHover();
        boolean click = Gdx.input.justTouched();

        switch (state) {
            case MENU:
                if (activated(click, 0, Input.Keys.ENTER)) startGame();
                else if (activated(click, 1, Input.Keys.ESCAPE)) Gdx.app.exit();
                break;

            case PAUSED:
                if (activated(click, 0, Input.Keys.ESCAPE, Input.Keys.P)) state = State.PLAYING;
                else if (activated(click, 1, Input.Keys.R)) startGame();
                else if (activated(click, 2, Input.Keys.M)) state = State.MENU;
                break;

            case GAME_OVER:
            case FINISHED:
                if (activated(click, 0, Input.Keys.ENTER, Input.Keys.R)) startGame();
                else if (activated(click, 1, Input.Keys.ESCAPE, Input.Keys.M)) state = State.MENU;
                break;

            default:
                break;
        }
    }

    private void updateHover() {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);

        Rectangle[] buttons = currentButtons();
        hovered = -1;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(touch.x, touch.y)) {
                hovered = i;
            }
        }
    }

    private Rectangle[] currentButtons() {
        switch (state) {
            case PAUSED:
                return pauseButtons;
            case GAME_OVER:
            case FINISHED:
                return endButtons;
            default:
                return startButtons;
        }
    }

    private boolean activated(boolean click, int buttonIndex, int... keys) {
        if (click && hovered == buttonIndex) return true;
        for (int key : keys) {
            if (Gdx.input.isKeyJustPressed(key)) return true;
        }
        return false;
    }

    private void renderScene(float delta) {
        if (LIGHT_SPIKE) {
            renderSceneWithLighting(delta);
            renderLightDebug();
        } else {
            batch.begin();
            gameWorld.renderBackground(batch);
            gameWorld.renderActors(batch);
            gameWorld.renderBullets(batch);
            batch.end();
        }
    }

    private void renderSceneWithLighting(float delta) {
        lightingManager.updateFog(
            gameWorld.getElapsed(),
            gameWorld.getCycleState(),
            gameWorld.getLayerOffset(LAYER_GROUND)
        );

        sceneFrameBuffer.begin();
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        batch.begin();
        gameWorld.renderBackground(batch);
        lightingManager.renderFog();
        lightingManager.renderShadows(gameWorld, gameWorld.getCycleState());
        gameWorld.renderActors(batch);
        gameWorld.renderBullets(batch);
        batch.end();
        sceneFrameBuffer.end();

        lightingManager.update(
            delta,
            gameWorld.getCycleState(),
            gameWorld.getElapsed(),
            gameWorld.getLayerOffset(LAYER_GROUND),
            gameWorld.getLayerWidth(LAYER_GROUND),
            gameWorld.getLayerOffset(LAYER_VILLAGE),
            gameWorld.getLayerWidth(LAYER_VILLAGE),
            DEBUG_LIGHTS,
            gameWorld
        );
        lightingManager.render(sceneFrameBuffer.getColorBufferTexture());
        batch.setShader(null);
    }

    private void renderLightDebug() {
        boolean debugLines = LightingManager.DEBUG_SHADOW || LightingManager.DEBUG_FOG;
        boolean debugShapes = DEBUG_LIGHTS || LightingManager.DEBUG_MUZZLE || LightingManager.DEBUG_IMPACT;
        if (!debugLines && !debugShapes) {
            return;
        }

        viewport.apply();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(debugLines ? ShapeRenderer.ShapeType.Line : ShapeRenderer.ShapeType.Filled);
        if (debugShapes) {
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

    private void drawHud() {
        GameStats stats = gameWorld.getStats();
        int phase = stats.getPhaseNumber();

        batch.begin();
        drawForceBar();
        drawHudPanels(phase, stats);
        drawHearts();
        drawControlsHint();
        batch.end();
    }

    private void drawForceBar() {
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_forca.png", 8f, 168f);
        drawUiTexture(IN_GAME_UI_FOLDER + "barra_forca_moldura.png", 7f, 154f);

        Texture fill = uiTexture(IN_GAME_UI_FOLDER + "barra_forca_preenchimento.png");
        int fillWidth = Math.round(fill.getWidth()
            * MathUtils.clamp(gameController.getForce() / 4095f, 0f, 1f));
        if (fillWidth > 0) {
            batch.draw(fill, 10f, 157f, 0, 0, fillWidth, fill.getHeight());
        }
        drawUiTexture(IN_GAME_UI_FOLDER + "barra_forca_marcadores.png", 10f, 157f);
    }

    private void drawHudPanels(int phase, GameStats stats) {
        drawHudPanel("painel_fase.png", 6f, 137f, 47f, 14f);
        drawUiTexture(IN_GAME_UI_FOLDER + PHASE_ICONS[phase - 1], 8f, 137f, 14f, 14f);
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

    private void drawControlsHint() {
        float alpha = 1f - controlsFadeElapsed / CONTROLS_FADE_DURATION;
        if (isEndState() || alpha <= 0f) {
            return;
        }
        batch.setColor(1f, 1f, 1f, alpha);

        drawUiTexture(IN_GAME_UI_FOLDER + "painel_controles.png", 20f, 2f);
        Texture rightArrowKey = uiTexture(IN_GAME_UI_FOLDER + "tecla_direita.png");
        batch.draw(rightArrowKey, 46f, 5.5f, -15f, 15f);   // seta esquerda (direita espelhada)
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_cima.png", 47f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_baixo.png", 63f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_direita.png", 79f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_mover.png", 94f, 8.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "tecla_espaco.png", 138f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_tiro.png", 184f, 8.5f);
        drawUiTexture(PAUSE_UI_FOLDER + "tecla_esc.png", 221f, 5.5f);
        drawUiTexture(IN_GAME_UI_FOLDER + "rotulo_pausar.png", 251f, 8.5f);

        batch.setColor(Color.WHITE);
    }

    private void drawStartMenu() {
        batch.begin();
        drawUiTexture(MENU_UI_FOLDER + "fundo_inicial.png", 0f, 0f,
            GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        drawUiTexture(MENU_UI_FOLDER + "logo_completa.png", 7f, 104f);
        drawUiTexture(MENU_UI_FOLDER + (hovered == 0
                ? "opcao_novo_jogo_selecionada.png" : "opcao_novo_jogo_normal.png"),
            MENU_OPTION_X, MENU_OPTION_Y);
        drawUiTexture(MENU_UI_FOLDER + (hovered == 1
                ? "opcao_sair_selecionada.png" : "opcao_sair_normal.png"),
            MENU_OPTION_X, MENU_OPTION_Y - (10f + MENU_OPTION_GAP));
        batch.end();
    }

    private void drawPauseScreen() {
        int selected = Math.max(hovered, 0);

        batch.begin();
        drawMenuPanel();
        drawUiTexture(PAUSE_UI_FOLDER + "titulo_pausado.png", 96.5f, 130f);
        drawMenuButton("continuar", pauseButtons[0], selected == 0, "tecla_esc.png", 117f);
        drawMenuButton("reiniciar", pauseButtons[1], selected == 1, "tecla_r.png", 127f);
        drawMenuButton("menu_principal", pauseButtons[2], selected == 2, "tecla_m.png", 127f);
        batch.end();
    }

    private void drawEndScreen() {
        int selected = Math.max(hovered, 0);

        batch.begin();
        drawMenuPanel();
        drawEndTitle();
        drawSummary(SUMMARY_TOP);
        drawMenuButton("reiniciar", endButtons[0], selected == 0, "tecla_r.png", 127f);
        drawMenuButton("menu_principal", endButtons[1], selected == 1, "tecla_m.png", 127f);
        batch.end();
    }

    private void drawMenuPanel() {
        drawUiTexture(PAUSE_UI_FOLDER + "overlay_escurecer.png", 0f, 0f);
        drawUiTexture(PAUSE_UI_FOLDER + "painel_pausa.png", PANEL_X, PANEL_Y);
    }

    private void drawMenuButton(String name, Rectangle bounds, boolean selected, String keyAsset, float keyX) {
        drawUiTexture(PAUSE_UI_FOLDER + "botao_" + name
            + (selected ? "_selecionado.png" : "_normal.png"), bounds.x, bounds.y);
        drawUiTexture(PAUSE_UI_FOLDER + keyAsset, bounds.x + keyX, bounds.y + 3.5f);
        if (selected) {
            drawUiTexture(PAUSE_UI_FOLDER + "seta_cursor_direita.png", bounds.x - 13f, bounds.y + 4.5f);
        }
    }

    private void drawEndTitle() {
        boolean finished = state == State.FINISHED;
        String asset = PAUSE_UI_FOLDER + (finished ? TITLE_FINISHED : TITLE_GAME_OVER);

        Texture title = uiTextures.get(asset);
        if (title != null) {
            drawUiTexture(asset, (GameWorld.WORLD_WIDTH - title.getWidth()) / 2f, END_TITLE_Y);
            return;
        }

        font.getData().setScale(1.3f);
        drawCentered(finished ? "FIM DE JOGO" : "GAME OVER", END_TITLE_Y + 15f, finished ? YELLOW : ORANGE);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void drawSummary(float top) {
        GameStats stats = gameWorld.getStats();
        float[] col = {82f, 112f, 162f, 204f};
        float lineH = 11f;

        font.getData().setScale(0.75f);
        font.setColor(YELLOW);
        font.draw(batch, "Fase", col[0], top);
        font.draw(batch, "Apertos", col[1], top);
        font.draw(batch, "Pontos", col[2], top);
        font.draw(batch, "Aper./min", col[3], top);

        font.setColor(CREAM);
        for (int i = 0; i < stats.getPhaseNumber(); i++) {
            float y = top - lineH * (i + 1);
            font.draw(batch, String.valueOf(i + 1), col[0], y);
            font.draw(batch, String.valueOf(stats.getSqueezes(i)), col[1], y);
            font.draw(batch, String.valueOf(stats.getScore(i)), col[2], y);
            font.draw(batch, String.format("%.1f", stats.getSqueezesPerMinute(i)), col[3], y);
        }

        float yTotal = top - lineH * (stats.getPhaseNumber() + 1);
        font.setColor(ORANGE);
        font.draw(batch, "Total", col[0], yTotal);
        font.draw(batch, String.valueOf(stats.getTotalSqueezes()), col[1], yTotal);
        font.draw(batch, String.valueOf(stats.getTotalScore()), col[2], yTotal);
        font.draw(batch, String.format("%.1f", stats.getTotalSqueezesPerMinute()), col[3], yTotal);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }


    private void loadUiTextures(String folder, String[] assets) {
        for (String asset : assets) {
            loadUiTexture(folder + asset);
        }
    }

    private void loadOptionalUiTextures(String folder, String[] assets) {
        for (String asset : assets) {
            if (Gdx.files.internal(folder + asset).exists()) {
                loadUiTexture(folder + asset);
            }
        }
    }

    private void loadUiTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        uiTextures.put(path, texture);
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

    private void drawCentered(String text, float y, Color color) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, (GameWorld.WORLD_WIDTH - layout.width) / 2f, y);
    }
}
