package utils.stp;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Created by Stefan Croes
 */
public class STPGraph {
    private String name;
    private Date date;
    private String creator;
    private String remark;
    private String problem;

    private SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    private Set<Integer> terminals;
    private Map<Integer, Pair<Integer, Integer>> coordinates;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(String date) {
        try {
            this.date = DateFormat.getDateTimeInstance().parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> getGraph() {
        return graph;
    }

    public void setGraph(SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph) {
        this.graph = graph;
    }

    public Set<Integer> getTerminals() {
        return terminals;
    }

    public void setTerminals(Set<Integer> terminals) {
        this.terminals = terminals;
    }

    public Map<Integer, Pair<Integer, Integer>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Map<Integer, Pair<Integer, Integer>> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return "STPGraph{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", creator='" + creator + '\'' +
                ", remark='" + remark + '\'' +
                ", problem='" + problem + '\'' +
                ", graph=" + graph +
                ", terminals=" + terminals +
                '}';
    }
}
