package cz.cuni.mff.kotal.simulation;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.TestOnly;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.perimeter;

public class BasicAgent {
	protected final long id;
	protected final double l; // Length of the agent
	protected final double w; // Width of the agent
	protected int entry;
	protected final int exit;
	protected final int entryDirection;
	protected final int exitDirection;
	protected final double speed;
	protected final double arrivalTime;
	transient private double perimeter = -1;

	/**
	 * TODO
	 * Cause gson
	 */
	public BasicAgent() {
		id = 0;
		l = 0;
		w = 0;
		exit = 0;
		entryDirection = 0;
		exitDirection = 0;
		speed = 0;
		arrivalTime = 0;
	}

	public BasicAgent(BasicAgent agent) {
		id = agent.getId();
		l = agent.getL();
		w = agent.getW();
		entry = agent.getEntry();
		exit = agent.getExit();
		entryDirection = agent.getEntryDirection();
		exitDirection = agent.getExitDirection();
		speed = agent.getSpeed();
		arrivalTime = agent.getArrivalTime();
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
	 */
	protected BasicAgent(long id, Integer entry, Integer exit, int entryDirection, int exitDirection, double speed, double arrivalTime, double l, double w) {
		this.id = id;
		this.entry = entry;
		this.exit = exit;
		this.entryDirection = entryDirection;
		this.exitDirection = exitDirection;
		this.speed = speed;
		this.arrivalTime = arrivalTime;
		this.l = l;
		this.w = w;
	}

	@TestOnly
	protected BasicAgent(long id, double arrivalTime, Integer entry) {
		this.id = id;
		this.arrivalTime = arrivalTime;
		this.entry = entry;
		l = 0;
		w = 0;
		speed = 0;
		exit = 0;
		entryDirection = 0;
		exitDirection = 0;
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
	public BasicAgent setEntry(Integer entry) {
		this.entry = entry;
		return this;
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

	public double getArrivalTime() {
		return arrivalTime;
	}

	public double getAgentPerimeter() {
		if (perimeter < 0) {
			perimeter = getAgentPerimeter(IntersectionModel.getGraph());
		}
		return perimeter;
	}

	public double getAgentPerimeter(SimulationGraph graph) {
		if (perimeter < 0) {
			perimeter = perimeter(getL(), getW()) * graph.getCellSize();
		}
		return perimeter;
	}
}
