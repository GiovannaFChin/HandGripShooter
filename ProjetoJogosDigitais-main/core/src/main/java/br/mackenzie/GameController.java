package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public final class GameController {
    private final GameWorld gameWorld;
    private final ArduinoReaderThread arduino;
    private static final boolean TEST_MODE = false;

    public GameController(GameWorld gameWorld, String serialPort) {
        this.gameWorld = gameWorld;
        this.arduino = new ArduinoReaderThread(serialPort);
    }

    public void start() {
        arduino.start();
    }

    public void update(float delta) {
        float horizontal = 0f;
        float vertical = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            horizontal += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            horizontal -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            vertical += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            vertical -= 1f;
        }

        gameWorld.update(delta, horizontal, vertical, Gdx.input.isKeyPressed(Input.Keys.SPACE));

        Integer level;
        while ((level = arduino.pollTiro()) != null) {
            gameWorld.registerSqueeze(level);
        }
        if (TEST_MODE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) gameWorld.registerSqueeze(1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) gameWorld.registerSqueeze(2);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) gameWorld.registerSqueeze(3);
            if (Gdx.input.isKeyJustPressed(Input.Keys.N)) gameWorld.advancePhase();
        }
    }

    public int getForce() {
        return arduino.getForca();
    }
    public void clearPendingShots() {
        while (arduino.pollTiro() != null) {
        }
    }

    public void dispose() {
        arduino.stopReading();
        try {
            arduino.join(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
