# OpenNoC Dev Container

This directory provides a Ubuntu 22.04 development environment for OpenNoC.
It installs Git, GTKWave, Verilator `v4.216`, SBT, and Mill `0.11.6`. The
Verilator and Mill versions follow `../OpenNoC/setup.sh`; Ubuntu 22.04's
OpenJDK 17 is used in place of the script's obsolete OpenJDK 8 PPA.

## Use with VS Code

Open the `OpenNoC` repository root in VS Code, then run **Dev Containers:
Reopen in Container**. VS Code uses `.devcontainer/docker-compose.yml` to
build the fixed image `OpenNoC:latest`, start the fixed container
`OpenNoC-dev`, mount the whole repository at `/workspace/OpenNoC`, and
open the terminal in `/workspace/OpenNoC/OpenNoC` as `root@dev`.

The configuration forwards the WSLg X11 socket, so `gtkwave` can open a window
when VS Code is launched from a WSL distribution with Docker Desktop WSL
integration enabled.

## Build

Run this command from the repository root (the directory containing
`OpenNoC`):

```bash
docker compose -f .devcontainer/docker-compose.yml build
```

## Run with WSLg GUI support

In a WSL terminal with Docker Desktop WSL integration enabled, run the
following command from the repository root. It bind-mounts the complete
repository, including the parent `.git/modules/OpenNoC` metadata required by
OpenNoC's Git submodule checkout. The working directory remains
`/workspace/OpenNoC/OpenNoC`. It also forwards the WSLg X11 socket, so
applications such as GTKWave can open windows on the Windows desktop.

```bash
docker run -it \
  --name OpenNoC \
  --hostname dev \
  -v "$PWD:/workspace/OpenNoC" \
  --workdir /workspace/OpenNoC/OpenNoC \
  -e DISPLAY="$DISPLAY" \
  -v /mnt/wslg/.X11-unix:/tmp/.X11-unix \
  OpenNoC:ubuntu22.04
```

Verify the graphical connection inside the container:

```bash
gtkwave
```

If the WSLg X11 socket is unavailable, update WSL and ensure Docker Desktop's
**Settings > Resources > WSL Integration** enables the Linux distribution in
which the command is run.

## Tool check

```bash
git --version
dpkg-query -W -f='${Version}\n' gtkwave
verilator --version
sbt --version
mill --version
```
