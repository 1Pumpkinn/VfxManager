package net.rose.vfxManager.vfx.effects;

import net.rose.vfxManager.vfx.VFXBeam;
import net.rose.vfxManager.vfx.VFXContext;
import net.rose.vfxManager.vfx.VFXEffect;
import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXMath;
import net.rose.vfxManager.vfx.VFXRenderer;
import net.rose.vfxManager.vfx.VFXColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Effect wrapper around {@link VFXBeam}, so a beam gets the central tick loop, duration and automatic cleanup.
 * <p>
 * Usage:
 * <pre>{@code
 * vfx.beam(start, end)
 *         .width(0.4)
 *         .duration(20)
 *         .play();
 * }</pre>
 * The single ItemDisplay is created once and updated in place every tick; it is removed when the effect stops.
 */
public final class BeamEffect extends VFXEffect {
    private final VFXBeam beam;
    private final Location start;
    private final Location end;
    private Color coreColor = VFXColor.rgb(255, 255, 255);
    private boolean particleCore = true;

    public BeamEffect(final VFXManager manager, final Location start, final Location end) {
        super(manager, midpoint(start, end));
        this.start = start.clone();
        this.end = end.clone();
        this.beam = new VFXBeam(manager, start, end);
        setDuration(20);
        setRenderDistance(96.0D);
        setBoundingRadius(Math.max(1.0D, start.distance(end) / 2.0D));
    }

    public BeamEffect width(final double width) {
        this.beam.setWidth((float) width);
        return this;
    }

    public BeamEffect model(final ItemStack model) {
        this.beam.setModel(model);
        return this;
    }

    public BeamEffect duration(final int ticks) {
        setDuration(ticks);
        return this;
    }

    public BeamEffect coreColor(final Color color) {
        this.coreColor = color;
        return this;
    }

    /** Disable the particle line along the beam (display only). */
    public BeamEffect particleCore(final boolean enabled) {
        this.particleCore = enabled;
        return this;
    }

    /** Moves the start point; picked up on the next tick without respawning the display. */
    public BeamEffect moveStart(final Location location) {
        this.start.setX(location.getX());
        this.start.setY(location.getY());
        this.start.setZ(location.getZ());
        return this;
    }

    /** Moves the end point; picked up on the next tick without respawning the display. */
    public BeamEffect moveEnd(final Location location) {
        this.end.setX(location.getX());
        this.end.setY(location.getY());
        this.end.setZ(location.getZ());
        return this;
    }

    public BeamEffect play() {
        start();
        return this;
    }

    @Override
    protected void onStart() {
        cleanupTask(this.beam::remove);
    }

    @Override
    protected void onTick() {
        // Keep origin at the midpoint so render-distance checks and the debug view stay correct.
        originMutable().setX((this.start.getX() + this.end.getX()) * 0.5D);
        originMutable().setY((this.start.getY() + this.end.getY()) * 0.5D);
        originMutable().setZ((this.start.getZ() + this.end.getZ()) * 0.5D);

        this.beam.setStart(this.start).setEnd(this.end).update();
    }

    @Override
    protected void onRender(final VFXContext context) {
        if (!this.particleCore || !quality().secondaryEffects() || context.viewersWithinRange().isEmpty()) {
            return;
        }

        final Location origin = context.origin();
        final Vector from = this.start.toVector().subtract(origin.toVector());
        final Vector to = this.end.toVector().subtract(origin.toVector());
        final int count = quality().scalePoints((int) Math.round(this.start.distance(this.end) * 3.0D));
        final List<Vector> points = VFXMath.line(from, to, count);
        VFXRenderer.particles(points).color(this.coreColor, 0.7F).randomChance(0.5D).render(context);
    }

    @Override
    protected List<Vector> debugPoints() {
        final Location origin = origin();
        return List.of(
                this.start.toVector().subtract(origin.toVector()),
                this.end.toVector().subtract(origin.toVector())
        );
    }

    private static Location midpoint(final Location start, final Location end) {
        return start.clone().add(end.clone().subtract(start).multiply(0.5D));
    }
}
