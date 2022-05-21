package steiner;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * record class containing results from running an algorithm on a problem graph
 */
public class SteinerResult {

    private SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt;
    private double weight;
    private long runtime;
    private boolean timeout;

    public SteinerResult(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt, double weight) {
        this.smt = smt;
        this.weight = weight;
    }

    public SteinerResult(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt, double weight, long runtime) {
        this(smt, weight);
        this.runtime = runtime;
    }

    /**
     * update result with new solution if weight is lower than current solution
     *
     * @param smt    solution to update result with
     * @param weight weight of smt solution
     */
    public void updateIfBetter(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt, double weight) {
        if (weight < this.weight) {
            this.smt = smt;
            this.weight = weight;
        }
    }

    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> getSmt() {
        return smt;
    }

    public void setSmt(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt) {
        this.smt = smt;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void setTimeout(boolean timeout) {
        this.timeout = timeout;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public SteinerResult withRuntime(long l) {
        this.setRuntime(l);
        return this;
    }

    public SteinerResult withRuntime(long l, boolean timeout) {
        this.setRuntime(l);
        this.setTimeout(timeout);
        return this;
    }

    @Override
    public String toString() {
        return "SteinerResult{" +
                "weight=" + weight +
                ", smt=" + smt +
                ", runtime=" + runtime +
                '}';
    }
}