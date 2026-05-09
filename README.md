**English | [简体中文](README_cn.md)**
<div id="top"></div>

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![License][license-shield]][license-url]


<!-- PROJECT LOGO -->
<br />
<div align="center">
    <a href="https://github.com/MoonGrt/OpenNoC">
    <img src="docs/images/logo.png" alt="Logo" width="80" height="80">
    </a>
<h3 align="center">OpenNoC</h3>
    <p align="center">
    Chisel bus building blocks: AMBA (AXI / AXI-Lite / APB / AHB stubs), bridges, and command-style master hosts.
    <br />
    <a href="https://github.com/MoonGrt/OpenNoC"><strong>Explore the docs »</strong></a>
    <br />
    <a href="https://github.com/MoonGrt/OpenNoC">View Demo</a>
    ·
    <a href="https://github.com/MoonGrt/OpenNoC/issues">Report Bug</a>
    ·
    <a href="https://github.com/MoonGrt/OpenNoC/issues">Request Feature</a>
    </p>
</div>




<!-- CONTENTS -->
<details open>
  <summary>Contents</summary>
  <ol>
    <li><a href="#about-the-project">About</a></li>
    <li><a href="#build--test">Build &amp; test</a></li>
    <li><a href="#layout">Source layout</a></li>
    <li><a href="#noc-components">NoC components</a></li>
    <li><a href="#command-style-masters">Command-style masters</a></li>
    <li><a href="#simulation">Simulation</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>





## About The Project

OpenNoC is a small **Chisel 7** library of on-chip interconnect pieces. The focus is **AMBA-style** ports and glue logic you can instantiate next to your own modules.

- **AXI4 / AXI4-Lite**: bundles, simple register slaves, crossbar (1:1), ID remap, stream FIFO, bridges (e.g. AXI-Lite → APB, AXI-Lite → AXI4 single-beat master).
- **APB**: directed master/slave IO, decoder, register slave, example subsystem.
- **AHB**: register slave and stubs for larger fabric (work in progress).
- **NoC** (`src/main/scala/noc`): reusable NoC components including topology, routing, router/switch, channel and NI blocks.
- **Other** (`fabric`, `wishbone`, `tilelink`, `avalon`, …): minimal stubs so the repo compiles as one library; expand as needed.

<p align="right">(<a href="#top">top</a>)</p>

## Build & test

