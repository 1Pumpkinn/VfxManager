package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class ArcShape implements VFXShape {
    private final double radius;
    private final double startAngle;
    private final double endAngle;
    private final int pointCount;

    public ArcShape(final double radius, final double startAngle, final double endAngle, final int pointCount) {
        this.radius = radius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.arc(this.radius, this.startAngle, this.endAngle, this.pointCount);
    }
}
