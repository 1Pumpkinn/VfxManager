package net.rose.vfxManager.vfx.shape;

import net.rose.vfxManager.vfx.VFXMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Wireframe of a regular polyhedron, centred on the origin. Points are the vertices (once each) plus evenly spaced
 * points along every edge, so no two points share a position.
 * <p>
 * Edges are detected as the vertex pairs at the minimum pairwise distance, which is exactly the edge set for all
 * five regular polyhedra. Rotate the returned points with {@link VFXMath} to spin the shape.
 */
public final class PolyhedronShape implements VFXShape {
    public enum Type {
        TETRAHEDRON, CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON
    }

    private static final double PHI = (1.0D + Math.sqrt(5.0D)) / 2.0D;

    private final List<Vector> vertices;
    private final int[][] edges;
    private final int pointsPerEdge;

    /**
     * @param radius        distance from the centre to every vertex
     * @param pointsPerEdge extra points between the two vertices of each edge
     */
    public PolyhedronShape(final Type type, final double radius, final int pointsPerEdge) {
        this.pointsPerEdge = Math.max(0, pointsPerEdge);
        this.vertices = buildVertices(type, radius);
        this.edges = buildEdges(this.vertices);
    }

    /** Copies of the vertices, e.g. for debug control points. */
    public List<Vector> vertices() {
        return VFXMath.copyOf(this.vertices);
    }

    public int edgeCount() {
        return this.edges.length;
    }

    public int pointCount() {
        return this.vertices.size() + (this.edges.length * this.pointsPerEdge);
    }

    @Override
    public List<Vector> points() {
        final List<Vector> points = new ArrayList<>(pointCount());
        for (final Vector vertex : this.vertices) {
            points.add(vertex.clone());
        }

        for (final int[] edge : this.edges) {
            final Vector from = this.vertices.get(edge[0]);
            final Vector to = this.vertices.get(edge[1]);
            for (int index = 1; index <= this.pointsPerEdge; index++) {
                points.add(VFXMath.lerp(from, to, index / (double) (this.pointsPerEdge + 1)));
            }
        }
        return points;
    }

    private static List<Vector> buildVertices(final Type type, final double radius) {
        final List<double[]> raw = new ArrayList<>();
        final double[] signs = {-1.0D, 1.0D};

        switch (type) {
            case TETRAHEDRON -> {
                raw.add(new double[]{1, 1, 1});
                raw.add(new double[]{1, -1, -1});
                raw.add(new double[]{-1, 1, -1});
                raw.add(new double[]{-1, -1, 1});
            }
            case CUBE -> addCube(raw);
            case OCTAHEDRON -> {
                for (final double sign : signs) {
                    raw.add(new double[]{sign, 0, 0});
                    raw.add(new double[]{0, sign, 0});
                    raw.add(new double[]{0, 0, sign});
                }
            }
            case ICOSAHEDRON -> {
                for (final double a : signs) {
                    for (final double b : signs) {
                        raw.add(new double[]{0, a, b * PHI});
                        raw.add(new double[]{a, b * PHI, 0});
                        raw.add(new double[]{b * PHI, 0, a});
                    }
                }
            }
            case DODECAHEDRON -> {
                addCube(raw);
                final double inverse = 1.0D / PHI;
                for (final double a : signs) {
                    for (final double b : signs) {
                        raw.add(new double[]{0, a * inverse, b * PHI});
                        raw.add(new double[]{a * inverse, b * PHI, 0});
                        raw.add(new double[]{b * PHI, 0, a * inverse});
                    }
                }
            }
        }

        final List<Vector> result = new ArrayList<>(raw.size());
        for (final double[] coordinates : raw) {
            result.add(new Vector(coordinates[0], coordinates[1], coordinates[2]).normalize().multiply(radius));
        }
        return result;
    }

    private static void addCube(final List<double[]> raw) {
        for (final double x : new double[]{-1.0D, 1.0D}) {
            for (final double y : new double[]{-1.0D, 1.0D}) {
                for (final double z : new double[]{-1.0D, 1.0D}) {
                    raw.add(new double[]{x, y, z});
                }
            }
        }
    }

    private static int[][] buildEdges(final List<Vector> vertices) {
        double minimum = Double.MAX_VALUE;
        for (int a = 0; a < vertices.size(); a++) {
            for (int b = a + 1; b < vertices.size(); b++) {
                minimum = Math.min(minimum, vertices.get(a).distance(vertices.get(b)));
            }
        }

        final List<int[]> edges = new ArrayList<>();
        final double limit = minimum * (1.0D + 1.0E-6D);
        for (int a = 0; a < vertices.size(); a++) {
            for (int b = a + 1; b < vertices.size(); b++) {
                if (vertices.get(a).distance(vertices.get(b)) <= limit) {
                    edges.add(new int[]{a, b});
                }
            }
        }
        return edges.toArray(new int[0][]);
    }
}
