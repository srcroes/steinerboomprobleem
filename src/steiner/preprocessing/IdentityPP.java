package steiner.preprocessing;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.Set;

/**
 * Identity preprocessing method (=does not modify anything)
 */
public class IdentityPP extends PPMethod {
    public IdentityPP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> preprocessing() throws InterruptedException {
        return this.graph;
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> backtracking(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt) {
        return smt;
    }
}
