package steiner.approx;

import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import utils.graphextensions.ClosureWeightedEdge;
import utils.graphextensions.GraphUtils;

import java.util.Set;

/**
 * 2-approximation steiner tree problem algorithm
 * based on "A faster approximation algorithm for the Steiner problem in graphs, Y. F. Wu, P. Widmayer, C. K. Wong"
 * *        DOI: 10.1007/bf00289500
 */
public class TwoApproximation extends SteinerAlgorithm {


    private final ShortestPathAlgorithm<Integer, DefaultWeightedEdge> paths;

    public TwoApproximation(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        this(graph, terminals, new DijkstraShortestPath<>(graph));
    }

    public TwoApproximation(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals, ShortestPathAlgorithm<Integer, DefaultWeightedEdge> paths) {
        super(graph, terminals);
        this.paths = paths;
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws InterruptedException {

        // * compute 'metric closure' / 'distance graph' of G => G1
        //      (complete subgraph containing all terminals where the edge weights represent the shortest path in G)

        SimpleWeightedGraph<Integer, ClosureWeightedEdge> closure = GraphUtils.getMetricClosure(
                this.graph,
                this.terminals,
                this.paths
        );

        // * compute MST of G1 => G2
        SpanningTree<ClosureWeightedEdge> closureMST = GraphUtils.getMST(closure);

        // * replace each edge in G2 by the corresponding path in G => G3
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> pathReplacedGraph = GraphUtils.getEdgePathGraph(
                closureMST,
                this.graph
        );

        // * compute MST of G3 => G4
        SpanningTree<DefaultWeightedEdge> pathMST = GraphUtils.getMST(pathReplacedGraph);

        // construct graph from MST
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> treeGraph = GraphUtils.getGraphFromSpanningTree(
                pathMST,
                pathReplacedGraph
        );

        // deleting edges so that no leaves are steiner vertices
        GraphUtils.removeSteinerLeaves(treeGraph, this.terminals);

        result.setSmt(treeGraph);
        result.setWeight(GraphUtils.getWeight(treeGraph));
        return result;
    }
}
