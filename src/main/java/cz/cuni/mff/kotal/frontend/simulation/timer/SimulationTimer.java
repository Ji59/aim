package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import cz.cuni.mff.kotal.frontend.simulation.*;
import cz.cuni.mff.kotal.helpers.Collisions;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Timer to be tick every frame to redraw agents and check for collisions.
 */
public class SimulationTimer extends AnimationTimer {
	private final Map<Long, AgentPane> agents;
	//	private final Map<Long, AgentPolygon> lastState = new HashMap<>();
	private final SimulationAgents simulationAgents;

	private final double cellSize;

	private long generatedStep = -1;


	/**
	 * Create new timer.
	 *
	 * @param agents           Collection of agents to get data from
	 * @param simulationAgents GUI node starting this timer
	 */
	public SimulationTimer(Map<Long, AgentPane> agents, SimulationAgents simulationAgents) {
		this.agents = agents;
		this.simulationAgents = simulationAgents;
		cellSize = simulationAgents.getSimulation().getIntersectionGraph().getCellSize() * IntersectionModel.getPreferredHeight(); // FIXME refactor
	}

	/**
	 * Compute agents positions, update values, check for collisions.
	 *
	 * @param now System time of frame
	 */
	@Override
	public void handle(long now) {
//		simulationAgents.resetRectangles(); TODO
		double step = simulationAgents.getSimulation().getStep(now);
		IntersectionMenu.setStep(step);

		simulationAgents.getSimulation().loadAgents(step);
		addArrivedAgents(step);

		simulationAgents.getSimulation().updateStatistics(step);

		synchronized (agents) {
			Set<Map.Entry<Long, AgentPane>> finishedAgents = agents.entrySet().stream().filter(a -> !a.getValue().isDisable() && a.getValue().handleTick(step)).collect(Collectors.toSet());
			finishedAgents.forEach(simulationAgents::removeAgent);

			Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(agents);
			overlappingAgents = overlappingAgents.stream().filter(pair -> Collisions.inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

			// Handle collisions
			overlappingAgents.forEach(agentPanePair -> {
				AgentPane agentPane0 = agentPanePair.getKey();
				AgentPane agentPane1 = agentPanePair.getValue();

				agentPane0.collide();
				agentPane1.collide();

				simulationAgents.getSimulation().addCollision();

				// TODO remove log
				System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());

				// Change agents, set timer for removal
				new Thread(() -> {
					try {
						// TODO replace with number of steps
						Thread.sleep(simulationAgents.getSimulation().getPeriod() / 1_000_000); // TODO millis
						Platform.runLater(() -> {
							simulationAgents.removeAgent(agentPane0.getAgentID());
							simulationAgents.removeAgent(agentPane1.getAgentID());
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (NullPointerException ignored) {
					}
				}).start();
			});

//			TODO
//			long stepDiscrete = (long) step;
//			if (stepDiscrete > generatedStep) {
//				generatedStep = stepDiscrete;
//				Collection<AgentPane> agentPanes = simulationAgents.addStepAgentPanes(generatedStep, agents.values());
//				assert agentPanes == null;
//				SimulationMenuTab3.setTimelineMaximum(stepDiscrete);
//			}
		}
		SimulationMenuTab3.setTimelineMaximum(step);
	}

	/**
	 * Stop this timer, save info about last state to all agents.
	 */
	@Override
	public void stop() {
		long now = System.nanoTime();
		super.stop();
		synchronized (agents) {
			agents.values().forEach(a -> a.pause(now));
		}
	}

	private void addArrivedAgents(double step) {
		Iterator<Agent> iterator = simulationAgents.getArrivingAgents().iterator();
		while (iterator.hasNext()) {
			Agent agent = iterator.next();
			if (agent.getPlannedTime() > step) {
				 return;
			}
			addAgentPane(agent);
			iterator.remove();
		}
	}

	private void addAgentPane(Agent agent) {
		long agentID = agent.getId();
		Simulation simulation = simulationAgents.getSimulation();
		long startTime = simulation.getTime(agent.getPlannedTime());
		AgentPane agentPane = new AgentPane(startTime, agent, simulation.getPeriod(), simulation.getIntersectionGraph().getVerticesWithIDs(), cellSize);

		synchronized (agents) {
			agents.put(agentID, agentPane);
		}
		simulationAgents.addAgentPane(agentPane);
	}
}
