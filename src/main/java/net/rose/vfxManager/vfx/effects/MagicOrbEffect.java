package net.rose.vfxManager.vfx.effects;

import net.rose.vfxManager.vfx.VFXColor;
import net.rose.vfxManager.vfx.VFXContext;
import net.rose.vfxManager.vfx.VFXEffect;
import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXMath;
import net.rose.vfxManager.vfx.VFXQuality;
import net.rose.vfxManager.vfx.VFXRenderer;
import net.rose.vfxManager.vfx.animation.Easing;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MagicOrbEffect extends VFXEffect {
    private static final List<Color> PALETTE = List.of(
            VFXColor.rgb(255, 80, 220),
            VFXColor.rgb(120, 255, 255),
            VFXColor.rgb(140, 110, 255),
            VFXColor.rgb(255, 155, 80),
            VFXColor.rgb(80, 180, 255)
    );

    private final List<RingDefinition> rings = new ArrayList<>();
    private final List<Vector> corePoints = new ArrayList<>();
    private final List<Vector> shellAnchors = new ArrayList<>();
    private final List<Vector> trailAnchors = new ArrayList<>();
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private int flashTicks;
    private double energyBias;

    public MagicOrbEffect(final VFXManager manager, final Location origin) {
        super(manager, origin);
        setDuration(100);
        setRenderDistance(72.0D);
        setBoundingRadius(7.0D);
    }

    @Override
    protected void onStart() {
        final VFXQuality quality = quality();
        final int ringCount = switch (quality) {
            case LOW -> 6;
            case MEDIUM -> 7;
            case HIGH -> 8;
            case ULTRA -> 10;
        };

        this.rings.clear();
        this.corePoints.clear();
        this.shellAnchors.clear();
        this.trailAnchors.clear();

        for (int index = 0; index < ringCount; index++) {
            final int points = quality.scalePoints(22 + this.random.nextInt(8));
            final double radiusX = this.random.nextDouble(0.48D, 1.12D);
            final double radiusZ = this.random.nextDouble(0.48D, 1.12D);
            final Vector baseRotation = new Vector(
                    this.random.nextDouble(0.0D, VFXMath.TAU),
                    this.random.nextDouble(0.0D, VFXMath.TAU),
                    this.random.nextDouble(0.0D, VFXMath.TAU)
            );
            final Vector rotationSpeed = new Vector(
                    this.random.nextDouble(-0.05D, 0.05D),
                    this.random.nextDouble(-0.08D, 0.08D),
                    this.random.nextDouble(-0.05D, 0.05D)
            );
            final Color color = PALETTE.get(index % PALETTE.size());
            this.rings.add(new RingDefinition(
                    VFXMath.ellipse(radiusX, radiusZ, points),
                    baseRotation,
                    rotationSpeed,
                    this.random.nextDouble(0.04D, 0.16D),
                    this.random.nextDouble(0.04D, 0.22D),
                    color,
                    0.9F + this.random.nextFloat() * 0.8F,
                    this.random.nextDouble(0.0D, VFXMath.TAU)
            ));
        }

        this.corePoints.addAll(VFXMath.randomSphereCloud(1.0D, quality.scalePoints(26)));
        this.shellAnchors.addAll(VFXMath.sphere(1.0D, quality.scalePoints(16)));
        this.trailAnchors.addAll(VFXMath.circle(1.0D, switch (quality) {
            case LOW -> 3;
            case MEDIUM -> 4;
            case HIGH -> 5;
            case ULTRA -> 6;
        }));

        timeline()
                .at(0, () -> this.energyBias = 0.25D)
                .at(Math.max(10, duration() / 4), () -> this.energyBias = 0.55D)
                .at(Math.max(30, duration() / 2), () -> this.energyBias = 0.9D)
                .at(Math.max(50, duration() - 14), () -> this.flashTicks = 6)
                .every(10, duration() - 6, 14, () -> this.flashTicks = Math.max(this.flashTicks, 2));
    }

    @Override
    protected void onTick() {
        if (this.flashTicks > 0) {
            this.flashTicks--;
        }

        final double chargedProgress = Math.min(1.0D, progress() / 0.72D);
        final double radius = Easing.BACK_OUT.apply(chargedProgress, 0.0D, 5.0D);
        final double pulse = 1.0D + (Math.sin((age() * 0.27D) + this.energyBias) * 0.07D) + (this.energyBias * 0.04D);
        setBoundingRadius(radius * pulse + 2.0D);
    }

    @Override
    protected void onRender(final VFXContext context) {
        final World world = context.world();
        final List<Player> viewers = context.viewersWithinRange();
        if (viewers.isEmpty()) {
            return;
        }

        final double chargeProgress = Math.min(1.0D, progress() / 0.72D);
        final double radiusBase = Easing.BACK_OUT.apply(chargeProgress, 0.0D, 5.0D);
        final double collapseProgress = Math.clamp((progress() - 0.88D) / 0.12D, 0.0D, 1.0D);
        final double collapseScale = 1.0D - (Easing.EASE_IN.apply(collapseProgress, 0.0D, 0.25D));
        final double pulse = 1.0D + (Math.sin((age() * 0.27D) + this.energyBias) * 0.08D);
        final double radius = radiusBase * pulse * collapseScale;

        renderRings(context, world, viewers, radius);
        renderCore(context, world, viewers, radius);
        renderOrbitingSparks(context, world, viewers, radius);
        renderElectricalShell(context, world, viewers, radius);
        renderOuterFragments(context, world, viewers, radius);
        renderArcs(context, world, viewers, radius);
        renderUndersideDrips(context, world, viewers, radius);
        renderVerticalTrails(context, world, viewers, radius);

        if (this.flashTicks > 0 || (context.quality().tertiaryEffects() && (age() % 18 == 0))) {
            for (final Player viewer : viewers) {
                viewer.spawnParticle(
                        Particle.FLASH,
                        originMutable().getX(),
                        originMutable().getY(),
                        originMutable().getZ(),
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D,
                        null,
                        true
                );
            }
        }
    }

    @Override
    protected void onStop() {
        final World world = world();
        if (world == null) {
            return;
        }

        final List<Player> viewers = manager().resolveViewers(this);
        if (viewers.isEmpty()) {
            return;
        }

        final List<Vector> burst = VFXMath.randomSphereCloud(2.4D, quality().scalePoints(48));
        VFXRenderer.particles(burst)
                .particle(Particle.DUST)
                .data(VFXColor.transition(VFXColor.rgb(255, 255, 255), VFXColor.rgb(120, 80, 255), 1.8F))
                .force(true)
                .render(world, originMutable(), viewers, quality());

        VFXRenderer.particles(VFXMath.randomSphereCloud(3.1D, quality().scalePoints(26)))
                .particle(Particle.ELECTRIC_SPARK)
                .randomChance(0.85D)
                .force(true)
                .render(world, originMutable(), viewers, quality());

        for (final Player viewer : viewers) {
            viewer.spawnParticle(
                    Particle.FLASH,
                    originMutable().getX(),
                    originMutable().getY(),
                    originMutable().getZ(),
                    2,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.0D,
                    null,
                    true
            );
        }
    }

    @Override
    protected List<Vector> debugPoints() {
        return this.trailAnchors.stream().map(anchor -> anchor.clone().multiply(5.0D)).toList();
    }

    private void renderRings(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        for (int ringIndex = 0; ringIndex < this.rings.size(); ringIndex++) {
            final RingDefinition ring = this.rings.get(ringIndex);
            final List<Vector> transformed = new ArrayList<>(ring.basePoints().size());

            for (int pointIndex = 0; pointIndex < ring.basePoints().size(); pointIndex++) {
                final Vector point = ring.basePoints().get(pointIndex);
                final double time = age() + (ringIndex * 3.0D);
                final double distortion = 1.0D
                        + (Math.sin((pointIndex * 0.42D) + (time * 0.11D) + ring.phase()) * ring.distortion())
                        + (Math.cos((point.getX() * 2.4D) + (time * 0.07D)) * ring.distortion() * 0.55D);

                Vector result = new Vector(
                        point.getX() * distortion,
                        Math.sin((time * 0.12D) + pointIndex + ring.phase()) * ring.verticalWarp(),
                        point.getZ() * distortion
                );

                result = VFXMath.rotateX(result, ring.baseRotation().getX() + (ring.rotationSpeed().getX() * age()));
                result = VFXMath.rotateY(result, ring.baseRotation().getY() + (ring.rotationSpeed().getY() * age()));
                result = VFXMath.rotateZ(result, ring.baseRotation().getZ() + (ring.rotationSpeed().getZ() * age()));
                result.multiply(radius);
                transformed.add(result);
            }

            VFXRenderer.particles(transformed)
                    .particle(Particle.DUST)
                    .data(VFXColor.dust(ring.color(), ring.size()))
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
        }
    }

    private void renderCore(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        final double coreRadius = Math.max(0.4D, (radius * 0.18D) + (Math.sin(age() * 0.35D) * 0.08D));
        final List<Vector> scaledCore = new ArrayList<>(this.corePoints.size());
        for (final Vector point : this.corePoints) {
            scaledCore.add(point.clone().multiply(coreRadius));
        }

        VFXRenderer.particles(scaledCore)
                .particle(Particle.DUST_COLOR_TRANSITION)
                .data(VFXColor.transition(VFXColor.rgb(255, 255, 255), VFXColor.rgb(150, 120, 255), 1.6F))
                .force(true)
                .render(world, originMutable(), viewers, context.quality());
    }

    private void renderOrbitingSparks(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        final int sparkCount = switch (context.quality()) {
            case LOW -> 4;
            case MEDIUM -> 5;
            case HIGH -> 7;
            case ULTRA -> 9;
        };

        for (int index = 0; index < sparkCount; index++) {
            Vector spark = this.shellAnchors.get(index % this.shellAnchors.size()).clone();
            spark = VFXMath.rotateY(spark, (age() * 0.07D) + (index * 0.9D));
            spark = VFXMath.rotateX(spark, (age() * 0.05D) + (index * 0.45D));
            spark.multiply(radius * (0.55D + (0.08D * Math.sin((age() * 0.23D) + index))));

            VFXRenderer.particles(List.of(spark))
                    .particle(Particle.END_ROD)
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
        }
    }

    private void renderElectricalShell(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        final int sparks = switch (context.quality()) {
            case LOW -> 4;
            case MEDIUM -> 6;
            case HIGH -> 8;
            case ULTRA -> 10;
        };

        final List<Vector> points = new ArrayList<>(sparks);
        for (int index = 0; index < sparks; index++) {
            Vector shell = this.shellAnchors.get((index + age()) % this.shellAnchors.size()).clone();
            shell = VFXMath.rotateAroundAxis(shell, new Vector(1.0D, 1.0D, 0.4D), age() * 0.03D);
            shell.multiply(radius * this.random.nextDouble(0.85D, 1.15D));
            points.add(shell);
        }

        VFXRenderer.particles(points)
                .particle(Particle.ELECTRIC_SPARK)
                .randomChance(0.8D)
                .force(true)
                .render(world, originMutable(), viewers, context.quality());
    }

    private void renderOuterFragments(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        if (!context.quality().secondaryEffects()) {
            return;
        }

        final List<Vector> fragments = new ArrayList<>();
        final int count = context.quality().scalePoints(12);
        for (int index = 0; index < count; index++) {
            Vector point = this.shellAnchors.get(index % this.shellAnchors.size()).clone();
            point = VFXMath.rotateY(point, age() * 0.05D);
            point.multiply(radius * this.random.nextDouble(1.02D, 1.35D));
            point.setY(point.getY() + this.random.nextDouble(-0.35D, 0.35D));
            fragments.add(point);
        }

        VFXRenderer.particles(fragments)
                .particle(Particle.ENCHANT)
                .randomChance(0.72D)
                .force(true)
                .render(world, originMutable(), viewers, context.quality());
    }

    private void renderArcs(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        if (!context.quality().secondaryEffects()) {
            return;
        }

        final int arcCount = context.quality() == VFXQuality.ULTRA ? 3 : 2;
        for (int index = 0; index < arcCount; index++) {
            final Vector start = rotatedShellPoint(radius, (index * 0.7D) + (age() * 0.08D));
            final Vector end = rotatedShellPoint(radius, (index * 1.05D) - (age() * 0.06D));
            final Vector controlA = start.clone().multiply(0.45D).add(new Vector(0.0D, radius * 0.28D, 0.0D));
            final Vector controlB = end.clone().multiply(0.45D).add(new Vector(0.0D, -radius * 0.18D, 0.0D));

            VFXRenderer.particles(VFXMath.bezier(start, controlA, controlB, end, context.quality().scalePoints(14)))
                    .particle(Particle.DUST)
                    .data(VFXColor.transition(VFXColor.rgb(255, 255, 255), VFXColor.rgb(90, 220, 255), 1.0F))
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
        }
    }

    private void renderUndersideDrips(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        final int drips = switch (context.quality()) {
            case LOW -> 2;
            case MEDIUM -> 3;
            case HIGH -> 4;
            case ULTRA -> 5;
        };

        for (int index = 0; index < drips; index++) {
            final double angle = ((VFXMath.TAU / drips) * index) + (age() * 0.03D);
            final Vector anchor = new Vector(Math.cos(angle) * radius * 0.35D, -radius * 0.58D, Math.sin(angle) * radius * 0.35D);
            final Vector end = anchor.clone().add(new Vector(0.0D, -1.2D - (radius * 0.08D), 0.0D));

            VFXRenderer.particles(VFXMath.line(anchor, end, context.quality().scalePoints(6)))
                    .particle(Particle.DUST)
                    .data(VFXColor.dust(VFXColor.rgb(130, 90, 255), 0.8F))
                    .randomChance(0.85D)
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
        }
    }

    private void renderVerticalTrails(final VFXContext context, final World world, final List<Player> viewers, final double radius) {
        if (!context.quality().tertiaryEffects()) {
            return;
        }

        for (int index = 0; index < this.trailAnchors.size(); index++) {
            Vector anchor = this.trailAnchors.get(index).clone();
            anchor = VFXMath.rotateY(anchor, (age() * 0.02D) + (index * 0.6D));
            anchor.multiply(radius * this.random.nextDouble(0.58D, 0.9D));
            final double sway = Math.sin((age() * 0.16D) + index) * 0.25D;
            final Vector start = anchor.clone().add(new Vector(sway, -radius * 0.65D, -sway));
            final Vector end = anchor.clone().add(new Vector(-sway, radius * 0.72D, sway));

            VFXRenderer.particles(VFXMath.line(start, end, context.quality().scalePoints(9)))
                    .particle(Particle.DUST)
                    .data(VFXColor.dust(PALETTE.get(index % PALETTE.size()), 0.9F))
                    .randomChance(0.75D)
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
        }
    }

    private Vector rotatedShellPoint(final double radius, final double phase) {
        Vector point = this.shellAnchors.get(this.random.nextInt(this.shellAnchors.size())).clone();
        point = VFXMath.rotateY(point, phase);
        point = VFXMath.rotateX(point, phase * 0.67D);
        point.multiply(radius * this.random.nextDouble(0.72D, 1.0D));
        return point;
    }

    private record RingDefinition(
            List<Vector> basePoints,
            Vector baseRotation,
            Vector rotationSpeed,
            double distortion,
            double verticalWarp,
            Color color,
            float size,
            double phase
    ) {
    }
}
