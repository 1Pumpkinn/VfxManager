package net.rose.vfxManager.vfx;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;

public record VFXContext(
        Plugin plugin,
        World world,
        Location origin,
        int tick,
        VFXQuality quality,
        boolean debug,
        double renderDistanceSquared,
        Collection<Player> viewers,
        boolean preFiltered
) {
    /** Convenience constructor for hand-built contexts; viewers are filtered on every {@link #viewersWithinRange()}. */
    public VFXContext(
            final Plugin plugin,
            final World world,
            final Location origin,
            final int tick,
            final VFXQuality quality,
            final boolean debug,
            final double renderDistanceSquared,
            final Collection<Player> viewers
    ) {
        this(plugin, world, origin, tick, quality, debug, renderDistanceSquared, viewers, false);
    }

    /**
     * Viewers that are online, in this world and inside the render distance.
     * <p>
     * {@link VFXManager#createContext} already filters by world and distance once per tick, so this returns that
     * list directly instead of re-filtering (and calling {@code Player#getLocation()}) on every render call.
     * Contexts built by hand with an arbitrary collection still get the full filter.
     */
    public List<Player> viewersWithinRange() {
        if (this.viewers.isEmpty()) {
            return List.of();
        }

        if (this.preFiltered && this.viewers instanceof List<Player> list) {
            return list;
        }

        return this.viewers.stream()
                .filter(Player::isOnline)
                .filter(player -> player.getWorld().equals(this.world))
                .filter(player -> player.getLocation().distanceSquared(this.origin) <= this.renderDistanceSquared)
                .toList();
    }
}
