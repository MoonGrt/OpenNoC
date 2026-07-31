#include "VBusSmokeTop.h"
#include "verilated.h"

#include <array>
#include <cstdint>
#include <iostream>
#include <random>

namespace {
uint64_t cycles = 0;
void eval(VBusSmokeTop& dut) { dut.eval(); }
void tick(VBusSmokeTop& dut) {
  dut.clock = 0; eval(dut); ++cycles;
  dut.clock = 1; eval(dut); ++cycles;
}

uint32_t transact(VBusSmokeTop& dut, bool write, uint8_t address,
                  uint32_t data = 0, uint8_t strobe = 0xf) {
  unsigned waits = 0;
  dut.req_valid = 1;
  dut.req_write = write;
  dut.req_addr = address;
  dut.req_wdata = data;
  dut.req_strb = strobe;
  while (true) {
    dut.clock = 0; eval(dut);
    const bool accepted = dut.req_valid && dut.req_ready;
    dut.clock = 1; eval(dut); cycles += 2;
    if (accepted) break;
    if (++waits > 1000) {
      std::cerr << "request timeout at address 0x" << std::hex
                << static_cast<unsigned>(address) << "\n";
      throw "request timeout";
    }
  }
  dut.req_valid = 0;
  dut.rsp_ready = 1;
  waits = 0;
  while (true) {
    dut.clock = 0; eval(dut);
    if (dut.rsp_valid) {
      if (dut.rsp_error) throw "response error";
      const uint32_t result = dut.rsp_rdata;
      dut.clock = 1; eval(dut); cycles += 2;
      dut.rsp_ready = 0;
      return result;
    }
    dut.clock = 1; eval(dut); cycles += 2;
    if (++waits > 1000) {
      std::cerr << "response timeout at address 0x" << std::hex
                << static_cast<unsigned>(address) << "\n";
      throw "response timeout";
    }
  }
}
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  VBusSmokeTop dut;
  dut.reset = 1; dut.req_valid = 0; dut.rsp_ready = 0; dut.tx_ready = 0;
  for (int i = 0; i < 4; ++i) tick(dut);
  dut.reset = 0;

  std::mt19937 rng(12345);
  std::array<uint32_t, 64> expected{};
  try {
    for (int i = 0; i < 64; ++i) {
      expected[i] = rng();
      transact(dut, true, static_cast<uint8_t>(i), expected[i]);
    }
    for (int i = 0; i < 64; ++i) {
      const uint32_t got = transact(dut, false, static_cast<uint8_t>(i));
      if (got != expected[i]) {
        std::cerr << "RAM mismatch address " << i << ": got 0x" << std::hex
                  << got << " expected 0x" << expected[i] << "\n";
        return 1;
      }
    }
    transact(dut, true, 7, 0x0000aa00u, 0x2);
    expected[7] = (expected[7] & 0xffff00ffu) | 0x0000aa00u;
    if (transact(dut, false, 7) != expected[7]) {
      std::cerr << "RAM byte strobe mismatch\n";
      return 1;
    }

    transact(dut, true, 0x80, 'N');
    dut.clock = 0; eval(dut);
    if (!dut.tx_valid || dut.tx_data != 'N') {
      std::cerr << "UART transmit mismatch\n";
      return 1;
    }
    dut.tx_ready = 1; tick(dut); dut.tx_ready = 0;
    if ((transact(dut, false, 0x84) & 1u) == 0) {
      std::cerr << "UART status did not become ready\n";
      return 1;
    }
  } catch (const char* error) {
    std::cerr << error << "\n";
    return 1;
  }
  dut.final();
  std::cout << "PASS BusSmokeTop: RAM, byte strobes, UART\n";
  return 0;
}
