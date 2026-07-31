OPENNOC_HOME ?= $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
export OPENNOC_HOME

-include $(OPENNOC_HOME)/.config
BACKEND ?= auto
TOOL ?= $(if $(CONFIG_SBT),sbt,mill)

ifeq ($(BACKEND),auto)
  ifeq ($(CONFIG_CHISEL),y)
    HDL_BACKEND := chisel
  else ifeq ($(CONFIG_SPINAL),y)
    HDL_BACKEND := spinal
  else
    HDL_BACKEND := verilog
  endif
else
  HDL_BACKEND := $(BACKEND)
endif

ifneq ($(filter $(HDL_BACKEND),verilog chisel spinal),$(HDL_BACKEND))
$(error Unsupported BACKEND=$(BACKEND))
endif

DATA_WIDTH := $(or $(CONFIG_DATA_WIDTH),32)
MESH_X := $(or $(CONFIG_MESH_X),2)
MESH_Y := $(or $(CONFIG_MESH_Y),2)
NODE_ID_WIDTH := $(or $(CONFIG_NODE_ID_WIDTH),2)
VC_NUM := $(or $(CONFIG_VC_NUM),1)
BUFFER_DEPTH := $(or $(CONFIG_BUFFER_DEPTH),2)
PACKETS := $(or $(CONFIG_TEST_PACKETS),200)
SEED := $(or $(CONFIG_TEST_SEED),1)
WAVE_START := $(or $(CONFIG_WAVE_START),0)
WAVE_END := $(or $(CONFIG_WAVE_END),100000)
export MESH_X MESH_Y DATA_WIDTH NODE_ID_WIDTH VC_NUM BUFFER_DEPTH

include $(OPENNOC_HOME)/scripts/rtl/$(HDL_BACKEND).mk

BUILD_DIR := $(OPENNOC_HOME)/hw/build
VBUILD := $(BUILD_DIR)/verilated/$(HDL_BACKEND)
TEST_EXE := $(VBUILD)/MeshNoCTest
CONFIG_DEPS := $(wildcard $(OPENNOC_HOME)/.config)
VERILATOR ?= verilator
VERILATOR_FLAGS := -cc --exe --build -Wall -Wno-fatal --top-module MeshNoCTop --Mdir $(VBUILD)
ifeq ($(HDL_BACKEND),verilog)
VERILATOR_PARAMS := -GMESH_X=$(MESH_X) -GMESH_Y=$(MESH_Y) -GDATA_WIDTH=$(DATA_WIDTH) -GNODE_ID_WIDTH=$(NODE_ID_WIDTH) -GVC_NUM=$(VC_NUM) -GBUFFER_DEPTH=$(BUFFER_DEPTH)
endif
ifeq ($(CONFIG_WAVE),y)
VERILATOR_FLAGS += --trace
endif

$(TEST_EXE): $(RTL_SOURCES) $(OPENNOC_HOME)/hw/common/sim/MeshNoCTest.cpp $(CONFIG_DEPS)
	@mkdir -p $(VBUILD)
	$(VERILATOR) $(VERILATOR_FLAGS) -CFLAGS "-std=c++17" \
	  $(VERILATOR_PARAMS) \
	  $(RTL_SOURCES) $(OPENNOC_HOME)/hw/common/sim/MeshNoCTest.cpp \
	  -o MeshNoCTest

rtl: $(RTL_FINAL)
verilate: $(TEST_EXE)
run: $(TEST_EXE)
	$(TEST_EXE) --packets $(PACKETS) --seed $(SEED) \
	  $(if $(CONFIG_WAVE),--trace --wave $(VBUILD)/wave.vcd \
	    --wave-start $(WAVE_START) --wave-end $(WAVE_END))
wave: run
	@test -f $(VBUILD)/wave.vcd || { echo "Enable CONFIG_WAVE first." >&2; exit 2; }
	gtkwave $(VBUILD)/wave.vcd >/dev/null 2>&1 &

lint:
	bash $(OPENNOC_HOME)/scripts/check-params.sh $(HDL_BACKEND) $(TOOL)

BUS_RTL_DIR := $(BUILD_DIR)/rtl/bus-$(HDL_BACKEND)
BUS_VBUILD := $(BUILD_DIR)/verilated/bus-$(HDL_BACKEND)
BUS_TEST_EXE := $(BUS_VBUILD)/BusSmokeTest
ifeq ($(HDL_BACKEND),verilog)
BUS_RTL_FINAL := $(OPENNOC_HOME)/hw/verilog/vsrc/bus/demo/BusSmokeTop.sv
BUS_RTL_SOURCES := $(OPENNOC_HOME)/hw/verilog/vsrc/bus/demo/BusDemos.sv $(BUS_RTL_FINAL)
else ifeq ($(HDL_BACKEND),chisel)
BUS_RTL_FINAL := $(BUS_RTL_DIR)/BusSmokeTop.sv
BUS_RTL_SOURCES := $(BUS_RTL_FINAL)
$(BUS_RTL_FINAL): $(CHISEL_SRCS)
	@mkdir -p $(BUS_RTL_DIR)
	@cd $(OPENNOC_HOME)/hw/chisel && RTL_TARGET_DIR=$(BUS_RTL_DIR) \
	  $(if $(filter sbt,$(TOOL)),$(SBT) "runMain bus.demo.BusSmokeTop",$(MILL) --no-server chisel.runMain bus.demo.BusSmokeTop)
else
BUS_RTL_FINAL := $(BUS_RTL_DIR)/BusSmokeTop.sv
BUS_RTL_SOURCES := $(BUS_RTL_FINAL)
$(BUS_RTL_FINAL): $(SPINAL_SRCS)
	@mkdir -p $(BUS_RTL_DIR)
	@cd $(OPENNOC_HOME)/hw/spinal && RTL_TARGET_DIR=$(BUS_RTL_DIR) \
	  $(if $(filter sbt,$(TOOL)),$(SBT) "runMain opennoc.bus.demo.BusSmokeTop",$(MILL) --no-server spinal.runMain opennoc.bus.demo.BusSmokeTop)
	@sed -i 's/\<clk\>/clock/g' $(BUS_RTL_FINAL)
endif

$(BUS_TEST_EXE): $(BUS_RTL_SOURCES) $(OPENNOC_HOME)/hw/common/sim/BusSmokeTest.cpp
	@mkdir -p $(BUS_VBUILD)
	$(VERILATOR) -cc --exe --build -Wall -Wno-fatal --top-module BusSmokeTop \
	  --Mdir $(BUS_VBUILD) -CFLAGS "-std=c++17" \
	  $(BUS_RTL_SOURCES) $(OPENNOC_HOME)/hw/common/sim/BusSmokeTest.cpp \
	  -o BusSmokeTest

bus-rtl: $(BUS_RTL_FINAL)
bus-run: $(BUS_TEST_EXE)
	$(BUS_TEST_EXE)

clean:
	$(RM) -r $(BUILD_DIR)
	@for backend in verilog chisel spinal; do \
	  $(MAKE) -C $(OPENNOC_HOME)/hw/$$backend clean; \
	done

clean-all: clean
	@for backend in verilog chisel spinal; do \
	  $(MAKE) -C $(OPENNOC_HOME)/hw/$$backend clean-all; \
	done

.PHONY: rtl verilate run wave lint bus-rtl bus-run clean clean-all
