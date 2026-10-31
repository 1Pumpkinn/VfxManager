package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class SphereShape implements VFXShape {
    private final double radius;
    private final int pointCount;
    private final boolean hemisphere;

    public SphereShape(final double radius, final int pointCount) {
        this(radius, pointCount, false);
    }

    public SphereShape(final double radius, final int pointCount, final boolean hemisphere) {
        this.radius = radius;
        this.pointCount = pointCount;
        this.hemisphere = hemisphere;
    }

    @Override
    public List<Vector> points() {
        return this.hemisphere
                ? VFXMath.hemisphere(this.radius, this.pointCount)
                : VFXMath.sphere(this.radius, this.pointCount);
    }
}
