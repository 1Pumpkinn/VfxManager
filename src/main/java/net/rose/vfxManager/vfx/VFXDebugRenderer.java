package net.rose.vfxManager.vfx;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class VFXDebugRenderer {
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 70, 70), 0.8F);
    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(70, 255, 70), 0.8F);
    private static final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(70, 170, 255), 0.8F);
    private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 240, 70), 0.85F);
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0F);

    public void renderEffect(final VFXEffect effect, final VFXContext context) {
        final World world = context.world();
        final Location origin = context.origin();
        final List<Player> viewers = context.viewersWithinRange();
        if (viewers.isEmpty()) {
            return;
        }

        renderPoint(viewers, world, origin, WHITE);
        renderAxis(viewers, world, origin, new Vector(1.2D, 0.0D, 0.0D), RED);
        renderAxis(viewers, world, origin, new Vector(0.0D, 1.2D, 0.0D), GREEN);
        renderAxis(viewers, world, origin, new Vector(0.0D, 0.0D, 1.2D), BLUE);

        for (final Vector point : effect.debugPoints()) {
            renderPoint(viewers, world, origin.clone().add(point), YELLOW);
        }

        final List<Vector> radiusRing = VFXMath.circle(effect.boundingRadius(), 28);
        final List<Vector> flattened = new ArrayList<>(radiusRing.size());
        for (final Vector point : radiusRing) {
            flattened.add(point.clone().setY(0.0D));
        }

        VFXRenderer.particles(flattened)
                .particle(Particle.DUST)
                .data(WHITE)
                .force(true)
                .render(world, origin, viewers);
    }

    public void renderBeamEndpoints(final World world, final Location start, final Location end, final List<Player> viewers) {
        if (viewers.isEmpty()) {
            return;
        }

        renderPoint(viewers, world, start, RED);
        renderPoint(viewers, world, end, BLUE);
    }

    private void renderAxis(
            final List<Player> viewers,
            final World world,
            final Location origin,
            final Vector direction,
            final Particle.DustOptions dustOptions
    ) {
        VFXRenderer.particles(VFXMath.line(new Vector(), direction, 8))
                .particle(Particle.DUST)
                .data(dustOptions)
                .force(true)
                .render(world, origin, viewers);
    }

    private void renderPoint(
            final List<Player> viewers,
            final World world,
            final Location location,
            final Particle.DustOptions dustOptions
    ) {
        for (final Player viewer : viewers) {
            viewer.spawnParticle(
                    Particle.DUST,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    dustOptions,
                    true
            );
        }
    }
}
