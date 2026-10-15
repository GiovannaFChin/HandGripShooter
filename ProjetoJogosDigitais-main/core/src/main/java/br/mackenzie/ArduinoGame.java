package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class ArduinoGame extends ApplicationAdapter {
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private Rectangle player;
    private final float playerSpeed = 250f;
    private ArduinoReaderThread arduino;

    private static class Projetil {
        float x, y, vy, w, h;
        int tipo;
    }
    private final Array<Projetil> projeteis = new Array<>();

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        shapeRenderer = new ShapeRenderer();

        player = new Rectangle(385, 20, 30, 30);

        arduino = new ArduinoReaderThread("COM3"); // ajuste a porta (use o SerialPortLister)
        arduino.start();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Movimento no teclado (o hand grip cuida só dos tiros)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  player.x -= playerSpeed * dt;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) player.x += playerSpeed * dt;
        player.x = Math.max(0, Math.min(800 - player.width, player.x));

        // Consome todos os tiros pendentes vindos do ESP32
        Integer nivel;
        while ((nivel = arduino.pollTiro()) != null) {
            dispararTiro(nivel);
        }
        // Teclas de teste sem hardware
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) dispararTiro(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) dispararTiro(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) dispararTiro(3);

        // Atualiza projéteis
        for (int i = projeteis.size - 1; i >= 0; i--) {
            Projetil p = projeteis.get(i);
            p.y += p.vy * dt;
            if (p.y > 480) projeteis.removeIndex(i);
        }

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Jogador
        shapeRenderer.setColor(0, 1, 0, 1);
        shapeRenderer.rect(player.x, player.y, player.width, player.height);

        // Projéteis por tipo
        for (Projetil p : projeteis) {
            switch (p.tipo) {
                case 1: shapeRenderer.setColor(1, 1, 1, 1); break;     // simples
                case 2: shapeRenderer.setColor(1, 0.8f, 0, 1); break;  // reforçado
                default: shapeRenderer.setColor(1, 0, 0, 1); break;    // especial
            }
            shapeRenderer.rect(p.x, p.y, p.w, p.h);
        }

        // Barra de força em tempo real
        float fracao = Math.min(1f, arduino.getForca() / 4095f);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(10, 450, 200, 14);
        shapeRenderer.setColor(1, 0.5f, 0, 1);
        shapeRenderer.rect(10, 450, 200 * fracao, 14);

        shapeRenderer.end();
    }

    private void dispararTiro(int nivel) {
        float cx = player.x + player.width / 2;
        float topo = player.y + player.height;

        switch (nivel) {
            case 1: // simples
                add(cx - 3, topo, 0, 6, 12, 1, 400);
                break;
            case 2: // reforçado: maior e mais rápido
                add(cx - 6, topo, 0, 12, 24, 2, 550);
                break;
            default: // especial: 3 projéteis lado a lado
                add(cx - 25, topo, 0, 10, 24, 3, 600);
                add(cx - 5,  topo, 0, 10, 24, 3, 600);
                add(cx + 15, topo, 0, 10, 24, 3, 600);
                break;
        }
    }

    private void add(float x, float y, float unused, float w, float h, int tipo, float vy) {
        Projetil p = new Projetil();
        p.x = x; p.y = y; p.w = w; p.h = h; p.tipo = tipo; p.vy = vy;
        projeteis.add(p);
    }

    @Override
    public void dispose() {
        arduino.stopReading();
        try { arduino.join(500); } catch (InterruptedException ignored) {}
        shapeRenderer.dispose();
    }
}