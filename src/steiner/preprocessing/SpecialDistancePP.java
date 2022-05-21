package steiner.preprocessing;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import utils.graphextensions.ClosureWeightedEdge;
import utils.graphextensions.GraphUtils;

import java.util.*;

/**
 * special distance''
 * this version uses the largest closureEdge in the path in closureMST
 */
public class SpecialDistancePP extends PPMethod {
    private final FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths;

    public SpecialDistancePP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
        this.paths = new FloydWarshallShortestPaths<>(graph);
    }

    public SpecialDistancePP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals,
                             FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths) {
        super(graph, terminals);
        this.paths = paths;
    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> preprocessing() throws InterruptedException {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> cGraph = GraphUtils.copyGraph(graph);
        // 1a. metric closure / distance graph = (K, d)
        SimpleWeightedGraph<Integer, ClosureWeightedEdge> closure = GraphUtils.getMetricClosure(
                cGraph,
                this.terminals,
                this.paths
        );

        // 1b. MST of (K, d)
        SpanningTreeAlgorithm.SpanningTree<ClosureWeightedEdge> closureMST = GraphUtils.getMST(closure);
        // get delta (most costly edge in MST((K,d))) for later
        Optional<ClosureWeightedEdge> maxOpt = closureMST.getEdges().stream().max(Comparator.comparing(closure::getEdgeWeight));
        if (maxOpt.isEmpty())
            throw new IllegalStateException("MST of closure should have a largest edge");
        ClosureWeightedEdge deltaE = maxOpt.get();
        double delta = closure.getEdgeWeight(deltaE);
        SimpleWeightedGraph<Integer, ClosureWeightedEdge> closureMSTGraph = GraphUtils.getGraphFromClosureSpanningTree(closureMST, closure);

        // 2. calculate bottleneck lengths of edges
        FloydWarshallShortestPaths<Integer, ClosureWeightedEdge> pathsMST = new FloydWarshallShortestPaths<>(closureMSTGraph);
        HashMap<Pair<Integer, Integer>, Double> blSTR = new HashMap<>();
        for (Integer tSrc : terminals) {
            for (Integer tTgt : terminals) {
                if (tSrc >= tTgt) continue;
                GraphPath<Integer, ClosureWeightedEdge> path = pathsMST.getPath(tSrc, tTgt);
                Optional<ClosureWeightedEdge> optBL = path.getEdgeList().stream().max(Comparator.comparing(closureMSTGraph::getEdgeWeight));
                if (optBL.isEmpty()) {
                    throw new IllegalStateException("distance graph path should not be empty");
                }
                DefaultWeightedEdge bottleneckEdge = optBL.get();
                //test
//                double maxEdge = 0d;
//                for (ClosureWeightedEdge closureWeightedEdge : path.getEdgeList()) {
//                    GraphPath<Integer, DefaultWeightedEdge> closurePath = closureWeightedEdge.getClosurePath();
//                    Optional<DefaultWeightedEdge> maxEdgeOpt = closurePath.getEdgeList().stream().max(Comparator.comparing(graph::getEdgeWeight));
//                    if (maxEdgeOpt.isEmpty()) {
//                        throw new IllegalStateException("should have a maximum edge");
//                    }
//                    double maxWeight = graph.getEdgeWeight(maxEdgeOpt.get());
//                    if (maxWeight > maxEdge) {
//                        maxEdge = maxWeight;
//                    }
//
//                }
                blSTR.put(new Pair<>(tSrc, tTgt), cGraph.getEdgeWeight(bottleneckEdge));
                blSTR.put(new Pair<>(tTgt, tSrc), cGraph.getEdgeWeight(bottleneckEdge));
            }
        }
//        System.out.println("blSTR: " + blSTR);
        // 3. for every non-terminal: compute 3 shortest paths to a terminal
        HashSet<Integer> vertices = new HashSet<>(cGraph.vertexSet());
        vertices.removeAll(terminals);
        HashMap<Integer, List<Pair<Integer, Double>>> d3Map = new HashMap<>();
        for (Integer i : vertices) {
            List<Pair<Integer, Double>> paths3 = terminals
                    .stream()
                    .map(t -> paths.getPath(i, t))
                    .sorted(Comparator.comparing(GraphPath::getWeight))
                    .limit(3)
                    .map(path -> new Pair<>(
                                    path.getStartVertex().equals(i) ? path.getEndVertex() : path.getStartVertex(),
                                    path.getWeight()
                            )
                    )
                    .toList();
            d3Map.put(i, paths3);
        }
        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>();
        // for each edge ij compute sd''_ij from bottleneckLenthsS and d3Map
        for (DefaultWeightedEdge edge : cGraph.edgeSet()) {
            Integer src = cGraph.getEdgeSource(edge);
            Integer tgt = cGraph.getEdgeTarget(edge);
            int i = Math.min(src, tgt);
            int j = Math.max(src, tgt);
            Set<Double> choices = new HashSet<>();
            choices.add(delta);
            choices.add(cGraph.getEdgeWeight(edge));
            // calculate S^tr_ij for t,r \in 1, 2, 3
            List<Pair<Integer, Double>> ts = d3Map.get(i);
            List<Pair<Integer, Double>> rs = d3Map.get(j);
            if (ts != null && rs != null) { // i, j not terminals
//                System.out.println("i, j not terminals");
//                System.out.println("\t" + ts);
//                System.out.println("\t" + rs);
                addSTRs(blSTR, choices, ts, rs);
            } else if (ts != null) { // i not terminal, j terminal
//                System.out.println("i not terminal, j terminal");
                addSTRHalf(blSTR, choices, ts, j);
            } else if (rs != null) { // j not terminal, i terminal
//                System.out.println("j not terminal, i terminal");
                addSTRHalf(blSTR, choices, rs, i);
            } else { // i, j terminal
//                System.out.println("i, j terminals");
                choices.add(blSTR.get(new Pair<>(i, j)));
            }
            Optional<Double> specialDistanceOpt = choices.stream().min(Double::compareTo);
            if (specialDistanceOpt.isEmpty())
                throw new IllegalStateException("there should be a special distance upper bound for every edge");
            Double specialDistance = specialDistanceOpt.get();
            if (specialDistance < cGraph.getEdgeWeight(edge)) {
                edgesToRemove.add(edge);
            }
        }
        // remove selected edges (where sd''_ij < c_ij)
        cGraph.removeAllEdges(edgesToRemove);
        return cGraph;
    }

    // add Str values in case i, j not terminals
    private void addSTRs(HashMap<Pair<Integer, Integer>, Double> blSTR, Set<Double> choices, List<Pair<Integer, Double>> ts, List<Pair<Integer, Double>> rs) {
        for (Pair<Integer, Double> t : ts) {
            for (Pair<Integer, Double> r : rs) {
                Integer tInt = t.getFirst();
                Integer rInt = r.getFirst();
                double bl = tInt.equals(rInt) ? 0d : blSTR.get(new Pair<>(tInt, rInt));
                double sTR = Math.max(Math.max(
                                t.getSecond(),
                                bl),
                        r.getSecond());
                choices.add(sTR);
            }
        }
    }

    // add Str values in case 1 terminal
    private void addSTRHalf(HashMap<Pair<Integer, Integer>, Double> blSTR, Set<Double> choices, List<Pair<Integer, Double>> xs, Integer terminal) {
        for (Pair<Integer, Double> x : xs) {
            Integer xInt = x.getFirst();
            double bl = terminal.equals(xInt) ? 0d : blSTR.get(new Pair<>(terminal, xInt));
            double sTR = Math.max(
                    bl,
                    x.getSecond());
            choices.add(sTR);
        }

    }

    @Override
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> backtracking(SimpleWeightedGraph<Integer, DefaultWeightedEdge> smt) {
        return smt;
    }
}
