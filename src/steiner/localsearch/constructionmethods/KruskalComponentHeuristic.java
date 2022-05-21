package steiner.localsearch.constructionmethods;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;
import utils.graphextensions.GraphUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * construction heuristic that starts from single vertex components (terminals) and combines them until a tree is found
 */
public class KruskalComponentHeuristic implements ConstructionMethod {

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> constructSolution(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            HashMap<DefaultWeightedEdge, Double> weights) throws InterruptedException {

        SimpleWeightedGraph<Integer, DefaultWeightedEdge> weightGraph = GraphUtils.replaceWeights(graph, weights);
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths = GraphUtils.getShortestPaths(weightGraph);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        HashMap<Integer, Set<Integer>> components = new HashMap<>();
        terminals.forEach(v -> {
            solution.addVertex(v);
            components.put(v, new HashSet<>(Set.of(v)));
        });

        while (components.size() > 1) {
            // find 2 components that are cheapest to merge
            double minWeight = Double.POSITIVE_INFINITY;
            GraphPath<Integer, DefaultWeightedEdge> minPath = null;
            Integer componentJ1 = -1;
            Integer componentJ2 = -1;
            for (Map.Entry<Integer, Set<Integer>> entry1 : components.entrySet()) {
                for (Map.Entry<Integer, Set<Integer>> entry2 : components.entrySet()) {
                    Utils.notInterrupted();
                    Integer component1 = entry1.getKey();
                    Integer component2 = entry2.getKey();
                    if (component1 < component2) {
                        // find cheapest way to connect component1 and component2
                        double localMinWeight = Double.POSITIVE_INFINITY;
                        GraphPath<Integer, DefaultWeightedEdge> localMinPath = null;
                        for (Integer vertex1 : entry1.getValue()) {
                            for (Integer vertex2 : entry2.getValue()) {
                                GraphPath<Integer, DefaultWeightedEdge> path = paths.getPath(vertex1, vertex2);
                                double pathWeight = path.getWeight();
                                if (pathWeight < localMinWeight) {
                                    localMinWeight = pathWeight;
                                    localMinPath = path;
                                }
                            }
                        }
                        if (localMinWeight < minWeight) {
                            minWeight = localMinWeight;
                            minPath = localMinPath;
                            componentJ1 = component1;
                            componentJ2 = component2;
                        }
                    }
                }
            }
            // add path to solution
            if (minPath == null) {
//                System.out.println(components);
//                System.out.println(componentJ1);
//                System.out.println(componentJ2);
            }
            assert minPath != null;
            minPath.getVertexList().stream().filter(Predicate.not(solution::containsVertex)).forEach(solution::addVertex);
            minPath.getEdgeList().stream().filter(Predicate.not(solution::containsEdge)).forEach(
                    e -> solution.addEdge(weightGraph.getEdgeSource(e), weightGraph.getEdgeTarget(e), e));
            // merge components
            Set<Integer> component1Set = components.get(componentJ1);
            component1Set.addAll(components.get(componentJ2));
            component1Set.addAll(minPath.getVertexList());
            components.remove(componentJ2);
        }
        // standard cleanup optimization
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> finalSolution = GraphUtils.subgraph(
                weightGraph,
                solution.vertexSet()
        );
        finalSolution = GraphUtils.getGraphFromSpanningTree(GraphUtils.getMST(finalSolution), weightGraph);
        GraphUtils.removeSteinerLeaves(finalSolution, terminals);
        return finalSolution;
    }
}