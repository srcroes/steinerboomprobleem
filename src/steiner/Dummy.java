package steiner;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;

import java.util.Set;

/**
 * Created by Stefan Croes
 */
public class Dummy extends SteinerAlgorithm {
    protected Dummy(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws InterruptedException {
        long start = System.nanoTime();
        while (System.nanoTime() - start < 7 * ((long) 1000000000) && Utils.notInterrupted()) {
        }
        return result;
    }
}
