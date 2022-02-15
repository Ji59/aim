package cz.cuni.mff.kotal.simulation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LoadingSimulation extends Simulation {

	private Timer timer;
	private final ListIterator<Agent> sortedAgentsIterator;
	private long finalStep = 0;

	public LoadingSimulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents, String path) throws FileNotFoundException {
		super(intersectionGraph, algorithm, simulationAgents);
		sortedAgentsIterator = loadAgents(path);
	}

	@Override
	protected void start() {
		long delay = isRunning() ? period : 0L;

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, period);
	}

	private TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				step++;
				long currentStep = getStep(); // TODO

				long stepTime = System.nanoTime();
				if (sortedAgentsIterator.hasNext()) {
					List<Agent> newAgents = new ArrayList<>();
					Agent nextAgent = null;
					while (sortedAgentsIterator.hasNext() && (nextAgent = sortedAgentsIterator.next()).getArrivalTime() <= step) {
						newAgents.add(nextAgent);
					}
					if (nextAgent != null && nextAgent.getArrivalTime() > step && sortedAgentsIterator.hasPrevious()) {
						sortedAgentsIterator.previous();
					}

					newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
				} else {

					if (delayedAgents.values().stream().allMatch(Collection::isEmpty)) {
						if (step > finalStep) {
							stop();
						} else {
							updateStatistics(sortedAgentsIterator.nextIndex());
							return;
						}
					}
				}

				List<Agent> entriesAgents = delayedAgents.values().stream().map(entryList -> entryList.stream().findFirst().orElse(null)).filter(Objects::nonNull).toList();

				Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents, currentStep);
				plannedAgents.forEach(agent -> {
					simulationAgents.addAgent(stepTime, agent);
					agent.setPlannedTime(currentStep);
					long leavingTime = agent.getPath().size() + currentStep;
					if (leavingTime > finalStep) {
						finalStep = leavingTime;
					}
				});

				delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(plannedAgents));
				// TODO replace with iterator
				Set<Agent> rejectedAgents = delayedAgents.values().parallelStream().flatMap(Collection::parallelStream).filter(agent -> agent.getArrivalTime() < getStep() - maximumDelay).collect(Collectors.toSet());
				delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(rejectedAgents));

				agentsRejected += rejectedAgents.size();
				agentsDelay += delayedAgents.values().parallelStream().mapToLong(Collection::size).sum();

				agentsDelay += plannedAgents.stream().mapToLong(agent -> agent.getPath().size() - getIntersectionGraph().getLines().get(agent.getEntry()).get(agent.getPath().get(agent.getPath().size() - 1)).size()).sum();

				updateStatistics(sortedAgentsIterator.nextIndex());

//				System.out.println(step); // TODO
				assert getStep() == currentStep;
			}
		};
	}

	@Override
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

	private ListIterator<Agent> loadAgents(String path) throws FileNotFoundException {
		FileReader reader = new FileReader(path);
		Gson gson = new Gson();
		List<Agent> agents = gson.fromJson(reader, new TypeToken<List<Agent>>() {
		}.getType());
		agents.sort(Comparator.comparingDouble(Agent::getArrivalTime));
		allAgents.putAll(agents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
		return agents.listIterator();
	}
}
