## Build Instructions

1. Install Oracle JDK 7 (or Oracle JDK 8 of you're on Linux)
	- OpenJDK won't work; you need the one from Oracle
	- If you use apt-get, just type `sudo add-apt-repository ppa:webupd8team/java; sudo apt-get update; sudo apt-get install oracle-java8-installer`
2. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
3. In this directory, type `lein uberjar`
