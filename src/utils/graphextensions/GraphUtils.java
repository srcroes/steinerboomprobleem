package utils.graphextensions;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Stefan Croes
 */

/**
 * some graph utilities that can be reused in different algorithms
 * all of these are JGraphT-specific
 */
public class GraphUtils {

    private GraphUtils() {
    }


    /**
     * computes the metric closure of a graph containing only terminals
     * the metric closure is the complete subgraph containing terminals, where the edges
     * represent the shortest path in the full graph between the two vertices
     *
     * @param graph     the graph of which to compute the metric closure
     * @param terminals the vertices to include in the metric closure
     * @return the metric closure, where each edge keeps track of the path in the full graph it represents
     */
    public static SimpleWeightedGraph<Integer, ClosureWeightedEdge> getMetricClosure(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals, ShortestPathAlgorithm<Integer, DefaultWeightedEdge> paths) throws InterruptedException {
        // construct new graph to represent the metric closure
        SimpleWeightedGraph<Integer, ClosureWeightedEdge> closure = new SimpleWeightedGraph<>(ClosureWeightedEdge.class);
        // use Floyd Warshall to compute all pairwise shortest paths in G
        // (get from arg or function overloading for reuse purposes)

        // add all terminals as vertices of the closure
        terminals.forEach(closure::addVertex);

        for (Integer src : terminals) {
            for (Integer dst : terminals) {
                Utils.notInterrupted();
                if (!src.equals(dst) && !closure.containsEdge(src, dst)) {
                    // get shortest path
                    GraphPath<Integer, DefaultWeightedEdge> path = paths.getPath(src, dst);
                    // add edge to closure
                    ClosureWeightedEdge edge = closure.addEdge(src, dst);
                    // add shortest path to edge
                    edge.setClosurePath(path);
                    // set weight of shortest path as edge weight
                    closure.setEdgeWeight(edge, path.getWeight());
                }
            }
        }
        return closure;
    }

    /**
     * get metric closure without specifying shortest paths
     * revert to dijkstra in this case
     *
     * @param graph     graph
     * @param terminals terminals
     * @return metric closure of the graph
     */
    public static SimpleWeightedGraph<Integer, ClosureWeightedEdge> getMetricClosure(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) throws InterruptedException {
        return getMetricClosure(graph, terminals, new DijkstraShortestPath<>(graph));
    }

    /**
     * get all pairs shortest paths of graph
     *
     * @param graph graph
     * @return paths
     */
    public static FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> getShortestPaths(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        return new FloydWarshallShortestPaths<>(graph);
    }

    /**
     * get minimum spanning tree of graph in graph form
     *
     * @param graph graph
     * @param <T>   edge class
     * @return minimum spanning tree of graph
     */
    public static <T extends DefaultWeightedEdge> SpanningTree<T> getMST(Graph<Integer, T> graph) {
        KruskalMinimumSpanningTree<Integer, T> spanningTree = new KruskalMinimumSpanningTree<>(graph);
        return spanningTree.getSpanningTree();
    }

    /**
     * replace edges of spanning tree with paths from closure
     *
     * @param pathsTree spanning tree containing edges that represent paths
     * @param graph     graph in which the full paths are contained
     * @return graph of spanning tree where the edges are replaced by their corresponding paths
     */
    public static SimpleWeightedGraph<Integer, DefaultWeightedEdge> getEdgePathGraph(
            SpanningTree<ClosureWeightedEdge> pathsTree,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> pathGraph = new SimpleWeightedGraph<>(
                DefaultWeightedEdge.class
        );
        pathsTree.getEdges().forEach(e -> addEdgesToGraph(graph, pathGraph, e.getClosurePath().getEdgeList()));
        return pathGraph;
    }

    /**
     * remove leaves that are steiner points (vertex and edge)
     *
     * @param graph graph on which to remove the leaves
     */
    public static void removeSteinerLeaves(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Collection<Integer> terminals) {
        graph.removeAllVertices(
                graph
                        .vertexSet()
                        .stream()
                        .filter(v -> !terminals.contains(v) && graph.edgesOf(v).size() < 2)
                        .toList()
        );
    }

    /**
     * get graph representing a spanning tree
     *
     * @param mst spanning tree of which to construct graph
     * @return graph
     */
    public static SimpleWeightedGraph<Integer, DefaultWeightedEdge> getGraphFromSpanningTree(
            SpanningTree<DefaultWeightedEdge> mst,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> treeGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        addEdgesToGraph(graph, treeGraph, mst.getEdges());
        return treeGraph;
    }

