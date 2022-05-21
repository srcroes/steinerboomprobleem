package steiner.localsearch;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.SteinerAlgorithm;
import steiner.SteinerResult;
import steiner.localsearch.constructionmethods.*;
import utils.Logger;
import utils.Utils;
import utils.graphextensions.GraphUtils;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * steiner algorithm that uses different construction methods,
 * *                                     weight perturbations methods,
 * *                                     local search methods,
 * *                                     and path relinking strategies
 */
public class HybridGRASP extends SteinerAlgorithm {
    private static final int MAX_ITER = 10;
    private static final int ELITE_SIZE = 5;
    Random random;

    public HybridGRASP(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
        super(graph, terminals);
        // use fixed seed for reproducibility in testing/debugging
        random = new Random(1024 * 1024 - 1);
    }

    @Override
    public SteinerResult runInstance(SteinerResult result) throws Exception {
        return grasp(result);
    }

    /**
     * main outline of the algorithm
     *
     * @param result result objects to place solution into
     * @return the result
     * @throws Exception key path removal not resulting in 2 subtrees
     */
    private SteinerResult grasp(SteinerResult result) throws Exception {
        PerturbationEnum[] perturbationMethods = PerturbationEnum.values();
        ConstructionEnum[] constructionMethods = ConstructionEnum.values();
        HashMap<DefaultWeightedEdge, Double> weights = new HashMap<>();
        HashMap<DefaultWeightedEdge, Integer> tCount = new HashMap<>();
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions = new ArrayList<>();

        /* start with original weights */
        graph.edgeSet().forEach(e -> weights.put(e, graph.getEdgeWeight(e)));
        /* initialize tCount to 0 */
        graph.edgeSet().forEach(e -> tCount.put(e, 0));

        for (int i = 0; i < MAX_ITER; i++) {
            Utils.notInterrupted();
            /** apply perturbation strategy to weights */
            Logger.debug("applying perturbation strategy");
            int cMethod;
            if (i >= constructionMethods.length) {
                cMethod = random.nextInt(0, constructionMethods.length);
                for (DefaultWeightedEdge edge : graph.edgeSet()) {
                    double wOriginal = graph.getEdgeWeight(edge);
                    int tI1 = tCount.get(edge);
                    /* rotate strategies every iteration */
                    double rI = perturbationMethods[i % perturbationMethods.length].getCoefficient(tI1, i);
                    double bound = wOriginal * rI;
                    double wI = random.nextDouble(min(wOriginal, bound), max(wOriginal, bound));
                    weights.put(edge, wI);
                }

            } else {
                // make sure to use every construction method once for the first couple iterations
                cMethod = i;
            }

            /** construct greedy solution using perturbated weights */
            Logger.debug("constructing greedy solution");
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution = constructionMethods[cMethod]
                    .getConstructionMethod()
                    .constructSolution(graph, terminals, weights);
            GraphUtils.replaceWeightsInPlace(solution, graph);
            double weight = GraphUtils.getWeight(solution);
            // replace solution in result if better
            result.updateIfBetter(solution, weight);

            /** apply local search to greedy solution using original weights */
            Logger.debug("applying local search to greedy solution");
            Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> localPair = hybridLocalSearch(
                    solution,
                    weight,
                    result
            );

            SimpleWeightedGraph<Integer, DefaultWeightedEdge> localSolution = localPair.getSecond();
            // update tCount
            for (DefaultWeightedEdge edge : localSolution.edgeSet()) {
                Integer source = localSolution.getEdgeSource(edge);
                Integer target = localSolution.getEdgeTarget(edge);
                DefaultWeightedEdge graphEdge = graph.getEdge(source, target);
                tCount.put(graphEdge, tCount.get(graphEdge) + 1);
            }


            /** update elite solutions */
            insertEliteSolution(eliteSolutions, localSolution, localPair.getFirst());
        }

        /** apply path relinking to pool of elite solutions */
        Logger.debug("applying path relinking");
        eliteSolutions = PathRelinkingLoop(eliteSolutions, result, RelinkingType.HYBRID);

        /** return best solution found */
        Logger.debug("best solution:");
        eliteSolutions.sort(Comparator.comparing(Pair::getFirst));
        Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> pair = eliteSolutions.get(0);
        result.updateIfBetter(pair.getSecond(), pair.getFirst());
        return result;
    }

