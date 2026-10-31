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

    /** Removes every scheduled event and loop section. */
    public Timeline clear() {
        this.oneShotEvents.clear();
        this.repeatingEvents.clear();
        this.loopSection = null;
        return this;
    }

    public void tick(final int rawTick) {
        final int virtualTick = toVirtualTick(rawTick);
        final List<Runnable> actions = this.oneShotEvents.get(virtualTick);
        if (actions != null) {
            // Iterate a copy so an action may safely schedule more events with at().
            for (final Runnable action : List.copyOf(actions)) {
                action.run();
            }
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

        // Loop finished: time carries on past the end of the section instead of freezing on its last tick,
        // which would re-fire the events at that tick every single game tick.
        if (this.loopSection.maxRepeats >= 0 && repeatsElapsed > this.loopSection.maxRepeats) {
            return rawTick - (loopLength * this.loopSection.maxRepeats);
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
