package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;


public class SimulationTimer extends AnimationTimer {
	private final Map<Long, AgentPane> agents;
	private final SimulationAgents simulationAgents;

	public SimulationTimer(Map<Long, AgentPane> agents, SimulationAgents simulationAgents) {
		this.agents = agents;
		this.simulationAgents = simulationAgents;
	}

	@Override
	public void handle(long now) {
		simulationAgents.resetRectangles();
		synchronized (agents) {
			Set<Map.Entry<Long, AgentPane>> finishedAgents = agents.entrySet().stream().filter(a -> !a.getValue().isDisable()).filter(entry -> entry.getValue().handleTick(now)).collect(Collectors.toSet());
			finishedAgents.forEach(simulationAgents::removeAgent);

			Set<Pair<AgentPane, AgentPane>> overlappingAgents = getBoundingBoxesOverlaps();
			overlappingAgents = overlappingAgents.stream().filter(pair -> inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

			// TODO
			overlappingAgents.forEach(agentPanePair -> {
				AgentPane agentPane0 = agentPanePair.getKey();
				AgentPane agentPane1 = agentPanePair.getValue();

				agentPane0.collide();
				agentPane1.collide();

				simulationAgents.getSimulation().addCollision();

				// TODO remove log
				System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());

				new Thread(() -> {
					try {
						// TODO replace with number of steps
						Thread.sleep(simulationAgents.getSimulation().getPeriod());
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
		}
	}

	// TODO refactor
	private Set<Pair<AgentPane, AgentPane>> getBoundingBoxesOverlaps() {
		List<AgentBoundingBox> boundingBoxes;
		synchronized (agents) {
			boundingBoxes = agents.values().stream().filter(a -> !a.isDisabled()).map(a -> new AgentBoundingBox(a, a.getBoundingBox())).sorted().collect(Collectors.toList());
		}

//			TODO is needed collision pair?
//			Set<AgentPane> overlappingAgents = new HashSet<>();
		Set<Pair<AgentPane, AgentPane>> overlappingAgents = new HashSet<>();
		List<AgentBoundingBox> actualBoundingBoxes = new LinkedList<>();

		SortedSet<AgentBoundingBox> stopValues = new TreeSet<>((a0, a1) -> {
			int c0 = Double.compare(a0.getEndX(), a1.getEndX());
			if (c0 != 0) {
				return c0;
			}
			int c1 = Double.compare(a0.getStartY(), a1.getStartY());
			if (c1 != 0) {
				return c1;
			}
			return Long.compare(a0.getAgentPane().getAgentID(), a1.getAgentPane().getAgentID());
		});

		for (AgentBoundingBox agentBoundingBox : boundingBoxes) {
			double boundingBoxStartX = agentBoundingBox.getStartX();
			Iterator<AgentBoundingBox> iterator = stopValues.iterator();
			AgentBoundingBox next;
			while (iterator.hasNext() && boundingBoxStartX > (next = iterator.next()).getEndX()) {
				assert (next.getAgentPane().getAgentID() == actualBoundingBoxes.get(actualBoundingBoxes.indexOf(next)).getAgentPane().getAgentID());
				actualBoundingBoxes.remove(next);
				iterator.remove();
			}
			stopValues.add(agentBoundingBox);

			double boundingBoxStartY = agentBoundingBox.getStartY();
			double boundingBoxEndY = agentBoundingBox.getEndY();
			int neighbourIndex;

			// skip all ending before right Y
			for (neighbourIndex = 0; neighbourIndex < actualBoundingBoxes.size(); neighbourIndex++) {
				AgentBoundingBox neighbour = actualBoundingBoxes.get(neighbourIndex);
				if (neighbour.getEndY() >= boundingBoxStartY) {
					break;
				}
			}

			// check others
			for (; neighbourIndex < actualBoundingBoxes.size(); neighbourIndex++) {
				AgentBoundingBox neighbour = actualBoundingBoxes.get(neighbourIndex);
				if (neighbour.getStartY() > boundingBoxEndY) {
					// can't collide and the next can't to
					break;
				}

				if (neighbour.getEndY() >= boundingBoxStartY) {
					// collision
					overlappingAgents.add(new Pair<>(neighbour.getAgentPane(), agentBoundingBox.getAgentPane()));
				}
			}

			actualBoundingBoxes.add(neighbourIndex, agentBoundingBox);
		}

		return overlappingAgents;
	}

	static private boolean inCollision(AgentPane agent0, AgentPane agent1) {
		List<Point> cornerPoints0 = agent0.getCornerPoints();
		List<Point> cornerPoints1 = agent1.getCornerPoints();

		if (existsSeparatingLine(cornerPoints0, cornerPoints1)) {
			return false;
		}

		return !existsSeparatingLine(cornerPoints1, cornerPoints0);
	}

	static boolean existsSeparatingLine(List<Point> points0, List<Point> points1) {
		points0 = new LinkedList<>(points0);
		points1 = new LinkedList<>(points1);

		int points0Size = points0.size();

		Point point0 = points0.remove(0);
		Point point1 = null;

		for (int i = 0; i < points0Size; i++) {
			point1 = points0.remove(0);
			LineSegment line = new LineSegment(point0, point1);

			double side = line.getSide(points0.get(0));
			assert side != 0;
			int sign = side > 0 ? 1 : -1;
			assert points0.stream().allMatch(p -> line.getSide(p) * sign >= 0);

			if (points1.stream().allMatch(p -> line.getSide(p) * sign < 0)) {
				return true;
			}

			points0.add(point0);
			point0 = point1;
		}

		return false;
	}

	@Override
	public void stop() {
		long now = System.nanoTime();
		super.stop();
		synchronized (agents) {
			agents.values().forEach(a -> a.pause(now));
		}
	}
}
