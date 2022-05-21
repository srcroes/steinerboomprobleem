package steiner.approx;

import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import utils.Utils;
import utils.graphextensions.ClosureWeightedEdge;
import utils.graphextensions.GraphUtils;
import utils.setutils.PowerSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 11/6-approximation using zelikovsky's algorithm:
 * - iterate over all triples t1..t3 of terminals S and determine center with minimal weight
 * *                                        (v in G for which sum(distance(v,t1..t3)) is minimal)
 * - for each triple:
 * -    - contract edges between terminals of the triple
 * -    - calculate weight of original tree - weight of the contracted tree + weight of the center point
 * - for the triple with maximum metric above of all triples:
 * - if the weight of the contracted tree + weight of the center is lower than the weight of the uncontracted tree:
 * -    - pin center as steiner vertex in set w
 * - repeat until no improvement in the maximum metric
 * - calculate smt using 2-approximation on G with S u W as terminals
 */
public class Zelikovsky_11_6 extends SteinerAlgorithm {

    public Zelikovsky_11_6(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws InterruptedException {
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths = GraphUtils.getShortestPaths(this.graph);
        SimpleWeightedGraph<Integer, ClosureWeightedEdge> graphF = GraphUtils.getMetricClosure(
                this.graph,
                this.terminals,
                paths);
        Set<Integer> setW = new HashSet<>();
        Map<Set<Integer>, Pair<Integer, Double>> mapVD = new HashMap<>();

        // iterate over all subsets of length 3 of the terminals
        PowerSet<Integer> powerSetTriples = new PowerSet<>(this.terminals, 3, true);
        for (Set<Integer> setZ : powerSetTriples) {
            double minD = Double.POSITIVE_INFINITY;
            int minV = -1;
            for (Integer v : this.graph.vertexSet()) {
                Utils.notInterrupted();
                int sum = 0;
                for (Integer s : setZ) {
                    sum += paths.getPathWeight(v, s);
                }
                if (sum < minD) {
                    minD = sum;
                    minV = v;
                }
            }
            mapVD.put(setZ, new Pair<>(minV, minD));
        }
        while (true) {
            Utils.notInterrupted();
            double treeFWeight = GraphUtils.getMST(graphF).getWeight();
            double maxWin = Double.NEGATIVE_INFINITY;
            Set<Integer> maxZ = null;
            for (Set<Integer> setZ : powerSetTriples) {
                Pair<Integer, Double> pairVD = mapVD.get(setZ);
                double dz = pairVD.getSecond();
                Map<ClosureWeightedEdge, Double> edgeWeights = new HashMap<>();
                // construct F[Z] and save edge weights to restore to F later
                for (Integer s : setZ) {
                    for (Integer s2 : setZ) {
                        if (s < s2) {
                            ClosureWeightedEdge edge = graphF.getEdge(s, s2);
                            edgeWeights.put(edge, graphF.getEdgeWeight(edge));
                            graphF.setEdgeWeight(edge, 0.0);
                        }
                    }
                }
                double treeFZWeight = GraphUtils.getMST(graphF).getWeight();
                double win = treeFWeight - treeFZWeight - dz;
                if (win > maxWin) {
                    maxWin = win;
                    maxZ = setZ;
                }

                // restore edge weights that where contracted earlier (restore F from F[Z])
                edgeWeights.forEach(graphF::setEdgeWeight);
            }
            if (maxWin <= 0) break;

            // construct F[maxZ]
            for (Integer s : maxZ) {
                for (Integer s2 : maxZ) {
                    if (s < s2) {
                        ClosureWeightedEdge edge = graphF.getEdge(s, s2);
                        graphF.setEdgeWeight(edge, 0.0);
                    }
                }
            }
            int vz = mapVD.get(maxZ).getFirst();
            setW.add(vz);
        }
        HashSet<Integer> unionWS = new HashSet<>(this.terminals);
        unionWS.addAll(setW);
        TwoApproximation twoApproximation = new TwoApproximation(this.graph, unionWS);
        twoApproximation.runInstance(result);
        return result;
    }
}
