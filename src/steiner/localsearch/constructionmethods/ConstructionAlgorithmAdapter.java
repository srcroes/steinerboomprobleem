package steiner.localsearch.constructionmethods;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import utils.graphextensions.GraphUtils;

import java.util.HashMap;
import java.util.Set;

public class ConstructionAlgorithmAdapter extends SteinerAlgorithm {
    private final ConstructionMethod method;
    private final HashMap<DefaultWeightedEdge, Double> weights;

    public ConstructionAlgorithmAdapter(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            ConstructionMethod method) {
        super(graph, terminals);
        this.method = method;
        this.weights = new HashMap<>();
        this.graph.edgeSet().forEach(e -> weights.put(e, graph.getEdgeWeight(e)));
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws Exception {
        long start = System.nanoTime();
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt = this.method.constructSolution(graph, terminals, weights);
        result.setRuntime(System.nanoTime() - start);
        result.setSmt(smt);
        result.setWeight(GraphUtils.getWeight(smt));
        return result;
    }
}
