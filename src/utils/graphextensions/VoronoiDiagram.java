package utils.graphextensions;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * voronoi diagram for local search
 * !contains some bugs
 */
public class VoronoiDiagram {
    private Map<Integer, Integer> base;
    private Map<Integer, Integer> predecessor;
    private Map<Integer, Double> vdist;
    private Map<Integer, Set<DefaultWeightedEdge>> boundaries;
    private Map<DefaultWeightedEdge, Pair<Integer, Integer>> inverseBoundaries;
    private final FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> shortestPaths;
    private SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    private Set<Integer> bases;

    public VoronoiDiagram(SimpleWeightedGraph<Integer, DefaultWeightedEdge> vGraph, Set<Integer> vBases) {
        this.graph = vGraph;
        this.bases = new HashSet<>(vBases);
        this.base = new HashMap<>();
        this.predecessor = new HashMap<>();
        this.vdist = new HashMap<>();
        this.boundaries = new HashMap<>();
        this.inverseBoundaries = new HashMap<>();

        shortestPaths = GraphUtils.getShortestPaths(graph);

        fullVoronoi(bases);
    }

    public void convertToBases(Set<Integer> newBases) {
        Set<Integer> outdatedBases = new HashSet<>(this.bases);
        outdatedBases.removeAll(newBases);
        Set<Integer> outdatedVertices = this.graph.vertexSet().stream().filter(v -> outdatedBases.contains(
                this.base.get(v))).collect(Collectors.toSet());
        voronoiCalc(outdatedVertices, newBases);
        this.bases = new HashSet<>(newBases);
        updateBoundaries(outdatedVertices);
    }

    public void fullVoronoi(Set<Integer> bases) {
        voronoiCalc(this.graph.vertexSet(), bases);
        this.bases = new HashSet<>(bases);
        fullBoundaries();
    }

    private void voronoiCalc(Set<Integer> vertices, Set<Integer> bases) {
        for (Integer v : vertices) {
            GraphPath<Integer, DefaultWeightedEdge> vPath = null;
            double vWeight = Double.POSITIVE_INFINITY;
            int vBase = -1;
            for (Integer basis : bases) {
                GraphPath<Integer, DefaultWeightedEdge> path = shortestPaths.getPath(v, basis);
                double weight = path.getWeight();
                if (weight < vWeight) {
                    vPath = path;
                    vBase = basis;
                    vWeight = weight;
                }
            }
            base.put(v, vBase);
            if (vPath != null) {
                List<Integer> vertexList = vPath.getVertexList();
                vdist.put(v, vPath.getWeight());
                if (vertexList.size() > 1) {
                    predecessor.put(v, vertexList.get(1));
                } else {
                    predecessor.put(v, v);
                }
            }
        }
    }

    private void updateBoundaries(Set<Integer> outdatedBases) {
        Set<DefaultWeightedEdge> edges = outdatedBases
                .stream()
                .map(this.graph::edgesOf)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        for (DefaultWeightedEdge edge : edges) {
            Pair<Integer, Integer> pair = inverseBoundaries.get(edge);
            if (pair != null) { // not every edge is a boundary edge, so it can be null
                boundaries.get(pair.getFirst()).remove(edge);
                boundaries.get(pair.getSecond()).remove(edge);
            }
        }
        outdatedBases.forEach(boundaries::remove);

        boundariesCalc(edges);
    }

    private void fullBoundaries() {
        boundariesCalc(this.graph.edgeSet());
    }

    private void boundariesCalc(Set<DefaultWeightedEdge> edges) {
        for (DefaultWeightedEdge edge : edges) {
            Integer src = this.graph.getEdgeSource(edge);
            Integer trg = this.graph.getEdgeTarget(edge);
            Integer srcBase = base.get(src);
            Integer trgBase = base.get(trg);
            if (!srcBase.equals(trgBase)) {
                if (!boundaries.containsKey(srcBase))
                    boundaries.put(srcBase, new HashSet<>());
                boundaries.get(srcBase).add(edge);
                if (!boundaries.containsKey(trgBase))
                    boundaries.put(trgBase, new HashSet<>());
                boundaries.get(trgBase).add(edge);
                inverseBoundaries.put(edge, new Pair<>(srcBase, trgBase));
            }
        }
    }

    public Map<Integer, Integer> getBase() {
        return base;
    }

    public void setBase(Map<Integer, Integer> base) {
        this.base = base;
    }

    public Map<Integer, Integer> getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Map<Integer, Integer> predecessor) {
        this.predecessor = predecessor;
    }

    public Map<Integer, Double> getVdist() {
        return vdist;
    }

    public void setVdist(Map<Integer, Double> vdist) {
        this.vdist = vdist;
    }

    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> getGraph() {
        return graph;
    }

    public void setGraph(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        this.graph = graph;
    }

    public Set<Integer> getBases() {
        return bases;
    }

    public void setBases(Set<Integer> bases) {
        this.bases = bases;
    }

    public Map<Integer, Set<DefaultWeightedEdge>> getBoundaries() {
        return boundaries;
    }

    public void setBoundaries(Map<Integer, Set<DefaultWeightedEdge>> boundaries) {
        this.boundaries = boundaries;
    }

    public Map<DefaultWeightedEdge, Pair<Integer, Integer>> getInverseBoundaries() {
        return inverseBoundaries;
    }

    public void setInverseBoundaries(Map<DefaultWeightedEdge, Pair<Integer, Integer>> inverseBoundaries) {
        this.inverseBoundaries = inverseBoundaries;
    }

    public FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> getShortestPaths() {
        return shortestPaths;
    }

    @Override
    public String toString() {
        return "VoronoiDiagram{" +
                "\n\tbase=" + base +
                ",\n\tpredecessor=" + predecessor +
                ",\n\tvdist=" + vdist +
                ",\n\tboundaries=" + boundaries +
                ",\n\tinverseBoundaries=" + inverseBoundaries +
                ",\n\tshortestPaths=" + shortestPaths +
                ",\n\tgraph=" + graph +
                ",\n\tbases=" + bases +
                "\n}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoronoiDiagram that = (VoronoiDiagram) o;
        return Objects.equals(base, that.base)
                && Objects.equals(predecessor, that.predecessor)
                && Objects.equals(vdist, that.vdist)
                && Objects.equals(boundaries, that.boundaries)
                && Objects.equals(inverseBoundaries, that.inverseBoundaries)
                && Objects.equals(shortestPaths, that.shortestPaths)
                && Objects.equals(graph, that.graph)
                && Objects.equals(bases, that.bases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, predecessor, vdist, boundaries, inverseBoundaries, shortestPaths, graph, bases);
    }
}
