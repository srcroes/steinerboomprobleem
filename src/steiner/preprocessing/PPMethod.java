package steiner.preprocessing;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.Set;

/**
 * Created by Stefan Croes
 */
public abstract class PPMethod {

    final SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    final Set<Integer> terminals;

    public PPMethod(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        this.graph = graph;
        this.terminals = terminals;
    }

    /**
     * do some operations on the input graph to simplify the problem
     *
     * @return simplified graph
     */
    public abstract SimpleWeightedGraph<Integer, DefaultWeightedEdge> preprocessing() throws InterruptedException;

    /**
     * do the inverse operations on the solution from the simplified graph to construct valid solution
     *
     * @param smt the steiner minimal tree of the simplified graph
     * @return modified solution corresponding to the original input graph
     */
    public abstract SimpleWeightedGraph<Integer, DefaultWeightedEdge> backtracking(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt);

}
