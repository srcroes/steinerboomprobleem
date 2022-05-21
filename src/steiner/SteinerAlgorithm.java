package steiner;
/**
 * Created by Stefan Croes
 */

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.Utils;

import java.util.Set;
import java.util.concurrent.*;

/**
 * base class for steiner tree problem algorithms
 */
public abstract class SteinerAlgorithm {

    // the given graph and terminals to calculate a steiner tree on
    protected final SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    protected final Set<Integer> terminals;

    protected SteinerAlgorithm(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        this.graph = graph;
        this.terminals = terminals;
    }

    /**
     * run the instance of the algorithm with its parameters
     *
     * @param result
     * @return the results of running the instance(the resulting steiner tree and its weight)
     * note: this method should only be used internally in algorithms that depend on another steiner tree algorithm
     * it doesn't provide timing for simplicity of implementation
     */
    public abstract SteinerResult runInstance(SteinerResult result) throws Exception;

    /**
     * calls runInstance (see above) and times the execution time
     *
     * @return the results of runInstance with the addition of the execution time in nanoseconds
     */
    public final SteinerResult getResult() throws Exception {
//        // time execution of runInstance
//        long start = System.nanoTime();
//        SteinerResult result = runInstance();
//        long end = System.nanoTime();
//        return result.withRuntime(end - start);
        return getResultTimeout();
    }

    /**
     * get result with timeout (if possible result is updated during execution before the timeout)
     *
     * @return result
     * @throws Exception
     */
    public final SteinerResult getResultTimeout() throws Exception {
        SteinerResult result = new SteinerResult(null, Double.POSITIVE_INFINITY, -1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<SteinerResult> future = executor.submit(() -> {
                    long start = System.nanoTime();
                    try {
                        runInstance(result);
                    } catch (InterruptedException e) {
                        result.setTimeout(true);
                    }
                    long runtime = System.nanoTime() - start;
                    return result.withRuntime(runtime);
                }
        );
        try {
            return future.get(Utils.timeout, Utils.timeoutUnit);
        } catch (TimeoutException ex) {
//            System.out.println("timeout reached");
            future.cancel(true);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        return result;
    }
}