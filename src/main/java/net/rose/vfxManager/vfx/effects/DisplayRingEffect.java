package net.rose.vfxManager.vfx.effects;

import net.rose.vfxManager.vfx.VFXDisplay;
import net.rose.vfxManager.vfx.VFXEffect;
import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXMath;
import net.rose.vfxManager.vfx.animation.Easing;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of the Display-entity renderer: a ring of {@link ItemDisplay}s orbiting and spinning around the origin.
 * <p>
 * The displays are spawned once in {@link #onStart()} and then only have their transformation updated, with a short
 * client-side interpolation, so motion is smooth without respawning anything. They are registered through
 * {@link #trackDisplay} and are removed automatically when the effect ends.
 * <pre>{@code
 * vfx.displayRing(location)
 *         .radius(3)
 *         .count(8)
 *         .model(new ItemStack(Material.AMETHYST_SHARD))
 *         .duration(80)
 *         .play();
 * }</pre>
 */
public final class DisplayRingEffect extends VFXEffect {
    private static final int INTERPOLATION_TICKS = 2;

    private final List<VFXDisplay<ItemDisplay>> displays = new ArrayList<>();
    private final Quaternionf spin = new Quaternionf();
    private ItemStack model = new ItemStack(Material.AMETHYST_SHARD);
    private double radius = 3.0D;
    private int count = 8;
    private float itemSize = 0.8F;
    private double orbitSpeed = 0.06D;
    private double lastX;
    private double lastY;
    private double lastZ;

    public DisplayRingEffect(final VFXManager manager, final Location origin) {
        super(manager, origin);
        setDuration(80);
        setRenderDistance(64.0D);
    }

    public DisplayRingEffect radius(final double radius) {
        this.radius = Math.max(0.25D, radius);
        setBoundingRadius(this.radius + 1.0D);
        return this;
    }

    public DisplayRingEffect count(final int count) {
        this.count = Math.clamp(count, 1, 32);
        return this;
    }

    public DisplayRingEffect model(final ItemStack model) {
        this.model = model.clone();
        return this;
    }

    public DisplayRingEffect itemSize(final float size) {
        this.itemSize = Math.max(0.05F, size);
        return this;
    }

    public DisplayRingEffect orbitSpeed(final double radiansPerTick) {
        this.orbitSpeed = radiansPerTick;
        return this;
    }

    public DisplayRingEffect duration(final int ticks) {
        setDuration(ticks);
        return this;
    }

    public DisplayRingEffect play() {
        start();
        return this;
    }

    @Override
    protected void onStart() {
        this.displays.clear();

        // Lower quality = fewer display entities (the expensive part of this effect).
        final int total = Math.min(this.count, quality().scalePoints(this.count));
        final Location origin = origin();
        this.lastX = origin.getX();
        this.lastY = origin.getY();
        this.lastZ = origin.getZ();

        for (int index = 0; index < total; index++) {
            final VFXDisplay<ItemDisplay> display = trackDisplay(VFXDisplay.item(manager(), origin, this.model))
                    .billboard(Display.Billboard.FIXED)
                    .brightness(15, 15)
                    .teleportDuration(1)
                    .interpolation(0, INTERPOLATION_TICKS)
                    .scale(0.0F, 0.0F, 0.0F);
            display.apply();
            this.displays.add(display);
        }
    }

    @Override
    protected void onTick() {
        final Location origin = originMutable();
        final boolean moved = origin.getX() != this.lastX || origin.getY() != this.lastY || origin.getZ() != this.lastZ;
        if (moved) {
            this.lastX = origin.getX();
            this.lastY = origin.getY();
            this.lastZ = origin.getZ();
        }

        final double progress = progress();
        // Grow in over the first 15%, shrink away over the last 15%.
        final double grow = Easing.EASE_OUT.apply(Math.min(1.0D, progress / 0.15D), 0.0D, 1.0D);
        final double fade = progress > 0.85D ? Easing.EASE_IN.apply((progress - 0.85D) / 0.15D, 1.0D, 0.0D) : 1.0D;
        final float scale = (float) (this.itemSize * Math.max(0.0D, grow * fade));

        final double time = age();
        final int total = this.displays.size();
        for (int index = 0; index < total; index++) {
            final double angle = ((VFXMath.TAU / total) * index) + (time * this.orbitSpeed);
            final float x = (float) (Math.cos(angle) * this.radius);
            final float z = (float) (Math.sin(angle) * this.radius);
            final float y = (float) (Math.sin((time * 0.12D) + index) * 0.25D);
            this.spin.identity().rotationY((float) ((time * 0.15D) + index));

            final VFXDisplay<ItemDisplay> display = this.displays.get(index);
            if (moved) {
                display.teleport(origin);
            }
            display.translation(x, y, z)
                    .rotation(this.spin)
                    .scale(scale, scale, scale)
                    .apply();
        }
    }

    @Override
    protected List<Vector> debugPoints() {
        return List.of(new Vector(this.radius, 0.0D, 0.0D), new Vector(0.0D, 0.0D, this.radius));
    }
}
