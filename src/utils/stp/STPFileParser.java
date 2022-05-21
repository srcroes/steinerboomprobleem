package utils.stp;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.SupplierUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by Stefan Croes
 */

/**
 * parser for the STP graph format
 */
public class STPFileParser {

    private Scanner scanner;

    private STPGraph stpGraph;

    public STPFileParser(String path) {
        try {
            scanner = new Scanner(new File(path));
            stpGraph = new STPGraph();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String stripQuotes(String string) {
        return string.replace("\"", "");
    }

    private String getNextLine() {
        return scanner.nextLine().split("#")[0];
    }

    private String[] getTokens(String string) {
        return string.split("\s+");
    }

    private String[] getTokens(String string, int arraySize) {
        return string.split("\s+", arraySize);
    }

    public STPGraph readFromSTPFile() throws Exception {
        while (scanner.hasNext()) {
            String line = getNextLine();
            String[] tokens = getTokens(line);

            if ("section".equalsIgnoreCase(tokens[0])) {
                parseSection(tokens[1]);
            }
        }
        return stpGraph;
    }

    private void parseSection(String sectionType) throws Exception {
        switch (sectionType.toLowerCase()) {
            case "comment" -> parseCommentSection();
            case "graph" -> parseGraph();
            case "terminals" -> parseTerminals();
            case "maximumdegrees" -> parseMaximumDegrees();
            case "coordinates" -> parseCoordinates();
            default -> throw new Exception("unknown section: " + sectionType);
        }
    }

    private void parseCommentSection() throws Exception {
        while (scanner.hasNext()) {
            String[] tokens = getTokens(getNextLine(), 2);

            String token = tokens[0].toLowerCase();
            if (token.equals("end")) {
                return;
            }
            String value = stripQuotes(tokens[1]);
            switch (token) {
                case "name" -> stpGraph.setName(value);
                case "date" -> stpGraph.setDate(value);
                case "creator" -> stpGraph.setCreator(value);
                case "remark" -> stpGraph.setRemark(value);
                case "problem" -> stpGraph.setProblem(value);
                default -> throw new Exception("unknown keyword: " + tokens[1]);
            }
        }
    }

    private void parseGraph() {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(
                SupplierUtil.createIntegerSupplier(1),
                SupplierUtil.createDefaultWeightedEdgeSupplier());

        while (scanner.hasNext()) {
            String[] tokens = getTokens(getNextLine());

            switch (tokens[0].toLowerCase()) {
                case "end" -> {
                    this.stpGraph.setGraph(graph);
                    return;
                }
                case "nodes" -> {
                    // call addVertex *Nodes* times
                    // the vertex ids are handled automatically by the vertexSupplier (starting from 1)
                    for (int i = 0; i < Integer.parseInt(tokens[1]); i++) {
                        graph.addVertex();
                    }
                }
                case "e" -> {
                    // add an edge and set the weight
                    DefaultWeightedEdge edge = graph.addEdge(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                    graph.setEdgeWeight(edge, Double.parseDouble(tokens[3]));
                }
                case "obstacles", "edges", "arcs", "a" -> {/*not implemented*/}
            }
        }
    }

    private void parseTerminals() {
        Set<Integer> terminals = new HashSet<>();
        while (scanner.hasNext()) {
            String[] tokens = getTokens(getNextLine());
            switch (tokens[0].toLowerCase()) {
                case "end" -> {
                    this.stpGraph.setTerminals(terminals);
                    return;
                }
                case "terminals" -> {/*not implemented*/}
                case "t" -> terminals.add(Integer.parseInt(tokens[1]));
            }
        }
    }

    private void parseMaximumDegrees() {
        /*not implemented*/
    }

    private void parseCoordinates() {
        HashMap<Integer, Pair<Integer, Integer>> coordinates = new HashMap<>();
        while (scanner.hasNext()) {
            String[] tokens = getTokens(getNextLine());
            switch (tokens[0].toLowerCase()) {
                case "end" -> {
                    this.stpGraph.setCoordinates(coordinates);
                    return;
                }
                case "dd" -> coordinates.put(
                        Integer.parseInt(tokens[1]), new Pair<>(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]))
                );
            }
        }
    }
}
