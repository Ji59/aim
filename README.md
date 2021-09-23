# AIM

## User documentation
When application is started, two windows 
`Autonomous Intersection Management` (*AIM*) and `Settings` open. 

The `AIM` window contains intersection and simulation settings.
The `Settings` window has five tabs where several parameters could be modified. 

### Creating intersection

Intersection parameters are set in the first tab in Settings window.

#### Model

User can set one of the models. 
They are `Square`, `Hexagonal`, `Octagonal` (and ~~Custom~~ not implemented).

#### Granularity

User can change granularity of the intersection using the slider or 
writing desired number into associated text box.  
It affects how many roads are on side of the intersection.

#### Entries and exits

Number of entries and exits could be changed too, also using slider or text box. 
Selected number means number of entries / exits in each direction.

Available amount depends on intersection granularity, 
sum of entries and exits can not be greater than granularity.

#### ~~Restrictions~~

not implemented

#### History

There are available `previous` and `next` buttons 
with whose help the intersection could be changed to last / next selected.

### Selecting agents

Agents are set in the second tab in Settings window.

#### Agents generating

Agents could be generating in on the run randomly
(~~or taken from a file~~ not implemented).

The method can be chosen in `Agents input file` combo box.

#### Number of steps

Number of steps in which new agents are generated 
can be set in the next text field `Number of steps`.

Value 0 means infinite simulation,
(~~positive integer means agents are generated in those first steps~~
not implemented).

#### Amount of new agents

This section contains two sliders, `Minimum` and `Maximum`. 
These sliders restrict number of new agents in each step.

Maximum value is restricted by `entries * 4`.

#### Direction distribution

In this section user can select with what 
probability agent arrives in each direction.

The values must sum to 100%. Values are affected by sliders, 
where value for each direction are computed 
relatively to other directions values.

### Algorithm selection
Used algorithm is set in the third tab in Settings window.

Algorithm is selected in combo box. 
There should be shown description of the algorithm underneath.

### Simulation 
Simulation parameters are set in the fourth tab in Settings window 
or on the left side of the intersection window.

#### Speed
The speed of simulation can be modified using this slider. 
The value can be changed while simulation is running.

#### ~~Timeline~~
not implemented

#### Control buttons

* `Play` button starts the simulation. 
On click the text changes to `Pause`. 
With another click, simulation is stopped.

* `Restart` button restarts the simulation to step 0 
and deletes all created agents.
* ~~Save agents~~ not implemented

#### Statistics
There are shown basic statistics about the current run.
* `Steps` label show in what step is the simulation currently.
* ~~Total delay~~ not implemented
* ~~Rejections~~ not implemented
* `Collisions` label counts number of collisions in the current run.
* ~~Remaining time~~ not implemented

### Agent parameters
Agents parameters can be modified in the last, fifth, tab in Settings window.

#### Size
Size of the generated agents can be changed 
in this section using provided sliders.

The size is set in how many vertices the agent is taking.

`Minimum` section modifies minimal possible length `L` 
and minimal possible width `W` of generated agents.

`Maximum` section modifies maximal values. 
These values are affected by *granularity*, *entries* and *exits*.

#### Speed
Speed of generated agents can be modified here.
The values are set in vertices traveled per step.

There are also `Minimum` and `Maximum`  sliders.

#### Deviation
Agent parameters, specifically *size* and *speed* 
can have some deviation when generating.

The algorithm does not know about this value, 
it simulates some imperfections in real world.

#### ~~RNG distribution~~
not implemented

### Abstract model
The intersection representation could be switched 
to more theoretical, graph-like representation 
using button in `AIM` window on the left to the top of statistics.

The button has `Real` text when started and the intersection 
has more realistic design.
After clicking the button, the text changes to `Abstrast` 
and the design of the intersection changes to the mathematical one. 