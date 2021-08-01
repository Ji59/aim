package cz.cuni.mff.kotal.frontend.simulation;


import com.sun.javafx.collections.ObservableListWrapper;
import cz.cuni.mff.kotal.MyNumberOperations;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.MyGenerator.generateRandomInt;


public class SimulationAgents extends Pane {
	private Simulation simulation;
	private final Map<Long, AgentPane> AGENTS = new HashMap<>();
	private SimulationTimer timer;

	public SimulationAgents(double height, Simulation simulation) {
		this.simulation = simulation;
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public void addAgent(long startTime, Agent agent, double period) {
		long agentID = agent.getId();
		AgentPane agentPane = new AgentPane(agentID, startTime, agent, period, AGENTS.values());

		synchronized (AGENTS) {
			AGENTS.put(agentID, agentPane);
		}

		Platform.runLater(() -> getChildren().add(agentPane));
	}

	public void addAgent(Long time, Agent agent) {
		addAgent(time, agent, IntersectionScene.getPeriod());
	}

	private void removeAgent(long agentID) {
		AgentPane agentPane;
		synchronized (AGENTS) {
			agentPane = AGENTS.remove(agentID);
		}
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	private void removeAgent(Map.Entry<Long, AgentPane> agentEntry) {
		synchronized (AGENTS) {
			AGENTS.remove(agentEntry.getKey());
		}
		AgentPane agentPane = agentEntry.getValue();
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	public void redraw(Pair<Long, Map<Long, Agent>> timeActualAgents) {
		// TODO remove
//		long time = timeActualAgents.getKey();
//		Map<Long, Agent> actualAgents = timeActualAgents.getValue();
//		Platform.runLater(() -> {
//			Iterator<Map.Entry<Long, AgentPane>> agentsIterator = AGENTS.entrySet().iterator();
//			while (agentsIterator.hasNext()) {
//				Map.Entry<Long, AgentPane> entry = agentsIterator.next();
//				long id = entry.getKey();
//				AgentPane pane = entry.getValue();
//				Agent a = actualAgents.get(id);
//				if (a == null) {
//					getChildren().remove(pane);
//					agentsIterator.remove();
//				} else {
//					pane.updateAgent(time);
//				}
//				actualAgents.remove(id);
//			}
//			actualAgents.forEach((id, agent) -> addAgent(time, agent));
//		});
	}

	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
	}

	public void pauseSimulation() {
		timer.stop();
		// TODO
//		synchronized (AGENTS) {
//			AGENTS.values().forEach(a -> a.pause());
//		}
	}

	public void resumeSimulation(double period) {
		long startTime = System.nanoTime();
		synchronized (AGENTS) {
			AGENTS.values().forEach(agentPane -> agentPane.resume(period, startTime));
		}
		timer = new SimulationTimer();
		timer.start();
	}

	public void resetSimulation() {
		synchronized (AGENTS) {
			timer.stop();
			AGENTS.values().forEach(agentPane -> {
				// TODO
//				agentPane.pause();
				getChildren().remove(agentPane);
			});
		}
		AGENTS.clear();
	}

	private class SimulationTimer extends AnimationTimer {
		@Override
		public void handle(long now) {
			synchronized (AGENTS) {
				Set<Map.Entry<Long, AgentPane>> finishedAgents = AGENTS.entrySet().stream().filter(entry -> entry.getValue().handleTick(now)).collect(Collectors.toSet());
				finishedAgents.forEach(SimulationAgents.this::removeAgent);

				Set<AgentPane> overlappingAgents = getBoundingBoxesOverlaps();
				overlappingAgents.forEach(agentPane -> {
					agentPane.collide();
//					TODO remove from simulation
//					 SimulationAgents.this.removeAgent(agentPane.getAgentID());
					AGENTS.remove(agentPane.getAgentID());
				});
			}
		}

		// TODO refactor
		private Set<AgentPane> getBoundingBoxesOverlaps() {

			class AgentBoundingBox implements Comparable<AgentBoundingBox> {
				private final AgentPane agentPane;
				private final Double[] boundingBox;

				AgentBoundingBox(AgentPane agentPane, Double[] boundingBox) {
					this.agentPane = agentPane;
					this.boundingBox = boundingBox;
				}

				public AgentPane getAgentPane() {
					return agentPane;
				}

				public Double getStartX() {
					return boundingBox[0];
				}

				public Double getStartY() {
					return boundingBox[1];
				}

				public Double getEndX() {
					return boundingBox[2];
				}

				public Double getEndY() {
					return boundingBox[3];
				}

				@Override
				public int compareTo(@NotNull AgentBoundingBox o) {
					int c0 = Double.compare(boundingBox[0], ((AgentBoundingBox) o).boundingBox[0]),
						c1 = Double.compare(boundingBox[1], ((AgentBoundingBox) o).boundingBox[1]);
					return c0 == 0 ? c1 : c0;
				}
			}


			List<AgentBoundingBox> boundingBoxes;
			synchronized (AGENTS) {
				boundingBoxes = AGENTS.values().parallelStream().map(a -> new AgentBoundingBox(a, a.getBoundingBox())).sorted().collect(Collectors.toList());
			}

//			TODO is needed collision pair?
//			 Set<Pair<AgentPane, AgentPane>> overlappingAgents = new HashSet<>();
			Set<AgentPane> overlappingAgents = new HashSet<>();
			List<AgentBoundingBox> actualBoundingBoxes = new LinkedList<>();
			List<AgentBoundingBox> stopValues = new SortedList<>(new ObservableListWrapper<>(actualBoundingBoxes), Comparator.comparingDouble(AgentBoundingBox::getEndX));
			for (AgentBoundingBox agentBoundingBox : boundingBoxes) {
				double boundingBoxStartX = agentBoundingBox.getStartX();
				while (!stopValues.isEmpty() && boundingBoxStartX > stopValues.get(0).getStartX()) {
					stopValues.remove(0);
				}

				double boundingBoxStartY = agentBoundingBox.getStartY(),
					boundingBoxEndY = agentBoundingBox.getEndY();
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
						overlappingAgents.add(neighbour.getAgentPane());
						overlappingAgents.add(agentBoundingBox.getAgentPane());
					}
				}


				actualBoundingBoxes.add(neighbourIndex, agentBoundingBox);
			}

			return overlappingAgents;
		}

		@Override
		public void stop() {
			long now = System.nanoTime();
			super.stop();
			synchronized (AGENTS) {
				AGENTS.values().forEach(a -> a.pause(now));
			}
		}
	}

// TODO remove
//	private class AgentTimer extends AnimationTimer {
//		private final long startTime;
//		private final double period,
//			relativeDistanceTraveled;
//		private final AgentPane agentPane;
//
//		public AgentTimer(long startTime, double period, double relativeDistanceTraveled, AgentPane agentPane) {
//			this.startTime = startTime;
//			this.period = period * 1_000_000; // convert to nanoseconds
//			this.relativeDistanceTraveled = relativeDistanceTraveled;
//			this.agentPane = agentPane;
//		}
//
//		public double getRelativeTimeTraveled(long time) {
//			return (time - startTime) / period;
//		}
//	}


