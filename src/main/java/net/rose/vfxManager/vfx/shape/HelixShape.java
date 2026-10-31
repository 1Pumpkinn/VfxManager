package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

public final class HelixShape implements VFXShape {
    private final double radius;
    private final double height;
    private final int turns;
    private final int pointCount;

    public HelixShape(final double radius, final double height, final int turns, final int pointCount) {
        this.radius = radius;
        this.height = height;
        this.turns = turns;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.helix(this.radius, this.height, this.turns, this.pointCount);
    }
}