    public static SimpleWeightedGraph<Integer, ClosureWeightedEdge> getGraphFromClosureSpanningTree(SpanningTree<ClosureWeightedEdge> closureMST, SimpleWeightedGraph<Integer, ClosureWeightedEdge> closure) {
        SimpleWeightedGraph<Integer, ClosureWeightedEdge> treeGraph = new SimpleWeightedGraph<>(ClosureWeightedEdge.class);
        addClosureEdgesToGraph(closure, treeGraph, closureMST.getEdges());
        return treeGraph;
    }

    /**
     * add specific weighted edges from one graph to another graph
     *
     * @param srcGraph source graph containing the edges and weights
     * @param dstGraph destination graph to add weighted edges to
     * @param edges    weighted edges to add to destination graph
     */
    public static void addEdgesToGraph(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> srcGraph,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> dstGraph,
            Collection<DefaultWeightedEdge> edges) {
        for (DefaultWeightedEdge e : edges) {
            Integer src = srcGraph.getEdgeSource(e);
            Integer dst = srcGraph.getEdgeTarget(e);
            dstGraph.addVertex(src);
            dstGraph.addVertex(dst);

            DefaultWeightedEdge newEdge = dstGraph.addEdge(src, dst);
            if (newEdge != null) dstGraph.setEdgeWeight(newEdge, srcGraph.getEdgeWeight(e));
        }
    }

    public static void addClosureEdgesToGraph(
            SimpleWeightedGraph<Integer, ClosureWeightedEdge> srcGraph,
            SimpleWeightedGraph<Integer, ClosureWeightedEdge> dstGraph,
            Collection<ClosureWeightedEdge> edges) {
        for (ClosureWeightedEdge e : edges) {
            Integer src = srcGraph.getEdgeSource(e);
            Integer dst = srcGraph.getEdgeTarget(e);
            dstGraph.addVertex(src);
            dstGraph.addVertex(dst);
            ClosureWeightedEdge newEdge = dstGraph.addEdge(src, dst);
            if (newEdge != null){
                dstGraph.setEdgeWeight(newEdge, srcGraph.getEdgeWeight(e));
                newEdge.setClosurePath(e.getClosurePath());
            }
        }
    }

    /**
     * verify the validity of a graph's steiner tree, "valid" meaning:
     * *        - every terminal is contained in the tree
     * *        - every terminal is reachable from every other terminal
     *
     * @param graph     graph of which the steiner tree is to be verified
     * @param terminals terminals for which the steiner tree was calculated
     * @param treeGraph steiner tree of the graph and terminals
     * @return whether or not the steiner tree is valid
     */
    public static boolean verifySteinerTree(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Collection<Integer> terminals,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> treeGraph) {
        if (treeGraph == null)
            return false;

        // steiner tree must contain all terminals
        if (!terminals.stream().allMatch(treeGraph::containsVertex))
            return false;

        // check that the tree is connected
        ConnectivityInspector<Integer, DefaultWeightedEdge> inspector = new ConnectivityInspector<>(treeGraph);
        if (!inspector.isConnected())
            return false;

        // check that all vertices in the tree exist in the given graph
        if (!graph.vertexSet().containsAll(treeGraph.vertexSet()))
            return false;
        // check that all edges in the tree exist in the given graph
        for (DefaultWeightedEdge treeEdge : treeGraph.edgeSet()) {
            Integer source = treeGraph.getEdgeSource(treeEdge);
            Integer target = treeGraph.getEdgeTarget(treeEdge);
            if (!graph.containsEdge(source, target)
                    || (treeGraph.getEdgeWeight(treeEdge) != graph.getEdgeWeight(graph.getEdge(source, target)))) {
//                System.out.println(!graph.containsEdge(source, target));
//                System.out.println(treeGraph.getEdgeWeight(treeEdge) != graph.getEdgeWeight(graph.getEdge(source, target)));
                return false;
            }
        }
        return true;
    }

    /**
     * get the total weight of all edges in the graph
     *
     * @param graph the graph of which to calculate the weight
     * @param <T>   edge class
     * @return the total weight of the graph
     */
    public static <T extends DefaultWeightedEdge> Double getWeight(SimpleWeightedGraph<Integer, T> graph) {
        return graph.edgeSet().stream().mapToDouble(graph::getEdgeWeight).sum();
    }

