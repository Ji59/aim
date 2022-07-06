package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.doubleAlmostEqual;


/**
 * Class representing agent.
 */
public class Agent extends BasicAgent {

	private long plannedTime = -1;
	private List<Integer> path = new ArrayList<>();
	private double collisionStep = Double.POSITIVE_INFINITY;
	private transient double x; // location
	private transient double y;

	private static final double PROXIMITY = 0.0001; // TODO

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
		super(id, entry, exit, entryDirection, exitDirection, speed, arrivalTime, l, w);
		this.x = x;
		this.y = y;
	}

	/**
	 * TODO
	 * For loading from GSON.
	 *
	 * @param basicAgent
	 */
	public Agent(BasicAgent basicAgent) {
		super(basicAgent);
	}

	@TestOnly
	public Agent(long id, double arrivalTime, long plannedTime, Integer entry) {
		super(id, arrivalTime, entry);
		this.plannedTime = plannedTime;
	}


	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public int getLastVisitedVertexIndex(double time) {
		return (int) (time * getSpeed());
	}

	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public int getNearestVertexIndex(double time) {
		return (int) Math.round(time * getSpeed());
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
	public Pair<Integer, Integer> getPreviousNextVertexIDs(double time) {
		// TODO add exception
		if (time < 0) {
			int first = path.get(0);
			return new Pair<>(first, first);
		} else if (doubleAlmostEqual(time, (path.size() - 1) / getSpeed(), PROXIMITY)) {
			int exitID = path.get(path.size() - 1);
			return new Pair<>(exitID, exitID);
		} else if (time > path.size() - 1) {
			return null;
		}
		int previousIndex = getLastVisitedVertexIndex(time);
		int nextIndex = previousIndex + 1;
		return new Pair<>(path.get(previousIndex), path.get(nextIndex));
	}

	/**
	 * Compute coordinates X and Y at given time.
	 *
	 * @param time     Time in steps since agent appeared
	 * @param vertices Map of vertices and their IDs of the graph the agent is moving on
	 * @throws IndexOutOfBoundsException TODO
	 */
	public boolean computeNextXY(double time, Vertex[] vertices) {
		double currentEdgeTravelPart = (time * getSpeed()) % 1;
		double currentEdgeTravelRemain = 1 - currentEdgeTravelPart;
		Pair<Integer, Integer> previousNextGoalID = getPreviousNextVertexIDs(time);
		if (previousNextGoalID == null) {
			return true;
		}

		GraphicalVertex previousGoal = (GraphicalVertex) vertices[previousNextGoalID.getVal0()];
		GraphicalVertex nextGoal = (GraphicalVertex) vertices[previousNextGoalID.getVal1()];

		// TODO optimize
		double previousGoalX = previousGoal.getX();
		double nextGoalX = nextGoal.getX();
		this.x = previousGoalX * currentEdgeTravelRemain + nextGoalX * currentEdgeTravelPart;
		double previousGoalY = previousGoal.getY();
		double nextGoalY = nextGoal.getY();
		this.y = previousGoalY * currentEdgeTravelRemain + nextGoalY * currentEdgeTravelPart;

		return false;
	}

	/**
	 * Set X and Y coordinates according to entry vertex ID.
	 *
	 * @param vertices Map of vertices to get entry vertex object.
	 * @throws RuntimeException TODO
	 */
	public void setStartingXY(Map<Integer, Vertex> vertices) {
		if (getEntry() < 0 || getExit() < 0) {
			throw new RuntimeException("Agent not planned.");
		}
		GraphicalVertex entryVertex = (GraphicalVertex) vertices.get(getEntry());
		x = entryVertex.getX();
		y = entryVertex.getY();
	}

	// TODO

	public Agent setPlannedTime(long plannedTime) {
		this.plannedTime = plannedTime;
		return this;
	}

	public long getPlannedTime() {
		return plannedTime;
	}

	public void setEntry(GraphicalVertex vertex) {
		// TODO
		entry = vertex.getID();
		x = vertex.getX();
		y = vertex.getY();
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

	public double getCollisionStep() {
		return collisionStep;
	}

	public Agent setCollisionStep(double step) {
		this.collisionStep = step;
		return this;
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

}