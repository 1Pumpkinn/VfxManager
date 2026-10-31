package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class BezierShape implements VFXShape {
    private final Vector p0;
    private final Vector p1;
    private final Vector p2;
    private final Vector p3;
    private final int pointCount;

    public BezierShape(final Vector p0, final Vector p1, final Vector p2, final Vector p3, final int pointCount) {
        this.p0 = p0.clone();
        this.p1 = p1.clone();
        this.p2 = p2.clone();
        this.p3 = p3.clone();
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.bezier(this.p0, this.p1, this.p2, this.p3, this.pointCount);
    }
}
