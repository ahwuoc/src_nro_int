package nro.models.player;

/**
 * Kết quả đột phá level đệ tử
 * @author 💖 ahwuocdz 💖
 */
public class BreakthroughResult {
    private final boolean success;
    private final int oldLevel;
    private final int newLevel;
    private final long expUsed;
    private final long remainingExp;

    public BreakthroughResult(boolean success, int oldLevel, int newLevel, long expUsed, long remainingExp) {
        this.success = success;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.expUsed = expUsed;
        this.remainingExp = remainingExp;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public long getExpUsed() {
        return expUsed;
    }

    public long getRemainingExp() {
        return remainingExp;
    }
}
