package br.mackenzie;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.physics.box2d.World;
import com.github.bennyOe.gdxNormalLight.core.GameLight;
import com.github.bennyOe.gdxNormalLight.core.LightEngine;
import java.util.Locale;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

final class LightingManager {
    static final boolean DEBUG_MUZZLE = false;
    static final boolean DEBUG_IMPACT = false;
    static final boolean DEBUG_SHADOW = false;
    static final boolean DEBUG_FOG = false;

    static final boolean FOG_ENABLED = true;

    private static final int FOG_TILE_WIDTH = 128;
    private static final int FOG_TILE_HEIGHT = 16;
    
    private static final float FOG_BACK_Y = 58f;
    private static final float FOG_BACK_HEIGHT = 21f;
    private static final float FOG_FRONT_Y = 48f;
    private static final float FOG_FRONT_HEIGHT = 12f;
    private static final float FOG_BACK_SPEED = 4f;
    private static final float FOG_FRONT_SPEED = 7f;
    private static final float FOG_BACK_ALPHA_FACTOR = 1f;
    private static final float FOG_FRONT_ALPHA_FACTOR = 0.55f;
    private static final Color FOG_MORNING_COLOR = new Color(0.95f, 0.92f, 0.85f, 1f);
    private static final Color FOG_AFTERNOON_COLOR = new Color(0.85f, 0.60f, 0.75f, 1f);
    private static final Color FOG_NIGHT_COLOR = new Color(0.55f, 0.65f, 0.85f, 1f);
    private static final float FOG_MORNING_ALPHA = 0.18f;
    private static final float FOG_AFTERNOON_ALPHA = 0.38f;
    private static final float FOG_NIGHT_ALPHA = 0.50f;

    private static final Color SHADOW_COLOR = new Color(0f, 0f, 0.08f, 1f);
    private static final float SHADOW_WIDTH_RATIO = 0.85f;
    private static final float SHADOW_MEDIUM_WIDTH_RATIO = 0.60f;
    private static final float SHADOW_INNER_WIDTH_RATIO = 0.35f;
    private static final float SHADOW_OUTER_HEIGHT = 4f;
    private static final float SHADOW_MEDIUM_HEIGHT = 3f;
    private static final float SHADOW_INNER_HEIGHT = 2f;
    private static final float SHADOW_OUTER_ALPHA = 0.094f;
    private static final float SHADOW_MEDIUM_ALPHA = 0.088f;
    private static final float SHADOW_INNER_ALPHA = 0.15f;
    private static final float SHADOW_SCALE = 1.3f;
    private static final float SHADOW_MAX_WIDTH_RATIO = 1.6f;
    private static final float SHADOW_INNER_OFFSET_RATIO = 0.20f;
    private static final float SHADOW_MEDIUM_OFFSET_RATIO = 0.55f;
    private static final float SHADOW_NIGHT_OFFSET_MULTIPLIER = 0.70f;
    private static final float SHADOW_MORNING_MULTIPLIER = 1f;
    private static final float SHADOW_AFTERNOON_MULTIPLIER = 1.2f;
    private static final float SHADOW_NIGHT_MULTIPLIER = 1.5f;
    private static final float SHADOW_MAX_HORIZONTAL_OFFSET = 10f;
    private static final int SHADOW_TEXTURE_WIDTH = 16;
    private static final int SHADOW_TEXTURE_HEIGHT = 6;

