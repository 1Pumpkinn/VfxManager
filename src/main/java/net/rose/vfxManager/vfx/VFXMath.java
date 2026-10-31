package net.rose.vfxManager.vfx;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class VFXMath {
    public static final double TAU = Math.PI * 2.0D;

    private VFXMath() {
    }

    public static List<Vector> circle(final double radius, final int pointCount) {
        return ellipse(radius, radius, pointCount);
    }

    public static List<Vector> ellipse(final double radiusX, final double radiusZ, final int pointCount) {
        final int safePointCount = Math.max(4, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double angle = (TAU * index) / safePointCount;
            points.add(new Vector(
                    Math.cos(angle) * radiusX,
                    0.0D,
                    Math.sin(angle) * radiusZ
            ));
        }

        return points;
    }

    public static List<Vector> sphere(final double radius, final int pointCount) {
        final int safePointCount = Math.max(8, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double t = (index + 0.5D) / safePointCount;
            final double inclination = Math.acos(1.0D - (2.0D * t));
            final double azimuth = Math.PI * (1.0D + Math.sqrt(5.0D)) * index;
            final double sin = Math.sin(inclination);
            points.add(new Vector(
                    radius * Math.cos(azimuth) * sin,
                    radius * Math.cos(inclination),
                    radius * Math.sin(azimuth) * sin
            ));
        }

        return points;
    }

    public static List<Vector> hemisphere(final double radius, final int pointCount) {
        final int safePointCount = Math.max(8, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (final Vector vector : sphere(radius, safePointCount * 2)) {
            if (vector.getY() >= 0.0D) {
                points.add(vector);
            }
        }

        return points;
    }

    public static List<Vector> line(final Vector start, final Vector end, final int pointCount) {
        final int safePointCount = Math.max(2, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double progress = safePointCount == 1 ? 0.0D : (double) index / (safePointCount - 1);
            points.add(lerp(start, end, progress));
        }

        return points;
    }

    public static List<Vector> arc(
            final double radius,
            final double startAngleRadians,
            final double endAngleRadians,
            final int pointCount
    ) {
        final int safePointCount = Math.max(2, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double progress = safePointCount == 1 ? 0.0D : (double) index / (safePointCount - 1);
            final double angle = startAngleRadians + ((endAngleRadians - startAngleRadians) * progress);
            points.add(new Vector(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
        }

        return points;
    }

    public static List<Vector> spiral(
            final double startRadius,
            final double endRadius,
            final double height,
            final int turns,
            final int pointCount
    ) {
        final int safePointCount = Math.max(4, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double progress = (double) index / Math.max(1, safePointCount - 1);
            final double angle = TAU * turns * progress;
            final double radius = startRadius + ((endRadius - startRadius) * progress);
            points.add(new Vector(
                    Math.cos(angle) * radius,
                    height * progress,
                    Math.sin(angle) * radius
            ));
        }

        return points;
    }

    public static List<Vector> helix(
            final double radius,
            final double height,
            final int turns,
            final int pointCount
    ) {
        return spiral(radius, radius, height, turns, pointCount);
    }

    public static List<Vector> cone(
            final double baseRadius,
            final double height,
            final int radialPoints,
            final int heightSegments
    ) {
        final int safeHeightSegments = Math.max(2, heightSegments);
        final List<Vector> points = new ArrayList<>(Math.max(8, radialPoints) * safeHeightSegments);

        for (int segment = 0; segment < safeHeightSegments; segment++) {
            final double progress = (double) segment / (safeHeightSegments - 1);
            final double ringRadius = baseRadius * (1.0D - progress);
            final double y = height * progress;
            if (ringRadius < 1.0E-4D) {
                // Apex: a zero-radius ring would stack radialPoints particles on one spot.
                points.add(new Vector(0.0D, y, 0.0D));
                continue;
            }
            points.addAll(translate(circle(ringRadius, radialPoints), new Vector(0.0D, y, 0.0D)));
        }

        return points;
    }

    public static List<Vector> cylinder(
            final double radius,
            final double height,
            final int radialPoints,
            final int heightSegments
    ) {
        final int safeHeightSegments = Math.max(2, heightSegments);
        final List<Vector> points = new ArrayList<>(Math.max(8, radialPoints) * safeHeightSegments);

        for (int segment = 0; segment < safeHeightSegments; segment++) {
            final double progress = (double) segment / (safeHeightSegments - 1);
            final double y = height * progress;
            points.addAll(translate(circle(radius, radialPoints), new Vector(0.0D, y, 0.0D)));
        }

        return points;
    }

    public static List<Vector> bezier(
            final Vector p0,
            final Vector p1,
            final Vector p2,
            final Vector p3,
            final int pointCount
    ) {
        final int safePointCount = Math.max(2, pointCount);
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double t = safePointCount == 1 ? 0.0D : (double) index / (safePointCount - 1);
            final double inv = 1.0D - t;
            final double inv2 = inv * inv;
            final double inv3 = inv2 * inv;
            final double t2 = t * t;
            final double t3 = t2 * t;

            points.add(new Vector(
                    (inv3 * p0.getX()) + (3.0D * inv2 * t * p1.getX()) + (3.0D * inv * t2 * p2.getX()) + (t3 * p3.getX()),
                    (inv3 * p0.getY()) + (3.0D * inv2 * t * p1.getY()) + (3.0D * inv * t2 * p2.getY()) + (t3 * p3.getY()),
                    (inv3 * p0.getZ()) + (3.0D * inv2 * t * p1.getZ()) + (3.0D * inv * t2 * p2.getZ()) + (t3 * p3.getZ())
            ));
        }

        return points;
    }

    public static List<Vector> randomSphereCloud(final double radius, final int pointCount) {
        final int safePointCount = Math.max(1, pointCount);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final List<Vector> points = new ArrayList<>(safePointCount);

        for (int index = 0; index < safePointCount; index++) {
            final double theta = random.nextDouble(0.0D, TAU);
            final double phi = Math.acos(1.0D - (2.0D * random.nextDouble()));
            final double distance = radius * Math.cbrt(random.nextDouble());
            final double sin = Math.sin(phi);
            points.add(new Vector(
                    distance * Math.cos(theta) * sin,
                    distance * Math.cos(phi),
                    distance * Math.sin(theta) * sin
            ));
        }

        return points;
    }

    public static Vector rotateX(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double y = (vector.getY() * cos) - (vector.getZ() * sin);
        final double z = (vector.getY() * sin) + (vector.getZ() * cos);
        return new Vector(vector.getX(), y, z);
    }

    public static Vector rotateY(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double x = (vector.getX() * cos) + (vector.getZ() * sin);
        final double z = (-vector.getX() * sin) + (vector.getZ() * cos);
        return new Vector(x, vector.getY(), z);
    }

    public static Vector rotateZ(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double x = (vector.getX() * cos) - (vector.getY() * sin);
        final double y = (vector.getX() * sin) + (vector.getY() * cos);
        return new Vector(x, y, vector.getZ());
    }

    public static Vector rotateAroundAxis(final Vector vector, final Vector axis, final double angleRadians) {
        final Vector normalizedAxis = axis.clone().normalize();
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double dot = vector.dot(normalizedAxis);

        final Vector term1 = vector.clone().multiply(cos);
        final Vector term2 = normalizedAxis.clone().crossProduct(vector.clone()).multiply(sin);
        final Vector term3 = normalizedAxis.clone().multiply(dot * (1.0D - cos));
        return term1.add(term2).add(term3);
    }

    /**
     * In-place variants of {@link #rotateX}/{@link #rotateY}/{@link #rotateZ}. They mutate and return the same
     * vector, so hot render loops can rotate preallocated buffers without creating garbage.
     */
    public static Vector rotateXInPlace(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double y = (vector.getY() * cos) - (vector.getZ() * sin);
        final double z = (vector.getY() * sin) + (vector.getZ() * cos);
        return vector.setY(y).setZ(z);
    }

    public static Vector rotateYInPlace(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double x = (vector.getX() * cos) + (vector.getZ() * sin);
        final double z = (-vector.getX() * sin) + (vector.getZ() * cos);
        return vector.setX(x).setZ(z);
    }

    public static Vector rotateZInPlace(final Vector vector, final double angleRadians) {
        final double cos = Math.cos(angleRadians);
        final double sin = Math.sin(angleRadians);
        final double x = (vector.getX() * cos) - (vector.getY() * sin);
        final double y = (vector.getX() * sin) + (vector.getY() * cos);
        return vector.setX(x).setY(y);
    }

    /** Copies {@code points} into a new list of independent vectors, e.g. to build a reusable work buffer. */
    public static List<Vector> copyOf(final List<Vector> points) {
        final List<Vector> copy = new ArrayList<>(points.size());
        for (final Vector point : points) {
            copy.add(point.clone());
        }
        return copy;
    }

    public static List<Vector> translate(final List<Vector> points, final Vector translation) {
        return points.stream().map(point -> point.clone().add(translation)).toList();
    }

    public static List<Vector> scale(final List<Vector> points, final double scale) {
        return scale(points, new Vector(scale, scale, scale));
    }

    public static List<Vector> scale(final List<Vector> points, final Vector scale) {
        return points.stream()
                .map(point -> new Vector(
                        point.getX() * scale.getX(),
                        point.getY() * scale.getY(),
                        point.getZ() * scale.getZ()
                ))
                .toList();
    }

    public static List<Vector> rotate(final List<Vector> points, final Vector rotationRadians) {
        final List<Vector> transformed = new ArrayList<>(points.size());
        for (final Vector point : points) {
            Vector result = rotateX(point, rotationRadians.getX());
            result = rotateY(result, rotationRadians.getY());
            result = rotateZ(result, rotationRadians.getZ());
            transformed.add(result);
        }
        return transformed;
    }

    public static List<Vector> rotateAroundOrigin(
            final List<Vector> points,
            final Vector origin,
            final Vector rotationRadians
    ) {
        final List<Vector> transformed = new ArrayList<>(points.size());
        for (final Vector point : points) {
            Vector shifted = point.clone().subtract(origin);
            shifted = rotateX(shifted, rotationRadians.getX());
            shifted = rotateY(shifted, rotationRadians.getY());
            shifted = rotateZ(shifted, rotationRadians.getZ());
            transformed.add(shifted.add(origin));
        }
        return transformed;
    }

    public static List<Vector> transform(
            final List<Vector> points,
            final Vector scale,
            final Vector rotationRadians,
            final Vector translation
    ) {
        final List<Vector> transformed = new ArrayList<>(points.size());
        for (final Vector point : points) {
            Vector result = new Vector(
                    point.getX() * scale.getX(),
                    point.getY() * scale.getY(),
                    point.getZ() * scale.getZ()
            );
            result = rotateX(result, rotationRadians.getX());
            result = rotateY(result, rotationRadians.getY());
            result = rotateZ(result, rotationRadians.getZ());
            transformed.add(result.add(translation));
        }
        return transformed;
    }

    public static Vector lerp(final Vector start, final Vector end, final double progress) {
        final double clamped = Math.clamp(progress, 0.0D, 1.0D);
        return new Vector(
                start.getX() + ((end.getX() - start.getX()) * clamped),
                start.getY() + ((end.getY() - start.getY()) * clamped),
                start.getZ() + ((end.getZ() - start.getZ()) * clamped)
        );
    }

    public static Location add(final Location origin, final Vector offset) {
        return origin.clone().add(offset);
    }

    public static double randomRange(final double min, final double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int randomInt(final int minInclusive, final int maxExclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxExclusive);
    }
}
