#include "VMeshNoCTop.h"
#include "verilated.h"
#if VM_TRACE
#include "verilated_vcd_c.h"
#endif

#include <array>
#include <cstdint>
#include <cstdlib>
#include <deque>
#include <iostream>
#include <random>
#include <string>
#include <unordered_map>

namespace {
constexpr int kNodes = 4;
constexpr int kDataWidth = 32;

struct Flit {
  uint32_t data;
  uint8_t dest;
  bool last;
};
struct Expected {
  uint8_t src;
  uint8_t dest;
  bool last;
};

uint64_t cycles = 0;
#if VM_TRACE
VerilatedVcdC* trace = nullptr;
uint64_t waveStart = 0;
uint64_t waveEnd = UINT64_MAX;
uint64_t lastDump = UINT64_MAX;
#endif

void eval(VMeshNoCTop& dut) {
  dut.eval();
#if VM_TRACE
  if (trace && cycles >= waveStart && cycles <= waveEnd && cycles != lastDump) {
    trace->dump(cycles);
    lastDump = cycles;
  }
#endif
}
void tick(VMeshNoCTop& dut) {
  dut.clock = 0; eval(dut); ++cycles;
  dut.clock = 1; eval(dut); ++cycles;
}
void putData(VMeshNoCTop& dut, int node, uint32_t value) {
  dut.in_data[node] = value;
}
uint32_t getData(const VMeshNoCTop& dut, int node) {
  return dut.out_data[node];
}
uint8_t field(uint32_t packed, int node) {
  return (packed >> (node * 2)) & 3u;
}
void setField(uint8_t& packed, int node, uint8_t value) {
  packed = static_cast<uint8_t>((packed & ~(3u << (node * 2))) |
                                ((value & 3u) << (node * 2)));
}
uint32_t makePayload(int src, int packet, int flit) {
  return 0xa0000000u | (static_cast<uint32_t>(src) << 24) |
         (static_cast<uint32_t>(packet) << 8) | flit;
}
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  int packets = 200;
  uint32_t seed = 1;
  std::string wave = "wave.vcd";
  bool enableTrace = false;
  for (int i = 1; i < argc; ++i) {
    const std::string arg = argv[i];
    if (arg == "--packets" && i + 1 < argc) packets = std::atoi(argv[++i]);
    else if (arg == "--seed" && i + 1 < argc) seed = std::strtoul(argv[++i], nullptr, 0);
    else if (arg == "--trace") enableTrace = true;
    else if (arg == "--wave" && i + 1 < argc) wave = argv[++i];
#if VM_TRACE
    else if (arg == "--wave-start" && i + 1 < argc) waveStart = std::strtoull(argv[++i], nullptr, 0);
    else if (arg == "--wave-end" && i + 1 < argc) waveEnd = std::strtoull(argv[++i], nullptr, 0);
#endif
  }
  if (packets < 1) return 2;

  VMeshNoCTop dut;
#if VM_TRACE
  if (enableTrace) {
    Verilated::traceEverOn(true);
    trace = new VerilatedVcdC;
    dut.trace(trace, 99);
    trace->open(wave.c_str());
  }
#else
  if (enableTrace) {
    std::cerr << "trace requested but this executable was built without --trace\n";
    return 2;
  }
#endif
  dut.reset = 1;
  dut.in_valid = 0;
  dut.in_last = 0;
  dut.in_dest = 0;
  dut.out_ready = 0;
  for (int i = 0; i < 4; ++i) tick(dut);
  dut.reset = 0;

  std::mt19937 rng(seed);
  std::array<std::deque<Flit>, kNodes> sources;
  std::array<bool, kNodes> sourceDriving{};
  std::array<bool, kNodes> sourcePacketActive{};
  std::array<uint8_t, kNodes> drivenDest{};
  std::unordered_map<uint32_t, Expected> expected;
  for (int p = 0; p < packets; ++p) {
    const int src = rng() % kNodes;
    const int dest = rng() % kNodes;
    const int length = 1 + rng() % 8;
    for (int f = 0; f < length; ++f) {
      const uint32_t payload = makePayload(src, p, f);
      const bool last = f == length - 1;
      sources[src].push_back({payload, static_cast<uint8_t>(dest), last});
      expected.emplace(payload, Expected{static_cast<uint8_t>(src),
                                         static_cast<uint8_t>(dest), last});
    }
  }