    private static final Color MORNING_AMBIENT = new Color(0.94f, 0.91f, 0.80f, 1f);
    private static final Color AFTERNOON_AMBIENT = new Color(0.58f, 0.40f, 0.55f, 1f);
    private static final Color NIGHT_AMBIENT = new Color(0.30f, 0.35f, 0.55f, 1f);
    private static final Color SUN_COLOR = new Color(1f, 0.78f, 0.48f, 1f);
    private static final Color AFTERNOON_MOON_COLOR = new Color(1f, 0.58f, 0.38f, 1f);
    private static final Color NIGHT_MOON_COLOR = new Color(0.60f, 0.75f, 1f, 1f);
    private static final float MORNING_SUN_INTENSITY = 0.70f;
    private static final float AFTERNOON_MOON_INTENSITY = 0.85f;
    private static final float NIGHT_MOON_INTENSITY = 0.65f;
    private static final float SUN_LIGHT_DISTANCE = 115f;
    private static final float NIGHT_MOON_LIGHT_DISTANCE = 180f;
    private static final Color BARREL_COLOR = new Color(1f, 0.36f, 0.08f, 1f);
    private static final float BARREL_X_IN_LAYER = 123f;
    private static final float BARREL_Y_FROM_TOP = 112f;
    private static final float BARREL_MORNING_INTENSITY = 0.03f;
    private static final float BARREL_AFTERNOON_INTENSITY = 0.38f;
    private static final float BARREL_NIGHT_INTENSITY = 0.82f;
    private static final float BARREL_LIGHT_DISTANCE = 58f;
    private static final float BARREL_FLICKER_AMPLITUDE = 0.045f;
    private static final float BARREL_FLICKER_SECOND_AMPLITUDE = 0.025f;
    private static final float BARREL_FLICKER_SPEED = 17f;
    private static final float BARREL_FLICKER_SECOND_SPEED = 23.7f;
    private static final float LIGHT_ON_THRESHOLD = 0.001f;
    private static final String BARREL_LAYER = "10_chao_e_props";
    private static final int MAX_ACTIVE_LIGHTS = 8;
    private static final int RESERVED_CORE_LIGHTS = 3;
    private static final int MAX_ACTIVE_PROP_LIGHTS = MAX_ACTIVE_LIGHTS - RESERVED_CORE_LIGHTS;
    private static final int VILLAGE_LAYER = 0;
    private static final int GROUND_LAYER = 1;
    private static final Color SIGN_LIGHT_COLOR = new Color(1f, 0.28f, 0.18f, 1f);
    private static final Color GREEN_LAMP_COLOR = new Color(0.35f, 1f, 0.52f, 1f);
    private static final Color YELLOW_LAMP_COLOR = new Color(1f, 0.82f, 0.42f, 1f);
    private static final float SIGN_MORNING_INTENSITY = 0.008f;
    private static final float SIGN_AFTERNOON_INTENSITY = 0.12f;
    private static final float SIGN_NIGHT_INTENSITY = 0.58f;
    private static final float SIGN_LIGHT_DISTANCE = 30f;
    private static final float SIGN_FLICKER_AMPLITUDE = 0.05f;
    private static final float SIGN_FLICKER_SPEED = 10.1f;
    private static final float SIGN_FLICKER_SECOND_AMPLITUDE = 0.02f;
    private static final float SIGN_FLICKER_SECOND_SPEED = 14.7f;
    private static final float GREEN_LAMP_MORNING_INTENSITY = 0.006f;
    private static final float GREEN_LAMP_AFTERNOON_INTENSITY = 0.20f;
    private static final float GREEN_LAMP_NIGHT_INTENSITY = 0.46f;
    private static final float GREEN_LAMP_LIGHT_DISTANCE = 22f;
    private static final float GREEN_LAMP_FLICKER_AMPLITUDE = 0.055f;
    private static final float GREEN_LAMP_FLICKER_SPEED = 1.8f;
    private static final float GREEN_LAMP_FLICKER_SECOND_AMPLITUDE = 0.02f;
    private static final float GREEN_LAMP_FLICKER_SECOND_SPEED = 3.3f;
    private static final float YELLOW_LAMP_MORNING_INTENSITY = 0.006f;
    private static final float YELLOW_LAMP_AFTERNOON_INTENSITY = 0.10f;
    private static final float YELLOW_LAMP_NIGHT_INTENSITY = 0.46f;
    private static final float YELLOW_LAMP_LIGHT_DISTANCE = 22f;
    private static final float YELLOW_LAMP_FLICKER_AMPLITUDE = 0.018f;
    private static final float YELLOW_LAMP_FLICKER_SPEED = 12f;
    private static final float YELLOW_LAMP_FLICKER_SECOND_AMPLITUDE = 0.01f;
    private static final float YELLOW_LAMP_FLICKER_SECOND_SPEED = 17f;
    private static final int SHOT_LIGHT_POOL_SIZE = 6;
    private static final int SHOT_FLASH_SLOT_COUNT = 2;
    private static final int MAX_ACTIVE_SHOT_LIGHTS = 4;
    private static final float SHOT_FLASH_DURATION = 0.15f;
    private static final float SHOT_FLASH_HOLD_DURATION = 0.045f;
    private static final float SHOT_FLASH_DISTANCE = 38f;
    private static final float SHOT_FLASH_TIP_OFFSET = 4f;
    private static final float SHOT_TRAIL_DISTANCE = 10f;
    private static final Color SHOT_FLASH_COLOR = new Color(1f, 0.62f, 0.24f, 1f);
    private static final Color SHOT_TRAIL_COLOR = new Color(1f, 0.78f, 0.42f, 1f);
    private static final float SHOT_FLASH_PEAK_INTENSITY = 2.5f;
    private static final float SHOT_FLASH_MORNING_MULTIPLIER = 1f;
    private static final float SHOT_FLASH_AFTERNOON_MULTIPLIER = 1.45f;
    private static final float SHOT_FLASH_NIGHT_MULTIPLIER = 2f;
    private static final float DEBUG_MUZZLE_INTENSITY = 2.5f;
    private static final Color SHOT_IMPACT_COLOR = new Color(1f, 0.82f, 0.48f, 1f);
    private static final float SHOT_IMPACT_DURATION = 0.12f;
    private static final float SHOT_IMPACT_HOLD_DURATION = 0.05f;
    private static final float SHOT_IMPACT_PEAK_INTENSITY = 2.5f;
    private static final float SHOT_IMPACT_MORNING_MULTIPLIER = 0.6f;
    private static final float SHOT_IMPACT_AFTERNOON_MULTIPLIER = 0.8f;
    private static final float SHOT_IMPACT_NIGHT_MULTIPLIER = 1f;
    private static final float SHOT_TRAIL_MORNING_INTENSITY = 0.12f;
    private static final float SHOT_TRAIL_AFTERNOON_INTENSITY = 0.18f;
    private static final float SHOT_TRAIL_NIGHT_INTENSITY = 0.25f;

    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final Viewport viewport;
    private final Vector2 lightUpdateCenter = new Vector2();
    private final Color ambientColor = new Color();
    private final Color sunColor = new Color();
    private final Color moonColor = new Color();
    private final Color shadowBatchColor = new Color();
    private final Color barrelColor = new Color(BARREL_COLOR);
    private final Vector2 barrelPosition = new Vector2();
    private final Vector2 muzzleDebugPosition = new Vector2();
    private final Vector2 sunDebugPosition = new Vector2(90f, GameWorld.WORLD_HEIGHT - 65f);
    private final Vector2 moonDebugPosition = new Vector2(262f, GameWorld.WORLD_HEIGHT - 35f);
    private final FogLayer fogLayer = new FogLayer();
    private Texture sceneTexture;
    private Texture shadowTexture;
    private final Function1<LightEngine, Unit> sceneDrawer = new Function1<LightEngine, Unit>() {
        @Override
        public Unit invoke(LightEngine engine) {
            engine.draw(sceneTexture, 0f, GameWorld.WORLD_HEIGHT,
                GameWorld.WORLD_WIDTH, -GameWorld.WORLD_HEIGHT, false);
            return Unit.INSTANCE;
        }
    };

    private World world;
    private RayHandler rayHandler;
    private LightEngine lightEngine;
    private GameLight.Point sunLight;
    private GameLight.Point moonLight;
    private GameLight.Point barrelLight;
    private PropLight[] propLights;
    private ShotLight[] shotLights;
    private float barrelIntensity;
    private int lastAfternoonDebugSecond = -1;

    void init() {
        world = new World(new Vector2(0f, 0f), true);
        rayHandler = new RayHandler(world);
        rayHandler.setShadows(false);
        shadowTexture = createShadowTexture();
        fogLayer.init();
        lightEngine = new LightEngine(
            rayHandler,
            camera,
            batch,
            viewport,
            true,
            32,
            (short) 0x0001,
            (short) -1,
            -1f
        );

        sunLight = createAstroLight(90f, 65f, SUN_COLOR, SUN_LIGHT_DISTANCE);
        moonLight = createAstroLight(262f, 35f, NIGHT_MOON_COLOR, NIGHT_MOON_LIGHT_DISTANCE);
        barrelLight = lightEngine.addPointLight(
            new Vector2(BARREL_X_IN_LAYER, GameWorld.WORLD_HEIGHT - BARREL_Y_FROM_TOP),
            new Color(BARREL_COLOR),
            1f,
            BARREL_LIGHT_DISTANCE,
            0.5f,
            1f,
            32,
            (short) 0x0001,
            (short) -1,
            true
        );
        sunLight.setOn(false);
        moonLight.setOn(false);
        barrelLight.setOn(false);
        createPropLights();
        createShotLights();
    }

