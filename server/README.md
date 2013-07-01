## Build Instructions

1. Install JDK 7
	- If you use apt-get, just type `sudo apt-get install openjdk-7-jdk`
2. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
3. In this directory, type `lein uberjar`
4. To run it in the background, type `java -jar target/Nightweb-{VERSION}-standalone.jar &`