    /**
     * combination of different local search methods:
     * vertex insertion, vertex deletion & key path exchange
     *
     * @param localSolution solution to perform local search on
     * @param localWeight   weight of the localSolution
     * @param result        update result if better solution is found
     * @return pair of best found solution and its weight
     * @throws Exception key path removal not resulting in 2 subtrees
     */
    private Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> hybridLocalSearch(
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> localSolution,
            double localWeight, SteinerResult result) throws Exception {
        boolean improved = true;
        while (improved && Utils.notInterrupted()) {
            improved = false;

            // insertion or deletion
            boolean localImproved = true;
            while (localImproved && Utils.notInterrupted()) {
                localImproved = false;
                HashSet<Integer> insertionCandidates = new HashSet<>(graph.vertexSet());
                insertionCandidates.removeAll(localSolution.vertexSet());
                HashSet<Integer> deletionCandidates = new HashSet<>(localSolution.vertexSet());
                deletionCandidates.removeAll(terminals);

                while (!(insertionCandidates.isEmpty() && deletionCandidates.isEmpty())) {
                    HashSet<Integer> vertices = new HashSet<>(localSolution.vertexSet());
                    if (!insertionCandidates.isEmpty()) { // insert random vertex from candidates
                        Integer candidate = Utils.getRandomSetElement(insertionCandidates);
                        insertionCandidates.remove(candidate);
                        vertices.add(candidate);
                    } else { // remove random vertex from candidates
                        Integer candidate = Utils.getRandomSetElement(deletionCandidates);
                        deletionCandidates.remove(candidate);
                        vertices.remove(candidate);
                    }
                    var newSolution = GraphUtils.getGraphFromSpanningTree(
                            GraphUtils.getMST(GraphUtils.subgraph(graph, vertices)),
                            graph
                    );
                    double newWeight = GraphUtils.getWeight(newSolution);
                    if (GraphUtils.verifySteinerTree(graph, terminals, newSolution) && newWeight <= localWeight) {
                        localSolution = newSolution;
                        if (newWeight < localWeight) {
                            localWeight = newWeight;
                            localImproved = true;
                            improved = true;
                            result.updateIfBetter(newSolution, newWeight);
                        }
                    }
                }
            }

            // key-path
            localImproved = true;
            outer:
            while (localImproved && Utils.notInterrupted()) {
                localImproved = false;
                Set<Integer> crucialVs = new HashSet<>(terminals);
                for (Integer v : localSolution.vertexSet()) {
                    if (localSolution.edgesOf(v).size() >= 3) {
                        crucialVs.add(v);
                    }
                }
                var solutionPaths = GraphUtils.getShortestPaths(localSolution);
//                ShortestPathAlgorithm<Integer, DefaultWeightedEdge> solutionPaths = new DijkstraShortestPath<>(localSolution);
                for (Integer crucialV : crucialVs) {
                    for (Integer crucialW : crucialVs) {
                        Utils.notInterrupted();
                        if (crucialV < crucialW) {
                            GraphPath<Integer, DefaultWeightedEdge> path = solutionPaths.getPath(crucialV, crucialW);
                            List<Integer> vertexList = path.getVertexList();
                            long count = vertexList.stream().filter(crucialVs::contains).count();
                            if (count == 2) { // only the start and end of the path are crucial vertices

                                // remove key path (remove internal vertices and their incident edges (implicitly))
                                Set<Integer> verticesToAdd = new HashSet<>(vertexList.subList(1, vertexList.size() - 1));
                                Set<DefaultWeightedEdge> edgesToAdd = new HashSet<>(
                                        verticesToAdd
                                                .stream()
                                                .map(localSolution::edgesOf)
                                                .flatMap(Collection::stream)
                                                .toList()
                                );
                                if (vertexList.size() == 2) {
                                    DefaultWeightedEdge edge = localSolution.removeEdge(crucialV, crucialW);
                                    edgesToAdd.add(edge);
                                }
                                localSolution.removeAllVertices(verticesToAdd);
                                double costToRestore = edgesToAdd
                                        .stream()
                                        .mapToDouble(localSolution::getEdgeWeight)
                                        .sum();

                                // get 2 subtrees
                                var inspector = new ConnectivityInspector<>(localSolution);
                                List<Set<Integer>> connectedComponents = inspector.connectedSets();
                                if (connectedComponents.size() != 2) {
                                    throw new Exception(
                                            "removing a key path \"" + vertexList
                                                    + "\" didn't result in 2 subtrees, this should be impossible!"
                                    );
                                }

                                // connect 2 subtrees
                                double bestWeight = Double.POSITIVE_INFINITY;
                                GraphPath<Integer, DefaultWeightedEdge> bestPath = null;
                                for (Integer vA : connectedComponents.get(0)) {
                                    for (Integer vB : connectedComponents.get(1)) {
                                        var connectionPath = solutionPaths.getPath(vA, vB);
                                        Set<DefaultWeightedEdge> edges = new HashSet<>(connectionPath.getEdgeList());
                                        edges.removeAll(localSolution.edgeSet());
                                        double pathWeight = edges.stream().mapToDouble(this.graph::getEdgeWeight).sum();
                                        if (pathWeight < bestWeight) {
                                            bestWeight = pathWeight;
                                            bestPath = connectionPath;
                                        }
                                    }
                                }

                                Set<DefaultWeightedEdge> edges = new HashSet<>(bestPath.getEdgeList());
                                edges.removeAll(localSolution.edgeSet());
                                Set<Integer> vertices = new HashSet<>(bestPath.getVertexList());
                                vertices.removeAll(localSolution.vertexSet());

                                //construct new solution
                                vertices.forEach(localSolution::addVertex);
                                Set<Integer> verticesToRemove = new HashSet<>(vertices);
                                for (DefaultWeightedEdge edge : edges) {
                                    localSolution.addEdge(
                                            localSolution.getEdgeSource(edge),
                                            localSolution.getEdgeTarget(edge),
                                            edge
                                    );
                                }
                                Set<DefaultWeightedEdge> edgesToRemove = new HashSet<>(edges);

                                // check new solution
                                double newWeight = localWeight - costToRestore + bestWeight;
                                if (GraphUtils.verifySteinerTree(
                                        this.graph,
                                        this.terminals,
                                        localSolution) && newWeight < localWeight) {
                                    localWeight = newWeight;
                                    result.setSmt(localSolution);
                                    result.setWeight(localWeight);
                                    localImproved = true;
                                    improved = true;
                                    continue outer;
                                }

                                //restore graph if solution wasn't improved
                                edgesToRemove.forEach(localSolution::removeEdge);
                                verticesToRemove.forEach(localSolution::removeVertex);
                                verticesToAdd.forEach(localSolution::addVertex);
                                for (DefaultWeightedEdge edge : edgesToAdd) {
                                    localSolution.addEdge(
                                            localSolution.getEdgeSource(edge),
                                            localSolution.getEdgeTarget(edge),
                                            edge);
                                }
                            }
                        }
                    }
                }
            }
        }
        return new Pair<>(localWeight, localSolution);
    }

