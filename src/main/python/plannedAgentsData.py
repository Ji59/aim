#! /usr/bin/env python3
from queue import PriorityQueue

import numpy as np

import generateIntersection
import loadDirectory


def steps_planned_agents(last_step: int, agents: [loadDirectory.Agent]) -> (int, [int]):
	agents_planned = [0] * last_step
	not_planned = 0
	for agent in agents:
		planned_time = agent.plannedTime
		if planned_time >= 0:
			agents_planned[planned_time] += 1
		else:
			not_planned += 1
	return not_planned, agents_planned


def steps_travelling_agents(last_step: int, agents: [loadDirectory.Agent]) -> [int]:
	travelling_agents = [0] * last_step
	for agent in agents:
		planned_step = agent.plannedTime
		traveling_time = len(agent.path)
		if planned_step >= 0:
			for s in range(planned_step, min(planned_step + traveling_time, last_step)):
				travelling_agents[s] += 1
	return travelling_agents


def find_exits(graph: loadDirectory.Graph, agent: loadDirectory.Agent) -> [int]:
	if agent.exit >= 0:
		return [agent.exit]
	else:
		exits = []
		for vertex in graph.vertices:
			type = "EXIT" + str(agent.exitDirection)
			if vertex.type == type:
				exits.append(vertex.id)
		return exits


def a_star_distance(graph: loadDirectory.Graph, start: int, end: int) -> int:
	queue = PriorityQueue()
	queue.put((0, 0, start))

	visited = [False] * len(graph.vertices)

	end_vertex = graph.vertices[end]
	end_x, end_y = end_vertex.x, end_vertex.y

	while not queue.empty():
		state = queue.get()
		vertex_id = state[2]
		if visited[vertex_id]:
			continue
		if vertex_id == end:
			return state[0]

		visited[vertex_id] = True
		vertex = graph.vertices[vertex_id]
		dist = state[0] + 1
		for neighbour_id in vertex.neighbourIds:
			if visited[neighbour_id]:
				continue
			neighbour = graph.vertices[neighbour_id]
			neighbour_x, neighbour_y = neighbour.x, neighbour.y
			heuristic = (neighbour_x - end_x) ** 2 + (neighbour_y - end_y) ** 2
			queue.put((dist, heuristic, neighbour_id))


def agents_delay(agents: [loadDirectory.Agent], graph: loadDirectory.Graph) -> [int]:
	distances = {}
	delays = []
	for agent in agents:
		if agent.plannedTime < 0:
			continue

		exits = find_exits(graph, agent)
		assert agent.path[-1] in exits
		entry = agent.entry
		if entry not in distances:
			distances[entry] = {}
		entry_distances = distances[entry]
		min_distance = np.infty
		for exit_ in exits:
			if exit_ not in entry_distances:
				entry_distances[exit_] = a_star_distance(graph, entry, exit_)
			min_distance = min(min_distance, entry_distances[exit_])
		delays.append(agent.plannedTime - agent.arrivalTime + len(agent.path) - min_distance)

	return delays


def vertices_visits(vertices: int, agents: [loadDirectory.Agent]) -> [int]:
	visits = [0] * vertices
	for agent in agents:
		for vertexID in agent.path:
			visits[vertexID] += 1
	return visits


if __name__ == "__main__":
	import sys
	import os.path as path

	directories = loadDirectory.get_directories(sys.argv)

	for directory in directories:
		agents_data = {}
		last_step = 0
		for subdir in loadDirectory.get_subdirectories(directory):
			subdir_path = path.join(directory, subdir)
			agents = loadDirectory.get_agents(subdir_path)
			parameters = loadDirectory.get_parameters(subdir_path)

			simulation_last_step = parameters.steps
			last_step = max(int(np.ceil(simulation_last_step)), last_step)

			not_planned, agents_planned = steps_planned_agents(last_step, agents)

			travelling_agents = steps_travelling_agents(last_step, agents)

			agents_data[subdir] = (not_planned, agents_planned, travelling_agents)

			graph = parameters.graph
			vertices_visits_ = vertices_visits(len(graph.vertices), agents)
			vertices_occupation = [visits / simulation_last_step for visits in vertices_visits_]
			intersection, locations, entries, exits = generateIntersection.generate_intersection(graph)
			# plotting.plot_vertices_usage(intersection, locations, vertices_occupation)

			delays = agents_delay(agents, graph)

			planning_times = parameters.simulatingTime

			print(
				f"{subdir}: "
				f"{last_step}, "
				f"{not_planned}, "
				# f"{np.mean(agents_planned):.2f}, {np.std(agents_planned):.2f}, "
				f"{np.mean(travelling_agents):.2f}, {np.std(travelling_agents):.2f}, "
				f"{int(sum(delays))}, {np.mean(delays):.2f}, "
				f"{np.mean(planning_times):.2f}, "
				# f"{np.std(planning_times):.2f}"
			)

		travelling_data = {}
		for subdir, data in agents_data.items():
			travelling_data[subdir] = data[2] + [0] * (last_step - len(data[2]))
# travelling_data[subdir + "_p"] = data[1] + [0] * (last_step - len(data[1]))

# plotting.plot_ints(travelling_data)
