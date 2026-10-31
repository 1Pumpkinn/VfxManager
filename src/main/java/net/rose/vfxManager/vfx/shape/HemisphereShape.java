package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Upper half of a sphere (y >= 0). Generates points only; render them with particles or Displays. */
public final class HemisphereShape implements VFXShape {
    private final double radius;
    private final int pointCount;

    public HemisphereShape(final double radius, final int pointCount) {
        this.radius = radius;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.hemisphere(this.radius, this.pointCount);
    }
}
