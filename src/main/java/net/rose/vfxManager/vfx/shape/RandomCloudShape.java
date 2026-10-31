package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Random points inside a sphere. Re-generates new random points on every call to points(). Generates points only; render them with particles or Displays. */
public final class RandomCloudShape implements VFXShape {
    private final double radius;
    private final int pointCount;

    public RandomCloudShape(final double radius, final int pointCount) {
        this.radius = radius;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.randomSphereCloud(this.radius, this.pointCount);
    }
}
