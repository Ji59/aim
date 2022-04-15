package cz.cuni.mff.kotal.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;


/**
 * Class representing bounding box surrounding agent.
 * Class was created for collisions detection algorithms.
 */
public class AgentBoundingBox implements Comparable<AgentBoundingBox> {
	private final AgentPane agentPane;
	private final double[] boundingBox;

	/**
	 * Create new agent bounding box.
	 *
	 * @param agentPane   Agent pane inside the box
	 * @param boundingBox Array of points creating the bounding box, in format [min_x, min_y, max_x, max_y]
	 */
	public AgentBoundingBox(AgentPane agentPane, double[] boundingBox) {
		assert boundingBox.length == 4;

		this.agentPane = agentPane;
		this.boundingBox = boundingBox;
	}

	/**
	 * @return Agent pane inside the bounding box
	 */
	public AgentPane getAgentPane() {
		return agentPane;
	}

	/**
	 * @return Minimal X value
	 */
	public double getStartX() {
		return boundingBox[0];
	}

	/**
	 * @return Minimal Y value
	 */
	public double getStartY() {
		return boundingBox[1];
	}

	/**
	 * @return Maximal X value
	 */
	public double getEndX() {
		return boundingBox[2];
	}

	/**
	 * @return Maximal Y value
	 */
	public double getEndY() {
		return boundingBox[3];
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AgentBoundingBox that = (AgentBoundingBox) o;
		return agentPane.equals(that.agentPane) && Arrays.equals(boundingBox, that.boundingBox);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(agentPane);
		result = 31 * result + Arrays.hashCode(boundingBox);
		return result;
	}

	/**
	 * Compare two bounding boxes.
	 * This bounding box is smaller than the other if and one if minimal X is smaller or minimal X is same and minimal Y smaller.
	 *
	 * @param agentBoundingBox Compared bounding box
	 * @return The value 0 if bounding boxes are has same top left corner coordinates; a value less than 0 if the top left corner of this bounding box is to the left to the other one or if they are horizontally equal, and this one is up to the other; and a greater value otherwise.
	 */
	@Override
	public int compareTo(@NotNull AgentBoundingBox agentBoundingBox) {
		int c0 = Double.compare(boundingBox[0], agentBoundingBox.boundingBox[0]);
		if (c0 != 0) {
			return c0;
		}
		return Double.compare(boundingBox[1], agentBoundingBox.boundingBox[1]);
	}
}
