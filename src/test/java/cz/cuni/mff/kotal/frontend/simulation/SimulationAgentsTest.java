package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.Agent;
import org.junit.jupiter.api.Test;

import static cz.cuni.mff.kotal.MyNumberOperations.*;


class SimulationAgentsTest {
	@Test
	public void testAgentPaneRotate() {
		double angel = computeRotation(0, 0, 0, 0);
		assert doubleAlmostEqual(angel, -1, 0.0001);

		angel = computeRotation(0, 0, 0, 1);
		assert doubleAlmostEqual(angel, 0, 0.0001);

		angel = computeRotation(0, 0, 0, -1);
		assert doubleAlmostEqual(angel, 180, 0.0001);

		angel = computeRotation(0, 0, 1, 0);
		assert doubleAlmostEqual(angel, 90, 0.0001);

		angel = computeRotation(0, 0, -1, 0);
		assert doubleAlmostEqual(angel, 270, 0.0001);

		angel = computeRotation(1, 0, 0, 0);
		assert doubleAlmostEqual(angel, 270, 0.0001);

		angel = computeRotation(0, 1, -1, 0);
		assert doubleAlmostEqual(angel, 225, 0.0001);

		angel = computeRotation(0, 0, 1, 1);
		assert doubleAlmostEqual(angel, 45, 0.0001);


		angel = computeRotation(0, 0, Math.sqrt(3), 3);
		assert doubleAlmostEqual(angel, 30, 0.0001);

		angel = computeRotation(0, -1, Math.sqrt(3), 0);
		assert doubleAlmostEqual(angel, 60, 0.0001);

		angel = computeRotation(-Math.sqrt(3), 2, 0, -1);
		assert doubleAlmostEqual(angel, 150, 0.0001);

		angel = computeRotation(Math.sqrt(3) + 1, -1, 1, -2);
		assert doubleAlmostEqual(angel, 240, 0.0001);

		angel = computeRotation(Math.sqrt(3) - 1, 0, -1, 3);
		assert doubleAlmostEqual(angel, 330, 0.0001);
	}
}