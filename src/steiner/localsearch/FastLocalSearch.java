package steiner.localsearch;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import steiner.approx.TwoApproximation;
import utils.Utils;
import utils.graphextensions.GraphUtils;
import utils.graphextensions.VoronoiDiagram;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 4 local searches from "Fast Local Search for Steiner Trees in Graphs"
 * not every implementation is completely faithful to the paper for simplicity,
 * but every local search follows the idea of the paper
 */
public class FastLocalSearch extends SteinerAlgorithm {

    private final boolean vInsertion;
    private final boolean vElimination;
    private final boolean kPExchange;
    private final boolean kVElimination;

    private final FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> paths;

    private SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution;
    private double weight;
    private SteinerResult result;

    public FastLocalSearch(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        this(graph, terminals, true, true, true, true);
    }

    public FastLocalSearch(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Set<Integer> terminals,
            boolean vInsertion,
            boolean vElimination,
            boolean kPExchange,
            boolean kVElimination
    ) {
        super(graph, terminals);
        this.vInsertion = vInsertion;
        this.vElimination = vElimination;
        this.kPExchange = kPExchange;
        this.kVElimination = kVElimination;
        paths = new FloydWarshallShortestPaths<>(this.graph);
    }

    /**
     * - run every local search as long as it improves
     * - repeat if any local search improved this iteration
     *
     * @param result object to put results in in case the algorithm times out
     * @return result
     * @throws Exception
     */
    @Override
    public SteinerResult runInstance(SteinerResult result) throws Exception {
        this.result = result;
        // get starting solution
        TwoApproximation twoApproximation = new TwoApproximation(this.graph, this.terminals, this.paths);
        twoApproximation.runInstance(result);
        solution = result.getSmt();
        weight = result.getWeight();

        boolean improved = true;
        while (improved && Utils.notInterrupted()) {
            improved = false;

            // key path exchange
            if (kPExchange) {
                boolean kPExchangeImproved = false;
                boolean iterImproved;
                do {
                    iterImproved = alternativeKeyPathExchange();
                    kPExchangeImproved |= iterImproved;
                } while (iterImproved && Utils.notInterrupted());
                improved |= kPExchangeImproved;
            }

            // key vertex elimination
            if (kVElimination) {
                boolean kVEliminationImproved = false;
                boolean iterImproved;
                do {
                    iterImproved = keyVertexElimination();
                    kVEliminationImproved |= iterImproved;
                } while (iterImproved && Utils.notInterrupted());
                improved |= kVEliminationImproved;
            }

            // steiner vertex insertion
            if (vInsertion) {
                boolean vInsertionImproved = false;
                boolean iterImproved;
                do {
                    iterImproved = steinerVertexInsertion();
                    vInsertionImproved |= iterImproved;
                } while (iterImproved && Utils.notInterrupted());
                improved |= vInsertionImproved;
            }

            //steiner vertex elimination
            if (vElimination) {
                boolean vEliminationImproved = false;
                boolean iterImproved;
                do {
                    iterImproved = simpleSteinerVertexElimination();
                    vEliminationImproved |= iterImproved;
                } while (iterImproved && Utils.notInterrupted());
                improved |= vEliminationImproved;
            }
        }

        return this.result;
    }

