package steiner.exact;

import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import utils.Utils;
import utils.graphextensions.GraphUtils;
import utils.setutils.PowerSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * exact algorithm by Dreyfus & Wagner
 * construct the smt bottom-up by constructing all possible subtrees using dynamic programming
 * !this needs a table of exponential size, as the table holds all subsets of the terminals
 */
public class DreyfusWagner extends SteinerAlgorithm {

    public DreyfusWagner(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws InterruptedException {
        // prep
        Set<Integer> vertices = this.graph.vertexSet();
        HashMap<Pair<Set<Integer>, Integer>, Double> stmap = new HashMap<>();
        Set<Integer> c = new HashSet<>(this.terminals);
        Integer q = Utils.getRandomSetElement(c);
        c.remove(q);
        // shortest paths
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> shortestPaths = GraphUtils.getShortestPaths(this.graph);
        /* precompute single element results */
        for (Integer t : c) {
            for (Integer j : vertices) {
                stmap.put(new Pair<>(Set.of(t), j), shortestPaths.getPathWeight(t, j));
            }
        }
        PowerSet<Integer> powerSet = new PowerSet<>(c, 2);
        /* for all subsets (mask) of terminals */
        while (powerSet.hasNext()) {
            Set<Integer> d = powerSet.next();
//            if (d.size() == c.size()) continue;
            /* for all roots i */
            for (Integer i : vertices) {
                stmap.put(new Pair<>(d, i), Double.POSITIVE_INFINITY);
            }
            for (Integer j : vertices) {
                double u = inner(stmap, d, j);
                for (Integer i : vertices) {
                    Pair<Set<Integer>, Integer> dI = new Pair<>(d, i);
                    stmap.put(dI, Math.min(stmap.get(dI), shortestPaths.getPathWeight(i, j) + u));
                }
            }
        }
        double v = Double.POSITIVE_INFINITY;
        for (Integer j : vertices) {
            double u = inner(stmap, c, j);
            v = Math.min(v, shortestPaths.getPathWeight(q, j) + u);
        }
        result.setWeight(v);
        return result;
    }

    private double inner(HashMap<Pair<Set<Integer>, Integer>, Double> stmap, Set<Integer> superset, Integer j) throws InterruptedException {
        double u = Double.POSITIVE_INFINITY;
        Integer superMin = superset.stream().min(Integer::compare).orElse(0);
        PowerSet<Integer> ePowerSet = new PowerSet<>(superset, 1);
        while (ePowerSet.hasNext()) {
            Utils.notInterrupted();
            Set<Integer> e = ePowerSet.next();
            if (e.equals(superset) || !e.contains(superMin)) continue;
            Set<Integer> temp = new HashSet<>(superset);
            temp.removeAll(e);
            u = Math.min(u, stmap.get(new Pair<>(e, j)) + stmap.get(new Pair<>(temp, j)));
        }
        return u;
    }


}
