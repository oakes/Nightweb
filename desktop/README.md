## Build Instructions

1. Install JDK 6
	- JDK 7 works as well, but the resulting JAR won't work on JRE 6
	- If you use apt-get, just type `sudo apt-get install openjdk-6-jdk`
2. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
3. In this directory, type `lein uberjar`