    /**
     * generic loop structure for performing path relinking until no improvement is found
     *
     * @param eliteSolutions list of elite solutions
     * @param result         result to be updated
     * @param relinkingType  type of path relinking strategy that is used
     * @return best iteration of elite solutions
     * @throws Exception key path removal not resulting in 2 subtrees
     */
    private List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> PathRelinkingLoop(
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions,
            SteinerResult result,
            RelinkingType relinkingType
    ) throws Exception {
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> tempSolutions;
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutionsNext = eliteSolutions;
        do {
            Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> first = eliteSolutionsNext.get(0);
            result.updateIfBetter(first.getSecond(), first.getFirst());
            Logger.debug("start elite solutions iteration");
            tempSolutions = eliteSolutionsNext;
            eliteSolutionsNext = switch (relinkingType) {
                case COMPLEMENTARY_MOVES -> complementaryPathRelinking(tempSolutions, tempSolutions);
                case WEIGHT_PENALIZATION -> weightPenalizationPathRelinking(tempSolutions, tempSolutions, result);
                case HYBRID -> hybridPathRelinking(tempSolutions, result);
            };
            eliteSolutionsNext.sort(Comparator.comparing(Pair::getFirst));
            tempSolutions.sort(Comparator.comparing(Pair::getFirst));
            Logger.debug("eliteSolutionsNext: " + eliteSolutionsNext);
            Logger.debug("tempSolutions: " + tempSolutions);
        } while (!eliteSolutionsNext.isEmpty() && eliteSolutionsNext.get(0).getFirst() < tempSolutions.get(0).getFirst()
                && Utils.notInterrupted());
        return tempSolutions;
    }

