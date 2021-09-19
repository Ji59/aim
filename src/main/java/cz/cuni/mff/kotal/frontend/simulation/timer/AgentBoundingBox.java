package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import org.jetbrains.annotations.NotNull;


public class AgentBoundingBox implements Comparable<AgentBoundingBox> {
	private final AgentPane agentPane;
	private final double[] boundingBox;

	public AgentBoundingBox(AgentPane agentPane, double[] boundingBox) {
		this.agentPane = agentPane;
		this.boundingBox = boundingBox;
	}

	public AgentPane getAgentPane() {
		return agentPane;
	}

	public double getStartX() {
		return boundingBox[0];
	}

	public double getStartY() {
		return boundingBox[1];
	}

	public double getEndX() {
		return boundingBox[2];
	}

	public double getEndY() {
		return boundingBox[3];
	}

	@Override
	public int compareTo(@NotNull AgentBoundingBox agentBoundingBox) {
		int c0 = Double.compare(boundingBox[0], agentBoundingBox.boundingBox[0]);
		if (c0 != 0) {
			return c0;
		}
		return Double.compare(boundingBox[1], agentBoundingBox.boundingBox[1]);
	}
}
