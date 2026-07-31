RTL_DIR := $(OPENNOC_HOME)/hw/build/rtl/spinal
RTL_FINAL := $(RTL_DIR)/MeshNoCTop.sv
RTL_SOURCES := $(RTL_FINAL)
SPINAL_SRCS := $(shell find $(OPENNOC_HOME)/hw/spinal/src -name '*.scala')
MILL ?= mill
SBT ?= sbt

$(RTL_FINAL): $(SPINAL_SRCS) $(OPENNOC_HOME)/hw/spinal/build.mill
	@mkdir -p $(RTL_DIR)
	@if test "$(TOOL)" = sbt; then \
	  cd $(OPENNOC_HOME)/hw/spinal && RTL_TARGET_DIR=$(RTL_DIR) $(SBT) "runMain opennoc.noc.system.MeshNoCTop"; \
	else \
	  cd $(OPENNOC_HOME)/hw/spinal && RTL_TARGET_DIR=$(RTL_DIR) $(MILL) --no-server spinal.runMain opennoc.noc.system.MeshNoCTop; \
	fi
	@sed -i 's/\<clk\>/clock/g' $(RTL_FINAL)
