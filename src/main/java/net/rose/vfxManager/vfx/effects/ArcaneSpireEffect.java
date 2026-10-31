package net.rose.vfxManager.vfx.effects;

import net.rose.vfxManager.vfx.VFXColor;
import net.rose.vfxManager.vfx.VFXContext;
import net.rose.vfxManager.vfx.VFXEffect;
import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXMath;
import net.rose.vfxManager.vfx.VFXQuality;
import net.rose.vfxManager.vfx.VFXRenderer;
import net.rose.vfxManager.vfx.animation.Easing;
import net.rose.vfxManager.vfx.shape.PolyhedronShape;
import net.rose.vfxManager.vfx.shape.RayConeShape;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Arcane spire: a cinematic "sky cannon" effect.
 * <ul>
 *   <li>two counter-rotating wireframe polyhedra at the top (the origin),</li>
 *   <li>a bright white orb circling them,</li>
 *   <li>a funnel of tilted, spinning rings that shrink toward the ground (the top rings are star polygons),</li>
 *   <li>a cone of dotted rays flowing down and converging on a point {@code height} blocks below,</li>
 *   <li>flickering L-shaped rune fragments drifting at both sides,</li>
 *   <li>a shockwave burst at the impact point when it ends.</li>
 * </ul>
 * <pre>{@code
 * vfx.arcaneSpire(location)      // location = top of the spire
 *         .radius(2.2)
 *         .height(14)
 *         .color(255, 110, 200)
 *         .duration(140)
 *         .play();
 * }</pre>
 * Configure {@code radius}, {@code height} and {@code color} before {@code play()}: the geometry is built once in
 * {@link #onStart()} and the render loop only rotates preallocated buffers in place.
 */
public final class ArcaneSpireEffect extends VFXEffect {
    private static final Color WHITE = VFXColor.rgb(255, 255, 255);
    /** Local shape of one rune fragment: an "L" made of five points (unit size, in the local XY plane). */
    private static final double[][] GLYPH = {{0.0D, 0.0D}, {0.0D, 0.5D}, {0.0D, 1.0D}, {0.5D, 0.0D}, {1.0D, 0.0D}};

    private double radius = 2.2D;
    private double height = 12.0D;
    private Color color = VFXColor.rgb(255, 110, 200);

    private PolyhedronShape outerShape;
    private PolyhedronShape innerShape;
    private List<Vector> outerBase;
    private List<Vector> outerWork;
    private List<Vector> innerBase;
    private List<Vector> innerWork;

    private final List<Ring> rings = new ArrayList<>();

    private RayConeShape rays;
    private List<Vector> rayWork;
    private double rayTopY;

    private List<Vector> orbBase;
    private List<Vector> orbWork;

    private int fragmentCount;
    private double[] fragmentX;
    private double[] fragmentY;
    private double[] fragmentZ;
    private double[] fragmentYaw;
    private double[] fragmentPitch;
    private double[] fragmentSize;
    private int[] fragmentCycle;
    private int[] fragmentPhase;
    private List<Vector> fragmentWork;
    private final List<Vector> fragmentVisible = new ArrayList<>();

    public ArcaneSpireEffect(final VFXManager manager, final Location origin) {
        super(manager, origin);
        setDuration(140);
        setRenderDistance(96.0D);
    }

    // ---- fluent configuration ----

    public ArcaneSpireEffect radius(final double radius) {
        this.radius = Math.max(0.5D, radius);
        return this;
    }

    public ArcaneSpireEffect height(final double height) {
        this.height = Math.max(3.0D, height);
        return this;
    }

    public ArcaneSpireEffect color(final int red, final int green, final int blue) {
        this.color = VFXColor.rgb(red, green, blue);
        return this;
    }

    public ArcaneSpireEffect color(final Color color) {
        this.color = color;
        return this;
    }

    public ArcaneSpireEffect duration(final int ticks) {
        setDuration(ticks);
        return this;
    }

    public ArcaneSpireEffect follow(final Entity entity, final Vector offset) {
        attach(entity);
        setOffset(offset);
        return this;
    }

    public ArcaneSpireEffect play() {
        start();
        return this;
    }

    // ---- lifecycle ----

    @Override
    protected void onStart() {
        final VFXQuality quality = quality();
        setBoundingRadius((this.height / 2.0D) + this.radius);

        // Polyhedra: icosahedron outside, dodecahedron inside; fewer points on the inner one.
        final int edgePoints = quality.scalePoints(5);
        this.outerShape = new PolyhedronShape(PolyhedronShape.Type.ICOSAHEDRON, this.radius, edgePoints);
        this.innerShape = new PolyhedronShape(PolyhedronShape.Type.DODECAHEDRON, this.radius * 0.62D, Math.max(2, edgePoints - 2));
        this.outerBase = this.outerShape.points();
        this.innerBase = this.innerShape.points();
        this.outerWork = VFXMath.copyOf(this.outerBase);
        this.innerWork = VFXMath.copyOf(this.innerBase);

        // Orb.
        this.orbBase = VFXMath.randomSphereCloud(1.0D, quality.scalePoints(10));
        this.orbWork = VFXMath.copyOf(this.orbBase);

        buildRings(quality);
        buildRays(quality);
        buildFragments(quality);
    }

    private void buildRings(final VFXQuality quality) {
        this.rings.clear();
        final int ringCount = switch (quality) {
            case LOW -> 4;
            case MEDIUM -> 5;
            case HIGH -> 7;
            case ULTRA -> 8;
        };

        for (int index = 0; index < ringCount; index++) {
            final double fraction = (index + 1.0D) / ringCount;
            final double ringRadius = this.radius * 1.7D * (1.0D - (fraction * 0.88D));
            final double y = -(this.radius * 1.5D) - (this.height * 0.5D * Math.pow(fraction, 1.4D));

            // The widest two rings are star polygons (heptagrams) for the overlapping-lines look.
            final boolean star = index < 2 && quality.secondaryEffects();
            final List<Vector> base = star
                    ? starPolygon(ringRadius, 7, 2, quality.scalePoints(6))
                    : VFXMath.ellipse(ringRadius, ringRadius * 0.78D, quality.scalePoints((int) (ringRadius * 14.0D) + 10));

            final double direction = (index % 2 == 0) ? 1.0D : -1.0D;
            this.rings.add(new Ring(
                    base,
                    VFXMath.copyOf(base),
                    y,
                    0.12D + (0.28D * fraction),
                    direction * (0.03D + (0.025D * index)),
                    index * 1.7D
            ));
        }
    }

    private void buildRays(final VFXQuality quality) {
        this.rayTopY = -(this.radius * 1.6D);
        final double depth = Math.max(2.0D, this.height - (this.radius * 1.6D));
        this.rays = new RayConeShape(this.radius * 0.9D, depth, quality.scalePoints(26), quality.scalePoints(11));
        this.rayWork = this.rays.newBuffer();
    }

    private void buildFragments(final VFXQuality quality) {
        // Rune fragments are secondary detail: skipped on LOW.
        this.fragmentCount = quality.secondaryEffects() ? quality.scalePoints(14) : 0;
        this.fragmentX = new double[this.fragmentCount];
        this.fragmentY = new double[this.fragmentCount];
        this.fragmentZ = new double[this.fragmentCount];
        this.fragmentYaw = new double[this.fragmentCount];
        this.fragmentPitch = new double[this.fragmentCount];
        this.fragmentSize = new double[this.fragmentCount];
        this.fragmentCycle = new int[this.fragmentCount];
        this.fragmentPhase = new int[this.fragmentCount];

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < this.fragmentCount; index++) {
            final double side = (index % 2 == 0) ? 1.0D : -1.0D;
            this.fragmentX[index] = side * ((this.radius * 2.0D) + random.nextDouble(0.0D, 2.2D));
            this.fragmentY[index] = -random.nextDouble(this.radius, Math.max(this.radius + 1.0D, this.height * 0.5D));
            this.fragmentZ[index] = random.nextDouble(-2.0D, 2.0D);
            this.fragmentYaw[index] = random.nextDouble(0.0D, VFXMath.TAU);
            this.fragmentPitch[index] = random.nextDouble(-0.9D, 0.9D);
            this.fragmentSize[index] = random.nextDouble(0.14D, 0.3D);
            this.fragmentCycle[index] = random.nextInt(20, 41);
            this.fragmentPhase[index] = random.nextInt(0, 40);
        }

        this.fragmentWork = new ArrayList<>(this.fragmentCount * GLYPH.length);
        for (int index = 0; index < this.fragmentCount * GLYPH.length; index++) {
            this.fragmentWork.add(new Vector());
        }
    }

    @Override
    protected void onRender(final VFXContext context) {
        final List<Player> viewers = context.viewersWithinRange();
        if (viewers.isEmpty()) {
            return;
        }

        final double progress = progress();
        // Polyhedra pop in with overshoot; everything fades out over the last 10%.
        final double fade = progress > 0.9D ? Easing.EASE_IN.apply((progress - 0.9D) / 0.1D, 1.0D, 0.0D) : 1.0D;
        final double grow = Easing.BACK_OUT.apply(Math.min(1.0D, progress / 0.18D), 0.0D, 1.0D) * fade;
        // The funnel and rays extend downward after the polyhedra appear.
        final double funnel = Easing.EASE_OUT.apply(Math.clamp((progress - 0.08D) / 0.3D, 0.0D, 1.0D), 0.0D, 1.0D) * fade;

        final World world = context.world();
        renderPolyhedra(context, world, viewers, Math.max(0.0D, grow));
        renderOrb(context, world, viewers, Math.max(0.0D, grow));
        if (funnel > 0.02D) {
            renderRings(context, world, viewers, funnel);
            renderRays(context, world, viewers, funnel);
        }
        if (progress > 0.3D) {
            renderFragments(context, world, viewers, fade);
        }

        if (context.quality().tertiaryEffects() && age() % 25 == 0) {
            final Location origin = originMutable();
            for (final Player viewer : viewers) {
                viewer.spawnParticle(Particle.FLASH, origin.getX(), origin.getY(), origin.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D, WHITE, true);
            }
        }
    }

    private void renderPolyhedra(final VFXContext context, final World world, final List<Player> viewers, final double scale) {
        if (scale <= 0.0D) {
            return;
        }

        final double time = age();
        final double pulse = 1.0D + (Math.sin(time * 0.2D) * 0.03D);

        transformInto(this.outerBase, this.outerWork, time * 0.035D, (time * 0.02D) + 0.6D, time * 0.015D, scale * pulse, 0.0D, 0.0D, 0.0D);
        VFXRenderer.particles(this.outerWork).color(this.color, 1.0F).force(true).render(world, originMutable(), viewers, context.quality());

        transformInto(this.innerBase, this.innerWork, -time * 0.05D, -time * 0.03D, (time * 0.02D) + 1.0D, scale * pulse, 0.0D, 0.0D, 0.0D);
        VFXRenderer.particles(this.innerWork)
                .color(VFXColor.lerp(this.color, WHITE, 0.55D), 0.8F)
                .force(true)
                .render(world, originMutable(), viewers, context.quality());
    }

    private void renderOrb(final VFXContext context, final World world, final List<Player> viewers, final double scale) {
        if (scale <= 0.0D) {
            return;
        }

        final double time = age();
        final double orbit = time * 0.06D;
        final double distance = this.radius * 1.2D * scale;
        final double centerX = Math.cos(orbit) * distance;
        final double centerY = Math.sin(time * 0.08D) * this.radius * 0.25D * scale;
        final double centerZ = Math.sin(orbit) * distance;
        final double orbRadius = (0.28D + (Math.sin(time * 0.4D) * 0.05D)) * scale;

        for (int index = 0; index < this.orbBase.size(); index++) {
            final Vector base = this.orbBase.get(index);
            this.orbWork.get(index)
                    .setX(centerX + (base.getX() * orbRadius))
                    .setY(centerY + (base.getY() * orbRadius))
                    .setZ(centerZ + (base.getZ() * orbRadius));
        }

        VFXRenderer.particles(this.orbWork).color(WHITE, 1.8F).force(true).render(world, originMutable(), viewers, context.quality());
    }

    private void renderRings(final VFXContext context, final World world, final List<Player> viewers, final double funnel) {
        final double time = age();
        for (final Ring ring : this.rings) {
            final double tiltX = Math.sin((time * 0.05D) + ring.phase()) * ring.tilt();
            final double tiltZ = Math.cos((time * 0.04D) + (ring.phase() * 1.3D)) * ring.tilt();
            // Spin first (around the ring's own axis), then tilt, so the rings wobble instead of orbiting.
            transformInto(ring.base(), ring.work(), (time * ring.spin()) + ring.phase(), tiltX, tiltZ, funnel, 0.0D, ring.y() * funnel, 0.0D, true);
            VFXRenderer.particles(ring.work()).color(this.color, 0.9F).force(true).render(world, originMutable(), viewers, context.quality());
        }
    }

    private void renderRays(final VFXContext context, final World world, final List<Player> viewers, final double funnel) {
        this.rays.flowInto(this.rayWork, age() * 0.35D, funnel, age() * 0.01D, this.rayTopY * funnel);
        VFXRenderer.particles(this.rayWork)
                .particle(Particle.DUST_COLOR_TRANSITION)
                .data(VFXColor.transition(this.color, WHITE, 0.7F))
                .randomChance(0.6D)
                .force(true)
                .render(world, originMutable(), viewers, context.quality());
    }

    private void renderFragments(final VFXContext context, final World world, final List<Player> viewers, final double scale) {
        if (this.fragmentCount == 0 || scale <= 0.0D) {
            return;
        }

        final double spin = age() * 0.012D;
        final double cosSpin = Math.cos(spin);
        final double sinSpin = Math.sin(spin);

        this.fragmentVisible.clear();
        int slot = 0;
        for (int index = 0; index < this.fragmentCount; index++) {
            final int cycle = this.fragmentCycle[index];
            final int life = (age() + this.fragmentPhase[index]) % cycle;
            if (life > (cycle * 0.65D)) {
                continue; // hidden part of this fragment's flicker cycle
            }

            // Centre: anchor rotated slowly around the spire axis, drifting upward over its life.
            final double centerX = (this.fragmentX[index] * cosSpin) + (this.fragmentZ[index] * sinSpin);
            final double centerZ = (-this.fragmentX[index] * sinSpin) + (this.fragmentZ[index] * cosSpin);
            final double centerY = this.fragmentY[index] + (life * 0.03D);

            final double cosPitch = Math.cos(this.fragmentPitch[index]);
            final double sinPitch = Math.sin(this.fragmentPitch[index]);
            final double yaw = this.fragmentYaw[index] + spin;
            final double cosYaw = Math.cos(yaw);
            final double sinYaw = Math.sin(yaw);
            final double size = this.fragmentSize[index] * scale;

            for (final double[] glyph : GLYPH) {
                final double localX = glyph[0] * size;
                final double localY = glyph[1] * size;
                // Pitch around X, then yaw around Y, so each glyph lies in its own random plane.
                final double y1 = localY * cosPitch;
                final double z1 = localY * sinPitch;
                final double x2 = (localX * cosYaw) + (z1 * sinYaw);
                final double z2 = (-localX * sinYaw) + (z1 * cosYaw);

                final Vector point = this.fragmentWork.get(slot++);
                point.setX(centerX + x2).setY(centerY + y1).setZ(centerZ + z2);
                this.fragmentVisible.add(point);
            }
        }

        if (!this.fragmentVisible.isEmpty()) {
            VFXRenderer.particles(this.fragmentVisible)
                    .color(VFXColor.lerp(this.color, WHITE, 0.25D), 0.8F)
                    .force(true)
                    .render(world, originMutable(), viewers, context.quality());
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

        // Impact burst where the rays converged.
        final Location impact = origin().add(0.0D, -this.height, 0.0D);
        final VFXQuality quality = quality();

        VFXRenderer.particles(VFXMath.randomSphereCloud(2.6D, quality.scalePoints(40)))
                .particle(Particle.DUST_COLOR_TRANSITION)
                .data(VFXColor.transition(WHITE, this.color, 1.8F))
                .force(true)
                .render(world, impact, viewers, quality);

        VFXRenderer.particles(VFXMath.circle(3.5D, quality.scalePoints(36)))
                .color(this.color, 1.4F)
                .force(true)
                .render(world, impact, viewers, quality);

        VFXRenderer.particles(VFXMath.randomSphereCloud(3.2D, quality.scalePoints(24)))
                .particle(Particle.ELECTRIC_SPARK)
                .randomChance(0.85D)
                .force(true)
                .render(world, impact, viewers, quality);

        for (final Player viewer : viewers) {
            viewer.spawnParticle(Particle.FLASH, impact.getX(), impact.getY(), impact.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D, WHITE, true);
        }
    }

    @Override
    protected List<Vector> debugPoints() {
        return List.of(new Vector(0.0D, -this.height, 0.0D), new Vector(0.0D, this.rayTopY, 0.0D));
    }

    // ---- helpers ----

    /** Rotates X, then Y, then Z (matches VFXMath.rotateX/Y/Z order), scales, translates, into {@code work}. */
    private static void transformInto(
            final List<Vector> base, final List<Vector> work,
            final double angleX, final double angleY, final double angleZ,
            final double scale, final double offsetX, final double offsetY, final double offsetZ
    ) {
        transformInto(base, work, angleX, angleY, angleZ, scale, offsetX, offsetY, offsetZ, false);
    }

    /**
     * In-place friendly transform into a preallocated buffer.
     *
     * @param spinFirst if true the arguments are (spinY, tiltX, tiltZ) and are applied in the order Y, X, Z; otherwise
     *                  they are (angleX, angleY, angleZ) applied in the order X, Y, Z
     */
    private static void transformInto(
            final List<Vector> base, final List<Vector> work,
            final double first, final double second, final double third,
            final double scale, final double offsetX, final double offsetY, final double offsetZ,
            final boolean spinFirst
    ) {
        final double angleX = spinFirst ? second : first;
        final double angleY = spinFirst ? first : second;
        final double angleZ = third;
        final double cosX = Math.cos(angleX);
        final double sinX = Math.sin(angleX);
        final double cosY = Math.cos(angleY);
        final double sinY = Math.sin(angleY);
        final double cosZ = Math.cos(angleZ);
        final double sinZ = Math.sin(angleZ);

        for (int index = 0; index < base.size(); index++) {
            final Vector source = base.get(index);
            double x = source.getX();
            double y = source.getY();
            double z = source.getZ();

            if (spinFirst) {
                // Y (spin), then X (tilt), then Z (tilt).
                final double x1 = (x * cosY) + (z * sinY);
                final double z1 = (-x * sinY) + (z * cosY);
                final double y2 = (y * cosX) - (z1 * sinX);
                final double z2 = (y * sinX) + (z1 * cosX);
                final double x3 = (x1 * cosZ) - (y2 * sinZ);
                final double y3 = (x1 * sinZ) + (y2 * cosZ);
                x = x3;
                y = y3;
                z = z2;
            } else {
                // X, then Y, then Z.
                final double y1 = (y * cosX) - (z * sinX);
                final double z1 = (y * sinX) + (z * cosX);
                final double x2 = (x * cosY) + (z1 * sinY);
                final double z2 = (-x * sinY) + (z1 * cosY);
                final double x3 = (x2 * cosZ) - (y1 * sinZ);
                final double y3 = (x2 * sinZ) + (y1 * cosZ);
                x = x3;
                y = y3;
                z = z2;
            }

            work.get(index).setX((x * scale) + offsetX).setY((y * scale) + offsetY).setZ((z * scale) + offsetZ);
        }
    }

    /** Star polygon {vertices/step} on the XZ plane: corners once, plus evenly spaced points along each edge. */
    private static List<Vector> starPolygon(final double radius, final int vertices, final int step, final int pointsPerEdge) {
        final List<Vector> corners = new ArrayList<>(vertices);
        final List<Vector> points = new ArrayList<>(vertices * (1 + pointsPerEdge));
        for (int index = 0; index < vertices; index++) {
            final double angle = (VFXMath.TAU * index) / vertices;
            final Vector corner = new Vector(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            corners.add(corner);
            points.add(corner.clone());
        }

        for (int index = 0; index < vertices; index++) {
            final Vector from = corners.get(index);
            final Vector to = corners.get((index + step) % vertices);
            for (int point = 1; point <= pointsPerEdge; point++) {
                points.add(VFXMath.lerp(from, to, point / (double) (pointsPerEdge + 1)));
            }
        }
        return points;
    }

    private record Ring(List<Vector> base, List<Vector> work, double y, double tilt, double spin, double phase) {
    }
}