    private Texture createShadowTexture() {
        Pixmap pixmap = new Pixmap(SHADOW_TEXTURE_WIDTH, SHADOW_TEXTURE_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        float centerX = (SHADOW_TEXTURE_WIDTH - 1f) * 0.5f;
        float centerY = (SHADOW_TEXTURE_HEIGHT - 1f) * 0.5f;
        float radiusX = SHADOW_TEXTURE_WIDTH * 0.5f;
        float radiusY = SHADOW_TEXTURE_HEIGHT * 0.5f;
        for (int y = 0; y < SHADOW_TEXTURE_HEIGHT; y++) {
            for (int x = 0; x < SHADOW_TEXTURE_WIDTH; x++) {
                float dx = (x - centerX) / radiusX;
                float dy = (y - centerY) / radiusY;
                if (dx * dx + dy * dy <= 1f) {
                    pixmap.drawPixel(x, y);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    LightingManager(OrthographicCamera camera, SpriteBatch batch, Viewport viewport) {
        this.camera = camera;
        this.batch = batch;
        this.viewport = viewport;
    }

    void setAmbientLight(Color ambient) {
        ambientColor.set(ambient);
        lightEngine.updateShaderAmbientColor(ambientColor);
        lightEngine.setBox2dAmbientLight(ambientColor);
    }

    void updateFog(
        float elapsed,
        ParallaxBackground.CycleState cycleState,
        float groundLayerOffset
    ) {
        if (FOG_ENABLED) {
            fogLayer.update(elapsed, cycleState, groundLayerOffset);
        }
    }

    void renderFog() {
        if (FOG_ENABLED) {
            fogLayer.render(batch);
        }
    }

    void renderFogDebug(ShapeRenderer shapeRenderer) {
        if (FOG_ENABLED && DEBUG_FOG) {
            fogLayer.renderDebug(shapeRenderer);
        }
    }

    void update(
        float delta,
        ParallaxBackground.CycleState cycleState,
        float elapsed,
        float barrelLayerOffset,
        float barrelLayerWidth,
        float villageLayerOffset,
        float villageLayerWidth,
        boolean debugLights,
        GameWorld gameWorld
    ) {
        ParallaxBackground.Period period = cycleState.getPeriod();
        ParallaxBackground.Period nextPeriod = cycleState.getNextPeriod();
        float fadeProgress = cycleState.getFadeProgress();

        ambientColor.set(getAmbient(period)).lerp(getAmbient(nextPeriod), fadeProgress);
        sunColor.set(SUN_COLOR);
        moonColor.set(getMoonColor(period)).lerp(getMoonColor(nextPeriod), fadeProgress);

        float sunIntensity = MathUtils.lerp(
            getSunIntensity(period),
            getSunIntensity(nextPeriod),
            fadeProgress
        );
        float moonIntensity = MathUtils.lerp(
            getMoonIntensity(period),
            getMoonIntensity(nextPeriod),
            fadeProgress
        );
        sunColor.a = sunIntensity;
        moonColor.a = moonIntensity;

        setAmbientLight(ambientColor);
        updateAstroLight(sunLight, sunColor, sunIntensity);
        updateAstroLight(moonLight, moonColor, moonIntensity);
        updateBarrelLight(cycleState, elapsed, barrelLayerOffset, barrelLayerWidth);
        updatePropLights(
            cycleState,
            elapsed,
            barrelLayerOffset,
            barrelLayerWidth,
            villageLayerOffset,
            villageLayerWidth
        );
        updateShotLights(cycleState, gameWorld);
        if (debugLights && period == ParallaxBackground.Period.AFTERNOON) {
            logPropLights(elapsed);
        } else if (period != ParallaxBackground.Period.AFTERNOON) {
            lastAfternoonDebugSecond = -1;
        }
        lightEngine.update();
    }

    private void createPropLights() {
        propLights = new PropLight[] {
            new PropLight(
                "Letreiro", GROUND_LAYER, 399.5f, 98.5f, SIGN_LIGHT_COLOR,
                SIGN_MORNING_INTENSITY, SIGN_AFTERNOON_INTENSITY, SIGN_NIGHT_INTENSITY,
                SIGN_LIGHT_DISTANCE, SIGN_FLICKER_AMPLITUDE, SIGN_FLICKER_SPEED,
                SIGN_FLICKER_SECOND_AMPLITUDE, SIGN_FLICKER_SECOND_SPEED, 0.4f
            ),
            new PropLight(
                "Lampiao verde vila A", VILLAGE_LAYER, 114.3f, 105.7f, GREEN_LAMP_COLOR,
                GREEN_LAMP_MORNING_INTENSITY, GREEN_LAMP_AFTERNOON_INTENSITY,
                GREEN_LAMP_NIGHT_INTENSITY, GREEN_LAMP_LIGHT_DISTANCE,
                GREEN_LAMP_FLICKER_AMPLITUDE, GREEN_LAMP_FLICKER_SPEED,
                GREEN_LAMP_FLICKER_SECOND_AMPLITUDE, GREEN_LAMP_FLICKER_SECOND_SPEED, 0.3f
            ),
            new PropLight(
                "Lampiao verde vila B", VILLAGE_LAYER, 220.3f, 105.8f, GREEN_LAMP_COLOR,
                GREEN_LAMP_MORNING_INTENSITY, GREEN_LAMP_AFTERNOON_INTENSITY,
                GREEN_LAMP_NIGHT_INTENSITY, GREEN_LAMP_LIGHT_DISTANCE,
                GREEN_LAMP_FLICKER_AMPLITUDE, GREEN_LAMP_FLICKER_SPEED,
                GREEN_LAMP_FLICKER_SECOND_AMPLITUDE, GREEN_LAMP_FLICKER_SECOND_SPEED, 1.4f
            ),
            new PropLight(
                "Lampiao verde vila C", VILLAGE_LAYER, 297.4f, 117.5f, GREEN_LAMP_COLOR,
                GREEN_LAMP_MORNING_INTENSITY, GREEN_LAMP_AFTERNOON_INTENSITY,
                GREEN_LAMP_NIGHT_INTENSITY, GREEN_LAMP_LIGHT_DISTANCE,
                GREEN_LAMP_FLICKER_AMPLITUDE, GREEN_LAMP_FLICKER_SPEED,
                GREEN_LAMP_FLICKER_SECOND_AMPLITUDE, GREEN_LAMP_FLICKER_SECOND_SPEED, 2.2f
            ),
            new PropLight(
                "Lampiao amarelo vila A", VILLAGE_LAYER, 459.2f, 95.5f, YELLOW_LAMP_COLOR,
                YELLOW_LAMP_MORNING_INTENSITY, YELLOW_LAMP_AFTERNOON_INTENSITY,
                YELLOW_LAMP_NIGHT_INTENSITY, YELLOW_LAMP_LIGHT_DISTANCE,
                YELLOW_LAMP_FLICKER_AMPLITUDE, YELLOW_LAMP_FLICKER_SPEED,
                YELLOW_LAMP_FLICKER_SECOND_AMPLITUDE, YELLOW_LAMP_FLICKER_SECOND_SPEED, 0.7f
            ),
            new PropLight(
                "Lampiao amarelo vila B", VILLAGE_LAYER, 538.7f, 91.1f, YELLOW_LAMP_COLOR,
                YELLOW_LAMP_MORNING_INTENSITY, YELLOW_LAMP_AFTERNOON_INTENSITY,
                YELLOW_LAMP_NIGHT_INTENSITY, YELLOW_LAMP_LIGHT_DISTANCE,
                YELLOW_LAMP_FLICKER_AMPLITUDE, YELLOW_LAMP_FLICKER_SPEED,
                YELLOW_LAMP_FLICKER_SECOND_AMPLITUDE, YELLOW_LAMP_FLICKER_SECOND_SPEED, 1.9f
            )
        };

        for (PropLight propLight : propLights) {
            propLight.light = lightEngine.addPointLight(
                propLight.position,
                new Color(propLight.baseColor),
                1f,
                propLight.distance,
                0.5f,
                1f,
                32,
                (short) 0x0001,
                (short) -1,
                true
            );
            propLight.light.setOn(false);
        }
    }

    private void createShotLights() {
        shotLights = new ShotLight[SHOT_LIGHT_POOL_SIZE];
        for (int i = 0; i < shotLights.length; i++) {
            boolean flashSlot = i < SHOT_FLASH_SLOT_COUNT;
            ShotLight shotLight = new ShotLight(flashSlot);
            shotLight.light = lightEngine.addPointLight(
                shotLight.position,
                new Color(SHOT_FLASH_COLOR),
                1f,
                flashSlot ? SHOT_FLASH_DISTANCE : SHOT_TRAIL_DISTANCE,
                0.5f,
                1f,
                32,
                (short) 0x0001,
                (short) -1,
                true
            );
            shotLight.light.setOn(false);
            shotLights[i] = shotLight;
        }
    }

    private void updateShotLights(
        ParallaxBackground.CycleState cycleState,
        GameWorld gameWorld
    ) {
        if (DEBUG_MUZZLE) {
            updateDebugMuzzleLight(gameWorld);
            return;
        }

        for (ShotLight shotLight : shotLights) {
            shotLight.selected = false;
            if (shotLight.flash != null && shotLight.flash.getElapsed() >= getEffectLightDuration(shotLight.flash)) {
                shotLight.flash = null;
            }
            if (shotLight.bullet != null && !gameWorld.containsBullet(shotLight.bullet)) {
                shotLight.bullet = null;
            }
        }

        for (int i = gameWorld.getBulletEffectCount() - 1; i >= 0; i--) {
            Bullet.Effect effect = gameWorld.getBulletEffect(i);
            if ((effect.getType() != Bullet.EffectType.MUZZLE_FLASH
                    && effect.getType() != Bullet.EffectType.IMPACT)
                || effect.getElapsed() >= getEffectLightDuration(effect)
                || hasFlash(effect)) {
                continue;
            }
            if (effect.getType() == Bullet.EffectType.IMPACT && effect.isImpactLightClaimed()) {
                continue;
            }
            ShotLight slot = findFreeShotLight(true);
            if (slot == null) {
                slot = findOldestShotLight(true);
                if (slot != null && slot.flash.getElapsed() <= effect.getElapsed()) {
                    slot = null;
                }
            }
            if (slot != null) {
                slot.flash = effect;
                slot.bullet = null;
                if (effect.getType() == Bullet.EffectType.IMPACT) {
                    effect.claimImpactLight();
                }
            }
        }

        for (int i = gameWorld.getBulletCount() - 1; i >= 0; i--) {
            Bullet bullet = gameWorld.getBullet(i);
            if (hasBullet(bullet)) {
                continue;
            }
            ShotLight slot = findFreeShotLight(false);
            if (slot == null) {
                slot = findOldestShotLight(false);
                if (slot != null && slot.bullet.getElapsed() <= bullet.getElapsed()) {
                    slot = null;
                }
            }
            if (slot != null) {
                slot.bullet = bullet;
            }
        }

        for (int selectedCount = 0; selectedCount < MAX_ACTIVE_SHOT_LIGHTS; selectedCount++) {
            ShotLight newestFlash = findNewestFlash();
            if (newestFlash == null) {
                break;
            }
            newestFlash.selected = true;
        }
        for (int selectedCount = 0; selectedCount < MAX_ACTIVE_SHOT_LIGHTS; selectedCount++) {
            int activeCount = countSelectedShotLights();
            if (activeCount >= MAX_ACTIVE_SHOT_LIGHTS) {
                break;
            }
            ShotLight newestBullet = findNewestBullet();
            if (newestBullet == null) {
                break;
            }
            newestBullet.selected = true;
        }

        float flashIntensity = getMuzzleFlashIntensity(cycleState);
        float trailIntensity = getShotIntensity(cycleState, false);
        for (ShotLight shotLight : shotLights) {
            if (!shotLight.selected) {
                shotLight.light.setOn(false);
                continue;
            }

            float intensity;
            if (shotLight.flash != null) {
                shotLight.position.set(shotLight.flash.getX(), shotLight.flash.getY());
                float elapsed = shotLight.flash.getElapsed();
                if (shotLight.flash.getType() == Bullet.EffectType.IMPACT) {
                    float fadeProgress = MathUtils.clamp(
                        (elapsed - SHOT_IMPACT_HOLD_DURATION)
                            / (SHOT_IMPACT_DURATION - SHOT_IMPACT_HOLD_DURATION),
                        0f,
                        1f
                    );
                    intensity = getImpactIntensity(cycleState) * (1f - fadeProgress);
                    shotLight.color.set(SHOT_IMPACT_COLOR);
                } else {
                    float fadeProgress = MathUtils.clamp(
                        (elapsed - SHOT_FLASH_HOLD_DURATION)
                            / (SHOT_FLASH_DURATION - SHOT_FLASH_HOLD_DURATION),
                        0f,
                        1f
                    );
                    intensity = flashIntensity * (1f - fadeProgress);
                    shotLight.color.set(SHOT_FLASH_COLOR);
                }
            } else {
                shotLight.position.set(shotLight.bullet.getCenterX(), shotLight.bullet.getCenterY());
                intensity = trailIntensity;
                shotLight.color.set(SHOT_TRAIL_COLOR);
            }
            shotLight.color.a = intensity;
            shotLight.light.setPosition(shotLight.position);
            shotLight.light.setColor(shotLight.color);
            shotLight.light.setOn(intensity > LIGHT_ON_THRESHOLD);
            shotLight.light.setShaderIntensity(intensity);
        }
    }

    private void updateDebugMuzzleLight(GameWorld gameWorld) {
        ShotLight debugLight = shotLights[0];
        muzzleDebugPosition.set(
            gameWorld.getMuzzleX() + gameWorld.getPlayerFacing() * SHOT_FLASH_TIP_OFFSET,
            gameWorld.getMuzzleY()
        );
        debugLight.position.set(muzzleDebugPosition);
        debugLight.color.set(SHOT_FLASH_COLOR);
        debugLight.light.setPosition(debugLight.position);
        debugLight.light.setColor(debugLight.color);
        debugLight.light.setShaderIntensity(DEBUG_MUZZLE_INTENSITY);
        debugLight.light.setOn(true);
        for (int i = 1; i < shotLights.length; i++) {
            shotLights[i].light.setOn(false);
        }
    }

    private boolean hasFlash(Bullet.Effect effect) {
        for (ShotLight shotLight : shotLights) {
            if (shotLight.flash == effect) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBullet(Bullet bullet) {
        for (ShotLight shotLight : shotLights) {
            if (shotLight.bullet == bullet) {
                return true;
            }
        }
        return false;
    }

    private ShotLight findFreeShotLight(boolean flashSlot) {
        for (ShotLight shotLight : shotLights) {
            if (shotLight.flashSlot == flashSlot
                && shotLight.flash == null && shotLight.bullet == null) {
                return shotLight;
            }
        }
        return null;
    }

    private ShotLight findOldestShotLight(boolean flashSlot) {
        ShotLight oldest = null;
        float oldestElapsed = -1f;
        for (ShotLight shotLight : shotLights) {
            if (shotLight.flashSlot != flashSlot) {
                continue;
            }
            if (flashSlot && shotLight.flash != null
                && shotLight.flash.getElapsed() > oldestElapsed) {
                oldest = shotLight;
                oldestElapsed = shotLight.flash.getElapsed();
            } else if (!flashSlot && shotLight.bullet != null
                && shotLight.bullet.getElapsed() > oldestElapsed) {
                oldest = shotLight;
                oldestElapsed = shotLight.bullet.getElapsed();
            }
        }
        return oldest;
    }

    private ShotLight findNewestFlash() {
        ShotLight newest = null;
        float youngestElapsed = Float.MAX_VALUE;
        for (ShotLight shotLight : shotLights) {
            if (!shotLight.selected && shotLight.flash != null
                && shotLight.flash.getElapsed() < youngestElapsed) {
                newest = shotLight;
                youngestElapsed = shotLight.flash.getElapsed();
            }
        }
        return newest;
    }

    private ShotLight findNewestBullet() {
        ShotLight newest = null;
        float youngestElapsed = Float.MAX_VALUE;
        for (ShotLight shotLight : shotLights) {
            if (!shotLight.selected && shotLight.bullet != null
                && shotLight.bullet.getElapsed() < youngestElapsed) {
                newest = shotLight;
                youngestElapsed = shotLight.bullet.getElapsed();
            }
        }
        return newest;
    }

    private int countSelectedShotLights() {
        int count = 0;
        for (ShotLight shotLight : shotLights) {
            if (shotLight.selected) {
                count++;
            }
        }
        return count;
    }

    private float getShotIntensity(ParallaxBackground.CycleState cycleState, boolean flash) {
        float morning = SHOT_TRAIL_MORNING_INTENSITY;
        float afternoon = SHOT_TRAIL_AFTERNOON_INTENSITY;
        float night = SHOT_TRAIL_NIGHT_INTENSITY;
        return MathUtils.lerp(
            getIntensityForPeriod(cycleState.getPeriod(), morning, afternoon, night),
            getIntensityForPeriod(cycleState.getNextPeriod(), morning, afternoon, night),
            cycleState.getFadeProgress()
        );
    }

    private float getMuzzleFlashIntensity(ParallaxBackground.CycleState cycleState) {
        float morning = SHOT_FLASH_PEAK_INTENSITY * SHOT_FLASH_MORNING_MULTIPLIER;
        float afternoon = SHOT_FLASH_PEAK_INTENSITY * SHOT_FLASH_AFTERNOON_MULTIPLIER;
        float night = SHOT_FLASH_PEAK_INTENSITY * SHOT_FLASH_NIGHT_MULTIPLIER;
        return MathUtils.lerp(
            getIntensityForPeriod(cycleState.getPeriod(), morning, afternoon, night),
            getIntensityForPeriod(cycleState.getNextPeriod(), morning, afternoon, night),
            cycleState.getFadeProgress()
        );
    }

    private float getImpactIntensity(ParallaxBackground.CycleState cycleState) {
        float morning = SHOT_IMPACT_PEAK_INTENSITY * SHOT_IMPACT_MORNING_MULTIPLIER;
        float afternoon = SHOT_IMPACT_PEAK_INTENSITY * SHOT_IMPACT_AFTERNOON_MULTIPLIER;
        float night = SHOT_IMPACT_PEAK_INTENSITY * SHOT_IMPACT_NIGHT_MULTIPLIER;
        return MathUtils.lerp(
            getIntensityForPeriod(cycleState.getPeriod(), morning, afternoon, night),
            getIntensityForPeriod(cycleState.getNextPeriod(), morning, afternoon, night),
            cycleState.getFadeProgress()
        );
    }

    private float getEffectLightDuration(Bullet.Effect effect) {
        return effect.getType() == Bullet.EffectType.IMPACT
            ? SHOT_IMPACT_DURATION
            : SHOT_FLASH_DURATION;
    }

    private float getIntensityForPeriod(
        ParallaxBackground.Period period,
        float morning,
        float afternoon,
        float night
    ) {
        switch (period) {
            case MORNING:
                return morning;
            case AFTERNOON:
                return afternoon;
            case NIGHT:
                return night;
            default:
                throw new IllegalArgumentException("Unknown day period: " + period);
        }
    }

    private void updatePropLights(
        ParallaxBackground.CycleState cycleState,
        float elapsed,
        float groundLayerOffset,
        float groundLayerWidth,
        float villageLayerOffset,
        float villageLayerWidth
    ) {
        float halfVisibleWidth = viewport.getWorldWidth() * camera.zoom * 0.5f;
        float halfVisibleHeight = viewport.getWorldHeight() * camera.zoom * 0.5f;

        for (PropLight propLight : propLights) {
            float layerOffset = propLight.layer == VILLAGE_LAYER
                ? villageLayerOffset
                : groundLayerOffset;
            float layerWidth = propLight.layer == VILLAGE_LAYER
                ? villageLayerWidth
                : groundLayerWidth;
            propLight.selected = false;
            propLight.visible = layerWidth > 0f;
            if (!propLight.visible) {
                propLight.intensity = 0f;
                continue;
            }

            float x = propLight.baseX - layerOffset;
            x += MathUtils.round((camera.position.x - x) / layerWidth) * layerWidth;
            float y = GameWorld.WORLD_HEIGHT - propLight.yFromTop;
            propLight.position.set(x, y);
            propLight.visible = x >= camera.position.x - halfVisibleWidth
                && x <= camera.position.x + halfVisibleWidth
                && y >= camera.position.y - halfVisibleHeight
                && y <= camera.position.y + halfVisibleHeight;
            float dx = x - camera.position.x;
            float dy = y - camera.position.y;
            propLight.distanceSquared = dx * dx + dy * dy;

            float baseIntensity = MathUtils.lerp(
                propLight.getIntensity(cycleState.getPeriod()),
                propLight.getIntensity(cycleState.getNextPeriod()),
                cycleState.getFadeProgress()
            );
            float flicker = 1f
                + propLight.flickerAmplitude * MathUtils.sin(elapsed * propLight.flickerSpeed
                    + propLight.flickerPhase)
                + propLight.secondFlickerAmplitude
                    * MathUtils.sin(elapsed * propLight.secondFlickerSpeed
                        + propLight.flickerPhase * 1.7f);
            propLight.intensity = MathUtils.clamp(baseIntensity * flicker, 0f, 1f);
        }

        for (int slot = 0; slot < MAX_ACTIVE_PROP_LIGHTS; slot++) {
            PropLight nearest = null;
            for (PropLight propLight : propLights) {
                if (propLight.visible && !propLight.selected
                    && propLight.intensity > LIGHT_ON_THRESHOLD
                    && (nearest == null || propLight.distanceSquared < nearest.distanceSquared)) {
                    nearest = propLight;
                }
            }
            if (nearest == null) {
                break;
            }
            nearest.selected = true;
        }

        for (PropLight propLight : propLights) {
            if (propLight.selected) {
                propLight.color.set(propLight.baseColor);
                propLight.color.a = propLight.intensity;
                propLight.light.setPosition(propLight.position);
                propLight.light.setColor(propLight.color);
                propLight.light.setOn(true);
                propLight.light.setShaderIntensity(propLight.intensity);
            } else {
                propLight.light.setOn(false);
            }
        }
    }

    private void updateBarrelLight(
        ParallaxBackground.CycleState cycleState,
        float elapsed,
        float layerOffset,
        float layerWidth
    ) {
        float baseIntensity = MathUtils.lerp(
            getBarrelIntensity(cycleState.getPeriod()),
            getBarrelIntensity(cycleState.getNextPeriod()),
            cycleState.getFadeProgress()
        );
        float flicker = 1f
            + BARREL_FLICKER_AMPLITUDE * MathUtils.sin(elapsed * BARREL_FLICKER_SPEED)
            + BARREL_FLICKER_SECOND_AMPLITUDE
                * MathUtils.sin(elapsed * BARREL_FLICKER_SECOND_SPEED + 1.7f);
        float intensity = MathUtils.clamp(baseIntensity * flicker, 0f, 1f);

        float x = BARREL_X_IN_LAYER - layerOffset;
        x += MathUtils.round((camera.position.x - x) / layerWidth) * layerWidth;
        float halfVisibleWidth = viewport.getWorldWidth() * camera.zoom * 0.5f;
        boolean visible = x >= camera.position.x - halfVisibleWidth
            && x <= camera.position.x + halfVisibleWidth;

        barrelPosition.set(x, GameWorld.WORLD_HEIGHT - BARREL_Y_FROM_TOP);
        if (visible) {
            barrelLight.setPosition(barrelPosition);
        }
        barrelColor.set(BARREL_COLOR);
        barrelColor.a = intensity;
        barrelLight.setColor(barrelColor);
        barrelLight.setOn(visible && intensity > LIGHT_ON_THRESHOLD);
        barrelIntensity = intensity;
    }

    void renderDebug(ShapeRenderer shapeRenderer) {
        drawDebugSquare(shapeRenderer, sunDebugPosition, Color.WHITE);
        drawDebugSquare(shapeRenderer, moonDebugPosition, Color.WHITE);
        drawDebugSquare(shapeRenderer, barrelPosition, BARREL_COLOR);
        if (DEBUG_MUZZLE) {
            drawDebugSquare(shapeRenderer, muzzleDebugPosition, SHOT_FLASH_COLOR);
        }
        if (DEBUG_IMPACT) {
            for (ShotLight shotLight : shotLights) {
                if (shotLight.flash != null
                    && shotLight.flash.getType() == Bullet.EffectType.IMPACT
                    && shotLight.flash.getElapsed() < SHOT_IMPACT_DURATION) {
                    drawDebugSquare(shapeRenderer, shotLight.position, SHOT_IMPACT_COLOR);
                }
            }
        }
        for (PropLight propLight : propLights) {
            drawDebugSquare(shapeRenderer, propLight.position, propLight.baseColor);
        }
    }

    void renderShadows(GameWorld gameWorld, ParallaxBackground.CycleState cycleState) {
        float periodMultiplier = interpolateCycleValue(
            cycleState,
            SHADOW_MORNING_MULTIPLIER,
            SHADOW_AFTERNOON_MULTIPLIER,
            SHADOW_NIGHT_MULTIPLIER
        );
        float offset = getShadowDirectionalOffset(cycleState);
        Player player = gameWorld.getPlayer();
        shadowBatchColor.set(batch.getColor());
        drawShadow(
            player.getX() + Player.WIDTH * 0.5f,
            player.getY(),
            Player.WIDTH,
            offset,
            periodMultiplier
        );
        for (int i = 0; i < gameWorld.getEnemyCount(); i++) {
            Enemy enemy = gameWorld.getEnemy(i);
            drawShadow(
                enemy.getX() + Enemy.SIZE * 0.5f,
                enemy.getY(),
                gameWorld.getEnemyRenderedWidth(i),
                offset,
                periodMultiplier
            );
        }
        batch.setColor(shadowBatchColor);
    }

    private void drawShadow(
        float feetX,
        float feetY,
        float actorWidth,
        float offset,
        float periodMultiplier
    ) {
        float outerWidth = Math.min(
            MathUtils.round(actorWidth * SHADOW_WIDTH_RATIO * SHADOW_SCALE),
            MathUtils.round(actorWidth * SHADOW_MAX_WIDTH_RATIO)
        );
        drawShadowTier(
            feetX + offset,
            feetY,
            outerWidth,
            MathUtils.round(SHADOW_OUTER_HEIGHT * SHADOW_SCALE),
            SHADOW_OUTER_ALPHA * periodMultiplier
        );
        drawShadowTier(
            feetX + offset * SHADOW_MEDIUM_OFFSET_RATIO,
            feetY,
            MathUtils.round(outerWidth * SHADOW_MEDIUM_WIDTH_RATIO),
            MathUtils.round(SHADOW_MEDIUM_HEIGHT * SHADOW_SCALE),
            SHADOW_MEDIUM_ALPHA * periodMultiplier
        );
        drawShadowTier(
            feetX + offset * SHADOW_INNER_OFFSET_RATIO,
            feetY,
            MathUtils.round(outerWidth * SHADOW_INNER_WIDTH_RATIO),
            MathUtils.round(SHADOW_INNER_HEIGHT * SHADOW_SCALE),
            SHADOW_INNER_ALPHA * periodMultiplier
        );
    }

    private void drawShadowTier(
        float feetX,
        float feetY,
        float width,
        float height,
        float alpha
    ) {
        float x = MathUtils.round(feetX - width * 0.5f);
        float y = MathUtils.round(feetY - height * 0.5f);
        batch.setColor(SHADOW_COLOR.r, SHADOW_COLOR.g, SHADOW_COLOR.b, alpha);
        batch.draw(shadowTexture, x, y, width, height);
    }

    void renderShadowDebug(
        GameWorld gameWorld,
        ParallaxBackground.CycleState cycleState,
        ShapeRenderer shapeRenderer
    ) {
        if (!DEBUG_SHADOW) {
            return;
        }
        float offset = getShadowDirectionalOffset(cycleState);
        shapeRenderer.setColor(Color.WHITE);
        Player player = gameWorld.getPlayer();
        drawShadowDebug(
            shapeRenderer,
            player.getX() + Player.WIDTH * 0.5f,
            player.getY(),
            Player.WIDTH,
            offset
        );
        for (int i = 0; i < gameWorld.getEnemyCount(); i++) {
            Enemy enemy = gameWorld.getEnemy(i);
            drawShadowDebug(
                shapeRenderer,
                enemy.getX() + Enemy.SIZE * 0.5f,
                enemy.getY(),
                gameWorld.getEnemyRenderedWidth(i),
                offset
            );
        }
    }

    private void drawShadowDebug(
        ShapeRenderer shapeRenderer,
        float feetX,
        float feetY,
        float actorWidth,
        float offset
    ) {
        float outerWidth = Math.min(
            MathUtils.round(actorWidth * SHADOW_WIDTH_RATIO * SHADOW_SCALE),
            MathUtils.round(actorWidth * SHADOW_MAX_WIDTH_RATIO)
        );
        drawShadowTierDebug(
            shapeRenderer,
            feetX + offset,
            feetY,
            outerWidth,
            MathUtils.round(SHADOW_OUTER_HEIGHT * SHADOW_SCALE)
        );
        drawShadowTierDebug(
            shapeRenderer,
            feetX + offset * SHADOW_MEDIUM_OFFSET_RATIO,
            feetY,
            MathUtils.round(outerWidth * SHADOW_MEDIUM_WIDTH_RATIO),
            MathUtils.round(SHADOW_MEDIUM_HEIGHT * SHADOW_SCALE)
        );
        drawShadowTierDebug(
            shapeRenderer,
            feetX + offset * SHADOW_INNER_OFFSET_RATIO,
            feetY,
            MathUtils.round(outerWidth * SHADOW_INNER_WIDTH_RATIO),
            MathUtils.round(SHADOW_INNER_HEIGHT * SHADOW_SCALE)
        );
        shapeRenderer.circle(MathUtils.round(feetX), MathUtils.round(feetY), 1f, 4);
    }

    private void drawShadowTierDebug(
        ShapeRenderer shapeRenderer,
        float feetX,
        float feetY,
        float width,
        float height
    ) {
        float x = MathUtils.round(feetX - width * 0.5f);
        float y = MathUtils.round(feetY - height * 0.5f);
        shapeRenderer.rect(x, y, width, height);
    }

    private float interpolateCycleValue(
        ParallaxBackground.CycleState cycleState,
        float morning,
        float afternoon,
        float night
    ) {
        return MathUtils.lerp(
            getIntensityForPeriod(cycleState.getPeriod(), morning, afternoon, night),
            getIntensityForPeriod(cycleState.getNextPeriod(), morning, afternoon, night),
            cycleState.getFadeProgress()
        );
    }

    private float getShadowDirectionalOffset(ParallaxBackground.CycleState cycleState) {
        return interpolateCycleValue(
            cycleState,
            SHADOW_MAX_HORIZONTAL_OFFSET,
            -SHADOW_MAX_HORIZONTAL_OFFSET,
            -SHADOW_MAX_HORIZONTAL_OFFSET * SHADOW_NIGHT_OFFSET_MULTIPLIER
        );
    }

    private void drawDebugSquare(ShapeRenderer shapeRenderer, Vector2 position, Color color) {
        shapeRenderer.setColor(color);
        shapeRenderer.rect(position.x - 1.5f, position.y - 1.5f, 3f, 3f);
    }

    private void logPropLights(float elapsed) {
        int second = (int) elapsed;
        if (second == lastAfternoonDebugSecond) {
            return;
        }
        lastAfternoonDebugSecond = second;
        System.out.printf(
            Locale.ROOT,
            "[DEBUG_LIGHTS] elapsed=%.1fs, barril pos=(%.2f, %.2f) ativo=%s "
                + "intensidadeShader=%.3f intensidadeCalculada=%.3f%n",
            elapsed,
            barrelPosition.x,
            barrelPosition.y,
            barrelLight.isOn(),
            barrelLight.getShaderIntensity(),
            barrelIntensity
        );
        for (PropLight propLight : propLights) {
            System.out.printf(
                Locale.ROOT,
                "[DEBUG_LIGHTS] %s pos=(%.2f, %.2f) ativo=%s intensidadeShader=%.3f%n",
                propLight.name,
                propLight.position.x,
                propLight.position.y,
                propLight.light.isOn(),
                propLight.light.getShaderIntensity()
            );
        }
    }

    private float getBarrelIntensity(ParallaxBackground.Period period) {
        switch (period) {
            case MORNING:
                return BARREL_MORNING_INTENSITY;
            case AFTERNOON:
                return BARREL_AFTERNOON_INTENSITY;
            case NIGHT:
                return BARREL_NIGHT_INTENSITY;
            default:
                throw new IllegalArgumentException("Unknown day period: " + period);
        }
    }

    private GameLight.Point createAstroLight(float x, float yFromTop, Color color, float distance) {
        return lightEngine.addPointLight(
            new Vector2(x, GameWorld.WORLD_HEIGHT - yFromTop),
            new Color(color),
            1f,
            distance,
            0.5f,
            1f,
            64,
            (short) 0x0001,
            (short) -1,
            true
        );
    }

    private void updateAstroLight(GameLight.Point light, Color color, float intensity) {
        light.setColor(color);
        light.setOn(intensity > LIGHT_ON_THRESHOLD);
    }

    private Color getAmbient(ParallaxBackground.Period period) {
        switch (period) {
            case MORNING:
                return MORNING_AMBIENT;
            case AFTERNOON:
                return AFTERNOON_AMBIENT;
            case NIGHT:
                return NIGHT_AMBIENT;
            default:
                throw new IllegalArgumentException("Unknown day period: " + period);
        }
    }

    private Color getMoonColor(ParallaxBackground.Period period) {
        return period == ParallaxBackground.Period.AFTERNOON
            ? AFTERNOON_MOON_COLOR
            : NIGHT_MOON_COLOR;
    }

    private float getSunIntensity(ParallaxBackground.Period period) {
        return period == ParallaxBackground.Period.MORNING ? MORNING_SUN_INTENSITY : 0f;
    }

    private float getMoonIntensity(ParallaxBackground.Period period) {
        switch (period) {
            case AFTERNOON:
                return AFTERNOON_MOON_INTENSITY;
            case NIGHT:
                return NIGHT_MOON_INTENSITY;
            case MORNING:
                return 0f;
            default:
                throw new IllegalArgumentException("Unknown day period: " + period);
        }
    }

    void render(Texture scene) {
        sceneTexture = scene;
        lightUpdateCenter.set(camera.position.x, camera.position.y);
        rayHandler.useCustomViewport(
            viewport.getScreenX(),
            viewport.getScreenY(),
            viewport.getScreenWidth(),
            viewport.getScreenHeight()
        );
        lightEngine.renderLights(lightUpdateCenter, sceneDrawer);
    }

    void dispose() {
        if (lightEngine != null) {
            lightEngine.dispose();
        }
        fogLayer.dispose();
        if (shadowTexture != null) {
            shadowTexture.dispose();
        }
        if (world != null) {
            world.dispose();
        }
    }

    private static final class FogLayer {
        private static final float[] MASK_ALPHA_LEVELS = {0.25f, 0.50f, 0.75f, 1f};

        private final Color color = new Color();
        private final Color originalBatchColor = new Color();
        private Texture texture;
        private float alpha;
        private float backOffset;
        private float frontOffset;

        private void init() {
            Pixmap pixmap = new Pixmap(FOG_TILE_WIDTH, FOG_TILE_HEIGHT, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            for (int y = 0; y < FOG_TILE_HEIGHT; y++) {
                float pixelAlpha = getMaskAlpha(y);
                for (int x = 0; x < FOG_TILE_WIDTH; x++) {
                    float ditheredAlpha = pixelAlpha;
                    if (y > 0 && y % 2 == 0 && ((x + y) & 1) == 0) {
                        ditheredAlpha = getMaskAlpha(y - 1);
                    }
                    pixmap.setColor(1f, 1f, 1f, ditheredAlpha);
                    pixmap.drawPixel(x, y);
                }
            }
            texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pixmap.dispose();
        }

        private void update(
            float elapsed,
            ParallaxBackground.CycleState cycleState,
            float groundLayerOffset
        ) {
            color.set(getFogColor(cycleState.getPeriod()))
                .lerp(getFogColor(cycleState.getNextPeriod()), cycleState.getFadeProgress());
            alpha = MathUtils.lerp(
                getFogAlpha(cycleState.getPeriod()),
                getFogAlpha(cycleState.getNextPeriod()),
                cycleState.getFadeProgress()
            );
            backOffset = wrapOffset(elapsed * FOG_BACK_SPEED + groundLayerOffset);
            frontOffset = wrapOffset(elapsed * FOG_FRONT_SPEED + groundLayerOffset);
        }

        private void render(SpriteBatch batch) {
            originalBatchColor.set(batch.getColor());
            drawBand(batch, backOffset, FOG_BACK_Y, FOG_BACK_HEIGHT, FOG_BACK_ALPHA_FACTOR);
            drawBand(batch, frontOffset, FOG_FRONT_Y, FOG_FRONT_HEIGHT, FOG_FRONT_ALPHA_FACTOR);
            batch.setColor(originalBatchColor);
        }

        private void drawBand(
            SpriteBatch batch,
            float offset,
            float y,
            float height,
            float alphaFactor
        ) {
            int startX = -MathUtils.round(offset);
            int tileY = MathUtils.round(y);
            int tileHeight = MathUtils.round(height);
            batch.setColor(color.r, color.g, color.b, alpha * alphaFactor);
            for (int x = startX; x < GameWorld.WORLD_WIDTH; x += FOG_TILE_WIDTH) {
                batch.draw(texture, x, tileY, FOG_TILE_WIDTH, tileHeight);
            }
        }

        private void renderDebug(ShapeRenderer shapeRenderer) {
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.rect(0f, FOG_BACK_Y, GameWorld.WORLD_WIDTH, FOG_BACK_HEIGHT);
            shapeRenderer.rect(0f, FOG_FRONT_Y, GameWorld.WORLD_WIDTH, FOG_FRONT_HEIGHT);
        }

        private float wrapOffset(float value) {
            float wrapped = value % FOG_TILE_WIDTH;
            return wrapped < 0f ? wrapped + FOG_TILE_WIDTH : wrapped;
        }

        private float getMaskAlpha(int y) {
            int distanceFromEdge = Math.min(y, FOG_TILE_HEIGHT - 1 - y);
            int level = Math.min(distanceFromEdge / 2, MASK_ALPHA_LEVELS.length - 1);
            return MASK_ALPHA_LEVELS[level];
        }

        private Color getFogColor(ParallaxBackground.Period period) {
            switch (period) {
                case MORNING:
                    return FOG_MORNING_COLOR;
                case AFTERNOON:
                    return FOG_AFTERNOON_COLOR;
                case NIGHT:
                    return FOG_NIGHT_COLOR;
                default:
                    throw new IllegalArgumentException("Unknown day period: " + period);
            }
        }

        private float getFogAlpha(ParallaxBackground.Period period) {
            switch (period) {
                case MORNING:
                    return FOG_MORNING_ALPHA;
                case AFTERNOON:
                    return FOG_AFTERNOON_ALPHA;
                case NIGHT:
                    return FOG_NIGHT_ALPHA;
                default:
                    throw new IllegalArgumentException("Unknown day period: " + period);
            }
        }

        private void dispose() {
            if (texture != null) {
                texture.dispose();
            }
        }
    }

    private static final class PropLight {
        private final String name;
        private final int layer;
        private final float baseX;
        private final float yFromTop;
        private final Color baseColor = new Color();
        private final Color color = new Color();
        private final float morningIntensity;
        private final float afternoonIntensity;
        private final float nightIntensity;
        private final float distance;
        private final float flickerAmplitude;
        private final float flickerSpeed;
        private final float secondFlickerAmplitude;
        private final float secondFlickerSpeed;
        private final float flickerPhase;
        private final Vector2 position = new Vector2();
        private GameLight.Point light;
        private boolean visible;
        private boolean selected;
        private float distanceSquared;
        private float intensity;

        private PropLight(
            String name,
            int layer,
            float baseX,
            float yFromTop,
            Color baseColor,
            float morningIntensity,
            float afternoonIntensity,
            float nightIntensity,
            float distance,
            float flickerAmplitude,
            float flickerSpeed,
            float secondFlickerAmplitude,
            float secondFlickerSpeed,
            float flickerPhase
        ) {
            this.name = name;
            this.layer = layer;
            this.baseX = baseX;
            this.yFromTop = yFromTop;
            this.baseColor.set(baseColor);
            this.morningIntensity = morningIntensity;
            this.afternoonIntensity = afternoonIntensity;
            this.nightIntensity = nightIntensity;
            this.distance = distance;
            this.flickerAmplitude = flickerAmplitude;
            this.flickerSpeed = flickerSpeed;
            this.secondFlickerAmplitude = secondFlickerAmplitude;
            this.secondFlickerSpeed = secondFlickerSpeed;
            this.flickerPhase = flickerPhase;
            position.set(baseX, GameWorld.WORLD_HEIGHT - yFromTop);
        }

        private float getIntensity(ParallaxBackground.Period period) {
            switch (period) {
                case MORNING:
                    return morningIntensity;
                case AFTERNOON:
                    return afternoonIntensity;
                case NIGHT:
                    return nightIntensity;
                default:
                    throw new IllegalArgumentException("Unknown day period: " + period);
            }
        }
    }

    private static final class ShotLight {
        private final boolean flashSlot;
        private final Vector2 position = new Vector2();
        private final Color color = new Color();
        private GameLight.Point light;
        private Bullet.Effect flash;
        private Bullet bullet;
        private boolean selected;

        private ShotLight(boolean flashSlot) {
            this.flashSlot = flashSlot;
        }
    }
}
