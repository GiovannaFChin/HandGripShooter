package br.mackenzie;

final class PlayerMovementAnimation {
    private static final int IDLE_FRAME = 1;
    private static final int FRAMES_PER_SECOND = 8;
    private static final int[] MOVEMENT_FRAMES = {1, 2, 4, 5, 7, 8};

    private float elapsedTime;
    private boolean moving;

    void update(float delta, boolean moving) {
        this.moving = moving;
        elapsedTime = moving ? elapsedTime + delta : 0f;
    }

    int getFrameIndex() {
        if (!moving) {
            return IDLE_FRAME;
        }

        int animationFrame = (int) (elapsedTime * FRAMES_PER_SECOND) % MOVEMENT_FRAMES.length;
        return MOVEMENT_FRAMES[animationFrame];
    }

    void reset() {
        elapsedTime = 0f;
        moving = false;
    }
}