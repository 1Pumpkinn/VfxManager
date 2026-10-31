package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class LineShape implements VFXShape {
    private final Vector start;
    private final Vector end;
    private final int pointCount;

    public LineShape(final Vector start, final Vector end, final int pointCount) {
        this.start = start.clone();
        this.end = end.clone();
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.line(this.start, this.end, this.pointCount);
    }
}
