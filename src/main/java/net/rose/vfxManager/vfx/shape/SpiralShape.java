package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.List;

/** Spiral whose radius changes from start to end while rising by height. Generates points only; render them with particles or Displays. */
public final class SpiralShape implements VFXShape {
    private final double startRadius;
    private final double endRadius;
    private final double height;
    private final int turns;
    private final int pointCount;

    public SpiralShape(final double startRadius, final double endRadius, final double height, final int turns, final int pointCount) {
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.height = height;
        this.turns = turns;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points() {
        return VFXMath.spiral(this.startRadius, this.endRadius, this.height, this.turns, this.pointCount);
    }
}
