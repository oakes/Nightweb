## Build Instructions

1. Install JDK 6
	- Do not try JDK 7, as the Android SDK doesn't support it
	- If you use apt-get, just type `sudo apt-get install openjdk-6-jdk`
2. Download the [Android SDK](http://developer.android.com/sdk/index.html)
	- Just the SDK Tools; you don't need the bundle
3. Install [Leiningen](https://github.com/technomancy/leiningen)
	- The version in your package manager may be out of date
	- I recommend the manual installation they describe in their README
4. Edit your `~/.lein/profiles.clj` to enable [lein-droid](https://github.com/alexander-yakushev/lein-droid) and point it to the Android SDK path
	- Here's what mine looks like:
    {:user {
        :plugins [[lein-droid "0.1.0-preview5"]]
        :android {:sdk-path "path/to/android-sdk-linux" :force-dex-optimize true}
    }}
5. In this directory, run `lein droid build && lein droid apk && lein droid install`
	- Make sure your Android device is plugged in
	- Read the [lein-droid tutorial](https://github.com/clojure-android/lein-droid/wiki/Tutorial) for more commands
