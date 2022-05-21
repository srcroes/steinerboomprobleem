package utils.dot;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import utils.graphextensions.GraphUtils;
import utils.stp.STPGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Stefan Croes
 */
public class SteinerDotExporter {
    public static final String COLOR = "color";
    public static final String FONTCOLOR = "fontcolor";
    public static final String LABEL = "label";
    public static final String COORDS = "pos";

    public static final String RED = "red";
    public static final String BLUE = "blue";

    private final SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    private final SimpleWeightedGraph<Integer, DefaultWeightedEdge> steinerTree;
    private final Collection<Integer> terminals;
    private final double weight;
    private final STPGraph stpGraph;
    private final boolean useCoords;

    public SteinerDotExporter(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> steinerTree,
            Collection<Integer> terminals,
            STPGraph stpGraph, boolean useCoords) {
        this.graph = graph;
        this.steinerTree = steinerTree;
        this.terminals = terminals;
        this.stpGraph = stpGraph;
        this.weight = GraphUtils.getWeight(steinerTree);
        this.useCoords = useCoords;
    }

    /**
     * print graph to dot file for visualizing with graphviz
     */
    public void printToDotFile(String file) throws IOException {
        FileWriter writer = new FileWriter(file);
        DOTExporter<Integer, DefaultWeightedEdge> dotExporter = new DOTExporter<>();

        dotExporter.setEdgeAttributeProvider(this::edgeAttributeProvider);
        dotExporter.setVertexAttributeProvider(this::vertexAttributeProvider);
        dotExporter.setGraphAttributeProvider(this::graphAttributeProvider);

        dotExporter.exportGraph(graph, writer);
    }


    private Map<String, Attribute> edgeAttributeProvider(DefaultWeightedEdge e) {
        HashMap<String, Attribute> map = new HashMap<>();

        map.put(LABEL, new DefaultAttribute<>((int) this.graph.getEdgeWeight(e), AttributeType.STRING));

        Integer src = this.graph.getEdgeSource(e);
        Integer dst = this.graph.getEdgeTarget(e);

        if (this.steinerTree.containsEdge(src, dst)) {
            map.put(COLOR, new DefaultAttribute<>(RED, AttributeType.STRING));
            map.put(FONTCOLOR, new DefaultAttribute<>(RED, AttributeType.STRING));
        }
        return map;
    }

    private Map<String, Attribute> vertexAttributeProvider(Integer v) {
        HashMap<String, Attribute> map = new HashMap<>();
        if (this.steinerTree.containsVertex(v)) {
            map.put(COLOR, new DefaultAttribute<>(RED, AttributeType.STRING));
        }
        if (this.terminals.contains(v)) {
            map.put(COLOR, new DefaultAttribute<>(BLUE, AttributeType.STRING));
        }
        Map<Integer, Pair<Integer, Integer>> coords = this.stpGraph.getCoordinates();
        if (useCoords && coords != null && coords.containsKey(v)) {
            Pair<Integer, Integer> pair = coords.get(v);
            map.put(COORDS, new DefaultAttribute<>(
                    "%s,%s!".formatted(pair.getFirst(), pair.getSecond()),
                    AttributeType.STRING)
            );
        }
        return map;
    }

    private Map<String, Attribute> graphAttributeProvider() {
        HashMap<String, Attribute> map = new HashMap<>();
        map.put(LABEL,
                new DefaultAttribute<>("\"%s: %s\"".formatted(
                        this.stpGraph.getName(),
                        (int) this.weight),
                        AttributeType.STRING
                )
        );
        return map;
    }
}