	private class AgentPane extends StackPane {
		private final long agentID;

		private final Rectangle rectangle;
		private final Rotate rotation = new Rotate();
		private final Agent agent;
		private double distanceTraveled;
		private final Collection<AgentPane> previousAgents;

		private long startTime;
		private double period; // period in nanoseconds
		private double relativeDistanceTraveled; // TODO is this necessary?

		public AgentPane(long agentID, long startTime, Agent agent, double period, Collection<AgentPane> previousAgents) {
			this.agentID = agentID;
			this.agent = agent;
			this.distanceTraveled = 0;
			this.previousAgents = previousAgents;
			this.startTime = startTime;
			this.period = period * 1_000_000;

			Rectangle rectangle = new Rectangle(agent.getL(), agent.getW());
			TextField text = new TextField(String.valueOf(agent.getId()));
			text.setAlignment(Pos.CENTER);

			// TODO set rotation based on the arriving location
			rotation.setPivotX(agent.getL() / 2);
			rotation.setPivotY(agent.getW() / 2);
			rectangle.getTransforms().add(rotation);

			this.rectangle = rectangle;

			text.setBackground(Background.EMPTY);

			// TODO set color properly
			rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

			// TODO set proper agent size
			double size = 50;
			setPrefWidth(size);
			setPrefHeight(size);

			updatePosition();

			getChildren().addAll(rectangle, text);
		}

		private void updatePosition() {
			// TODO do position properly
			setLayoutX(agent.getX() - agent.getL() / 2);
			setLayoutY(agent.getY() - agent.getW() / 2);
		}

		public void updateRotation(double time) {
			Map<Long, Vertex> vertices = simulation.getIntersectionGraph().getVerticesWithIDs();
			Pair<Long, Long> previousNext = agent.getPreviousNextVertexIDs(time);
			GraphicalVertex start = (GraphicalVertex) vertices.get(previousNext.getKey()),
				end = (GraphicalVertex) vertices.get(previousNext.getValue());

			double angel = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
			if (angel > 0) {
				rotation.setAngle(angel);
			}
		}

		public void updateAgent(double time) {
			updatePosition();
			updateRotation(time);
		}

		public double getRelativeTimeTraveled(long time) {
			return (time - startTime) / period;
		}

		public boolean handleTick(long now) {
			double time = relativeDistanceTraveled + getRelativeTimeTraveled(now);
			try {
				getAgent().computeNextXY(time, simulation.getIntersectionGraph().getVerticesWithIDs());
			} catch (IndexOutOfBoundsException e) {
				// TODO
//				removeAgent(agent.getId());
				return true;
			}
			updateAgent(time);
			return false;
		}

		public void getCornerPoints() {
			// TODO
//			rectangle.getTransformedArea();
		}

		public Double[] getBoundingBox() {
			Double[] corners = new Double[4];
			Bounds boundingBox = getBoundsInParent();
			corners[0] = boundingBox.getMinX();
			corners[1] = boundingBox.getMinY();
			corners[2] = boundingBox.getMaxX();
			corners[3] = boundingBox.getMaxY();
			return corners;
		}

		// TODO remove
		public void pause(long now) {
//			if (timer != null) {
//				long now = System.nanoTime();
//				timer.stop();
			distanceTraveled = getRelativeTimeTraveled(now);
//				timer = null;
//			}
		}

		//
		public void resume(double period, long now) {
//			assert timer == null;
//			long now = System.nanoTime();
//			timer = new AgentTimer(now, period, distanceTraveled, this);
			this.period = period * 1_000_000;
			this.relativeDistanceTraveled = distanceTraveled;
			this.startTime = now;
//			timer.start();
		}

		public void collide() {
			// TODO
//			this.stop();
			this.rectangle.setFill(Color.RED);
		}

		public long getAgentID() {
			return agentID;
		}

		public double getRotation() {
			return rotation.getAngle();
		}

		public Agent getAgent() {
			return agent;
		}
	}
}