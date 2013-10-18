## Build Instructions

1. Install JDK 7
	- If you use apt-get, just type `sudo apt-get install openjdk-7-jdk`
2. Download the [Android SDK](http://developer.android.com/sdk/index.html)
	- Just the SDK Tools; you don't need the bundle
3. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
4. Create or modify `~/.lein/profiles.clj` so it looks like this:

```clojure
{:user {
    :plugins [[lein-droid "x.x.x"]]
    :android {:sdk-path "path/to/android-sdk-linux"
              :force-dex-optimize true}
}}
```

Replace the "x.x.x" with the version below:

![](https://clojars.org/lein-droid/latest-version.svg)

Read the [lein-droid tutorial](https://github.com/clojure-android/lein-droid/wiki/Tutorial) to learn the commands.
