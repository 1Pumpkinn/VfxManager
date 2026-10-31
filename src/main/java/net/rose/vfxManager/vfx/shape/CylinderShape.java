package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Hollow cylinder from y = 0 to y = height. Generates points only; render them with particles or Displays. */
public final class CylinderShape implements VFXShape {
    private final double radius;
    private final double height;
    private final int radialPoints;
    private final int heightSegments;

    public CylinderShape(final double radius, final double height, final int radialPoints, final int heightSegments) {
        this.radius = radius;
        this.height = height;
        this.radialPoints = radialPoints;
        this.heightSegments = heightSegments;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.cylinder(this.radius, this.height, this.radialPoints, this.heightSegments);
    }
}