    /**
     * combination of complementary- and 'weight penalization'- path relinking
     *
     * @param eliteSolutions list of elite solutions
     * @param result         result to be updated
     * @return next generation of elite solutions
     * @throws Exception key path removal not resulting in 2 subtrees
     */
    private List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> hybridPathRelinking(
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions,
            SteinerResult result) throws Exception {
        Logger.debug("start hybridPathRelinking");
        eliteSolutions.sort(Comparator.comparing(Pair::getFirst));
        Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> best = eliteSolutions.remove(0);
        // handle pairs with best solution separately
        var bestList = List.of(best);
        long start = System.nanoTime();
        var tempSolutions = complementaryPathRelinking(
                eliteSolutions,
                bestList
        );
        long time1 = System.nanoTime() - start;
        start = System.nanoTime();
        var weightPenalizationList = weightPenalizationPathRelinking(
                eliteSolutions,
                bestList,
                result
        );
        long time2 = System.nanoTime() - start;
        weightPenalizationList.forEach(p -> insertEliteSolution(tempSolutions, p.getSecond(), p.getFirst()));
        // handle other pairs using fastest method of best pairs
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> temp;
        if (time1 < time2) {
            temp = complementaryPathRelinking(eliteSolutions, eliteSolutions);
        } else {
            temp = weightPenalizationPathRelinking(eliteSolutions, eliteSolutions, result);
        }
        temp.forEach(p -> insertEliteSolution(tempSolutions, p.getSecond(), p.getFirst()));
        eliteSolutions.add(best);
        return tempSolutions;
    }

    /**
     * path relinking using weight penalization:
     * edge weights are modified based on the appearance of edges in either or both solutions in a pair
     * pairs are formed using a solution from both lists
     * in general both lists should be passed the same, except for hybridPathelinking, which handles this slightly differently
     *
     * @param eliteSolutions1 list of elite solutions
     * @param eliteSolutions2 list of elite solutions
     * @param result          result to be updated
     * @return next iteration of elite solutions
     * @throws Exception key path removal not resulting in 2 subtrees
     */
    private List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> weightPenalizationPathRelinking(
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions1,
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions2,
            SteinerResult result
    ) throws Exception {
        Logger.debug("start weightPenalizationPathRelinking");
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutionsNext = new ArrayList<>();
        ShortestPathHeuristic sph = new ShortestPathHeuristic();
        for (Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> pair1 : eliteSolutions1) {
            for (Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> pair2 : eliteSolutions2) {
                Utils.notInterrupted();
                SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph1 = pair1.getSecond();
                SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph2 = pair2.getSecond();
                HashMap<DefaultWeightedEdge, Double> weights = new HashMap<>();
                for (DefaultWeightedEdge edge : graph.edgeSet()) {
                    Integer source = graph.getEdgeSource(edge);
                    Integer target = graph.getEdgeTarget(edge);
                    boolean contains1 = graph1.containsEdge(source, target);
                    boolean contains2 = graph2.containsEdge(source, target);
                    if (contains1 ^ contains2) {
                        weights.put(edge, graph.getEdgeWeight(edge) * random.nextDouble(50., 100.));
                    } else if (contains1) {
                        weights.put(edge, graph.getEdgeWeight(edge));
                    } else {
                        weights.put(edge, graph.getEdgeWeight(edge) * 2000.);
                    }
                }
                SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution = sph.constructSolution(
                        graph,
                        terminals,
                        weights
                );
                Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> pair = hybridLocalSearch(
                        solution,
                        GraphUtils.getWeight(solution),
                        result
                );
                insertEliteSolution(eliteSolutionsNext, pair.getSecond(), pair.getFirst());
            }
        }
        return eliteSolutionsNext;
    }

