package net.rose.vfxManager.vfx.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class Timeline {
    private final NavigableMap<Integer, List<Runnable>> oneShotEvents = new TreeMap<>();
    private final List<RepeatingEvent> repeatingEvents = new ArrayList<>();
    private LoopSection loopSection;

    public Timeline at(final int tick, final Runnable action) {
        this.oneShotEvents.computeIfAbsent(tick, ignored -> new ArrayList<>()).add(action);
        return this;
    }

    public Timeline every(final int interval, final Runnable action) {
        return every(0, Integer.MAX_VALUE, interval, action);
    }

    public Timeline every(final int startTick, final int endTick, final int interval, final Runnable action) {
        this.repeatingEvents.add(new RepeatingEvent(startTick, endTick, interval, action));
        return this;
    }

    public Timeline repeatSection(final int startTickInclusive, final int endTickInclusive) {
        return repeatSection(startTickInclusive, endTickInclusive, -1);
    }

    public Timeline repeatSection(
            final int startTickInclusive,
            final int endTickInclusive,
            final int maxRepeats
    ) {
        if (endTickInclusive < startTickInclusive) {
            throw new IllegalArgumentException("Loop end must be >= loop start");
        }

        this.loopSection = new LoopSection(startTickInclusive, endTickInclusive, maxRepeats);
        return this;
    }

    public void tick(final int rawTick) {
        final int virtualTick = toVirtualTick(rawTick);
        final List<Runnable> actions = this.oneShotEvents.get(virtualTick);
        if (actions != null) {
            actions.forEach(Runnable::run);
        }

        for (final RepeatingEvent repeatingEvent : this.repeatingEvents) {
            if (repeatingEvent.matches(virtualTick)) {
                repeatingEvent.action.run();
            }
        }
    }

    private int toVirtualTick(final int rawTick) {
        if (this.loopSection == null || rawTick <= this.loopSection.endTickInclusive) {
            return rawTick;
        }

        final int loopLength = this.loopSection.endTickInclusive - this.loopSection.startTickInclusive + 1;
        final int extraTicks = rawTick - this.loopSection.startTickInclusive;
        final int repeatsElapsed = extraTicks / loopLength;

        if (this.loopSection.maxRepeats >= 0 && repeatsElapsed > this.loopSection.maxRepeats) {
            return this.loopSection.endTickInclusive;
        }

        return this.loopSection.startTickInclusive + (extraTicks % loopLength);
    }

    private record RepeatingEvent(int startTick, int endTick, int interval, Runnable action) {
        private RepeatingEvent {
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be > 0");
            }
        }

        private boolean matches(final int tick) {
            return tick >= this.startTick
                    && tick <= this.endTick
                    && ((tick - this.startTick) % this.interval == 0);
        }
    }

    private record LoopSection(int startTickInclusive, int endTickInclusive, int maxRepeats) {
    }
}
