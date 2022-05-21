import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerEnum;
import steiner.SteinerResult;
import steiner.localsearch.HybridGRASP;
import steiner.preprocessing.*;
import utils.Logger;
import utils.RunUtils;
import utils.graphextensions.GraphUtils;
import utils.setutils.PowerSet;
import utils.stp.STPFileParser;
import utils.stp.STPGraph;

import java.util.List;
import java.util.Set;

/**
 * Created by Stefan Croes
 */
public class Main {


    public static void main(String[] args) throws Exception {
        Logger.setPrintDebug(false);
        List<SteinerEnum> algs = List.of(
////                SteinerEnum.DUMMY_LOOP
//                SteinerEnum.TWO_APPROXIMATION,
//                SteinerEnum.ZELIKOVSKY
//                SteinerEnum.FAST_LOCAL_SEARCH,
//                SteinerEnum.LOCAL_VERTEX_INSERTION,
//                SteinerEnum.LOCAL_VERTEX_ELIMINATION,
//                SteinerEnum.LOCAL_KEY_PATH_EXCHANGE,
//                SteinerEnum.LOCAL_KEY_VERTEX_ELIMINATION,
                SteinerEnum.HYBRID_GRASP_WITH_PERTURBATIONS
//                SteinerEnum.KRUSKAL_CONSTRUCTION,
//                SteinerEnum.MST_CONSTRUCTION,
//                SteinerEnum.SHORTEST_PATH_CONSTRUCTION,
//                SteinerEnum.TWO_APPROX_CONSTRUCTION
        );
//        String path = "graphs/steinlib/";
//        List<String> dirs = List.of("B", "C", "D", "E", "LIN", "DIW");
//////        List<String> dirs = List.of("D");
//        for (String s : dirs) {
//            RunUtils.runBench(path + s, algs);
//        }
        benchmarkPP();

//        RunUtils.runBench("graphs/steinlib/B", SteinerEnum.DREYFUS_WAGNER, false);
    }

    private static void benchmarkPP() throws Exception {
//        List<String> list = List.of("01", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");
        List<String> list = List.of(/*"01", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11",*/ "12", "13", "14", "15", "16", "17", "18", "19", "20");
        List<String> list2 = List.of(/*
                "lin01.stp",
                "lin01.stp",
                "lin02.stp",
                "lin03.stp",
                "lin04.stp",
                "lin05.stp",
                "lin06.stp",
                "lin07.stp",
                "lin08.stp",
                "lin09.stp",
                "lin10.stp",
                "lin11.stp",
                "lin12.stp",
                "lin13.stp",
                "lin14.stp",
                "lin15.stp",
                "lin16.stp",
                "lin17.stp",
                "lin18.stp",
                "lin19.stp",
                "lin20.stp",*/
                "lin21.stp",
                "lin22.stp",
                "lin23.stp"
        );
//        for (String file : list) {
//            if (Integer.parseInt(file) <= 18)
//                testPP("graphs/steinlib/B/b" + file + ".stp");
//        }
//        for (String file : list) {
//            testPP("graphs/steinlib/C/c" + file + ".stp");
//        }
//        for (String file : list) {
//            testPP("graphs/steinlib/D/d" + file + ".stp");
//        }
//        for (String file : list) {
//            testPP("graphs/steinlib/E/e" + file + ".stp");
//        }
        for (String file : list2) {
            testPP("graphs/steinlib/LIN/" + file);
        }
    }

