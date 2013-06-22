## Build Instructions

1. Install JDK 7
	- If you use apt-get, just type `sudo apt-get install openjdk-7-jdk`
2. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
3. In this directory, type `lein uberjar`

## Installer Instructions

1. Download [JWrapper and JRE pack 1.7](http://www.jwrapper.com/download.html)
2. Move the JWrapper file and the extracted JRE pack into the "installer" directory, so it contains three items:
	- jwrapper.xml
	- jwrapper-VERSION.jar
	- JRE-1.7/
3. `cd` into the "installer" directory and type `java -jar jwrapper-VERSION.jar jwrapper.xml`
