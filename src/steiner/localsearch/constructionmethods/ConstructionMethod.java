package steiner.localsearch.constructionmethods;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.HashMap;
import java.util.Set;

public interface ConstructionMethod {
    SimpleWeightedGraph<Integer, DefaultWeightedEdge> constructSolution(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            HashMap<DefaultWeightedEdge, Double> weights) throws InterruptedException;
}