    public static void testPP(String path) throws Exception {
        final String FORMAT = "\t%s | original: %s, SD: %s, SD2: %s, 1-2: %s, SD2->1-2: %s, 1-2->SD2: %s, LC: %s, 1-2_LC: %s, LC_1-2: %s%n";
        STPFileParser parser = new STPFileParser(path);
        STPGraph stpGraph = parser.readFromSTPFile();
        System.out.println(path);

        Set<Integer> terminals = stpGraph.getTerminals();
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = stpGraph.getGraph();

        // preprocessing
        long t01 = System.nanoTime();
        PPMethod ppMethod12_2 = new DegreeOneTwoPP(stpGraph.getGraph(), stpGraph.getTerminals());
        PPMethod ppMethod12_LC = new LeastCostPP(ppMethod12_2.preprocessing(), stpGraph.getTerminals());
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph12_LC = ppMethod12_LC.preprocessing();
        long t02 = System.nanoTime();
        long t_12_LC = t02 - t01;
        PPMethod ppMethodLC_3 = new LeastCostPP(stpGraph.getGraph(), stpGraph.getTerminals());
        PPMethod ppMethodLC_12 = new DegreeOneTwoPP(ppMethodLC_3.preprocessing(), stpGraph.getTerminals());
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphLC_12 = ppMethodLC_12.preprocessing();
        t01 = System.nanoTime();
        long t_LC_12 = t01 - t02;
        PPMethod ppMethodLC = new LeastCostPP(stpGraph.getGraph(), stpGraph.getTerminals());
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphLC = ppMethodLC.preprocessing();
        t02 = System.nanoTime();
        long t_LC = t02 - t01;

        long t1 = System.nanoTime();
        PPMethod oneTwo = new DegreeOneTwoPP(graph, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph12 = oneTwo.preprocessing();
        long t2 = System.nanoTime();
        PPMethod sd = new SpecialDistancePP(graph, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphSD = sd.preprocessing();
        long t3 = System.nanoTime();
        PPMethod sd2 = new SpecialDistance2PP(graph, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphSD2 = sd2.preprocessing();
        long t4 = System.nanoTime();
        PPMethod sd2pre = new SpecialDistance2PP(graph, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphSD2pre = sd2pre.preprocessing();
        PPMethod oneTwoPP = new DegreeOneTwoPP(graphSD2pre, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graphSD2OneTwo = oneTwoPP.preprocessing();
        long t5 = System.nanoTime();
        PPMethod oneTwoPre = new DegreeOneTwoPP(graph, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph12pre = oneTwoPre.preprocessing();
        PPMethod sd2PP = new SpecialDistance2PP(graph12pre, terminals);
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph12SD2 = sd2PP.preprocessing();
        long t6 = System.nanoTime();

        System.out.printf(FORMAT, "#edges   ", graph.edgeSet().size(), graphSD.edgeSet().size(), graphSD2.edgeSet().size(), graph12.edgeSet().size(),
                graphSD2OneTwo.edgeSet().size(), graph12SD2.edgeSet().size(),
                graphLC.edgeSet().size(), graph12_LC.edgeSet().size(), graphLC_12.edgeSet().size());
        System.out.printf(FORMAT, "#vertices", graph.vertexSet().size(), graphSD.vertexSet().size(), graphSD2.vertexSet().size(), graph12.vertexSet().size(),
                graphSD2OneTwo.vertexSet().size(), graph12SD2.vertexSet().size(),
                graphLC.vertexSet().size(), graph12_LC.vertexSet().size(), graphLC_12.vertexSet().size());
        double nano = 1000000000d;
        System.out.printf(FORMAT, "pp time  ", 0, (t3 - t2) / nano, (t4 - t3) / nano, (t2 - t1) / nano,
                (t5 - t4) / nano, (t6 - t5) / nano, t_LC / nano, t_12_LC / nano, t_LC_12 / nano);

        // grasp
        long t10 = System.nanoTime();
        SteinerAlgorithm ogAlg = new HybridGRASP(graph, terminals);
        SteinerResult ogResult = ogAlg.getResult();
        long t11 = System.nanoTime();
        SteinerAlgorithm oneTwoAlg = new HybridGRASP(graph12, terminals);
        SteinerResult oneTwoResult = oneTwoAlg.getResult();
        oneTwoResult.setSmt(oneTwo.backtracking(oneTwoResult.getSmt()));
        long t12 = System.nanoTime();
        SteinerAlgorithm sdAlg = new HybridGRASP(graphSD, terminals);
        SteinerResult sdResult = sdAlg.getResult();
        long t13 = System.nanoTime();
        SteinerAlgorithm sd2Alg = new HybridGRASP(graphSD2, terminals);
        SteinerResult sd2Result = sd2Alg.getResult();
        long t14 = System.nanoTime();
        SteinerAlgorithm sd2OneTwoAlg = new HybridGRASP(graphSD2OneTwo, terminals);
        SteinerResult sd2OneTwoResult = sd2OneTwoAlg.getResult();
        sd2OneTwoResult.setSmt(oneTwoPP.backtracking(sd2OneTwoResult.getSmt()));
        long t15 = System.nanoTime();
        SteinerAlgorithm oneTwoSD2Alg = new HybridGRASP(graph12SD2, terminals);
        SteinerResult oneTwoSD2Result = oneTwoSD2Alg.getResult();
        oneTwoSD2Result.setSmt(oneTwoPre.backtracking(oneTwoSD2Result.getSmt()));
        long t16 = System.nanoTime();
        SteinerAlgorithm algLC = new HybridGRASP(graphLC, stpGraph.getTerminals());
        SteinerResult resultLC = algLC.getResult();
        long t17 = System.nanoTime();
        SteinerAlgorithm alg12LC = new HybridGRASP(graph12_LC, stpGraph.getTerminals());
        SteinerResult result12LC = alg12LC.getResult();
        result12LC.setSmt(ppMethod12_2.backtracking(result12LC.getSmt()));
        long t18 = System.nanoTime();
        SteinerAlgorithm algLC12 = new HybridGRASP(graphLC_12, stpGraph.getTerminals());
        SteinerResult resultLC12 = algLC12.getResult();
        resultLC12.setSmt(ppMethodLC_12.backtracking(result12LC.getSmt()));
        long t19 = System.nanoTime();


        System.out.printf(FORMAT, "verified ",
                GraphUtils.verifySteinerTree(graph, terminals, ogResult.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, sdResult.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, sd2Result.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, oneTwoResult.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, sd2OneTwoResult.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, oneTwoSD2Result.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, resultLC.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, result12LC.getSmt()),
                GraphUtils.verifySteinerTree(graph, terminals, resultLC12.getSmt())
        );
        System.out.printf(FORMAT, "weight   ", ogResult.getWeight(), sdResult.getWeight(), sd2Result.getWeight(), oneTwoResult.getWeight(),
                sd2OneTwoResult.getWeight(), oneTwoSD2Result.getWeight(),
                resultLC.getWeight(), result12LC.getWeight(), resultLC12.getWeight());
        System.out.printf(FORMAT, "time     ", (t11 - t10) / nano, (t13 - t12) / nano, (t14 - t13) / nano, (t12 - t11) / nano,
                (t15 - t14) / nano, (t16 - t15) / nano, (t17 - t16) / nano, (t18 - t17) / nano, (t19 - t18) / nano);
    }
}