    /**
     * get the total weight of all edges in the graph, using weights from a different graph
     *
     * @param graph       the graph of which to use the edges
     * @param weightGraph the graph of which to use the weights
     * @param <T>         edge class
     * @return the total weight of the graph according to the weights of the weightGraph
     */
    public static <T extends DefaultWeightedEdge> Double getWeight(
            // TODO remove this method or change to not use the exact edge object, but source/sink
            SimpleWeightedGraph<Integer, T> graph,
            SimpleWeightedGraph<Integer, T> weightGraph) {
        return graph.edgeSet().stream().mapToDouble(weightGraph::getEdgeWeight).sum();
    }

    /**
     * print the source, destination, and weight of all edges in the graph
     *
     * @param graph the graph of which to print the edges
     * @param <T>   the edge class
     */
    public static <T extends DefaultWeightedEdge> void printWeightedEdges(Graph<Integer, T> graph) {
        printWeightedEdges(graph, graph.edgeSet());
    }

    /**
     * print the source, destination, and weight of the specified edges in the graph
     *
     * @param graph the graph where the specified edges are from
     * @param edges a subset of edges from the graph
     * @param <T>   edge class
     */
    public static <T extends DefaultWeightedEdge> void printWeightedEdges(
            Graph<Integer, T> graph,
            Collection<T> edges) {
        for (T edge : edges) {
            System.out.printf("Edge %d-%d: %s%n",
                    graph.getEdgeSource(edge),
                    graph.getEdgeTarget(edge),
                    graph.getEdgeWeight(edge));
        }
    }

    /**
     * make a deep copy of a graph
     *
     * @param graph graph to copy
     * @return the copy
     */
    public static SimpleWeightedGraph<Integer, DefaultWeightedEdge> copyGraph(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph1 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        graph.vertexSet().forEach(graph1::addVertex);
        graph.edgeSet().forEach(e -> graph1.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e)));
        graph1.edgeSet().forEach(e -> graph1.setEdgeWeight(
                e, graph.getEdgeWeight(graph.getEdge(graph1.getEdgeSource(e), graph1.getEdgeTarget(e))))
        );
        return graph1;
    }

    /**
     * replace weights in deep copy of graph
     *
     * @param graph   graph to be copied and changed
     * @param weights new weights
     * @return deep copy of graphs with new weights
     */
    public static SimpleWeightedGraph<Integer, DefaultWeightedEdge> replaceWeights(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Map<DefaultWeightedEdge, Double> weights) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> newGraph = copyGraph(graph);
        for (Map.Entry<DefaultWeightedEdge, Double> entry : weights.entrySet()) {
            DefaultWeightedEdge edge = entry.getKey();
            Integer source = graph.getEdgeSource(edge);
            Integer target = graph.getEdgeTarget(edge);
            newGraph.setEdgeWeight(source, target, entry.getValue());
        }
        return newGraph;
    }

    /**
     * replace weights in graph
     *
     * @param graph       graph to be copied and changed
     * @param weightGraph graph containing new weights
     */
    public static void replaceWeightsInPlace(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> weightGraph) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            Integer source = graph.getEdgeSource(edge);
            Integer target = graph.getEdgeTarget(edge);
            graph.setEdgeWeight(source, target, weightGraph.getEdgeWeight(weightGraph.getEdge(source, target)));
        }
    }

    /**
     * get subgraph induced in G by vertexSet
     *
     * @param graph     G
     * @param vertexSet vertices to induce subgraph by
     * @return subgraph induced by vertexset
     */
    public static SimpleWeightedGraph<Integer, DefaultWeightedEdge> subgraph(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> vertexSet) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> newGraph = copyGraph(graph);
        HashSet<Integer> vertices = new HashSet<>(newGraph.vertexSet());
        vertices.removeAll(vertexSet);
        newGraph.removeAllVertices(vertices);
        return newGraph;
    }

    /**
     * compare 2 solutions for edge-wise equaliy
     *
     * @param graph1 first graph
     * @param graph2 second graph
     * @return whether the edgesets are equal
     */
    public static boolean equals(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph1,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph2) {
        Set<DefaultWeightedEdge> edges1 = graph1.edgeSet();
        Set<DefaultWeightedEdge> edges2 = graph2.edgeSet();
        if (edges1.size() != edges2.size()) return false;
        // corresponding edges are not necessarily the same object
        Set<Pair<Integer, Integer>> pairs1 = edges1
                .stream()
                .map(e -> new Pair<>(graph1.getEdgeSource(e), graph1.getEdgeTarget(e))).collect(Collectors.toSet());
        Set<Pair<Integer, Integer>> pairs2 = edges2
                .stream()
                .map(e -> new Pair<>(graph2.getEdgeSource(e), graph2.getEdgeTarget(e))).collect(Collectors.toSet());
        return pairs1.equals(pairs2);
    }
}
