package net.rose.vfxManager.vfx.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * A cone of straight rays: every ray starts on a circle of {@code topRadius} at the top and converges to a single
 * apex {@code depth} blocks below. Each ray is a row of dots, so the dots bunch up toward the apex like a focused
 * beam of light.
 * <p>
 * {@link #points()} returns the static cone. For animation, create one buffer with {@link #newBuffer()} and call
 * {@link #flowInto} every tick: it moves the dots along their rays and writes the result into the buffer without
 * allocating.
 */
public final class RayConeShape implements VFXShape {
    private final double topRadius;
    private final double depth;
    private final int rayCount;
    private final int dotsPerRay;
    private final double[] cosines;
    private final double[] sines;

    public RayConeShape(final double topRadius, final double depth, final int rayCount, final int dotsPerRay) {
        this.topRadius = Math.max(0.0D, topRadius);
        this.depth = Math.max(0.1D, depth);
        this.rayCount = Math.max(3, rayCount);
        this.dotsPerRay = Math.max(2, dotsPerRay);
        this.cosines = new double[this.rayCount];
        this.sines = new double[this.rayCount];
        for (int ray = 0; ray < this.rayCount; ray++) {
            final double angle = (Math.PI * 2.0D * ray) / this.rayCount;
            this.cosines[ray] = Math.cos(angle);
            this.sines[ray] = Math.sin(angle);
        }
    }

    public int rayCount() {
        return this.rayCount;
    }

    public int dotsPerRay() {
        return this.dotsPerRay;
    }

    /** A reusable list with one vector per dot, for {@link #flowInto}. */
    public List<Vector> newBuffer() {
        final int size = this.rayCount * this.dotsPerRay;
        final List<Vector> buffer = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            buffer.add(new Vector());
        }
        return buffer;
    }

    /**
     * Writes the animated cone into {@code out}.
     *
     * @param phase    flow position in dot spacings; increase it every tick to make the dots travel toward the apex
     * @param length   0.0 to 1.0 fraction of the full depth (use it to grow the cone downward)
     * @param rotation radians to rotate the ray ring around the Y axis
     * @param yOffset  Y of the top circle
     */
    public void flowInto(final List<Vector> out, final double phase, final double length, final double rotation, final double yOffset) {
        final double cosRotation = Math.cos(rotation);
        final double sinRotation = Math.sin(rotation);
        final double travel = this.depth * length;

        int slot = 0;
        for (int ray = 0; ray < this.rayCount; ray++) {
            final double topX = ((this.cosines[ray] * cosRotation) - (this.sines[ray] * sinRotation)) * this.topRadius;
            final double topZ = ((this.sines[ray] * cosRotation) + (this.cosines[ray] * sinRotation)) * this.topRadius;

            for (int dot = 0; dot < this.dotsPerRay; dot++) {
                final double t = ((dot + phase) / this.dotsPerRay) % 1.0D;
                out.get(slot++)
                        .setX(topX * (1.0D - t))
                        .setY(yOffset - (travel * t))
                        .setZ(topZ * (1.0D - t));
            }
        }
    }

    @Override
    public List<Vector> points() {
        final List<Vector> buffer = newBuffer();
        flowInto(buffer, 0.0D, 1.0D, 0.0D, 0.0D);
        return buffer;
    }
}
