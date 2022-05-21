package utils;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerEnum;
import steiner.SteinerResult;
import utils.dot.DotFileUtils;
import utils.graphextensions.GraphUtils;
import utils.stp.STPFileNameFilter;
import utils.stp.STPFileParser;
import utils.stp.STPGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * utilities for running algorithms
 */
public class RunUtils {
    // convert nanoseconds to seconds by dividing by this constant
    public static final double NANO_TO_SECONDS = 1.0e9;

    private RunUtils() {
    }

    /**
     * run an algorithm on a directory of graph files
     *
     * @param directory directory containing graph files in .stp format
     * @param algorithm the enum value of the algorithm to run on the graphs
     * @param exportSVG whether or not to export .dot and .svg files for the resulting steiner trees in the graphs
     * @return a mapping of each filepath to the results of running the algorithm instance
     * @throws Exception either one of the graphs has an invalid format or something went wrong when trying to export dot/svg files
     */
    public static Map<String, SteinerResult> runAlgorithm(String directory, SteinerEnum algorithm, boolean exportSVG) throws Exception {
        HashMap<String, SteinerResult> results = new HashMap<>();
        Set<Process> processes = new HashSet<>();

        // get stp files directory from args
        File dirFile = new File(directory);
        // only keep .stp files
        File[] files = dirFile.listFiles(new STPFileNameFilter());
        if (files == null) return new HashMap<>();

        // check for graphviz availability
        if (exportSVG) {
            try {
                new ProcessBuilder("dot", "-?").start();
            } catch (IOException e) {
                System.out.println("graphviz not found in PATH, graphs won't be exported to SVG");
                exportSVG = false;
            }
        }

        for (File file : files) {
            // parse the .stp file
            STPFileParser parser = new STPFileParser(file.getPath());
            STPGraph stpGraph = parser.readFromSTPFile();

            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = stpGraph.getGraph();
            Set<Integer> terminals = stpGraph.getTerminals();

            SteinerAlgorithm alg = algorithm.getInstance(graph, terminals);

            String fileName = file.getName();
            System.out.println("start " + algorithm.name() + " on " + fileName);
            SteinerResult algResult = alg.getResult();
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> resultSMT = algResult.getSmt();

            if (algorithm.verifyTree() && !GraphUtils.verifySteinerTree(graph, terminals, resultSMT)) {
                algResult.setWeight(Double.POSITIVE_INFINITY);
            }
            System.out.printf("%s -> %.1f (%.3fs%s)%n",
                    fileName,
                    algResult.getWeight(),
                    algResult.getRuntime() / NANO_TO_SECONDS,
                    algResult.isTimeout() ? " (timeout)" : "");
            results.put(fileName, algResult);
            if (resultSMT != null) {
                if (exportSVG) {
                    Process process = DotFileUtils.exportSVG(file, dirFile, graph, terminals, resultSMT, stpGraph);
                    if (process != null) {
                        processes.add(process);
                    }
                }
            }
        }
        processes.forEach(process -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return results;
    }

    /**
     * run algorithms on a directory of graphs and export a summary comparing the resulting weight and execution times
     * this variant runs the algorithms based on the value of 'useInTest' boolean field in SteinerEnum
     *
     * @param directory directory containing the graph files
     * @throws Exception something went wrong (see runAlgorithm)
     */
    public static void runBench(String directory) throws Exception {
        List<SteinerEnum> algs = Arrays.stream(SteinerEnum.values()).filter(SteinerEnum::useInTest).toList();
        runBench(directory, algs);
    }

    /**
     * run specified algorithms on a directory of graphs and export a summary comparing the resulting weight and execution times
     *
     * @param directory directory containing the graph files
     * @param algs      list of algorithms to run
     * @throws Exception something went wrong (see runAlgorithm)
     */
    public static void runBench(String directory, List<SteinerEnum> algs) throws Exception {
        Map<SteinerEnum, Map<String, SteinerResult>> globalResults = new EnumMap<>(SteinerEnum.class);
        for (SteinerEnum alg : algs) {
            Map<String, SteinerResult> algResults = runAlgorithm(directory, alg, false);
            globalResults.put(alg, algResults);
        }
        File dirFile = new File(directory);
        if (!dirFile.isDirectory()) {
            throw new IOException("provided path \"" + directory + "\" is not a directory");
        }
        String tablePath = String.format("%s%s%s.txt", dirFile.getPath(), File.separator, dirFile.getName());
        File tableFile = new File(tablePath);
        try (FileWriter writer = new FileWriter(tableFile)) {
            AsciiTable at = new AsciiTable();
            CWC_LongestLine cwc = new CWC_LongestLine();
            at.getRenderer().setCWC(cwc);
            cwc.add(0, 0);
            //header
            at.addRule();
            at.addRow(Stream.concat(Stream.of(""), algs.stream().map(SteinerEnum::name)).toList());
            at.addRule();
            for (String file : globalResults.get(algs.get(0)).keySet().stream().sorted().toList()) {
                List<String> concat = Stream.concat(Stream.of(file), algs.stream().map(a -> {
                    SteinerResult steinerResult = globalResults.get(a).get(file);
                    double weight = steinerResult.getWeight();
                    return String.format("%s (%.3f)",
                            weight == Double.POSITIVE_INFINITY ? "NaN" : (int) weight,
                            steinerResult.getRuntime() / NANO_TO_SECONDS);
                })).toList();
                at.addRow(concat);
                at.addRule();
//                System.out.println(globalResults);
            }
            at.setTextAlignment(TextAlignment.JUSTIFIED);
            at.setPaddingLeftRight(1);
            writer.write(at.render());
        }
        System.out.println(tablePath);
    }
}
