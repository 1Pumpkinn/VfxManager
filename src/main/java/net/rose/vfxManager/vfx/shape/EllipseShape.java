package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Flat ellipse on the XZ plane. Generates points only; render them with particles or Displays. */
public final class EllipseShape implements VFXShape {
    private final double radiusX;
    private final double radiusZ;
    private final int pointCount;

    public EllipseShape(final double radiusX, final double radiusZ, final int pointCount) {
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.ellipse(this.radiusX, this.radiusZ, this.pointCount);
    }
}
