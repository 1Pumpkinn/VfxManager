package net.rose.vfxManager;

import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXQuality;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * In-game test command for the VFX framework: {@code /vfx <orb|circle|beam|displays|quality|debug|status>}.
 * Registered through plugin.yml; requires the {@code vfx.admin} permission (ops by default).
 */
public final class VFXCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("orb", "circle", "beam", "displays", "quality", "debug", "status");

    private final VFXManager vfx;

    public VFXCommand(final VFXManager vfx) {
        this.vfx = vfx;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        final String sub = args[0].toLowerCase(Locale.ROOT);

        // Subcommands that don't need a player location.
        switch (sub) {
            case "quality" -> {
                return handleQuality(sender, args);
            }
            case "debug" -> {
                final boolean enabled = args.length > 1 ? args[1].equalsIgnoreCase("on") : !this.vfx.isDebugMode();
                this.vfx.debugMode(enabled);
                sender.sendMessage("VFX debug mode: " + (enabled ? "on" : "off"));
                return true;
            }
            case "status" -> {
                sender.sendMessage("VFX active effects: " + this.vfx.activeEffectCount()
                        + "/" + this.vfx.maxActiveEffects()
                        + ", quality: " + this.vfx.quality()
                        + ", debug: " + this.vfx.isDebugMode()
                        + ", displays: " + this.vfx.managedEntityCount()
                        + ", dropped: " + this.vfx.rejectedEffectCount());
                return true;
            }
            default -> {
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This subcommand must be run by a player.");
            return true;
        }

        switch (sub) {
            case "orb" -> {
                this.vfx.magicOrb(player.getLocation().add(0.0D, 6.0D, 0.0D)).setDuration(100).start();
                sender.sendMessage("Spawned magic orb.");
            }
            case "circle" -> {
                final double radius = parseDouble(args, 1, 4.0D);
                this.vfx.magicCircle(player.getLocation().add(0.0D, 0.1D, 0.0D))
                        .radius(radius)
                        .rings(3)
                        .color(255, 40, 180)
                        .rotationSpeed(0.08D)
                        .duration(100)
                        .play();
                sender.sendMessage("Spawned magic circle (radius " + radius + ").");
            }
            case "displays" -> {
                final var ring = this.vfx.displayRing(player.getLocation())
                        .radius(3.0D)
                        .count(8)
                        .duration(100)
                        .play();
                ring.attach(player);
                ring.setOffset(new Vector(0.0D, 1.5D, 0.0D));
                sender.sendMessage("Spawned display ring following you.");
            }
            case "beam" -> {
                final double length = parseDouble(args, 1, 12.0D);
                final Location start = player.getEyeLocation().add(0.0D, -0.3D, 0.0D);
                final Vector direction = player.getEyeLocation().getDirection().normalize().multiply(length);
                final Location end = start.clone().add(direction);
                this.vfx.beam(start, end).width(0.3D).duration(40).play();
                sender.sendMessage("Spawned beam (length " + length + ").");
            }
            default -> sender.sendMessage("Unknown subcommand. Use: " + String.join(", ", SUBCOMMANDS));
        }
        return true;
    }

    private boolean handleQuality(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Current quality: " + this.vfx.quality() + ". Usage: /vfx quality <LOW|MEDIUM|HIGH|ULTRA>");
            return true;
        }

        try {
            final VFXQuality quality = VFXQuality.valueOf(args[1].toUpperCase(Locale.ROOT));
            this.vfx.quality(quality);
            sender.sendMessage("VFX quality set to " + quality + " (applies to effects started from now on).");
        } catch (final IllegalArgumentException exception) {
            sender.sendMessage("Unknown quality '" + args[1] + "'. Use LOW, MEDIUM, HIGH or ULTRA.");
        }
        return true;
    }

    private static double parseDouble(final String[] args, final int index, final double fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Math.clamp(Double.parseDouble(args[index]), 0.5D, 32.0D);
        } catch (final NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(name -> name.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("quality")) {
            return Arrays.stream(VFXQuality.values()).map(Enum::name)
                    .filter(name -> name.startsWith(args[1].toUpperCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
