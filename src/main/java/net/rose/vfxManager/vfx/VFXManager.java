package net.rose.vfxManager.vfx;

import net.rose.vfxManager.vfx.effects.MagicOrbEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VFXManager implements Listener {
    private final Plugin plugin;
    private final Set<VFXEffect> activeEffects = new LinkedHashSet<>();
    private final Set<VFXEffect> pendingAdditions = new LinkedHashSet<>();
    private final Set<VFXEffect> pendingRemovals = new LinkedHashSet<>();
    private final Set<UUID> managedEntities = new LinkedHashSet<>();
    private final VFXDebugRenderer debugRenderer = new VFXDebugRenderer();
    private BukkitTask tickTask;
    private VFXQuality quality = VFXQuality.HIGH;
    private int maxActiveEffects = 512;
    private boolean debugMode;
    private int tick;

    public VFXManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin plugin() {
        return this.plugin;
    }

    public VFXQuality quality() {
        return this.quality;
    }

    public VFXManager quality(final VFXQuality quality) {
        this.quality = quality;
        return this;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public VFXManager debugMode(final boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public int maxActiveEffects() {
        return this.maxActiveEffects;
    }

    public VFXManager maxActiveEffects(final int maxActiveEffects) {
        this.maxActiveEffects = Math.max(1, maxActiveEffects);
        return this;
    }

    public VFXDebugRenderer debugRenderer() {
        return this.debugRenderer;
    }

    public MagicOrbEffect magicOrb(final Location location) {
        return new MagicOrbEffect(this, location);
    }

    public VFXBeam beam(final Location start, final Location end) {
        return new VFXBeam(this, start, end);
    }

    public int activeEffectCount() {
        return this.activeEffects.size() + this.pendingAdditions.size();
    }

    public void start() {
        if (this.tickTask != null) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (this.tickTask != null) {
            this.tickTask.cancel();
            this.tickTask = null;
        }

        HandlerList.unregisterAll(this);

        final List<VFXEffect> effects = new ArrayList<>(this.activeEffects);
        effects.addAll(this.pendingAdditions);
        for (final VFXEffect effect : effects) {
            effect.stop();
        }

        this.activeEffects.clear();
        this.pendingAdditions.clear();
        this.pendingRemovals.clear();

        cleanupManagedEntities();
    }

    public boolean registerEffect(final VFXEffect effect) {
        if ((this.activeEffects.size() + this.pendingAdditions.size()) >= this.maxActiveEffects) {
            return false;
        }

        this.pendingAdditions.add(effect);
        this.pendingRemovals.remove(effect);
        return true;
    }

    public void unregisterEffect(final VFXEffect effect) {
        this.pendingAdditions.remove(effect);
        this.pendingRemovals.add(effect);
    }

    public void trackManagedEntity(final Entity entity) {
        this.managedEntities.add(entity.getUniqueId());
    }

    public VFXContext createContext(final VFXEffect effect) {
        final Location origin = effect.origin();
        final World world = origin.getWorld();
        if (world == null) {
            return new VFXContext(
                    this.plugin,
                    Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst(),
                    origin,
                    this.tick,
                    effect.quality(),
                    this.debugMode,
                    effect.renderDistanceSquared(),
                    List.of()
            );
        }

        return new VFXContext(
                this.plugin,
                world,
                origin,
                this.tick,
                effect.quality(),
                this.debugMode,
                effect.renderDistanceSquared(),
                resolveViewers(effect)
        );
    }

    public List<Player> resolveViewers(final VFXEffect effect) {
        final Location origin = effect.origin();
        final World world = origin.getWorld();
        if (world == null) {
            return List.of();
        }

        final Set<UUID> viewerIds = effect.viewerIds();
        final List<Player> viewers = new ArrayList<>();

        if (viewerIds.isEmpty()) {
            for (final Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(origin) <= effect.renderDistanceSquared()) {
                    viewers.add(player);
                }
            }
            return viewers;
        }

        for (final UUID viewerId : viewerIds) {
            final Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline() || !player.getWorld().equals(world)) {
                continue;
            }

            if (player.getLocation().distanceSquared(origin) <= effect.renderDistanceSquared()) {
                viewers.add(player);
            }
        }

        return viewers;
    }

    @EventHandler
    public void onWorldUnload(final WorldUnloadEvent event) {
        final World world = event.getWorld();
        for (final VFXEffect effect : new ArrayList<>(this.activeEffects)) {
            if (world.equals(effect.world())) {
                effect.stop();
            }
        }
    }

    private void tick() {
        this.tick++;
        flushChanges();

        final Iterator<VFXEffect> iterator = this.activeEffects.iterator();
        while (iterator.hasNext()) {
            final VFXEffect effect = iterator.next();
            try {
                effect.tick();
            } catch (final Exception exception) {
                this.plugin.getLogger().warning("Failed to tick VFX effect " + effect.getClass().getSimpleName() + ": " + exception.getMessage());
                effect.stop();
            }
        }

        flushChanges();
        cleanupManagedEntities();
    }

    private void flushChanges() {
        if (!this.pendingRemovals.isEmpty()) {
            this.activeEffects.removeAll(this.pendingRemovals);
            this.pendingRemovals.clear();
        }

        if (!this.pendingAdditions.isEmpty()) {
            this.activeEffects.addAll(this.pendingAdditions);
            this.pendingAdditions.clear();
        }
    }

    private void cleanupManagedEntities() {
        final Iterator<UUID> iterator = this.managedEntities.iterator();
        while (iterator.hasNext()) {
            final UUID entityId = iterator.next();
            final Entity entity = Bukkit.getEntity(entityId);
            if (entity == null || !entity.isValid()) {
                iterator.remove();
            }
        }
    }
}
