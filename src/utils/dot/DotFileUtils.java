package utils.dot;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.stp.STPGraph;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

/**
 * Created by Stefan Croes
 */

/**
 * utilities for exporting dot/svg files from algorithm results
 */
public class DotFileUtils {
    // maximum amount of vertices in graph to export dot/svg files
    private static final int SVG_MAX_VERTICES = 250;

    private DotFileUtils() {
    }

    /**
     * export dot and svg file to separate directories for the results of an algorithm instance
     *
     * @param file      name of the graph file
     * @param dirFile   directory of the graph file
     * @param graph     the graph on which the algorithm was run
     * @param terminals the terminals used to determine the resulting steiner tree
     * @param result    the resulting steiner tree
     * @param stpGraph  parsed graph file containing metadata
     * @return process exporting the svg file from the dot file (wait for this to finish)
     * @throws IOException something file-related went wrong
     */
    public static Process exportSVG(
            File file,
            File dirFile,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> result,
            STPGraph stpGraph) throws IOException {
        // construct the paths for the dot and svg files
        String str = file.getName();
        String dotPath = "%s%sdot%s%s.dot".formatted(
                dirFile.getPath(),
                File.separator,
                File.separator,
                str.substring(0, str.lastIndexOf('.'))
        );
        String svgPath = "%s%ssvg%s%s.svg".formatted(
                dirFile.getPath(),
                File.separator,
                File.separator,
                str.substring(0, str.lastIndexOf('.'))
        );

        // export the graph to .dot file, coloring the terminals and the steiner tree
        File dotDir = new File(dotPath).getParentFile();
        if (!dotDir.exists()) {
            dotDir.mkdirs();
        }
        SteinerDotExporter exporter = new SteinerDotExporter(graph, result, terminals, stpGraph, true);
        exporter.printToDotFile(dotPath);

        // call graphviz on the .dot file to get an SVG (if graphviz is callable)
        if (graph.vertexSet().size() < SVG_MAX_VERTICES) {
            ProcessBuilder processBuilder = new ProcessBuilder("dot", "-Tsvg", "-Kfdp", dotPath);
            File svgFile = new File(svgPath);
            File svgDirFile = svgFile.getParentFile();
            if (!svgDirFile.exists()) {
                svgDirFile.mkdirs();
            }
            if (svgFile.exists() || svgFile.createNewFile()) {
                processBuilder.redirectOutput(svgFile);
                Process process = processBuilder.start();
                process.onExit().thenRun(
                        () -> System.out.println(
                                MessageFormat.format("dot process for {0} stopped", svgFile.getName())
                        )
                );
                return process;
            } else {
                System.out.printf("something went wrong creating %s%n", svgFile.getPath());
            }
        }
        return null;
    }
}
