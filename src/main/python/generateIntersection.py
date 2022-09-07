#! /usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt
import networkx as nx
from typing import Any

ROAD_COLOR = (0.5, 0.5, 0.5)
ENTRY_COLOR = (0.5625, 0.4375, 0.4375)
EXIT_COLOR = (0.4375, 0.4375, 0.5625)


def generate_intersection(graph: dict[str, Any]) -> (nx.DiGraph, dict[int, (float, float)], [int], [int]):
	intersection = nx.DiGraph()
	locations = {}
	entries = []
	exits = []
	for vertex in graph.get("vertices"):
		location = (vertex.get('x'), 1 - vertex.get('y'))
		id_ = vertex.get('id')
		locations[id_] = location
		intersection.add_node(id_)
		intersection.add_edges_from([(id_, neighbour) for neighbour in vertex.get("neighbourIds")])
		type_ = vertex.get("type")
		if type_.startswith("ENTRY"):
			entries.append(id_)
		elif type_.startswith("EXIT"):
			exits.append(id_)

	return intersection, locations, entries, exits


def create_graph_plot(graph: dict[str, Any], intersection: nx.DiGraph, locations: dict[int, (float, float)], entries: [int], exits: [int]) -> None:
	granularity = graph.get("granularity")
	fig, ax = plt.subplots(1, 2, figsize=(11, 6))
	type_ = graph.get("type")
	fig.subplots_adjust(top=0.88)
	plt.subplots_adjust(wspace=0.02, left=0.01, right=0.99, bottom=0.01, top=0.9)
	# Set the x and y limits for each axis, leaving a 0.05 margin te ensure
	# that nodes near the edges of the graph are not clipped.
	min_lim = -0.05
	max_lim = 1.05
	ax[0].set_xlim([min_lim, max_lim])
	ax[0].set_ylim([min_lim, max_lim])
	ax[1].set_xlim([min_lim, max_lim])
	ax[1].set_ylim([min_lim, max_lim])
	ax[0].set_title("Grafová reprezentace")
	ax[1].set_title("Vzhled reálné křižovatky")

	colors = [ENTRY_COLOR if id_ in entries else EXIT_COLOR if id_ in exits else ROAD_COLOR for id_ in intersection.nodes]
	nx.draw_networkx(intersection, locations, node_color=colors, node_shape='o', labels={}, label="Graph", ax=ax[0])
	if type_ == "Square grid":
		name = "Čtvercový typ"
		cell_size = 1 / (granularity + 2)
		nx.draw_networkx_nodes(intersection, locations, node_size=17000 * cell_size, node_color=colors, node_shape='s', label="Real", ax=ax[1])
	elif type_ == "Octagonal grid":
		name = "Oktagonální typ"
		cell_size = 1 / (granularity + 2)
		octagonal_end = granularity * granularity - 4
		octagonal_vertices = np.arange(octagonal_end)
		nx.draw_networkx_nodes(intersection, locations, nodelist=octagonal_vertices, node_size=17000 * cell_size, node_color=ROAD_COLOR, node_shape='8', label="Real", ax=ax[1])
		entries_exits_end = octagonal_end + len(entries) + len(exits)
		entries_exits_vertices = np.arange(octagonal_end, entries_exits_end)
		entries_exits_colors = [ENTRY_COLOR if id_ in entries else EXIT_COLOR for id_ in entries_exits_vertices]
		nx.draw_networkx_nodes(intersection, locations, nodelist=entries_exits_vertices, node_size=10000 * cell_size, node_color=entries_exits_colors, node_shape='s', label="Real", ax=ax[1])
		diamond_vertices = np.arange(entries_exits_end, len(locations))
		nx.draw_networkx_nodes(intersection, locations, nodelist=diamond_vertices, node_size=4096 * cell_size, node_color=ROAD_COLOR, node_shape='D', label="Real", ax=ax[1])
	elif type_ == "Hexagonal grid":
		name = "Hexagonální typ"
		cell_size = 1. / (2 * granularity + 1)
		hexagonal_end = granularity * (granularity - 1) / 2 * 6 + 1
		hexagonal_vertices = np.arange(hexagonal_end)
		nx.draw_networkx_nodes(intersection, locations, nodelist=hexagonal_vertices, node_size=16384 * cell_size, node_color=ROAD_COLOR, node_shape='H', label="Real", ax=ax[1])
		entries_exits_vertices = np.arange(hexagonal_end, len(locations))
		entries_exits_colors = [ENTRY_COLOR if id_ in entries else EXIT_COLOR for id_ in entries_exits_vertices]
		nx.draw_networkx_nodes(intersection, locations, nodelist=entries_exits_vertices, node_size=8192 * cell_size, node_color=entries_exits_colors, node_shape='o', label="Real", ax=ax[1])

	fig.suptitle(name)
	plt.savefig(type_.replace(' ', '_') + ".pdf", format="pdf")


if __name__ == "__main__":
	import sys
	import loadDirectory

	directory = loadDirectory.get_directories(sys.argv)[0]
	graph = loadDirectory.get_intersection(directory)
	intersection, locations, entries, exits = generate_intersection(graph)

	create_graph_plot(graph, intersection, locations, entries, exits)
