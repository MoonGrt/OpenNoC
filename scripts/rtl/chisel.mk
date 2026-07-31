RTL_DIR := $(OPENNOC_HOME)/hw/build/rtl/chisel
RTL_FINAL := $(RTL_DIR)/MeshNoCTop.sv
RTL_SOURCES := $(RTL_FINAL)
CHISEL_SRCS := $(shell find $(OPENNOC_HOME)/hw/chisel/src -name '*.scala')
MILL ?= mill
SBT ?= sbt

$(RTL_FINAL): $(CHISEL_SRCS) $(OPENNOC_HOME)/hw/chisel/build.mill
	@mkdir -p $(RTL_DIR)
	@if test "$(TOOL)" = sbt; then \
	  cd $(OPENNOC_HOME)/hw/chisel && RTL_TARGET_DIR=$(RTL_DIR) $(SBT) "runMain noc.system.MeshNoCTop"; \
	else \
	  cd $(OPENNOC_HOME)/hw/chisel && RTL_TARGET_DIR=$(RTL_DIR) $(MILL) --no-server chisel.runMain noc.system.MeshNoCTop; \
	fi
