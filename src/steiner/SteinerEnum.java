package steiner;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import steiner.approx.TwoApproximation;
import steiner.approx.Zelikovsky_11_6;
import steiner.exact.DreyfusWagner;
import steiner.localsearch.FastLocalSearch;
import steiner.localsearch.HybridGRASP;
import steiner.localsearch.constructionmethods.*;

import java.util.Set;

public enum SteinerEnum {
    DUMMY_LOOP(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new Dummy(graph, terminals);
        }
    },

    /**
     * Approximation algorithms
     */
    TWO_APPROXIMATION(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new TwoApproximation(graph, terminals);
        }
    },

    ZELIKOVSKY(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new Zelikovsky_11_6(graph, terminals);
        }
    },

    FAST_LOCAL_SEARCH(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new FastLocalSearch(graph, terminals);
        }
    },

    LOCAL_VERTEX_INSERTION(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new FastLocalSearch(graph, terminals, true, false, false, false);
        }
    },

    LOCAL_VERTEX_ELIMINATION(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new FastLocalSearch(graph, terminals, false, true, false, false);
        }
    },

    LOCAL_KEY_PATH_EXCHANGE(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new FastLocalSearch(graph, terminals, false, false, true, false);
        }
    },

    LOCAL_KEY_VERTEX_ELIMINATION(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new FastLocalSearch(graph, terminals, false, false, false, true);
        }
    },

    HYBRID_GRASP_WITH_PERTURBATIONS(true) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new HybridGRASP(graph, terminals);
        }
    },

    /**
     * Construction methods used in GRASP
     */

    KRUSKAL_CONSTRUCTION(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new ConstructionAlgorithmAdapter(graph, terminals, new KruskalComponentHeuristic());
        }
    },

    MST_CONSTRUCTION(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new ConstructionAlgorithmAdapter(graph, terminals, new MSTHeuristic());
        }
    },

    SHORTEST_PATH_CONSTRUCTION(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new ConstructionAlgorithmAdapter(graph, terminals, new ShortestPathHeuristic());
        }
    },

    TWO_APPROX_CONSTRUCTION(false) {
        @Override
        public SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new ConstructionAlgorithmAdapter(graph, terminals, new TwoApproxHeuristic());
        }
    },

    /**
     * Exact algorithms
     */
    // too slow for comparative tests (result is the known optimum for any graph anyway since it's an exact algorithm)
    DREYFUS_WAGNER(false, false) {
        @Override
        public SteinerAlgorithm getInstance
                (SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals) {
            return new DreyfusWagner(graph, terminals);
        }
    };

    private final boolean useInTest;
    private final boolean verifyTree;

    SteinerEnum(boolean useInTest, boolean verifyTree) {
        this.useInTest = useInTest;
        this.verifyTree = verifyTree;
    }

    SteinerEnum(boolean useInTest) {
        this.useInTest = useInTest;
        this.verifyTree = true;
    }

    public abstract SteinerAlgorithm getInstance(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph, Set<Integer> terminals);

    public boolean useInTest() {
        return this.useInTest;
    }

    public boolean verifyTree() {
        return this.verifyTree;
    }
}
