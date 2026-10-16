package br.mackenzie;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {

    private static final String SerialPort = "COM3"; 
    private static final float W = 800f, H = 480f;
    private static final float VelocidadePlayer = 280f;

    // Subpastas dentro de assets/. Se os arquivos estiverem direto em assets/, funciona igual
    // (o código tenta primeiro a subpasta e depois a raiz)
    private static final String PASTA_INIMIGO = "inimigo/";
    private static final String PASTA_JOGADOR = "player/";

    // Qual rotate_N mostra o jogador virado para cima (parado). Teste de 0 a 7 até achar
    private static final int INDICE_JOGADOR_PARADO = 0;

    private static final float DURACAO_FRAME_INIMIGO = 0.12f; 
    private static final float DURACAO_FRAME_TIRO = 0.04f;    

    // ====== Classes internas ======
    private static class Projetil {
        Rectangle r;
        Texture tex;
        float vy;
        int dano;
    }

    private static class Inimigo {
        Rectangle r;
        int hp, hpMax;
        float vel;
        float tempoAnim = MathUtils.random(0f, 1f); // desencontra a animação entre inimigos
    }

    // ====== Câmera e desenho ======
    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private BitmapFont font;

    // ====== Texturas (sprites) ======
    private Texture texFundo;
    private Texture texJogadorParado;       // rotate_N.png
    private Texture[] framesTiroJogador;    // shoot_0..8.png
    private Texture[] framesInimigo;        // frame_000..005.png
    private float tempoTiroJogador = -1f;   // < 0 = não está atirando
    private Texture texTiro1, texTiro2, texTiro3;
    private Texture texPixel; // quadrado branco 1x1, usado nas barras

    private Rectangle jogador;
    private final Array<Projetil> projeteis = new Array<>();
    private final Array<Inimigo> inimigos = new Array<>();
    private float tempoSpawn;
    private int pontos;
    private int vidas;
    private boolean gameOver;

    private ArduinoReaderThread arduino;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Se o arquivo não existir na pasta assets, cria um retângulo colorido no lugar.
        texFundo   = carregar("fundo.png",    (int) W, (int) H, new Color(0.05f, 0.05f, 0.2f, 1));

        texJogadorParado = carregar(PASTA_JOGADOR, "rotate_" + INDICE_JOGADOR_PARADO + ".png", 40, 40, Color.GREEN);
        framesTiroJogador = new Texture[9];
        for (int i = 0; i < framesTiroJogador.length; i++) {
            framesTiroJogador[i] = carregar(PASTA_JOGADOR, "shoot_" + i + ".png", 40, 40, Color.GREEN);
        }

        framesInimigo = new Texture[6];
        for (int i = 0; i < framesInimigo.length; i++) {
            framesInimigo[i] = carregar(PASTA_INIMIGO, String.format("frame_%03d.png", i), 40, 40, Color.PURPLE);
        }

        texTiro1   = carregar("tiro1.png",    6, 14, Color.WHITE);
        texTiro2   = carregar("tiro2.png",    12, 24, Color.YELLOW);
        texTiro3   = carregar("tiro3.png",    10, 24, Color.RED);
        texPixel   = carregar("__pixel__",    1, 1, Color.WHITE);

        reiniciar();

        arduino = new ArduinoReaderThread(SerialPort);
        arduino.start();
    }

    /** Carrega uma imagem de assets/ ou gera um placeholder colorido se ela não existir. */
    private Texture carregar(String arquivo, int w, int h, Color cor) {
        FileHandle f = Gdx.files.internal(arquivo);
        if (f.exists()) return new Texture(f);

        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(cor);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    /** Tenta pasta + nome; se não achar, tenta só o nome; se não achar, gera placeholder. */
    private Texture carregar(String pasta, String nome, int w, int h, Color cor) {
        FileHandle f = Gdx.files.internal(pasta + nome);
        if (f.exists()) return new Texture(f);
        return carregar(nome, w, h, cor);
    }

    private void reiniciar() {
        jogador = new Rectangle(W / 2 - 20, 20, 40, 40);
        projeteis.clear();
        inimigos.clear();
        tempoSpawn = 0;
        pontos = 0;
        vidas = 3;
        gameOver = false;
        tempoTiroJogador = -1f;
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        if (!gameOver) {
            atualizar(dt);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
        }

        desenhar();
    }

    private void atualizar(float dt) {
        // Animação de tiro do jogador (roda uma vez e volta ao parado)
        if (tempoTiroJogador >= 0) {
            tempoTiroJogador += dt;
            if (tempoTiroJogador >= framesTiroJogador.length * DURACAO_FRAME_TIRO) tempoTiroJogador = -1f;
        }

        // Movimento no teclado (o hand grip cuida só dos tiros)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A))
            jogador.x -= VelocidadePlayer * dt;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
            jogador.x += VelocidadePlayer * dt;
        jogador.x = MathUtils.clamp(jogador.x, 0, W - jogador.width);

        // Tiros vindos do ESP32 (1 = simples, 2 = reforçado, 3 = especial)
        Integer nivel;
        while ((nivel = arduino.pollTiro()) != null) {
            disparar(nivel);
        }
        // Teclas de teste, sem hardware
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) disparar(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) disparar(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) disparar(3);

        // Spawn de inimigos
        tempoSpawn -= dt;
        if (tempoSpawn <= 0) {
            criarInimigo();
            tempoSpawn = MathUtils.random(0.8f, 1.8f);
        }

        // Projéteis
        for (int i = projeteis.size - 1; i >= 0; i--) {
            Projetil p = projeteis.get(i);
            p.r.y += p.vy * dt;
            if (p.r.y > H) projeteis.removeIndex(i);
        }

        // Inimigos
        for (int i = inimigos.size - 1; i >= 0; i--) {
            Inimigo e = inimigos.get(i);
            e.r.y -= e.vel * dt;
            e.tempoAnim += dt;

            if (e.r.y + e.r.height < 0) {            // passou pelo jogador
                inimigos.removeIndex(i);
                perderVida();
                continue;
            }
            if (e.r.overlaps(jogador)) {             // encostou no jogador
                inimigos.removeIndex(i);
                perderVida();
            }
        }

        // Colisão projétil x inimigo
        for (int i = projeteis.size - 1; i >= 0; i--) {
            Projetil p = projeteis.get(i);
            for (int j = inimigos.size - 1; j >= 0; j--) {
                Inimigo e = inimigos.get(j);
                if (p.r.overlaps(e.r)) {
                    e.hp -= p.dano;
                    projeteis.removeIndex(i);
                    if (e.hp <= 0) {
                        inimigos.removeIndex(j);
                        pontos += 10 * e.hpMax;
                    }
                    break;
                }
            }
        }
    }

    private void perderVida() {
        vidas--;
        if (vidas <= 0) gameOver = true;
    }

    private void criarInimigo() {
        Inimigo e = new Inimigo();
        e.hpMax = MathUtils.random(1, 3);
        e.hp = e.hpMax;
        e.vel = MathUtils.random(50f, 90f);
        e.r = new Rectangle(MathUtils.random(0, W - 40), H, 40, 40);
        inimigos.add(e);
    }

    private void disparar(int nivel) {
        float cx = jogador.x + jogador.width / 2;
        float topo = jogador.y + jogador.height;
        tempoTiroJogador = 0f; // inicia a animação shoot_0..8

        switch (nivel) {
            case 1: // simples
                addProjetil(texTiro1, cx - 3, topo, 6, 14, 400, 1);
                break;
            case 2: // reforçado: maior, mais rápido, mais dano
                addProjetil(texTiro2, cx - 6, topo, 12, 24, 550, 2);
                break;
            default: // especial: três projéteis lado a lado
                addProjetil(texTiro3, cx - 25, topo, 10, 24, 600, 2);
                addProjetil(texTiro3, cx - 5,  topo, 10, 24, 600, 2);
                addProjetil(texTiro3, cx + 15, topo, 10, 24, 600, 2);
                break;
        }
    }

    private void addProjetil(Texture tex, float x, float y, float w, float h, float vy, int dano) {
        Projetil p = new Projetil();
        p.tex = tex;
        p.r = new Rectangle(x, y, w, h);
        p.vy = vy;
        p.dano = dano;
        projeteis.add(p);
    }


    private void desenhar() {
        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // Fundo
        batch.setColor(Color.WHITE);
        batch.draw(texFundo, 0, 0, W, H);

        // Jogador
        Texture spriteJogador = texJogadorParado;
        if (tempoTiroJogador >= 0) {
            int i = Math.min((int) (tempoTiroJogador / DURACAO_FRAME_TIRO), framesTiroJogador.length - 1);
            spriteJogador = framesTiroJogador[i];
        }
        batch.draw(spriteJogador, jogador.x, jogador.y, jogador.width, jogador.height);

        // Inimigos (ficam mais escuros conforme perdem vida)
        for (Inimigo e : inimigos) {
            float brilho = 0.4f + 0.6f * ((float) e.hp / e.hpMax);
            batch.setColor(brilho, brilho, brilho, 1);
            int f = (int) (e.tempoAnim / DURACAO_FRAME_INIMIGO) % framesInimigo.length;
            batch.draw(framesInimigo[f], e.r.x, e.r.y, e.r.width, e.r.height);
        }
        batch.setColor(Color.WHITE);

        // Projéteis
        for (Projetil p : projeteis) {
            batch.draw(p.tex, p.r.x, p.r.y, p.r.width, p.r.height);
        }

        // Barra de força do hand grip
        float fracao = MathUtils.clamp(arduino.getForca() / 4095f, 0f, 1f);
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(texPixel, 10, 450, 200, 14);
        batch.setColor(1f, 0.5f, 0f, 1);
        batch.draw(texPixel, 10, 450, 200 * fracao, 14);
        batch.setColor(Color.WHITE);

        // HUD
        font.draw(batch, "Pontos: " + pontos, 10, 440);
        font.draw(batch, "Vidas: " + vidas, 10, 422);

        if (gameOver) {
            font.draw(batch, "GAME OVER - Pontos: " + pontos, W / 2 - 80, H / 2 + 10);
            font.draw(batch, "Pressione R para reiniciar", W / 2 - 85, H / 2 - 12);
        }

        batch.end();
    }


    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void dispose() {
        if (arduino != null) {
            arduino.stopReading();
            try { arduino.join(500); } catch (InterruptedException ignored) { }
        }
        batch.dispose();
        font.dispose();
        texFundo.dispose();
        texJogadorParado.dispose();
        for (Texture t : framesTiroJogador) t.dispose();
        for (Texture t : framesInimigo) t.dispose();
        texTiro1.dispose();
        texTiro2.dispose();
        texTiro3.dispose();
        texPixel.dispose();
    }
}