package steiner.localsearch.constructionmethods;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;
import utils.graphextensions.GraphUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * construction heuristic that continuously adds the cheapest path from a terminal to a vertex in the solution
 */
public class ShortestPathHeuristic implements ConstructionMethod {

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> constructSolution(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            HashMap<DefaultWeightedEdge, Double> weights) throws InterruptedException {

        SimpleWeightedGraph<Integer, DefaultWeightedEdge> weightGraph = GraphUtils.replaceWeights(graph, weights);
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths = GraphUtils.getShortestPaths(weightGraph);
        Integer root = Utils.getRandomSetElement(terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        HashSet<Integer> terminalsToAdd = new HashSet<>(terminals);
        terminalsToAdd.remove(root);
        solution.addVertex(root);

        while (!terminalsToAdd.isEmpty() && Utils.notInterrupted()) {
            double minWeight = Double.POSITIVE_INFINITY;
            int source = -1;
            int sink = -1;
            for (Integer terminal : terminalsToAdd) {
                for (Integer solVertex : solution.vertexSet()) {
                    double pathWeight = paths.getPathWeight(terminal, solVertex);
                    if (pathWeight < minWeight) {
                        minWeight = pathWeight;
                        source = terminal;
                        sink = solVertex;
                    }
                }
            }
            GraphPath<Integer, DefaultWeightedEdge> path = paths.getPath(source, sink);
            path.getVertexList().stream().filter(Predicate.not(solution::containsVertex)).forEach(v -> {
                solution.addVertex(v);
                terminalsToAdd.remove(v);
            });
            path.getEdgeList().stream().filter(Predicate.not(solution::containsEdge)).forEach(
                    e -> solution.addEdge(weightGraph.getEdgeSource(e), weightGraph.getEdgeTarget(e), e));
        }

        // standard cleanup optimization
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> finalSolution = GraphUtils.subgraph(
                weightGraph,
                solution.vertexSet()
        );
        finalSolution = GraphUtils.getGraphFromSpanningTree(GraphUtils.getMST(finalSolution), weightGraph);
        GraphUtils.removeSteinerLeaves(finalSolution, terminals);
        return solution;
    }
}
