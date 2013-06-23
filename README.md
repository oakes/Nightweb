## Introduction

Nightweb is an app for Android devices and PCs that connects you to an anonymous, peer-to-peer social network. It is written in Clojure and uses I2P and BitTorrent on the backend. Please see [the website](http://nightweb.net) for a general overview, and the [protocol page](http://nightweb.net/protocol.html) for a more in-depth explanation of how it works.

## Contents

- `android` The Android project with a native UI
- `common` The backend code that is common to all projects
- `desktop` The desktop project with a browser-based UI
- `graphics` SVG files for all the image resources
- `server` The server project with no UI

## Licensing

All source files that originate from this project are dedicated to the public domain. That particularly includes the files in `android/src/clojure/net/nightweb`, `desktop/src/nightweb_desktop`, and `common/clojure/nightweb` (the Android, desktop, and backend code respectively). All third-party code in this project remains under their original licenses. I would love pull requests, and will assume that any Clojure contributions are also dedicated to the public domain.
