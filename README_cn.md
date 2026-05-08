**简体中文 | [English](README.md)**
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
    基于 Chisel 的片上总线积木库：AMBA（AXI / AXI-Lite / APB / AHB 桩）、桥接与命令式主机封装。
    <br />
    <a href="https://github.com/MoonGrt/OpenNoC"><strong>浏览文档 »</strong></a>
    <br />
    <a href="https://github.com/MoonGrt/OpenNoC">查看 Demo</a>
    ·
    <a href="https://github.com/MoonGrt/OpenNoC/issues">反馈 Bug</a>
    ·
    <a href="https://github.com/MoonGrt/OpenNoC/issues">请求新功能</a>
    </p>
</div>




<!-- CONTENTS -->
<details open>
  <summary>目录</summary>
  <ol>
    <li><a href="#文件树">文件树</a></li>
    <li><a href="#关于本项目">关于本项目</a></li>
    <li><a href="#编译与测试">编译与测试</a></li>
    <li><a href="#目录结构">目录结构</a></li>
    <li><a href="#命令式主机">命令式主机</a></li>
    <li><a href="#仿真">仿真</a></li>
    <li><a href="#路线图">路线图</a></li>
    <li><a href="#贡献">贡献</a></li>
    <li><a href="#许可证">许可证</a></li>
    <li><a href="#联系我们">联系我们</a></li>
  </ol>
</details>


<!-- 文件树 -->
## 文件树

```
└─ Project
  ├─ .gitignore
  ├─ LICENSE
  ├─ README.md
  └─ /Document/

```


<!-- 关于本项目 -->
## 关于本项目

OpenNoC 是一个 **Chisel 7** 总线相关 RTL 积木库，便于在自定义模块旁例化标准端口与桥接逻辑。

- **AXI4 / AXI4-Lite**：通道 Bundle、简单寄存器从机、1:1 交叉开关、ID 重映射、流 FIFO、桥（如 AXI-Lite → APB、AXI-Lite → AXI4 单拍主机侧）。
- **APB**：显式 Master/Slave IO、地址译码、寄存器从机、示例子系统。
- **AHB**：寄存器从机与更大互连的桩（持续完善中）。
- **其它**（`fabric`、`wishbone`、`tilelink`、`avalon` 等）：最小桩保证整库可编译，可按需扩展。

<p align="right">(<a href="#top">top</a>)</p>

## 编译与测试

需要 **JDK 17+** 与 [sbt](https://www.scala-sbt.org/)。

```bash
sbt compile
sbt test
```

当前测试仅做 **elaboration**（`ChiselStage.emitCHIRRTL`），**不依赖 Verilator**。

<p align="right">(<a href="#top">top</a>)</p>

## 目录结构

| 路径 | 说明 |
|------|------|
| `src/main/scala/amba/axi/` | AXI4、AXI-Lite、AXI-Stream、桥、`host/` 命令式主机 |
| `src/main/scala/amba/apb/` | APB IO、从机、译码、`ApbMasterHost` |
| `src/main/scala/amba/ahb/` | AHB IO 与寄存器从机 |
| `src/main/scala/util/` | 队列、计数器等小工具 |
| `src/test/scala/amba/` | 综合前 elaboration 测试 |

<p align="right">(<a href="#top">top</a>)</p>

## 命令式主机

若希望用 **Decoupled** 接口「入队一条读写、在 `rsp` 取回结果」，可使用：

- **`amba.axi.host.AxiLiteMasterHost`** — 驱动 `AxiLiteMasterPort`，同时仅一条未完成事务。
- **`amba.axi.host.Axi4SingleBeatMasterHost`** — 全 AXI4 **单拍**传输，固定 ID `0`、`LEN=0`、INCR burst，`WLAST`/`RLAST` 置位。
- **`amba.apb.ApbMasterHost`** — APB 主机（SETUP → ACCESS 两拍）。

空闲口可用 **`AxiLiteHost.tieOffMasterIdle` / `tieOffSlaveIdle`**、**`Axi4Host.tieOffMasterIdle`** 接到安全默认值。

在你的 `Module` 中的典型接法：

```scala
import amba.axi.axilite._
import amba.axi.host._

val p = AxiLiteParams(addrBits = 16, dataBits = 32)
val host = Module(new AxiLiteMasterHost(p))
// host.io.cmd <> 命令队列
// host.io.rsp <> 响应队列
// host.io.axi <> 对端从机或桥
```

<p align="right">(<a href="#top">top</a>)</p>

## 仿真

使用 **ChiselSim** 做周期仿真时，通常需要系统安装 **Verilator** 并加入 `PATH`。本仓库默认测试不依赖仿真器。若本地增加 `simulate { ... }` 测试，请参考对应版本 [Chisel 文档](https://www.chisel-lang.org/)。

<p align="right">(<a href="#top">top</a>)</p>



<!-- 路线图 -->
## 路线图

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

到 [open issues](https://github.com/MoonGrt/OpenNoC/issues) 页查看所有请求的功能 （以及已知的问题）。

<p align="right">(<a href="#top">顶部</a>)</p>



<!-- 贡献 -->
## 贡献

贡献让开源社区成为了一个非常适合学习、互相激励和创新的地方。你所做出的任何贡献都是**受人尊敬**的。

如果你有好的建议，请复刻（fork）本仓库并且创建一个拉取请求（pull request）。你也可以简单地创建一个议题（issue），并且添加标签「enhancement」。不要忘记给项目点一个 star！再次感谢！

1. 复刻（Fork）本项目
2. 创建你的 Feature 分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的变更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到该分支 (`git push origin feature/AmazingFeature`)
5. 创建一个拉取请求（Pull Request）
<p align="right">(<a href="#top">top</a>)</p>



<!-- 许可证 -->
## 许可证

根据 MIT 许可证分发。打开 [LICENSE](LICENSE) 查看更多内容。
<p align="right">(<a href="#top">top</a>)</p>



<!-- 联系我们 -->
## 联系我们

MoonGrt - 1561145394@qq.com
Project Link: [MoonGrt/OpenNoC](https://github.com/MoonGrt/OpenNoC)
<p align="right">(<a href="#top">top</a>)</p>



<!-- 致谢 -->
## 致谢

* [Chisel](https://www.chisel-lang.org/)
* [CIRCT / firtool](https://github.com/llvm/circt)（经 Chisel 7 调用）

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

