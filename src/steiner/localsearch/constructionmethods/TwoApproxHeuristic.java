package steiner.localsearch.constructionmethods;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerResult;
import steiner.approx.TwoApproximation;
import utils.graphextensions.GraphUtils;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by Stefan Croes
 */

/**
 * construction heuristic that using the 2approximation algorithm
 */
public class TwoApproxHeuristic implements ConstructionMethod {
    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> constructSolution(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            HashMap<DefaultWeightedEdge, Double> weights) throws InterruptedException {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> weightGraph = GraphUtils.replaceWeights(graph, weights);
        TwoApproximation twoApproximation = new TwoApproximation(weightGraph, terminals);
        SteinerResult result = twoApproximation.runInstance(new SteinerResult(null, -1));
        return result.getSmt();
    }
}
