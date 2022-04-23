package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.util.Pair;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.doubleAlmostEqual;
import static cz.cuni.mff.kotal.helpers.MyNumberOperations.perimeter;


/**
 * Class representing agent.
 */
public class Agent {
	private final long id;
	private final double l; // Length of the agent
	private final double w; // Width of the agent
	private int entry;
	private final int exit;
	private final int entryDirection;
	private final int exitDirection;
	private final double speed;
	private final double arrivalTime;

	private transient long plannedTime = -1;

	private transient List<Integer> path = new ArrayList<>();
	private transient double x; // location
	private transient double y;

	private static final double PROXIMITY = 0.0001; // TODO

	/**
	 * TODO
	 * Cause gson
	 */
	public Agent() {
		id = 0;
		l = 0;
		w = 0;
		exit = 0;
		entryDirection = 0;
		exitDirection = 0;
		speed = 0;
		arrivalTime = 0;
	}

	/**
	 * Create new agent with specified attributes.
	 *
	 * @param id             ID of the agent
	 * @param entry          Starting vertex ID of the agent
	 * @param exit           Ending vertex ID of the agent
	 * @param entryDirection Direction in which the agent is entering the intersection
	 * @param exitDirection  Direction in which the agent is exiting the intersection
	 * @param speed          Speed of the agents in roads per step
	 * @param arrivalTime    Step number when this agent appeared
	 * @param l              Length of the agent in roads
	 * @param w              Width of the agent in roads
	 * @param x              Coordinate X of the agent
	 * @param y              Coordinate Y of the agent
	 */
	public Agent(long id, Integer entry, Integer exit, int entryDirection, int exitDirection, double speed, double arrivalTime, double l, double w, double x, double y) {
		this.id = id;
		this.entry = entry;
		this.exit = exit;
		this.entryDirection = entryDirection;
		this.exitDirection = exitDirection;
		this.speed = speed;
		this.arrivalTime = arrivalTime;
		this.l = l;
		this.w = w;
		this.x = x;
		this.y = y;
	}

	@TestOnly
	public Agent(long id, double arrivalTime, long plannedTime, Integer entry) {
		this.id = id;
		this.arrivalTime = arrivalTime;
		this.plannedTime = plannedTime;
		this.entry = entry;
		l = 0;
		w = 0;
		speed = 0;
		exit = 0;
		entryDirection = 0;
		exitDirection = 0;
	}


	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public int getLastVisitedVertexIndex(double time) {
		return (int) (time * speed);
	}

	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public int getNearestVertexIndex(double time) {
		return (int) Math.round(time * speed);
	}


	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public Integer getNearestPathVertexId(double time) {
		return path.get(getNearestVertexIndex(time));
	}


	/**
	 * Return previous and next target vertex ID at given time.
	 * If time is before arrival, return twice ID of starting vertex.
	 * If time is greater than arrival at the end time, throw exception.
	 * Else return the nearest next vertex ID and vertex ID of its predecessor.
	 *
	 * @param time Relative time to arrival time
	 * @return Pair od IDs of the previous and next vertex at given time
	 * @throws IndexOutOfBoundsException TODO
	 */
	public Pair<Integer, Integer> getPreviousNextVertexIDs(double time) throws IndexOutOfBoundsException {
		// TODO add exception
		if (time < 0) {
			int first = path.get(0);
			return new Pair<>(first, first);
		}
		if (doubleAlmostEqual(time, (path.size() - 1) / speed, PROXIMITY)) {
			int exitID = path.get(path.size() - 1);
			return new Pair<>(exitID, exitID);
		}
		int previousIndex = getLastVisitedVertexIndex(time);
		int nextIndex = previousIndex + 1;
		return new Pair<>(path.get(previousIndex), path.get(nextIndex));
	}

	/**
	 * @return ID of the agent
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set starting vertex in case only direction is generated.
	 *
	 * @param entry ID of starting vertex
	 */
	public Agent setEntry(Integer entry) {
		this.entry = entry;
		return this;
	}

