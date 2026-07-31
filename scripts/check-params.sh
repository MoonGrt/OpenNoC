#!/usr/bin/env bash
set -euo pipefail

repo=${OPENNOC_HOME:?OPENNOC_HOME is required}
backend=${1:?backend is required}
tool=${2:-mill}

for shape in 1x1 2x1 2x2; do
  mesh_x=${shape%x*}
  mesh_y=${shape#*x}
  target="$repo/hw/build/rtl/lint-$backend-$shape"
  mkdir -p "$target"
  case "$backend" in
    verilog)
      verilator --lint-only -Wall -Wno-fatal --top-module MeshNoCTop \
        -GMESH_X="$mesh_x" -GMESH_Y="$mesh_y" -GDATA_WIDTH=32 \
        -GNODE_ID_WIDTH=2 -GVC_NUM=1 -GBUFFER_DEPTH=2 \
        "$repo/hw/verilog/vsrc/noc/router/MeshRouter.sv" \
        "$repo/hw/verilog/vsrc/noc/system/MeshNoCTop.sv"
      ;;
    chisel)
      if [[ "$tool" == sbt ]]; then
        (cd "$repo/hw/chisel" && RTL_TARGET_DIR="$target" MESH_X="$mesh_x" \
          MESH_Y="$mesh_y" DATA_WIDTH=32 NODE_ID_WIDTH=2 VC_NUM=1 BUFFER_DEPTH=2 \
          sbt "runMain noc.system.MeshNoCTop")
      else
        (cd "$repo/hw/chisel" && RTL_TARGET_DIR="$target" MESH_X="$mesh_x" \
          MESH_Y="$mesh_y" DATA_WIDTH=32 NODE_ID_WIDTH=2 VC_NUM=1 BUFFER_DEPTH=2 \
          mill --no-server chisel.runMain noc.system.MeshNoCTop)
      fi
      verilator --lint-only -Wall -Wno-fatal --top-module MeshNoCTop \
        "$target/MeshNoCTop.sv"
      ;;
    spinal)
      if [[ "$tool" == sbt ]]; then
        (cd "$repo/hw/spinal" && RTL_TARGET_DIR="$target" MESH_X="$mesh_x" \
          MESH_Y="$mesh_y" DATA_WIDTH=32 NODE_ID_WIDTH=2 VC_NUM=1 BUFFER_DEPTH=2 \
          sbt "runMain opennoc.noc.system.MeshNoCTop")
      else
        (cd "$repo/hw/spinal" && RTL_TARGET_DIR="$target" MESH_X="$mesh_x" \
          MESH_Y="$mesh_y" DATA_WIDTH=32 NODE_ID_WIDTH=2 VC_NUM=1 BUFFER_DEPTH=2 \
          mill --no-server spinal.runMain opennoc.noc.system.MeshNoCTop)
      fi
      verilator --lint-only -Wall -Wno-fatal --top-module MeshNoCTop \
        "$target/MeshNoCTop.sv"
      ;;
    *)
      echo "unsupported backend: $backend" >&2
      exit 2
      ;;
  esac
done

if [[ "$backend" == verilog ]]; then
  verilator --lint-only -Wall -Wno-fatal \
    "$repo/hw/verilog/vsrc/noc/arbiter/NoCArbiters.sv" \
    "$repo/hw/verilog/vsrc/noc/channel/NoCChannels.sv" \
    "$repo/hw/verilog/vsrc/noc/routing/XYRouting.sv" \
    "$repo/hw/verilog/vsrc/noc/ni/PacketEndpoint.sv" \
    "$repo/hw/verilog/vsrc/noc/pe/PacketPE.sv" \
    "$repo/hw/verilog/vsrc/noc/switch/PacketCrossbar.sv" \
    "$repo/hw/verilog/vsrc/noc/router/VirtualChannel.sv"
  verilator --lint-only -Wall -Wno-fatal \
    "$repo/hw/verilog/vsrc/bus/util/BusUtils.sv" \
    "$repo/hw/verilog/vsrc/bus/fabric/BusFabric.sv" \
    "$repo/hw/verilog/vsrc/bus/demo/BusDemos.sv" \
    "$repo/hw/verilog/vsrc/bus/adapter/BusAdapters.sv"
  verilator --lint-only -Wall -Wno-fatal \
    "$repo/hw/verilog/vsrc/bus/common/OpenNoCBuses.sv"
elif [[ "$backend" == spinal ]]; then
  if [[ "$tool" == sbt ]]; then
    (cd "$repo/hw/spinal" && RTL_TARGET_DIR="$repo/hw/build/rtl/lint-spinal-library" \
      sbt "runMain opennoc.LibraryElaboration")
  else
    (cd "$repo/hw/spinal" && RTL_TARGET_DIR="$repo/hw/build/rtl/lint-spinal-library" \
      mill --no-server spinal.runMain opennoc.LibraryElaboration)
  fi
fi
