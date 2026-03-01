# Simple Packer

A simple Android APK packer using XOR encryption.

## Build

```bash
./build.sh
```

This produces `SimplePacker.jar` and `stub.dex`. Which are necesary for running the packing script, also because ART only reads dex. 

## Usage

```bash
./pack <input.apk> <output.apk>
```

## How it works

**Packing** — the packer reads your APK, pulls out the original `classes.dex`, XOR encrypts it, and stores it as `assets/p.dat` inside the APK. It also saves the original app entry points to `assets/app.txt` and `assets/entry.txt`. The original `classes.dex` is then replaced with a stub that handles unpacking at runtime. The manifest is patched to point to the stub as the new entry point.

**Unpacking at runtime** — when the packed app launches, the stub runs first. It reads `p.dat` from assets, decrypts the bytes back into the original DEX, and loads them into memory using `InMemoryDexClassLoader` — no disk writes. This loader is then swapped in as the app's class loader so all the real app classes become available. A stub launcher activity then reads `entry.txt` and starts the real app, handing it offcompletely so the user just sees the original app running normally.
