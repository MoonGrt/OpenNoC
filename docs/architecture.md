# OpenNoC architecture

OpenNoC owns its NoC flit format, router, endpoint adapters and bus signal
definitions. Chisel and SpinalHDL are implementation languages; neither
SpinalLib nor Rocket Chip is a hardware dependency.

The first compatibility profile is intentionally bounded: one outstanding
transaction per endpoint, AXI4 INCR bursts up to sixteen beats, TileLink-UL
`Get` and `PutFullData`, and one AHB-Lite master. The portable `NoCLink`
contract is valid/ready plus data, source and destination IDs.
# MeshNoCTop backend contract

`MeshNoCTop` is the simulation and RTL entry point for Chisel, SpinalHDL, and
native SystemVerilog. The implementations are independent; generated Chisel
RTL is never used by the other two backends.

For `NODES = MESH_X * MESH_Y`, endpoint `i` occupies the slice
`[(i + 1) * WIDTH - 1 : i * WIDTH]` of each packed signal.

| Direction | Signal | Meaning |
| --- | --- | --- |
| input | `in_valid`, `in_ready` | Source ready/valid handshake |
| input | `in_data`, `in_dest`, `in_last` | Payload, first-flit destination, packet tail |
| output | `out_valid`, `out_ready` | Sink ready/valid handshake |
| output | `out_data`, `out_src`, `out_dest`, `out_last` | Payload and packet metadata |

The destination is sampled on the first accepted flit. A destination remains
owned by that source until an accepted `last`, so flits from different packets
cannot interleave. An out-of-range destination makes `in_ready` low. All output
payload and metadata remain stable while `out_valid && !out_ready`.

The default regression is a 2x2 network with all four endpoints acting as
random sources and sinks. `make test-all` runs 200 packets with seeds 1, 7, and
12345 on all three backends. `make lint-all` elaborates and lints 1x1, 2x1, and
2x2 configurations.

Each backend implements a native hop-by-hop network. Every node owns a
five-port router (`Local`, `East`, `West`, `North`, `South`) with input FIFOs,
deterministic X-then-Y routing, round-robin output arbitration, and an output
owner held through the accepted tail flit. Boundary ports are tied off and
adjacent directional ports are connected with ready/valid links.