	public void setEntry(GraphicalVertex vertex) {
		// TODO
		entry = vertex.getID();
		x = vertex.getX();
		y = vertex.getY();
	}

	/**
	 * @return ID of starting vertex of this agent
	 */
	public Integer getEntry() {
		return entry;
	}

	/**
	 * @return ID of ending vertex of this agent
	 */
	public Integer getExit() {
		return exit;
	}

	/**
	 * @return ID of direction in which agent appears
	 */
	public int getEntryDirection() {
		return entryDirection;
	}

	/**
	 * @return ID of direction in which agent leaves the intersection
	 */
	public int getExitDirection() {
		return exitDirection;
	}

	/**
	 * @return Speed of this agent
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * @return Path of this agent
	 */
	public List<Integer> getPath() {
		return path;
	}

	/**
	 * Set path of this agent to new value.
	 * If exit was not yet set, set it to the last vertex of the path.
	 * TODO
	 *
	 * @param path        New path of this agent
	 * @param plannedTime
	 */
	public Agent setPath(List<Integer> path, long plannedTime) {
		this.path = path;
		this.plannedTime = plannedTime;
		return this;
	}

	/**
	 * Set path of this agent to new value.
	 * If exit was not yet set, set it to the last vertex of the path.
	 *
	 * @param path New path of this agent
	 */
	protected Agent setPath(List<Integer> path) {
		this.path = path;
		return this;
	}

	/**
	 * Compute coordinates X and Y at given time.
	 *
	 * @param time     Time in steps since agent appeared
	 * @param vertices Map of vertices and their IDs of the graph the agent is moving on
	 * @throws IndexOutOfBoundsException TODO
	 */
	public void computeNextXY(double time, Map<Integer, Vertex> vertices) throws IndexOutOfBoundsException {
		double currentEdgeTravelPart = (time * speed) % 1;
		double currentEdgeTravelRemain = 1 - currentEdgeTravelPart;
		Pair<Integer, Integer> previousNextGoalID = getPreviousNextVertexIDs(time);
		GraphicalVertex previousGoal = (GraphicalVertex) vertices.get(previousNextGoalID.getKey());
		GraphicalVertex nextGoal = (GraphicalVertex) vertices.get(previousNextGoalID.getValue());

		// TODO optimize
		double previousGoalX = previousGoal.getX() * IntersectionModel.getPreferredHeight();
		double nextGoalX = nextGoal.getX() * IntersectionModel.getPreferredHeight();
		this.x = previousGoalX * currentEdgeTravelRemain + nextGoalX * currentEdgeTravelPart;
		double previousGoalY = previousGoal.getY() * IntersectionModel.getPreferredHeight();
		double nextGoalY = nextGoal.getY() * IntersectionModel.getPreferredHeight();
		this.y = previousGoalY * currentEdgeTravelRemain + nextGoalY * currentEdgeTravelPart;
	}

	/**
	 * Set X and Y coordinates according to entry vertex ID.
	 *
	 * @param vertices Map of vertices to get entry vertex object.
	 * @throws RuntimeException TODO
	 */
	public void setStartingXY(Map<Integer, Vertex> vertices) {
		if (entry < 0 || exit < 0) {
			throw new RuntimeException("Agent not planned.");
		}
		GraphicalVertex entryVertex = (GraphicalVertex) vertices.get(entry);
		x = entryVertex.getX();
		y = entryVertex.getY();
	}

	/**
	 * @return Length of the agent
	 */
	public double getL() {
		return l;
	}

	/**
	 * @return Width of the agent
	 */
	public double getW() {
		return w;
	}

	/**
	 * @return Coordinate X of the agent
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return Coordinate Y of the agent
	 */
	public double getY() {
		return y;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	// TODO
	public Agent setPlannedTime(long plannedTime) {
		this.plannedTime = plannedTime;
		return this;
	}

	public long getPlannedTime() {
		return plannedTime;
	}

	public double getAgentPerimeter() {
		return getAgentPerimeter(IntersectionModel.getGraph());
	}

	public double getAgentPerimeter(SimulationGraph graph) {
		return perimeter(getL(), getW()) * graph.getCellSize();
	}
}