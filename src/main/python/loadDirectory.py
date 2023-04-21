#! /usr/bin/env python3
import os
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


def get_subdirectories(directory: str) -> [str]:
	assert path.isdir(directory)
	subdirectories = []
	for filename in os.listdir(directory):
		full_path = path.join(directory, filename)
		if path.isdir(full_path):
			subdirectories.append(filename)
		elif full_path.endswith(".lnk"):
			import win32com.client
			shell = win32com.client.Dispatch("WScript.Shell")
			shortcut = shell.CreateShortCut(full_path)
			subdirectories.append(shortcut.Targetpath)
	return subdirectories


class Vertex:
	def __init__(self, values: {int, Any}):
		self.__dict__ = values


class Graph:
	def __init__(self, values: {int, Any}):
		self.__dict__ = values
		vertices = [Vertex(data) for data in values["vertices"]]
		vertices.sort(key=lambda v: v.id)
		self.vertices = vertices


def get_intersection(directory: str) -> Graph:
	with open(path.join(directory, "Parameters.json"), 'r') as parameters_file:
		parameters = json.load(parameters_file)
		graph = Graph(parameters["graph"])
	return graph


class Parameters:
	def __init__(self, values: {int, Any}):
		self.__dict__ = values
		self.graph = Graph(values["graph"])


def get_parameters(directory: str) -> Parameters:
	with open(path.join(directory, "Parameters.json"), 'r') as parameters_file:
		parameters = Parameters(json.load(parameters_file))
		return parameters


class Agent:
	def __init__(self, values: {int, Any}):
		self.__dict__ = values


def get_agents(directory: str) -> [Agent]:
	agents_path = path.join(directory, "agents.json")
	with open(agents_path, 'r') as agents_file:
		json_agents = json.load(agents_file)
		return [Agent(agent) for agent in json_agents]


def get_planning_times(directory: str) -> [int]:
	with open(path.join(directory, "Parameters.json"), 'r') as parameters_file:
		parameters = json.load(parameters_file)
		planning_times = parameters.get("simulatingTime")
	return planning_times
