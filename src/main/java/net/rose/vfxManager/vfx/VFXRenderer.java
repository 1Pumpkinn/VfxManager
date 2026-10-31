package net.rose.vfxManager.vfx;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class VFXRenderer {
    private VFXRenderer() {
    }

    public static ParticleBatch particles(final List<Vector> points) {
        return new ParticleBatch(points);
    }

    public static final class ParticleBatch {
        private final List<Vector> points;
        private Particle particle = Particle.DUST;
        private int count = 1;
        private double offsetX;
        private double offsetY;
        private double offsetZ;
        private double speed;
        private Object data = new Particle.DustOptions(Color.WHITE, 1.0F);
        private boolean force;
        private double randomChance = 1.0D;

        private ParticleBatch(final List<Vector> points) {
            this.points = Objects.requireNonNull(points, "points");
        }

        public ParticleBatch particle(final Particle particle) {
            this.particle = particle;
            return this;
        }

        public ParticleBatch count(final int count) {
            this.count = Math.max(1, count);
            return this;
        }

        public ParticleBatch offset(final double x, final double y, final double z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        public ParticleBatch speed(final double speed) {
            this.speed = speed;
            return this;
        }

        public ParticleBatch data(final Object data) {
            this.data = data;
            return this;
        }

        public ParticleBatch color(final Color color) {
            this.data = new Particle.DustOptions(color, 1.0F);
            return this;
        }

        public ParticleBatch color(final Color color, final float size) {
            this.data = new Particle.DustOptions(color, size);
            return this;
        }

        public ParticleBatch size(final float size) {
            if (this.data instanceof Particle.DustOptions dustOptions) {
                this.data = new Particle.DustOptions(dustOptions.getColor(), size);
            } else if (this.data instanceof Particle.DustTransition transition) {
                this.data = new Particle.DustTransition(transition.getColor(), transition.getToColor(), size);
            } else {
                this.data = new Particle.DustOptions(Color.WHITE, size);
            }
            return this;
        }

        public ParticleBatch transition(final Color from, final Color to, final float size) {
            this.data = new Particle.DustTransition(from, to, size);
            return this;
        }

        public ParticleBatch force(final boolean force) {
            this.force = force;
            return this;
        }

        public ParticleBatch randomChance(final double randomChance) {
            this.randomChance = Math.clamp(randomChance, 0.0D, 1.0D);
            return this;
        }

        public void render(final World world, final Location origin) {
            render(world, origin, world.getPlayers());
        }

        public void render(final VFXContext context) {
            render(context.world(), context.origin(), context.viewersWithinRange(), context.quality());
        }

        public void render(final World world, final Location origin, final Collection<Player> viewers) {
            render(world, origin, viewers, VFXQuality.HIGH);
        }

        public void render(
                final World world,
                final Location origin,
                final Collection<Player> viewers,
                final VFXQuality quality
        ) {
            if (viewers.isEmpty()) {
                return;
            }

            final int particleCount = quality.scaleParticles(this.count);
            final ThreadLocalRandom random = ThreadLocalRandom.current();

            for (final Vector point : this.points) {
                if (random.nextDouble() > this.randomChance) {
                    continue;
                }

                final double x = origin.getX() + point.getX();
                final double y = origin.getY() + point.getY();
                final double z = origin.getZ() + point.getZ();

                for (final Player viewer : viewers) {
                    viewer.spawnParticle(
                            this.particle,
                            x,
                            y,
                            z,
                            particleCount,
                            this.offsetX,
                            this.offsetY,
                            this.offsetZ,
                            this.speed,
                            this.data,
                            this.force
                    );
                }
            }
        }
    }
}
