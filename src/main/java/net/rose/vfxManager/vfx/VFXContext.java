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
        Collection<Player> viewers
) {
    public List<Player> viewersWithinRange() {
        if (this.viewers.isEmpty()) {
            return List.of();
        }

        return this.viewers.stream()
                .filter(Player::isOnline)
                .filter(player -> player.getWorld().equals(this.world))
                .filter(player -> player.getLocation().distanceSquared(this.origin) <= this.renderDistanceSquared)
                .toList();
    }
}
