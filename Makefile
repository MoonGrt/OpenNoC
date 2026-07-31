OPENNOC_HOME := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
export OPENNOC_HOME

-include $(OPENNOC_HOME)/.config
include $(OPENNOC_HOME)/scripts/config.mk

BACKEND ?= auto
TOOL ?= $(if $(CONFIG_SBT),sbt,mill)
SUPPORTED_BACKENDS := auto verilog chisel spinal

ifneq ($(filter $(BACKEND),$(SUPPORTED_BACKENDS)),$(BACKEND))
$(error Unsupported BACKEND=$(BACKEND); use auto, verilog, chisel, or spinal)
endif
ifneq ($(filter $(TOOL),mill sbt),$(TOOL))
$(error Unsupported TOOL=$(TOOL); use mill or sbt)
endif

.DEFAULT_GOAL := run

rtl verilate run wave:
	$(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$(BACKEND) TOOL=$(TOOL) $@

bus-rtl bus-run:
	$(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$(BACKEND) TOOL=$(TOOL) $@

lint:
	$(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$(BACKEND) TOOL=$(TOOL) lint

lint-all:
	@set -e; for backend in verilog chisel spinal; do \
	  $(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$$backend TOOL=$(TOOL) lint; \
	done

parity-check:
	bash $(OPENNOC_HOME)/scripts/check-parity.sh

run-all: parity-check
	@set -e; for backend in verilog chisel spinal; do \
	  for seed in 1 7 12345; do \
	    $(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$$backend TOOL=$(TOOL) SEED=$$seed run; \
	  done; \
	  $(MAKE) -C $(OPENNOC_HOME)/hw BACKEND=$$backend TOOL=$(TOOL) bus-run; \
	done

clean:
	$(MAKE) -C $(OPENNOC_HOME)/hw clean

clean-all: clean
	$(MAKE) -C $(OPENNOC_HOME)/hw clean-all
	$(MAKE) -C $(OPENNOC_HOME)/tools/kconfig clean
	find $(OPENNOC_HOME) -type d -name __pycache__ -prune -exec $(RM) -r {} +
	$(RM) -r $(OPENNOC_HOME)/tools/kconfig/include
	$(RM) $(OPENNOC_HOME)/.config.old
	@echo "All OpenNoC build and HDL-generator artifacts have been removed."

.PHONY: rtl verilate run run-all wave bus-rtl bus-run lint lint-all parity-check clean clean-all
