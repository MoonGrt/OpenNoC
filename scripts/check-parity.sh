#!/usr/bin/env bash
set -euo pipefail

repo=${OPENNOC_HOME:?OPENNOC_HOME is required}
fail=0

need_dir() {
  if [[ ! -d "$repo/$1" ]]; then echo "missing backend group: $1" >&2; fail=1; fi
}
need_symbol() {
  if ! rg -q "$2" "$repo/$1"; then echo "missing symbol '$2' in $1" >&2; fail=1; fi
}

for group in arbiter channel config data ni pe router routing switch topology system; do
  need_dir "hw/chisel/src/main/scala/noc/$group"
  need_dir "hw/spinal/src/main/scala/noc/$group"
  need_dir "hw/verilog/vsrc/noc/$group"
done

for group in common util fabric adapter demo; do
  need_dir "hw/spinal/src/main/scala/bus/$group"
  need_dir "hw/verilog/vsrc/bus/$group"
done

for root in hw/chisel/src/main/scala hw/spinal/src/main/scala hw/verilog/vsrc; do
  need_symbol "$root" 'MeshNoCTop'
  need_symbol "$root" 'MeshRouter|RouterBuilder'
  need_symbol "$root" 'RoundRobin'
  need_symbol "$root" 'BufferedChannel'
  need_symbol "$root" 'XYRouting|routingType = "XY"'
  need_symbol "$root" 'VirtualChannel|vcNum'
  need_symbol "$root" 'Packet.*NI|PacketIngress'
  need_symbol "$root" 'PacketSink|FlitSink'
  need_symbol "$root" 'Crossbar'
done

for root in hw/spinal/src/main/scala/bus hw/verilog/vsrc/bus; do
  need_symbol "$root" 'Axi|AXI'
  need_symbol "$root" 'Apb|APB'
  need_symbol "$root" 'Ahb|AHB'
  need_symbol "$root" 'TileLink'
  need_symbol "$root" 'Wishbone'
  need_symbol "$root" 'Avalon'
  need_symbol "$root" 'SimpleBus'
  need_symbol "$root" 'Fifo|FIFO'
  need_symbol "$root" 'AddressDecoder'
  need_symbol "$root" 'Arbiter'
  need_symbol "$root" 'Ram|RAM'
  need_symbol "$root" 'Rom|ROM'
  need_symbol "$root" 'Uart|UART'
  need_symbol "$root" 'WidthAdapter'
  need_symbol "$root" 'ToApb|to_apb'
  need_symbol "$root" 'Host'
done

if rg -n 'OpenNoCTop|OpenNoCTest|MeshNoCSimTop|mesh-test|mesh-rtl' \
  "$repo/Makefile" "$repo/hw" "$repo/scripts" \
  --glob '!build/**' --glob '!**/out/**' --glob '!check-parity.sh'; then
  echo "legacy entry point remains" >&2
  fail=1
fi

if rg -n 'hw/chisel|build/rtl/chisel' \
  "$repo/hw/spinal/src" "$repo/hw/verilog/vsrc"; then
  echo "native backend depends on Chisel sources or generated RTL" >&2
  fail=1
fi

exit "$fail"
