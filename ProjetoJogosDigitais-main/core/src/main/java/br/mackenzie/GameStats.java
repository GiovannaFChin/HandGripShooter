package br.mackenzie;

import java.util.Arrays;

final class GameStats {
    static final int PHASES = 3;

    // Pontos por aperto válido, pelo nível do disparo (1 = leve, 2 = moderado, 3 = forte).
    private static final int[] POINTS_BY_LEVEL = {0, 10, 20, 30};

    private final int[] squeezes = new int[PHASES];
    private final int[] score = new int[PHASES];
    private final float[] seconds = new float[PHASES];
    private int phase;   // 0 = fase 1

    /** Soma o tempo da fase atual. Só roda durante a partida, então a pausa não conta. */
    void update(float delta) {
        seconds[phase] += delta;
    }

    void registerSqueeze() {
        squeezes[phase]++;
    }

    /** Tiro que acertou um inimigo. É aqui que os pontos são somados. */
    void registerHit(int level) {
        int l = Math.max(1, Math.min(level, POINTS_BY_LEVEL.length - 1));
        score[phase] += POINTS_BY_LEVEL[l];
    }

    /** Fecha a fase atual e abre a próxima. Retorna false se já estava na última. */
    boolean nextPhase() {
        if (phase >= PHASES - 1) return false;
        phase++;
        return true;
    }

    void reset() {
        Arrays.fill(squeezes, 0);
        Arrays.fill(score, 0);
        Arrays.fill(seconds, 0f);
        phase = 0;
    }

    int getPhaseNumber()   { return phase + 1; }
    int getSqueezes(int p) { return squeezes[p]; }
    int getScore(int p)    { return score[p]; }

    float getSqueezesPerMinute(int p) {
        return seconds[p] > 0f ? squeezes[p] * 60f / seconds[p] : 0f;
    }

    int getTotalSqueezes() { return sum(squeezes); }
    int getTotalScore()    { return sum(score); }

    float getTotalSqueezesPerMinute() {
        float total = 0f;
        for (float s : seconds) total += s;
        return total > 0f ? getTotalSqueezes() * 60f / total : 0f;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }
}
