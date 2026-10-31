package net.rose.vfxManager.vfx;

import org.bukkit.Color;
import org.bukkit.Particle;

public final class VFXColor {
    private VFXColor() {
    }

    public static Color rgb(final int red, final int green, final int blue) {
        return Color.fromRGB(clamp(red), clamp(green), clamp(blue));
    }

    public static Color lerp(final Color start, final Color end, final double progress) {
        final double clamped = Math.clamp(progress, 0.0D, 1.0D);
        return rgb(
                lerpChannel(start.getRed(), end.getRed(), clamped),
                lerpChannel(start.getGreen(), end.getGreen(), clamped),
                lerpChannel(start.getBlue(), end.getBlue(), clamped)
        );
    }

    public static Particle.DustOptions dust(final Color color, final float size) {
        return new Particle.DustOptions(color, size);
    }

    public static Particle.DustTransition transition(final Color from, final Color to, final float size) {
        return new Particle.DustTransition(from, to, size);
    }

    private static int lerpChannel(final int start, final int end, final double progress) {
        return clamp((int) Math.round(start + ((end - start) * progress)));
    }

    private static int clamp(final int value) {
        return Math.max(0, Math.min(255, value));
    }
}
