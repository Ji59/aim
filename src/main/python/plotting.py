import matplotlib.pyplot as plt
import networkx as nx


def plot_ints(planning_times: {str: [int]}):
	for run, data in planning_times.items():
		plt.plot(data, label=run)
	plt.legend()
	plt.show()


def plot_time_vs_agents(times, agents) -> None:
	fig = plt.figure()
	ax0 = fig.add_subplot(111, label="1")

	ax0.plot(times, color="C0")
	ax0.set_xlabel("Krok")
	ax0.set_ylabel("Čas plánování [ms]", color="C0")
	# ax0.tick_params(axis='x', colors="C0")
	ax0.tick_params(axis='y', colors="C0")

	ax2 = fig.add_subplot(111, label="2", frame_on=False)
	ax2.plot(agents, color="C1")
	# ax2.xaxis.tick_top()
	ax2.yaxis.tick_right()
	# ax2.set_xlabel('x label 2', color="C1")
	ax2.set_ylabel('Agentů na křižovatce', color="C1")
	ax2.xaxis.set_label_position('top')
	ax2.yaxis.set_label_position('right')
	# ax2.tick_params(axis='x', colors="C1")
	ax2.tick_params(axis='y', colors="C1")

	plt.show()


def plot_vertices_usage(intersection: nx.DiGraph, locations: dict[int, (float, float)], usage: [float]) -> None:
	colors = [(u, u, u) for u in usage]
	nx.draw_networkx(intersection, locations, node_color=colors, node_shape='o', labels={}, label="Graph")
	plt.show()


def convolute(data, conv: int = 8):
	import numpy as np

	length = len(data)
	convoluted_data = [0] * length
	convoluted_weights = [0] * length
	for i, d in enumerate(data):
		min_ = max(i - conv, 0)
		max_ = min(i + conv + 1, length)
		for j in range(min_, max_):
			weight = (conv - abs(i - j) + 1) / (conv + 1)
			convoluted_data[j] += d * weight
			convoluted_weights[j] += weight
			if j == i - conv:
				convoluted_data[j] /= convoluted_weights[j]  # min(2 * conv + 1, i)
	min_ = max(length - 2 * conv, 0)
	for i, j in enumerate(range(max(length - conv, 0), length)):
		convoluted_data[j] /= convoluted_weights[j]  # length - min_ - i
	return convoluted_data
