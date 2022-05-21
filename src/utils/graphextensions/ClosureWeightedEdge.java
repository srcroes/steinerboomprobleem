package utils.graphextensions;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * weighted edge that keeps track of the path it replaced in a metric closure
 */
public class ClosureWeightedEdge extends DefaultWeightedEdge {
    private GraphPath<Integer, DefaultWeightedEdge> closurePath;

    public void setClosurePath(GraphPath<Integer, DefaultWeightedEdge> closurePath) {
        this.closurePath = closurePath;
    }

    public GraphPath<Integer, DefaultWeightedEdge> getClosurePath() {
        return closurePath;
    }

    @Override
    public String toString() {
        return "ClosureWeightedEdge{" +
                "closurePath=" + closurePath +
                '}';
    }
}
