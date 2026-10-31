package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Hollow cone with its base at y = 0 and its tip at y = height. Generates points only; render them with particles or Displays. */
public final class ConeShape implements VFXShape {
    private final double baseRadius;
    private final double height;
    private final int radialPoints;
    private final int heightSegments;

    public ConeShape(final double baseRadius, final double height, final int radialPoints, final int heightSegments) {
        this.baseRadius = baseRadius;
        this.height = height;
        this.radialPoints = radialPoints;
        this.heightSegments = heightSegments;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.cone(this.baseRadius, this.height, this.radialPoints, this.heightSegments);
    }
}
