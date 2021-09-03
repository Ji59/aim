package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import org.jetbrains.annotations.NotNull;


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
	public int compareTo(@NotNull AgentBoundingBox agentBoundingBox) {
		int c0 = Double.compare(boundingBox[0], agentBoundingBox.boundingBox[0]);
		if (c0 != 0) {
			return c0;
		}
		return Double.compare(boundingBox[1], agentBoundingBox.boundingBox[1]);
	}
}
