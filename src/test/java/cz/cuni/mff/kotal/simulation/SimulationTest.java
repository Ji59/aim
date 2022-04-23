package cz.cuni.mff.kotal.simulation;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class SimulationTest {

	@Test
	void updateAgentsStats() {
		final List<Integer> path = List.of(16, 4, 5, 6, 77, 22);
		Agent a0 = new Agent(0, 0, 4, 16).setPath(path);
		Agent a1 = new Agent(1, 0, 1, 16).setPath(path);
		Agent a2 = new Agent(2, 1, -1, 16).setPath(path);
		Agent a3 = new Agent(3, 2, 8, 16).setPath(path);

		Simulation simulation = new InvalidSimulation();
		Simulation.maximumDelay = 8;

		simulation.createdAgentsQueue.addAll(List.of(a0, a1, a2, a3));
		simulation.allAgents.put(0L, a0);
		simulation.allAgents.put(1L, a1);
		simulation.allAgents.put(2L, a2);
		simulation.allAgents.put(3L, a3);

		simulation.delayedAgents.put(0, new ArrayList<>(1));
		simulation.delayedAgents.get(0).add(a0);
		simulation.delayedAgents.get(0).add(a2);
		simulation.delayedAgents.get(0).add(a3);

		simulation.plannedAgentsQueue.add(a1);

		double step = 0;
		for (; step < 1; step += 0.125) {
			simulation.updateAgentsStats(step);
			assertThat(simulation.agentsTotal).isEqualTo(2);
			assertThat(simulation.agentsDelay).isEqualTo(2);
		}

		for (; step < 2; step += 0.125) {
			simulation.updateAgentsStats(step);
			assertThat(simulation.agentsTotal).isEqualTo(3);
			assertThat(simulation.agentsDelay).isEqualTo(4);
		}

		for (; step < 4; step += 0.125) {
			simulation.updateAgentsStats(step);
			assertThat(simulation.agentsTotal).isEqualTo(4);
		}
		assertThat(simulation.agentsDelay).isEqualTo(10);
		assertThat(simulation.agentsRejected).isZero();

		simulation.plannedAgentsQueue.add(a0);

		simulation.delayedAgents.get(0).clear();
		simulation.delayedAgents.get(0).add(a3);

		simulation.rejectedAgentsQueue.add(a2);

		for (; step <= 6; step += 0.125) {
			simulation.updateAgentsStats(step);
		}
		assertThat(simulation.agentsDelay).isEqualTo(16);

		simulation.delayedAgents.get(0).clear();
		simulation.plannedAgentsQueue.add(a3);

		for (; step < 8; step += 0.125) {
			simulation.updateAgentsStats(step);
		}
		assertThat(simulation.agentsDelay).isEqualTo(18);
		assertThat(simulation.agentsRejected).isZero();

		step = 8;
		simulation.updateAgentsStats(step);
		assertThat(simulation.agentsDelay).isEqualTo(19);
		assertThat(simulation.agentsRejected).isZero();

		for (; step < 10; step += 0.125) {
			simulation.updateAgentsStats(step);
		}

		assertThat(simulation.agentsTotal).isEqualTo(4);
		assertThat(simulation.agentsDelay).isEqualTo(19);
		assertThat(simulation.agentsRejected).isOne();
	}
}