    /**
     * execute one pass of steiner vertex insertion on the current solution
     * see paper for explanation as the implementation follows it pretty closely
     *
     * @return whether the pass improved the solution
     */
    private boolean steinerVertexInsertion() {
        boolean improved = false;
        HashSet<Integer> vertices = new HashSet<>(this.graph.vertexSet());
        Set<Integer> solutionVs = solution.vertexSet();
        vertices.removeAll(solutionVs);
        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>();
        Set<DefaultWeightedEdge> edgesToAdd = new HashSet<>();

        for (Integer v : vertices) {
            List<DefaultWeightedEdge> edges = new ArrayList<>();
            // compute e1,e2,... here instead of before the loop
            for (DefaultWeightedEdge edge : this.graph.edgesOf(v)) {
                Integer src = this.graph.getEdgeSource(edge);
                Integer trg = this.graph.getEdgeTarget(edge);
                if ((src.equals(v) && solution.vertexSet().contains(trg))
                        || (trg.equals(v) && solution.vertexSet().contains(src))) {
                    edges.add(edge);
                }
            }

            //compute S1
            if (edges.isEmpty()) continue;
            solution.addVertex(v);
            DefaultWeightedEdge edge = edges.remove(0);
            DefaultWeightedEdge edge1 = solution.addEdge(this.graph.getEdgeSource(edge), this.graph.getEdgeTarget(edge));
            solution.setEdgeWeight(edge1, this.graph.getEdgeWeight(edge));
            edgesToRemove.add(edge1);

            //compute Si
            for (DefaultWeightedEdge edgeI : edges) {
                Integer src = this.graph.getEdgeSource(edgeI);
                Integer wI = src.equals(v) ? this.graph.getEdgeTarget(edgeI) : src;
                DijkstraShortestPath<Integer, DefaultWeightedEdge> path = new DijkstraShortestPath<>(solution);
                GraphPath<Integer, DefaultWeightedEdge> graphPath = path.getPath(v, wI);
                Optional<DefaultWeightedEdge> maxOpt = graphPath
                        .getEdgeList()
                        .stream()
                        .max(Comparator.comparing(this.graph::getEdgeWeight));
                if (maxOpt.isPresent()) {
                    DefaultWeightedEdge maxEdge = maxOpt.get();
                    if (this.graph.getEdgeWeight(maxEdge) > this.graph.getEdgeWeight(edgeI)) {
                        solution.removeEdge(maxEdge);
                        edgesToAdd.add(maxEdge);
                        DefaultWeightedEdge addedEdge = solution.addEdge(v, wI);
                        solution.setEdgeWeight(addedEdge, this.graph.getEdgeWeight(edgeI));
                        edgesToRemove.add(addedEdge);
                    }
                }
            }
            Double newWeight = GraphUtils.getWeight(solution);
            if (this.weight > newWeight) {
                // keep altered solution
                this.weight = newWeight;
                improved = true;
                result.setSmt(this.solution);
                result.setWeight(this.weight);
//                System.out.println("updated result");
            } else {
                // restore solution
                edgesToRemove.forEach(solution::removeEdge);
                for (DefaultWeightedEdge e : edgesToAdd) {
                    DefaultWeightedEdge edge2 = solution.addEdge(this.graph.getEdgeSource(e), this.graph.getEdgeTarget(e));
                    solution.setEdgeWeight(edge2, this.graph.getEdgeWeight(e));
                }
                solution.removeVertex(v);
            }
            edgesToRemove.clear();
            edgesToAdd.clear();
        }
        return improved;
    }

    /**
     * execute one pass of steiner vertex elimination on the current solution
     * this comes down to the following basic steps:
     * *    for each vertex v in 'Vs \ T' (= the set of steiner vertices)
     * *        - create subgraph of the full problem graph induced by 'Vs \ {v}'
     * *        - determine the MST of this subgraph (using kruskal)
     * *        - if this MST has lower total weight: replace current solution with the MST
     * !note that it might not be possible to determine a single connected MST of the subgraph
     * (e.g. when the vertex v that is deleted connects 2 components of the graph)
     * because of this the MST has to be verified as a correct steiner tree (using GraphUtils.verifySteinerTree)
     *
     * @return whether the pass improved the solution
     */
    private boolean simpleSteinerVertexElimination() {
        boolean improved = false;
        Set<Integer> vertices = new HashSet<>(solution.vertexSet());
        vertices.removeAll(terminals);
        for (Integer v : vertices) {
            HashSet<Integer> subVertices = new HashSet<>(solution.vertexSet());
            subVertices.remove(v);
            AsSubgraph<Integer, DefaultWeightedEdge> subgraph = new AsSubgraph<>(this.graph, subVertices);
            SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> mst = GraphUtils.getMST(subgraph);
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> newSol = GraphUtils.getGraphFromSpanningTree(mst, this.graph);
            double newWeight = mst.getWeight();
            // verify the steiner tree (could have made it impossible for the tree to be connected by excluding the vertex)
            if (this.weight > newWeight && GraphUtils.verifySteinerTree(this.graph, this.terminals, newSol)) {
                // keep altered solution
                this.weight = newWeight;
                solution = newSol;
                improved = true;
                result.setSmt(this.solution);
                result.setWeight(this.weight);
//                System.out.println("updated result");
            }
        }
        return improved;
    }


