package steiner.preprocessing;

import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.graphextensions.GraphUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by Stefan Croes
 */
public class DegreeOneTwoPP extends PPMethod {

    private List<OneTwoBT> backtrackingMap;
    private List<Triple<Integer, Integer, Double>> restoreWeights;

    public DegreeOneTwoPP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
        this.backtrackingMap = new ArrayList<>();
        this.restoreWeights = new ArrayList<>();
    }


    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> preprocessing() {
        this.backtrackingMap.clear();
        this.restoreWeights.clear();
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph1 = GraphUtils.copyGraph(graph);
        Set<Integer> vertices = new HashSet<>(graph1.vertexSet());
        vertices.removeAll(terminals);
        Set<Integer> verticesToRemove = new HashSet<>();
        for (Integer vertex : vertices) {
            int degree = graph1.degreeOf(vertex);
            if (degree == 1) {
                verticesToRemove.add(vertex);
            } else if (degree == 2) {
                List<DefaultWeightedEdge> vEdges = graph1.edgesOf(vertex).stream().toList();
                DefaultWeightedEdge pi = vEdges.get(0);
                DefaultWeightedEdge qi = vEdges.get(1);
                Integer p = graph1.getEdgeSource(pi);
                p = p.equals(vertex) ? graph1.getEdgeTarget(pi) : p;
                Integer q = graph1.getEdgeSource(qi);
                q = q.equals(vertex) ? graph1.getEdgeTarget(qi) : q;
                double piWeight = graph1.getEdgeWeight(pi);
                double qiWeight = graph1.getEdgeWeight(qi);
                double sum = piWeight + qiWeight;
                boolean containsEdge = graph1.containsEdge(p, q);
                double weight = containsEdge ? graph1.getEdgeWeight(graph1.getEdge(p, q)) : Double.POSITIVE_INFINITY;
                if (sum < weight) {
                    List<DefaultWeightedEdge> edges = List.of(pi, qi);
                    graph1.removeAllEdges(edges);
                    DefaultWeightedEdge e;
                    Double pqW;
                    if (!containsEdge) {
                        e = graph1.addEdge(p, q);
                        pqW = null;
                    } else {
                        e = graph1.getEdge(p, q);
                        pqW = graph1.getEdgeWeight(e);
                    }
                    graph1.setEdgeWeight(e, sum);
                    backtrackingMap.add(new OneTwoBT(p, q, vertex, piWeight, qiWeight, pqW));
                }
                verticesToRemove.add(vertex);
            }
        }
        graph1.removeAllVertices(verticesToRemove);
        Collections.reverse(this.backtrackingMap);
        return graph1;
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> backtracking(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt) {
        for (OneTwoBT entry : this.backtrackingMap) {
            if (smt.containsEdge(entry.p(), entry.q())) {
                if (entry.pqW() != null) {
                    smt.setEdgeWeight(entry.p(), entry.q(), entry.pqW());
                } else {
                    smt.removeEdge(entry.p(), entry.q());
                }
                Stream.of(entry.p(), entry.q(), entry.i()).filter(Predicate.not(smt::containsVertex)).forEach(smt::addVertex);
                DefaultWeightedEdge piEdge = smt.addEdge(entry.p(), entry.i());
                if (piEdge != null) {
                    smt.setEdgeWeight(piEdge, entry.piW());
                }
                DefaultWeightedEdge qiEdge = smt.addEdge(entry.q(), entry.i());
                if (qiEdge != null) {
                    smt.setEdgeWeight(qiEdge, entry.qiW());
                }
            }
        }
        return smt;
    }
}
