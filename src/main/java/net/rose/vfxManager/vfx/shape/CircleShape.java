package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class CircleShape implements VFXShape {
    private final double radiusX;
    private final double radiusZ;
    private final int pointCount;

    public CircleShape(final double radius, final int pointCount) {
        this(radius, radius, pointCount);
    }

    public CircleShape(final double radiusX, final double radiusZ, final int pointCount) {
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.ellipse(this.radiusX, this.radiusZ, this.pointCount);
    }
}