    /**
     * voronoi version, has some bugs
     *
     * @return
     * @throws Exception
     */
    private boolean keyPathExchange() throws Exception {
//        System.out.println("weight: " + this.weight);
        VoronoiDiagram voronoi = new VoronoiDiagram(this.graph, this.solution.vertexSet());
        Set<Integer> crucialVs = new HashSet<>(this.terminals);
        crucialVs.addAll(this.solution.vertexSet().stream().filter(v -> this.solution.edgesOf(v).size() >= 3).toList());
        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> solutionPaths = GraphUtils.getShortestPaths(this.solution);

        for (Integer crucialV : crucialVs) {
            for (Integer crucialW : crucialVs) {
                if (!crucialV.equals(crucialW)) {
                    GraphPath<Integer, DefaultWeightedEdge> path = solutionPaths.getPath(crucialV, crucialW);

                    if (path == null)
                        continue;

                    List<Integer> vertexList = path.getVertexList();
                    long count = vertexList.stream().filter(crucialVs::contains).count();
                    if (count == 2) { // only the start and end of the path are crucial vertices
                        // remove key path (= remove internal vertices and their incident edges (implicitly when removing vertices))
                        Set<Integer> verticesToAdd = new HashSet<>(vertexList.subList(1, vertexList.size() - 1));
                        Set<DefaultWeightedEdge> edgesToAdd = new HashSet<>(verticesToAdd
                                .stream()
                                .map(this.solution::edgesOf)
                                .flatMap(Collection::stream)
                                .toList());
                        if (vertexList.size() == 2) {
                            DefaultWeightedEdge edge = this.solution.removeEdge(crucialV, crucialW);
                            edgesToAdd.add(edge);
//                            System.out.println("removed " + edge);
                        }
                        this.solution.removeAllVertices(verticesToAdd);

                        // get 2 subtrees
                        ConnectivityInspector<Integer, DefaultWeightedEdge> inspector = new ConnectivityInspector<>(this.solution);
                        List<Set<Integer>> connectedSets = inspector.connectedSets();
                        if (connectedSets.size() != 2) {
//                            System.out.println("ERROR");
//                            System.out.println(this.solution);
//                            System.out.println(vertexList);
//                            System.out.println(connectedSets);
                            throw new Exception("removing a key path \"" + vertexList + "\" didn't result in 2 subtrees, this should be impossible!");
                        }
                        Set<Integer> treeA = connectedSets.get(0);
                        // connect 2 subtrees
//                        voronoi.fullVoronoi(this.solution.vertexSet());
                        voronoi = new VoronoiDiagram(this.graph, this.solution.vertexSet());
                        Map<Integer, Integer> base = voronoi.getBase();
                        Map<Integer, Set<DefaultWeightedEdge>> boundaries = voronoi.getBoundaries();
                        Map<DefaultWeightedEdge, Pair<Integer, Integer>> inverseBoundaries = voronoi.getInverseBoundaries();
                        Map<Integer, Double> vdist = voronoi.getVdist();
                        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> fullShortestPaths = voronoi.getShortestPaths();
                        Map<Integer, Integer> predecessor = voronoi.getPredecessor();

                        Set<Integer> basesA = treeA.stream().map(base::get).collect(Collectors.toSet());
                        Set<Integer> basesB = new HashSet<>(voronoi.getBases());
                        basesB.removeAll(basesA);

                        // find best boundary edge
                        Set<DefaultWeightedEdge> candidateEdges = basesA
                                .stream()
                                .map(boundaries::get)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toSet());
                        double bestWeight = Double.POSITIVE_INFINITY;
                        DefaultWeightedEdge bestEdge = null;
                        for (DefaultWeightedEdge candidateEdge : candidateEdges) {
                            Pair<Integer, Integer> pair = inverseBoundaries.get(candidateEdge);
                            Integer first = pair.getFirst();
                            Integer second = pair.getSecond();
                            Integer src = this.graph.getEdgeSource(candidateEdge);
                            Integer trg = this.graph.getEdgeTarget(candidateEdge);
                            if ((basesA.contains(first) && basesB.contains(second))
                                    || (basesA.contains(second) && basesB.contains(first))) {
                                // edge connects the trees
                                double edgeCost = vdist.get(first) + fullShortestPaths.getPathWeight(src, trg) + vdist.get(second);
                                if (edgeCost < bestWeight) {
                                    bestWeight = edgeCost;
                                    bestEdge = candidateEdge;
                                }
                            }
                        }
                        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>();
                        Set<Integer> verticesToRemove = new HashSet<>();
                        if (bestEdge != null) {
                            // add path containing best edge to solution
                            Integer src = this.graph.getEdgeSource(bestEdge);
                            Integer trg = this.graph.getEdgeTarget(bestEdge);

                            Integer vA = basesA.contains(base.get(src)) ? src : trg;
                            Integer vB = vA.equals(trg) ? src : trg;

                            sidePath(predecessor, basesA, vA, edgesToRemove, verticesToRemove, voronoi, basesB);
                            sidePath(predecessor, basesB, vB, edgesToRemove, verticesToRemove, voronoi, basesA);
                            DefaultWeightedEdge edge = this.solution.addEdge(vA, vB);
                            this.solution.setEdgeWeight(edge, this.graph.getEdgeWeight(this.graph.getEdge(vA, vB)));

                            // check new solution
                            assert GraphUtils.verifySteinerTree(this.graph, this.terminals, this.solution);
                            Double newWeight = GraphUtils.getWeight(this.solution);
                            if (newWeight < this.weight) {
//                                System.out.println("new weight: " + newWeight);
                                this.weight = newWeight;
                                result.setSmt(this.solution);
                                result.setWeight(this.weight);
//                                System.out.println("updated result");
                                return true;
                            }
                        }

                        //restore graph
                        edgesToRemove.forEach(this.solution::removeEdge);
                        verticesToRemove.forEach(this.solution::removeVertex);
                        verticesToAdd.forEach(this.solution::addVertex);
                        edgesToAdd.forEach(e -> this.solution.addEdge(this.solution.getEdgeSource(e), this.solution.getEdgeTarget(e), e));
                    }
                }
            }
        }
        return false;
    }

    /**
     * add path from one side of boundary edge to base(that side)
     *
     * @param predecessor      from voronoi
     * @param basesB           bases of side subtree
     * @param vB               vertex from side of boundary edge
     * @param edgesToRemove
     * @param verticesToRemove
     */
    private void sidePath(
            Map<Integer, Integer> predecessor,
            Set<Integer> basesB,
            Integer vB,
            Set<DefaultWeightedEdge> edgesToRemove,
            Set<Integer> verticesToRemove,
            VoronoiDiagram voronoiDiagram,
            Set<Integer> otherBases
    ) {
        Integer currentB = vB;
        if (!basesB.contains(currentB)) {
            Integer predB = predecessor.get(currentB);
            if (!this.solution.containsVertex(currentB)) {
                this.solution.addVertex(currentB);
                verticesToRemove.add(currentB);
            }
            while (!basesB.contains(currentB)) {
                if (!this.solution.containsVertex(predB)) {
                    this.solution.addVertex(predB);
                    verticesToRemove.add(predB);
                }
                if (!this.solution.containsEdge(currentB, predB)) {
                    if (currentB.equals(predB)) {
//                        System.out.println("weird");
//                        System.out.println(vB);
//                        System.out.println(currentB);
//                        System.out.println(basesB);
//                        System.out.println(otherBases);
//                        System.out.println(predecessor);
//                        System.out.println(voronoiDiagram);
                    }
                    DefaultWeightedEdge e = this.solution.addEdge(currentB, predB);
                    this.solution.setEdgeWeight(e, this.graph.getEdgeWeight(this.graph.getEdge(currentB, predB)));
                    edgesToRemove.add(e);
                }
                currentB = predB;
                predB = predecessor.get(predB);
            }
        }
    }

    /**
     * try to find a single improving key path exchange move:
     * - remove a key path (a path between 2 crucial vertices that doesnt contain any other crucial vertex)
     * - try to connect the resulting components as cheaply as possible
     * - if connection is cheaper: keep improved tree
     *
     * @return whether an improvement was found
     * @throws Exception something went wrong
     */
    private boolean alternativeKeyPathExchange() throws Exception {

        Set<Integer> crucialVs = new HashSet<>(this.terminals);
        crucialVs.addAll(this.solution.vertexSet().stream().filter(v -> this.solution.edgesOf(v).size() >= 3).toList());
//        FloydWarshallShortestPaths<Integer, DefaultWeightedEdge> solutionPaths = GraphUtils.getShortestPaths(this.solution);
        ShortestPathAlgorithm<Integer, DefaultWeightedEdge> solutionPaths = new DijkstraShortestPath<>(this.solution);

        for (Integer crucialV : crucialVs) {
            for (Integer crucialW : crucialVs) {
                if (crucialV < crucialW) {
                    GraphPath<Integer, DefaultWeightedEdge> path = solutionPaths.getPath(crucialV, crucialW);
                    List<Integer> vertexList = path.getVertexList();
                    long count = vertexList.stream().filter(crucialVs::contains).count();
                    if (count == 2) { // only the start and end of the path are crucial vertices

                        // remove key path (= remove internal vertices and their incident edges (implicitly when removing vertices))
                        Set<Integer> verticesToAdd = new HashSet<>(vertexList.subList(1, vertexList.size() - 1));
                        Set<DefaultWeightedEdge> edgesToAdd = new HashSet<>(verticesToAdd
                                .stream()
                                .map(this.solution::edgesOf)
                                .flatMap(Collection::stream)
                                .toList()
                        );
                        if (vertexList.size() == 2) {
                            DefaultWeightedEdge edge = this.solution.removeEdge(crucialV, crucialW);
                            edgesToAdd.add(edge);
                        }
                        this.solution.removeAllVertices(verticesToAdd);
                        double costToRestore = edgesToAdd.stream().mapToDouble(this.solution::getEdgeWeight).sum();

                        // get 2 subtrees
                        ConnectivityInspector<Integer, DefaultWeightedEdge> inspector = new ConnectivityInspector<>(
                                this.solution
                        );
                        List<Set<Integer>> connectedSets = inspector.connectedSets();
                        if (connectedSets.size() != 2) {
                            throw new Exception("removing a key path \"" + vertexList
                                    + "\" didn't result in 2 subtrees, this should be impossible!");
                        }

                        // connect 2 subtrees
                        double bestWeight = Double.POSITIVE_INFINITY;
                        GraphPath<Integer, DefaultWeightedEdge> bestPath = null;
                        for (Integer vA : connectedSets.get(0)) {
                            for (Integer vB : connectedSets.get(1)) {
                                GraphPath<Integer, DefaultWeightedEdge> connectionPath = this.paths.getPath(vA, vB);
                                Set<DefaultWeightedEdge> edges = new HashSet<>(connectionPath.getEdgeList());
                                edges.removeAll(this.solution.edgeSet());
                                double pathWeight = edges.stream().mapToDouble(this.graph::getEdgeWeight).sum();
                                if (pathWeight < bestWeight) {
                                    bestWeight = pathWeight;
                                    bestPath = connectionPath;
                                }
                            }
                        }

                        Set<DefaultWeightedEdge> edges = new HashSet<>(bestPath.getEdgeList());
                        edges.removeAll(this.solution.edgeSet());
                        Set<Integer> vertices = new HashSet<>(bestPath.getVertexList());
                        vertices.removeAll(this.solution.vertexSet());

                        //construct new solution
                        vertices.forEach(this.solution::addVertex);
                        Set<Integer> verticesToRemove = new HashSet<>(vertices);
                        edges.forEach(e -> this.solution.addEdge(
                                this.solution.getEdgeSource(e),
                                this.solution.getEdgeTarget(e),
                                e));
                        Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>(edges);

                        // check new solution
                        assert GraphUtils.verifySteinerTree(this.graph, this.terminals, this.solution);
                        double newWeight = this.weight - costToRestore + bestWeight;
                        if (newWeight < this.weight) {
                            this.weight = newWeight;
                            result.setSmt(this.solution);
                            result.setWeight(this.weight);
//                            System.out.println("updated result");
                            return true;
                        }

                        //restore graph if solution wasn't improved
                        edgesToRemove.forEach(this.solution::removeEdge);
                        verticesToRemove.forEach(this.solution::removeVertex);
                        verticesToAdd.forEach(this.solution::addVertex);
                        edgesToAdd.forEach(e -> this.solution.addEdge(
                                this.solution.getEdgeSource(e),
                                this.solution.getEdgeTarget(e),
                                e));
                    }
                }
            }
        }
        return false;
    }

    /**
     * remove a key vertex from the pinned steiner vertices and try 2-approximation without that key vertex
     *
     * @return whether an improvement was found
     */
    private boolean keyVertexElimination() throws InterruptedException {
        boolean improved = false;
        Set<Integer> keyVs = this.solution
                .vertexSet()
                .stream()
                .filter(v -> this.solution.edgesOf(v).size() >= 3)
                .collect(Collectors.toSet());
        keyVs.removeAll(this.terminals);
        for (Integer keyV : keyVs) {
            if (this.solution.vertexSet().contains(keyV)) {
                Set<Integer> vertices = new HashSet<>(this.solution.vertexSet());
                vertices.remove(keyV);
                TwoApproximation twoApprox = new TwoApproximation(this.graph, vertices, this.paths);
                SteinerResult tempResult = twoApprox.runInstance(new SteinerResult(null, -1));
                double newWeight = tempResult.getWeight();
                if (newWeight < this.weight) {
                    this.weight = newWeight;
                    this.solution = tempResult.getSmt();
                    this.result.updateIfBetter(this.solution, this.weight);
//                    System.out.println("updated result");
                    improved = true;
                }
            }
        }

        return improved;
    }
}
