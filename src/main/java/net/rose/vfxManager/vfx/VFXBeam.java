package net.rose.vfxManager.vfx;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class VFXBeam {
    private final VFXManager manager;
    private Location start;
    private Location end;
    private float width = 0.25F;
    private int interpolationDelay;
    private int interpolationDuration = 2;
    private int teleportDuration = 1;
    private ItemStack model = VFXDisplay.modelItem(Material.LIGHT, null);
    private VFXDisplay<ItemDisplay> display;

    public VFXBeam(final Location start, final Location end) {
        this(null, start, end);
    }

    public VFXBeam(final VFXManager manager, final Location start, final Location end) {
        this.manager = manager;
        this.start = start.clone();
        this.end = end.clone();
    }

    public VFXBeam setStart(final Location start) {
        this.start = start.clone();
        return this;
    }

    public VFXBeam setEnd(final Location end) {
        this.end = end.clone();
        return this;
    }

    public VFXBeam setWidth(final float width) {
        this.width = Math.max(0.01F, width);
        return this;
    }

    public VFXBeam setInterpolation(final int delay, final int duration) {
        this.interpolationDelay = Math.max(0, delay);
        this.interpolationDuration = Math.max(0, duration);
        return this;
    }

    public VFXBeam setTeleportDuration(final int teleportDuration) {
        this.teleportDuration = Math.max(0, teleportDuration);
        return this;
    }

    public VFXBeam setModel(final ItemStack model) {
        this.model = model;
        return this;
    }

    public ItemDisplay entity() {
        return this.display == null ? null : this.display.entity();
    }

    public void update() {
        if (this.start.getWorld() == null || this.end.getWorld() == null || !this.start.getWorld().equals(this.end.getWorld())) {
            remove();
            return;
        }

        final Vector delta = this.end.toVector().subtract(this.start.toVector());
        final double length = delta.length();
        if (length <= 0.0001D) {
            return;
        }

        final Location midpoint = this.start.clone().add(delta.clone().multiply(0.5D));
        if (this.display == null || !this.display.entity().isValid()) {
            this.display = createDisplay(midpoint);
        }

        final Vector3f forward = new Vector3f((float) delta.getX(), (float) delta.getY(), (float) delta.getZ()).normalize();
        final Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), forward);

        this.display
                .itemStack(this.model)
                .billboard(Display.Billboard.FIXED)
                .interpolation(this.interpolationDelay, this.interpolationDuration)
                .teleportDuration(this.teleportDuration)
                .brightness(15, 15)
                .teleport(midpoint)
                .rotation(rotation)
                .scale(this.width, this.width, (float) length)
                .apply();

        if (this.manager != null && this.manager.isDebugMode()) {
            final List<Player> viewers = new ArrayList<>();
            for (final Player player : this.start.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(midpoint) <= (96.0D * 96.0D)) {
                    viewers.add(player);
                }
            }
            this.manager.debugRenderer().renderBeamEndpoints(
                    this.start.getWorld(),
                    this.start,
                    this.end,
                    viewers
            );
        }
    }

    public void remove() {
        if (this.display != null) {
            this.display.remove();
            this.display = null;
        }
    }

    private VFXDisplay<ItemDisplay> createDisplay(final Location midpoint) {
        if (this.manager != null) {
            return VFXDisplay.item(this.manager, midpoint, this.model);
        }

        return VFXDisplay.item(midpoint, this.model);
    }
}
