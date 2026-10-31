package net.rose.vfxManager.vfx;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws debug overlays for effects: origin, XYZ axes, control points and the bounding radius.
 * <p>
 * Only runs while {@link VFXManager#isDebugMode()} is on. Axis lines and bounding rings are generated once and
 * cached, so leaving debug mode on does not add per-frame allocation on top of the effect's own cost.
 */
public final class VFXDebugRenderer {
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 70, 70), 0.8F);
    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(70, 255, 70), 0.8F);
    private static final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(70, 170, 255), 0.8F);
    private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 240, 70), 0.85F);
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0F);

    private static final List<Vector> AXIS_X = VFXMath.line(new Vector(), new Vector(1.2D, 0.0D, 0.0D), 8);
    private static final List<Vector> AXIS_Y = VFXMath.line(new Vector(), new Vector(0.0D, 1.2D, 0.0D), 8);
    private static final List<Vector> AXIS_Z = VFXMath.line(new Vector(), new Vector(0.0D, 0.0D, 1.2D), 8);

    private static final int MAX_CACHED_RADII = 32;

    // Bounding rings keyed by radius in quarter-block steps; cleared if many different radii show up.
    private final Map<Integer, BoundingRings> ringCache = new HashMap<>();

    public void renderEffect(final VFXEffect effect, final VFXContext context) {
        final World world = context.world();
        final Location origin = context.origin();
        final List<Player> viewers = context.viewersWithinRange();
        if (viewers.isEmpty()) {
            return;
        }

        renderPoint(viewers, origin.getX(), origin.getY(), origin.getZ(), WHITE);
        renderLines(viewers, world, origin, AXIS_X, RED);
        renderLines(viewers, world, origin, AXIS_Y, GREEN);
        renderLines(viewers, world, origin, AXIS_Z, BLUE);

        for (final Vector point : effect.debugPoints()) {
            renderPoint(viewers, origin.getX() + point.getX(), origin.getY() + point.getY(), origin.getZ() + point.getZ(), YELLOW);
        }

        // Bounding radius as three perpendicular rings (a wireframe sphere).
        final BoundingRings rings = boundingRings(effect.boundingRadius());
        renderLines(viewers, world, origin, rings.horizontal(), WHITE);
        renderLines(viewers, world, origin, rings.frontal(), WHITE);
        renderLines(viewers, world, origin, rings.lateral(), WHITE);
    }

    public void renderBeamEndpoints(final World world, final Location start, final Location end, final List<Player> viewers) {
        if (viewers.isEmpty()) {
            return;
        }

        renderPoint(viewers, start.getX(), start.getY(), start.getZ(), RED);
        renderPoint(viewers, end.getX(), end.getY(), end.getZ(), BLUE);
    }

    /** Renders arbitrary relative points, e.g. the control points of a Bezier curve, in yellow. */
    public void renderControlPoints(final World world, final Location origin, final List<Vector> points, final List<Player> viewers) {
        if (viewers.isEmpty()) {
            return;
        }

        for (final Vector point : points) {
            renderPoint(viewers, origin.getX() + point.getX(), origin.getY() + point.getY(), origin.getZ() + point.getZ(), YELLOW);
        }
    }

    private BoundingRings boundingRings(final double radius) {
        final int key = (int) Math.round(radius * 4.0D);
        BoundingRings rings = this.ringCache.get(key);
        if (rings == null) {
            if (this.ringCache.size() >= MAX_CACHED_RADII) {
                this.ringCache.clear();
            }

            final List<Vector> horizontal = VFXMath.circle(Math.max(0.25D, radius), 28);
            final List<Vector> frontal = new ArrayList<>(horizontal.size());
            final List<Vector> lateral = new ArrayList<>(horizontal.size());
            for (final Vector point : horizontal) {
                frontal.add(new Vector(point.getX(), point.getZ(), 0.0D));
                lateral.add(new Vector(0.0D, point.getX(), point.getZ()));
            }

            rings = new BoundingRings(horizontal, frontal, lateral);
            this.ringCache.put(key, rings);
        }
        return rings;
    }

    private void renderLines(
            final List<Player> viewers,
            final World world,
            final Location origin,
            final List<Vector> points,
            final Particle.DustOptions dustOptions
    ) {
        VFXRenderer.particles(points)
                .particle(Particle.DUST)
                .data(dustOptions)
                .force(true)
                .render(world, origin, viewers);
    }

    private void renderPoint(
            final List<Player> viewers,
            final double x,
            final double y,
            final double z,
            final Particle.DustOptions dustOptions
    ) {
        for (final Player viewer : viewers) {
            viewer.spawnParticle(Particle.DUST, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D, dustOptions, true);
        }
    }

    private record BoundingRings(List<Vector> horizontal, List<Vector> frontal, List<Vector> lateral) {
    }
}
