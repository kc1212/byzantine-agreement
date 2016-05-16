byzantine-agreement
===================

We implement the randomised Byzantine agreement protocol proposed by Michael Ben-Or: https://www.cs.utexas.edu/~lorenzo/corsi/cs380d/papers/p27-ben-or.pdf.

Make sure `rmiregistry` is running in the classpath before trying to run the algorithm.
You can run the algorithm like so - `java Byzantine_Main multi 6 1`.
We support two modes - `single` and `multi`, the prior simply runs a single node, the latter runs multiple nodes each in their own thread.
Detailed command line options can be found by typing `java Byzangine_Main`.
Restart `rmiregistry` if you are observing `AlreadyBoundException`.
