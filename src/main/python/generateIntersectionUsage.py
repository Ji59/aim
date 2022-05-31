#! /usr/bin/env python3

import matplotlib.pyplot as plt
import networkx as nx


def generate_intersection(graph: dict) -> nx.DiGraph:
    intersection = nx.DiGraph()
    for vertex in graph.get("vertices"):
        location = {'x': vertex.get('x'), 'y': vertex.get('y')}
        intersection.add_node(vertex.get('id'))
    return intersection


if __name__ == "__main__":
    import sys
    import loadDirectory

    directory = loadDirectory.get_directory(sys.argv)
    graph = loadDirectory.get_intersection(directory)
    intersection = generate_intersection(graph)
    print(intersection)