  std::array<int, kNodes> activeSource;
  std::array<bool, kNodes> stalled{};
  std::array<uint32_t, kNodes> stalledData{};
  std::array<uint8_t, kNodes> stalledSrc{}, stalledDest{};
  std::array<bool, kNodes> stalledLast{};
  std::array<int, kNodes> activePacket;
  std::array<int, kNodes> nextFlit{};
  activePacket.fill(-1);
  activeSource.fill(-1);
  uint64_t received = 0;
  const uint64_t expectedFlits = expected.size();
  const uint64_t timeout = expectedFlits * 100 + 10000;
  while (received < expectedFlits && cycles < timeout) {
    dut.in_valid = 0;
    dut.in_last = 0;
    dut.in_dest = 0;
    for (int s = 0; s < kNodes; ++s) {
      if (!sourceDriving[s] && !sources[s].empty() && (rng() % 100) < 75)
      {
        sourceDriving[s] = true;
        drivenDest[s] = sourcePacketActive[s] ? static_cast<uint8_t>(rng() % kNodes)
                                               : sources[s].front().dest;
      }
      if (sourceDriving[s]) {
        const auto& f = sources[s].front();
        dut.in_valid |= 1u << s;
        if (f.last) dut.in_last |= 1u << s;
        setField(dut.in_dest, s, drivenDest[s]);
        putData(dut, s, f.data);
      }
    }
    dut.out_ready = 0;
    for (int d = 0; d < kNodes; ++d)
      if ((rng() % 100) < 70) dut.out_ready |= 1u << d;

    dut.clock = 0; eval(dut);
    for (int d = 0; d < kNodes; ++d) {
      if (stalled[d] &&
          (getData(dut, d) != stalledData[d] ||
           field(dut.out_src, d) != stalledSrc[d] ||
           field(dut.out_dest, d) != stalledDest[d] ||
           static_cast<bool>((dut.out_last >> d) & 1u) != stalledLast[d] ||
           !static_cast<bool>((dut.out_valid >> d) & 1u))) {
        std::cerr << "output changed under backpressure at destination "
                  << d << "\n";
        return 1;
      }
      stalled[d] = ((dut.out_valid >> d) & 1u) &&
                   !((dut.out_ready >> d) & 1u);
      if (stalled[d]) {
        stalledData[d] = getData(dut, d);
        stalledSrc[d] = field(dut.out_src, d);
        stalledDest[d] = field(dut.out_dest, d);
        stalledLast[d] = (dut.out_last >> d) & 1u;
      }
    }
    const uint8_t acceptedIn = dut.in_valid & dut.in_ready;
    const uint8_t acceptedOut = dut.out_valid & dut.out_ready;
    for (int d = 0; d < kNodes; ++d) {
      if (!(acceptedOut & (1u << d))) continue;
      const uint32_t payload = getData(dut, d);
      const uint8_t src = field(dut.out_src, d);
      const uint8_t outDest = field(dut.out_dest, d);
      const bool last = (dut.out_last >> d) & 1u;
      const auto it = expected.find(payload);
      if (it == expected.end() || it->second.src != src ||
          it->second.dest != outDest || outDest != d ||
          it->second.last != last) {
        std::cerr << "metadata, duplicate, or unexpected flit error at cycle "
                  << cycles << "\n";
        return 1;
      }
      if (activeSource[d] < 0) activeSource[d] = src;
      if (activeSource[d] != src) {
        std::cerr << "packet interleaving at destination " << d << "\n";
        return 1;
      }
      const int packetId = (payload >> 8) & 0xffff;
      const int flitId = payload & 0xff;
      if (activePacket[d] < 0) {
        activePacket[d] = packetId;
        nextFlit[d] = 0;
      }
      if (activePacket[d] != packetId || nextFlit[d] != flitId) {
        std::cerr << "flit order error at destination " << d << "\n";
        return 1;
      }
      ++nextFlit[d];
      if (last) {
        activeSource[d] = -1;
        activePacket[d] = -1;
      }
      expected.erase(it);
      ++received;
    }
    dut.clock = 1; eval(dut); ++cycles;
    for (int s = 0; s < kNodes; ++s)
      if (acceptedIn & (1u << s)) {
        sourcePacketActive[s] = !sources[s].front().last;
        sources[s].pop_front();
        sourceDriving[s] = false;
      }
    ++cycles;
  }

#if VM_TRACE
  if (trace) { trace->close(); delete trace; }
#endif
  dut.final();
  if (!expected.empty()) {
    std::cerr << "timeout: " << expected.size() << " flits missing\n";
    return 1;
  }
  std::cout << "PASS backend MeshNoCTop: " << packets << " packets, "
            << received << " flits, seed " << seed << "\n";
  return 0;
}
