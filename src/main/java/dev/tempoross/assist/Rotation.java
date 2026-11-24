package dev.tempoross.assist;

/** Resource checkpoints. Inputs are sampled together at the end of a server tick. */
final class Rotation {
    enum Task {
        NONE, TAKE_BUCKET, TAKE_ROPE, TAKE_HARPOON, FILL_BUCKETS, FISH, COOK, LOAD
    }

    static final class Bag {
        final int raw, cooked, crystal, emptyBuckets, water, free;
        final boolean ropeReady, harpoonReady;
        Bag(int raw, int cooked, int crystal, int emptyBuckets, int water, int free,
            boolean ropeReady, boolean harpoonReady) {
            this.raw = raw;
            this.cooked = cooked;
            this.crystal = crystal;
            this.emptyBuckets = emptyBuckets;
            this.water = water;
            this.free = free;
            this.ropeReady = ropeReady;
            this.harpoonReady = harpoonReady;
        }
        int fish() { return raw + cooked + crystal; }
    }

    private boolean started, firstCook, waitingForPool, sawPool, ambiguous;
    private int batch, delivered, previousPoints;
    private int pendingFish, expectedPoints, bankedPoints, unmatchedTicks;
    private Bag previous;

    boolean allowDoubleCatch(Bag bag) {
        return (batch == 0 ? 16 : 19) - delivered - bag.fish() >= 2;
    }

    void reset() {
        started = firstCook = waitingForPool = sawPool = ambiguous = false;
        batch = delivered = previousPoints = 0;
        pendingFish = expectedPoints = bankedPoints = unmatchedTicks = 0;
        previous = null;
    }

    Task update(Bag bag, int points, boolean loadingAtCrate, boolean poolOpen, boolean tethered) {
        if (!started) {
            // Zero score and no fish identify the untouched opening in this solo instance.
            if (points != 0 || bag.fish() != 0) return Task.NONE;
            started = true;
        }
        if (previous != null) {
            int cookedLost = Math.max(0, previous.cooked - bag.cooked);
            int crystalLost = Math.max(0, previous.crystal - bag.crystal);
            if (loadingAtCrate) {
                // The score and backpack can arrive on adjacent ticks.
                pendingFish += cookedLost + crystalLost;
                expectedPoints += 65 * cookedLost + 20 * crystalLost;
                bankedPoints += points - previousPoints;
                if (bag.raw < previous.raw) ambiguous = true;
                if (expectedPoints == bankedPoints) {
                    delivered += pendingFish;
                    pendingFish = expectedPoints = bankedPoints = unmatchedTicks = 0;
                } else if (++unmatchedTicks > 1) {
                    if (pendingFish != 0) ambiguous = true;
                    pendingFish = expectedPoints = bankedPoints = unmatchedTicks = 0;
                }
            } else {
                if (pendingFish != 0) ambiguous = true;
                pendingFish = expectedPoints = bankedPoints = unmatchedTicks = 0;
            }
        }
        previous = bag;
        previousPoints = points;
        int goal = batch == 0 ? 16 : 19;
        if (delivered > goal) ambiguous = true;
        if (ambiguous || pendingFish != 0) return Task.NONE;
        if (delivered == goal) {
            delivered = 0;
            batch++;
            waitingForPool = batch >= 2;
            sawPool = false;
        }
        if (waitingForPool) {
            sawPool |= poolOpen;
            if (!sawPool || poolOpen) return Task.NONE;
            waitingForPool = false;
        }
        if (tethered || poolOpen || batch >= 4) return Task.NONE;
        if (!bag.ropeReady) return bag.free > 0 ? Task.TAKE_ROPE : Task.NONE;
        if (!bag.harpoonReady) return bag.free > 0 ? Task.TAKE_HARPOON : Task.NONE;
        if (bag.water + bag.emptyBuckets < 4 && batch == 0)
            return bag.free > 0 ? Task.TAKE_BUCKET : Task.NONE;
        if (bag.emptyBuckets > 0 && batch == 0 && bag.fish() == 0 && delivered == 0)
            return Task.FILL_BUCKETS;
        int remaining = (batch == 0 ? 16 : 19) - delivered;
        if (bag.cooked + bag.crystal >= 8) firstCook = true;
        if (batch == 0 && !firstCook && bag.raw > 0 && bag.fish() >= 8) return Task.COOK;
        if (bag.fish() >= remaining || bag.free == 0) {
            if (bag.raw > 0) return Task.COOK;
            // An unattended Fill empties the entire inventory. Never cue an oversized load.
            if (bag.fish() > remaining || bag.fish() == 0) return Task.NONE;
            return Task.LOAD;
        }
        // Finish a partially loaded batch before collecting the next one.
        if (delivered > 0 && bag.raw == 0 && bag.fish() > 0) return Task.LOAD;
        return Task.FISH;
    }
}
