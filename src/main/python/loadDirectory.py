#! /usr/bin/env python3

import os.path as path
import json
from typing import Any


def get_directories(argv: [str]) -> [str]:
	if len(argv) > 1:
		directories = argv[1:]
	else:
		directories = [path.expanduser(path.join('~', "statistics"))]  # "C:\Users\kodas\OneDrive\Documents\statistics\Parameters.json"
	for directory in directories:
		assert path.isdir(directory)
	return directories


def get_intersection(directory: str) -> dict[str, Any]:
	with open(path.join(directory, "Parameters.json"), 'r') as parameters_file:
		parameters = json.load(parameters_file)
		graph = parameters.get("graph")
	return graph


def get_agents(directory: str):
	agents_path = path.join(directory, "agents.json")
	# TODO implement
	pass