    /**
     * path relinking using complementary moves
     * basic idea: for each pair of elite solutions:
     * *           - - move from one solution to the other
     * *           - - remember best solution on this trajectory
     *
     * @param eliteSolutions1 list of elite solutions
     * @param eliteSolutions2 list of elite solutions
     * @return next iteration of elite solutions
     */
    private List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> complementaryPathRelinking(
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions1,
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions2) throws InterruptedException {
        Logger.debug("start complementaryPathRelinking");
        List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutionsNext = new ArrayList<>();
        for (Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> initial : eliteSolutions1) {
            for (Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> guiding : eliteSolutions2) {
                Utils.notInterrupted();
                if (initial != guiding) {
                    SimpleWeightedGraph<Integer, DefaultWeightedEdge> initialGraph = initial.getSecond();
                    SimpleWeightedGraph<Integer, DefaultWeightedEdge> guidingGraph = guiding.getSecond();
                    Set<Integer> symmetricDiff = Utils.diff(initialGraph.vertexSet(), guidingGraph.vertexSet());
                    // while guiding solution isn't yet attained by initial solution
                    SimpleWeightedGraph<Integer, DefaultWeightedEdge> currentGraph = initialGraph;
                    SimpleWeightedGraph<Integer, DefaultWeightedEdge> temp;
                    SimpleWeightedGraph<Integer, DefaultWeightedEdge> best = initialGraph;
                    double bestWeight = initial.getFirst();
                    while (!symmetricDiff.isEmpty()) {
                        SimpleWeightedGraph<Integer, DefaultWeightedEdge> localBest = null;
                        double localBestWeight = Double.POSITIVE_INFINITY;
                        Integer bestMove = -1;
                        Integer lastMove = -1;
                        // try every move left to pick best move next
                        for (Integer move : symmetricDiff) {
                            Set<Integer> vertices = new HashSet<>(currentGraph.vertexSet());
                            if (vertices.contains(move)) {
                                vertices.remove(move);
                            } else {
                                vertices.add(move);
                            }
                            temp = GraphUtils.getGraphFromSpanningTree(
                                    GraphUtils.getMST(GraphUtils.subgraph(graph, vertices)),
                                    graph
                            );
                            double tempWeight = GraphUtils.getWeight(temp);
                            if (GraphUtils.verifySteinerTree(graph, terminals, temp) && tempWeight <= localBestWeight) {
                                localBest = temp;
                                localBestWeight = tempWeight;
                                bestMove = move;
                            }
                            lastMove = move;
                        }
                        if (bestMove != -1) {
                            symmetricDiff.remove(bestMove);
                            currentGraph = localBest;
                            if (localBestWeight < bestWeight) {
                                best = localBest;
                                bestWeight = localBestWeight;
                            }
                        } else {
                            symmetricDiff.remove(lastMove);
                        }
                    }
                    insertEliteSolution(eliteSolutionsNext, best, bestWeight);
                }
            }
        }
        return eliteSolutionsNext;
    }

    /**
     * try to insert a solution into a list of elite solutions
     * * solution gets inserted if the list of elite solutions is not full
     * * OR if the solution is better than the worst elite solution in the list
     *
     * @param eliteSolutions list of elite solutions to try inserting into
     * @param solution       solution to try to insert
     * @param weight         weight of solution
     */
    private void insertEliteSolution(
            List<Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>>> eliteSolutions,
            SimpleWeightedGraph<Integer, DefaultWeightedEdge> solution, double weight) {
        eliteSolutions.sort(Comparator.comparing(Pair::getFirst));
        if ((eliteSolutions.size() < ELITE_SIZE || eliteSolutions.get(eliteSolutions.size() - 1).getFirst() > weight)
                && eliteSolutions.stream().noneMatch(sol -> GraphUtils.equals(sol.getSecond(), solution))) {
            Pair<Double, SimpleWeightedGraph<Integer, DefaultWeightedEdge>> pair = new Pair<>(weight, solution);
            if (eliteSolutions.size() >= ELITE_SIZE) {
                eliteSolutions.remove(eliteSolutions.size() - 1);
            }
            eliteSolutions.add(pair);
        }
    }

    /**
     * weight perturbation methods (these are alternated in the GRASP iterations)
     */
    private enum PerturbationEnum {
        /* intensification */
        I {
            @Override
            double getCoefficient(int tI1, int i) {
                return 2 - 0.75 * tI1 / (i - 1);
            }
        },
        /* diversification */
        D {
            @Override
            double getCoefficient(int tI1, int i) {
                return 1.25 + 0.75 * tI1 / (i - 1);
            }
        },
        /* uniform penalization */
        U {
            @Override
            double getCoefficient(int tI1, int i) {
                return 2;
            }
        };

        abstract double getCoefficient(int tI1, int i);
    }

    /**
     * construction methods/heuristics
     */
    private enum ConstructionEnum {

        /**
         * Shortest path heuristic
         */
        SPH(new ShortestPathHeuristic()),

        /**
         * Component based heuristic
         */
        COMPONENT_BASED(new KruskalComponentHeuristic()),

        /**
         * Minimal spanning tree heuristic
         */
        MST_BASED(new MSTHeuristic()),

        /**
         * 2-approximation heuristic
         */
        TWO_APP(new TwoApproxHeuristic());

        private final ConstructionMethod constructionMethod;

        ConstructionEnum(ConstructionMethod constructionMethod) {
            this.constructionMethod = constructionMethod;
        }

        public ConstructionMethod getConstructionMethod() {
            return constructionMethod;
        }
    }

    /**
     * path relinking strategies
     */
    private enum RelinkingType {
        COMPLEMENTARY_MOVES,
        WEIGHT_PENALIZATION,
        HYBRID
    }
}
