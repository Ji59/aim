package cz.cuni.mff.kotal.simulation;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LoadingSimulation extends Simulation {

	private @Nullable Timer timer;
	private final @NotNull ListIterator<Agent> sortedAgentsIterator;

	public LoadingSimulation(@NotNull SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents, @NotNull String path) throws FileNotFoundException {
		super(intersectionGraph, algorithm, simulationAgents);
		sortedAgentsIterator = loadAgents(path);
	}

	@Override
	@Deprecated
	protected void start() {
		long msPeriod = period / 1_000_000;
		long delay = isRunning() ? msPeriod : 0L;
		startTime = System.nanoTime() - (isRunning() ? period : 0);

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, msPeriod);
	}

	@Override
	protected @NotNull Collection<Agent> loadAgents(long step) {
		if (sortedAgentsIterator.hasNext()) {
			@NotNull List<Agent> newAgents = new ArrayList<>();
			@Nullable Agent nextAgent = null;
			while (sortedAgentsIterator.hasNext() && (nextAgent = sortedAgentsIterator.next()).getArrivalTime() <= step) {
				newAgents.add(nextAgent);
			}
			if (nextAgent != null && nextAgent.getArrivalTime() > step && sortedAgentsIterator.hasPrevious()) {
				sortedAgentsIterator.previous();
			}
			return newAgents;
		} else {
			synchronized (delayedAgents) {
				if (delayedAgents.values().stream().allMatch(Collection::isEmpty)) {
					// FIXME refactor
					if (step > finalStep) {
						ended = true;
					}
				}
			}
		}
		return new ArrayList<>(0);
	}

	private @NotNull ListIterator<Agent> loadAgents(@NotNull String path) throws FileNotFoundException {
		@NotNull FileReader reader = new FileReader(path);
		@NotNull Gson gson = new Gson();
		List<BasicAgent> basicAgents = gson.fromJson(reader, new TypeToken<List<BasicAgent>>() {
		}.getType());
		@NotNull List<Agent> agents = basicAgents.stream().sorted(Comparator.comparingDouble(BasicAgent::getArrivalTime)).map(Agent::new).toList();
		allAgents.putAll(agents.stream().collect(Collectors.toMap(BasicAgent::getId, Function.identity())));
		return agents.listIterator();
	}

	@Override
	@Deprecated
	protected void stopSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	protected void resetSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Deprecated
	private @NotNull TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				System.out.println(step);
				long currentStep = getStep(); // TODO

				loadAgents(currentStep);

				updateAgentsStats(step);
				updateStatistics(allAgents.size());
				step++;
			}
		};
	}
}
