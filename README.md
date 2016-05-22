byzantine-agreement
===================

We implement the randomised Byzantine agreement protocol proposed by Michael Ben-Or: https://www.cs.utexas.edu/~lorenzo/corsi/cs380d/papers/p27-ben-or.pdf.

The algorithm can be started as follows.
```
cd ~/code/byzantine-agreement/out/production/byzantine-agreement
rmiregistry &
java -classpath ~/.m2/repository/com/beust/jcommander/1.48/jcommander-1.48.jar:. Byzantine_Main -n 6 -f 1
```

Detailed command line options can be found using the `-h` flag.
```
java -classpath ~/.m2/repository/com/beust/jcommander/1.48/jcommander-1.48.jar:. Byzantine_Main -h
```

Restart `rmiregistry` if you are observing `AlreadyBoundException`.
```
pkill rmiregistry
rmiregistry &
```
