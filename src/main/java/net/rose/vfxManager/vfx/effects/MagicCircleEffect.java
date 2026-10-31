package net.rose.vfxManager.vfx.effects;

import net.rose.vfxManager.vfx.VFXColor;
import net.rose.vfxManager.vfx.VFXContext;
import net.rose.vfxManager.vfx.VFXEffect;
import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXMath;
import net.rose.vfxManager.vfx.VFXRenderer;
import net.rose.vfxManager.vfx.animation.Easing;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat, rotating ground/air magic circle made of concentric rings and rune spokes.
 * <p>
 * Usage:
 * <pre>{@code
 * vfx.magicCircle(location)
 *         .radius(5)
 *         .rings(3)
 *         .color(255, 40, 180)
 *         .rotationSpeed(0.08)
 *         .duration(60)
 *         .play();
 * }</pre>
 * Point lists are generated once in {@link #onStart()} and rotated into reusable vectors each tick,
 * so the render loop does not allocate new vectors.
 */
public final class MagicCircleEffect extends VFXEffect {
    private double radius = 3.0D;
    private int ringCount = 3;
    private double rotationSpeed = 0.05D;
    private float particleSize = 1.1F;
    private Color color = VFXColor.rgb(255, 40, 180);
    private Color accent = VFXColor.rgb(255, 255, 255);

    private final List<Ring> ringData = new ArrayList<>();
    private final List<Vector> spokeBase = new ArrayList<>();
    private final List<Vector> spokeWork = new ArrayList<>();

    public MagicCircleEffect(final VFXManager manager, final Location origin) {
        super(manager, origin);
        setDuration(60);
        setRenderDistance(64.0D);
    }

    // ---- fluent configuration (all return MagicCircleEffect so chains keep their type) ----

    public MagicCircleEffect radius(final double radius) {
        this.radius = Math.max(0.25D, radius);
        setBoundingRadius(this.radius);
        return this;
    }

    public MagicCircleEffect rings(final int rings) {
        this.ringCount = Math.clamp(rings, 1, 8);
        return this;
    }

    public MagicCircleEffect color(final int red, final int green, final int blue) {
        this.color = VFXColor.rgb(red, green, blue);
        return this;
    }

    public MagicCircleEffect color(final Color color) {
        this.color = color;
        return this;
    }

    public MagicCircleEffect accent(final Color accent) {
        this.accent = accent;
        return this;
    }

    public MagicCircleEffect rotationSpeed(final double radiansPerTick) {
        this.rotationSpeed = radiansPerTick;
        return this;
    }

    public MagicCircleEffect particleSize(final float size) {
        this.particleSize = Math.max(0.1F, size);
        return this;
    }

    public MagicCircleEffect duration(final int ticks) {
        setDuration(ticks);
        return this;
    }

    public MagicCircleEffect follow(final org.bukkit.entity.Entity entity, final Vector offset) {
        attach(entity);
        setOffset(offset);
        return this;
    }

    /** Starts the effect. Readable alias for {@link #start()} at the end of a builder chain. */
    public MagicCircleEffect play() {
        start();
        return this;
    }

    // ---- lifecycle ----

    @Override
    protected void onStart() {
        this.ringData.clear();
        this.spokeBase.clear();
        this.spokeWork.clear();

        for (int index = 0; index < this.ringCount; index++) {
            final double fraction = (index + 1) / (double) this.ringCount;
            final double ringRadius = this.radius * fraction;
            final int points = quality().scalePoints((int) Math.round(ringRadius * 10.0D) + 14);

            final List<Vector> base = VFXMath.circle(ringRadius, points);
            final List<Vector> work = new ArrayList<>(base.size());
            for (final Vector vector : base) {
                work.add(vector.clone());
            }
            // Alternate spin direction and slightly vary speed so rings don't look locked together.
            final double direction = (index % 2 == 0) ? 1.0D : -1.0D;
            this.ringData.add(new Ring(base, work, direction * (0.7D + (0.3D * fraction))));
        }

        // Rune spokes join the innermost ring to the outer one; secondary detail, skipped on LOW.
        if (quality().secondaryEffects()) {
            final int spokes = Math.max(4, this.ringCount * 2);
            final int spokePoints = quality().scalePoints((int) Math.round(this.radius * 3.0D) + 4);
            final Vector inner = new Vector(this.radius / this.ringCount, 0.0D, 0.0D);
            final Vector outer = new Vector(this.radius, 0.0D, 0.0D);
            final List<Vector> line = VFXMath.line(inner, outer, spokePoints);
            for (int spoke = 0; spoke < spokes; spoke++) {
                final double angle = (VFXMath.TAU / spokes) * spoke;
                for (final Vector vector : line) {
                    final Vector rotated = VFXMath.rotateY(vector.clone(), angle);
                    this.spokeBase.add(rotated);
                    this.spokeWork.add(rotated.clone());
                }
            }
        }
    }

    @Override
    protected void onRender(final VFXContext context) {
        if (context.viewersWithinRange().isEmpty()) {
            return;
        }

        final double progress = progress();
        // Grow in over the first 20% with overshoot, shrink away over the last 15%.
        final double grow = Easing.BACK_OUT.apply(Math.min(1.0D, progress / 0.2D), 0.0D, 1.0D);
        final double fade = progress > 0.85D ? Easing.EASE_IN.apply((progress - 0.85D) / 0.15D, 1.0D, 0.0D) : 1.0D;
        final double scale = Math.max(0.0D, grow * fade);
        final double angle = age() * this.rotationSpeed;

        for (int index = 0; index < this.ringData.size(); index++) {
            final Ring ring = this.ringData.get(index);
            rotateInto(ring.base, ring.work, angle * ring.spin, scale);
            VFXRenderer.particles(ring.work).color(this.color, this.particleSize).render(context);
        }

        if (!this.spokeWork.isEmpty()) {
            rotateInto(this.spokeBase, this.spokeWork, -angle * 0.5D, scale);
            VFXRenderer.particles(this.spokeWork)
                    .color(this.accent, this.particleSize * 0.8F)
                    .randomChance(0.6D)
                    .render(context);
        }
    }

    @Override
    protected List<Vector> debugPoints() {
        return List.of(new Vector(this.radius, 0.0D, 0.0D), new Vector(-this.radius, 0.0D, 0.0D));
    }

    /**
     * Rotates {@code base} around Y by {@code angle}, scales it, and writes into the preallocated {@code work}
     * vectors. {@link VFXMath#rotateY} returns a new vector, so this avoids per-tick allocation in the hot loop.
     */
    private static void rotateInto(final List<Vector> base, final List<Vector> work, final double angle, final double scale) {
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        for (int index = 0; index < base.size(); index++) {
            final Vector source = base.get(index);
            final double x = source.getX();
            final double z = source.getZ();
            work.get(index).setX(((x * cos) + (z * sin)) * scale)
                    .setY(source.getY() * scale)
                    .setZ(((-x * sin) + (z * cos)) * scale);
        }
    }

    private record Ring(List<Vector> base, List<Vector> work, double spin) {
    }
}
