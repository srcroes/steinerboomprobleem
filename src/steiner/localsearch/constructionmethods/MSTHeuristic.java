package steiner.localsearch.constructionmethods;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;
import utils.graphextensions.GraphUtils;

import java.util.HashMap;
import java.util.Set;

/**
 * construction heuristic that continuously prunes steiner leaves
 */
public class MSTHeuristic implements ConstructionMethod {

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> constructSolution(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            HashMap<DefaultWeightedEdge, Double> weights) throws InterruptedException {

        SimpleWeightedGraph<Integer, DefaultWeightedEdge> weightGraph = GraphUtils.replaceWeights(graph, weights);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution = weightGraph;

        boolean next = true;
        while (next && Utils.notInterrupted()) {
            solution = GraphUtils.subgraph(weightGraph, solution.vertexSet());
            solution = GraphUtils.getGraphFromSpanningTree(GraphUtils.getMST(solution), weightGraph);
            int size = solution.vertexSet().size();
            GraphUtils.removeSteinerLeaves(solution, terminals);
            next = size - solution.vertexSet().size() > 0;
        }
        return solution;
    }
}
