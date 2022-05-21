package steiner.preprocessing;

import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.graphextensions.GraphUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Stefan Croes
 */
public class LeastCostPP extends PPMethod {

    public LeastCostPP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> preprocessing() {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph1 = GraphUtils.copyGraph(graph);
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths = GraphUtils.getShortestPaths(graph1);
        Set<DefaultWeightedEdge> redundant = graph1
                .edgeSet()
                .stream()
                .filter(e -> paths.getPath(graph1.getEdgeSource(e), graph1.getEdgeTarget(e)).getWeight()
                        < graph1.getEdgeWeight(e))
                .collect(Collectors.toSet());
        graph1.removeAllEdges(redundant);
        return graph1;
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> backtracking(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt) {
        return smt;
    }
}