Requires **JDK 17+** and [sbt](https://www.scala-sbt.org/).

```bash
sbt compile
sbt test
```

Tests use **elaboration only** (`ChiselStage.emitCHIRRTL`) so they do not require Verilator.

<p align="right">(<a href="#top">top</a>)</p>

## Layout

| Path | Role |
|------|------|
| `src/main/scala/bus/amba/axi/` | AXI4, AXI-Lite, AXI-Stream, bridges, `host/` command masters |
| `src/main/scala/bus/amba/apb/` | APB IO, slaves, decoder, `ApbMasterHost` |
| `src/main/scala/bus/amba/ahb/` | AHB IO and register slave |
| `src/main/scala/noc/` | NoC blocks: flit/packet, routing, topology, router/switch, NI and system examples |
| `src/main/scala/bus/util/` | Small helpers (queues, counters, …) |
| `src/test/scala/bus/amba/` | Elaboration tests |

<p align="right">(<a href="#top">top</a>)</p>

## NoC components

The NoC code in `src/main/scala/noc` is organized as composable building blocks:

- **Config & data model** (`noc/config`, `noc/data`): `NoCConfig`, flit header layout, packet/flit helpers.
- **Topology** (`noc/topology`): Ring, Mesh, Torus, Cube and custom topology wiring.
- **Routing & arbitration** (`noc/routing`, `noc/arbiter`, `noc/router`): deterministic/adaptive routing, VC allocator, switch allocator, virtual channel handling.
- **Transport & integration** (`noc/channel`, `noc/ni`, `noc/system`): wire/buffer/pipeline channels, stream/AXI/TL NIs, and complete systems such as `MeshNoC`/`RingNoC`.
- **Examples** (`noc/demo`, `noc/pe`): generation/demo modules for quick bring-up and integration testing.

<p align="right">(<a href="#top">top</a>)</p>

## Command-style masters

For a **Decoupled** “enqueue one transaction” interface, use:

- **`amba.axi.host.AxiLiteMasterHost`** — drives `AxiLiteMasterPort` (one outstanding read or write).
- **`amba.axi.host.Axi4SingleBeatMasterHost`** — single-beat full AXI4 with fixed ID `0`, `LEN=0`, INCR burst, `WLAST`/`RLAST` asserted.
- **`amba.apb.ApbMasterHost`** — two-phase APB master (`SETUP` then `ACCESS`).

Helpers **`AxiLiteHost.tieOffMasterIdle`** / **`tieOffSlaveIdle`** and **`Axi4Host.tieOffMasterIdle`** tie unused ports to a safe idle pattern.

Example pattern (inside your `Module`):

```scala
import amba.axi.axilite._
import amba.axi.host._

val p = AxiLiteParams(addrBits = 16, dataBits = 32)
val host = Module(new AxiLiteMasterHost(p))
// host.io.cmd <> yourCommandSource
// host.io.rsp <> yourResultSink
// host.io.axi <> axiLiteSlaveOrBridge
```

<p align="right">(<a href="#top">top</a>)</p>

## Simulation

Cycle-accurate tests with **ChiselSim** typically need **Verilator** on `PATH`. This repository’s default tests avoid that. If you add `simulate { ... }` tests locally, install Verilator and follow the [Chisel documentation](https://www.chisel-lang.org/) for your Chisel version.

<p align="right">(<a href="#top">top</a>)</p>



<!-- ROADMAP -->
## Roadmap

- [ ] Basic
  - Components:
    - Flit
    - Arbiter: RoundRobin; FixedPriority;
    - Topology: Ring; Mesh; Torus; Cube;
    - Routing: Deterministic(Ring; XY/YX) Adaptive;
    - Channel: Wire; Buffer; Pipeline;
    - NI: Stream
  - Module: Router; Switch; VC
  - TODO: AXINI; TLNI
- [ ] Advanced
  - TODO: Flit: QoS; Priority; Credit; CRC;
  - TODO: Arbiter: WeightedRoundRobin;
  - TODO: Topology: Tree; Hierarchical;
  - TODO: Routing: ECMP;

See the [open issues](https://github.com/MoonGrt/OpenNoC/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
<p align="right">(<a href="#top">top</a>)</p>



<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.
<p align="right">(<a href="#top">top</a>)</p>



<!-- CONTACT -->
## Contact

MoonGrt - 1561145394@qq.com
Project Link: [MoonGrt/OpenNoC](https://github.com/MoonGrt/OpenNoC)
<p align="right">(<a href="#top">top</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* [Chisel](https://www.chisel-lang.org/)
* [CIRCT / firtool](https://github.com/llvm/circt) (via Chisel 7)

<p align="right">(<a href="#top">top</a>)</p>


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/MoonGrt/OpenNoC.svg?style=for-the-badge
[contributors-url]: https://github.com/MoonGrt/OpenNoC/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/MoonGrt/OpenNoC.svg?style=for-the-badge
[forks-url]: https://github.com/MoonGrt/OpenNoC/network/members
[stars-shield]: https://img.shields.io/github/stars/MoonGrt/OpenNoC.svg?style=for-the-badge
[stars-url]: https://github.com/MoonGrt/OpenNoC/stargazers
[issues-shield]: https://img.shields.io/github/issues/MoonGrt/OpenNoC.svg?style=for-the-badge
[issues-url]: https://github.com/MoonGrt/OpenNoC/issues
[license-shield]: https://img.shields.io/github/license/MoonGrt/OpenNoC.svg?style=for-the-badge
[license-url]: https://github.com/MoonGrt/OpenNoC/blob/master/LICENSE

