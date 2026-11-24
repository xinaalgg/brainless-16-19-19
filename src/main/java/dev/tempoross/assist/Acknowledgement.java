package dev.tempoross.assist;

/** Scoped to a concrete entity instance and one option. No elapsed-time phase advancement. */
final class Acknowledgement {
    private Object entity;
    private Rotation.Task task;
    private int lastEvidence;
    void clear() { entity = null; task = null; }
    void accept(Object entity, Rotation.Task task, int tick) {
        this.entity = entity;
        this.task = task;
        lastEvidence = tick;
    }
    boolean applies(Object entity, Rotation.Task task) {
        return entity != null && entity == this.entity && task == this.task;
    }
    boolean hidden(Object entity, Rotation.Task task, int tick, boolean evidence) {
        if (!applies(entity, task)) { clear(); return false; }
        if (evidence) lastEvidence = tick;
        if (tick - lastEvidence <= 4) return true;
        clear();
        return false;
    }
}
