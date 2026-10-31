package net.rose.vfxManager.vfx.shape;

import org.bukkit.util.Vector;

import java.util.List;

@FunctionalInterface
public interface VFXShape {
    List<Vector> points();
}
