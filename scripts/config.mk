COLOR_RED := $(shell printf '\033[1;31m')
COLOR_END := $(shell printf '\033[0m')

ifeq ($(wildcard $(OPENNOC_HOME)/.config),)
$(warning $(COLOR_RED)No .config found; run 'make opennoc_verilog_defconfig' or 'make menuconfig'.$(COLOR_END))
endif

# OpenNoC owns its host-side Kconfig utilities and requires no neighbouring
# repository.
KCONFIG_PATH := $(OPENNOC_HOME)/tools/kconfig
Kconfig := $(OPENNOC_HOME)/Kconfig
CONF := $(KCONFIG_PATH)/build/conf
MCONF := $(KCONFIG_PATH)/build/mconf
silent := -s

$(CONF):
	@$(MAKE) $(silent) -C $(KCONFIG_PATH) NAME=conf

$(MCONF):
	@$(MAKE) $(silent) -C $(KCONFIG_PATH) NAME=mconf

menuconfig: $(MCONF) $(CONF)
	@cd $(OPENNOC_HOME) && $(MCONF) $(Kconfig)
	@cd $(OPENNOC_HOME) && $(CONF) $(silent) --syncconfig $(Kconfig)

savedefconfig: $(CONF)
	@cd $(OPENNOC_HOME) && $< $(silent) --savedefconfig=configs/defconfig $(Kconfig)

%defconfig: $(CONF)
	@test -f $(OPENNOC_HOME)/configs/$@ || { echo "Unknown configuration: $@" >&2; exit 2; }
	@cd $(OPENNOC_HOME) && $< $(silent) --defconfig=configs/$@ $(Kconfig)
	@cd $(OPENNOC_HOME) && $< $(silent) --syncconfig $(Kconfig)
	@echo "Loaded configs/$@"

help:
	@echo "OpenNoC targets:"
	@echo "  menuconfig                         configure backend and NoC parameters"
	@echo "  opennoc_{verilog,chisel,spinal}_defconfig"
	@echo "  rtl | verilate | test | test-all | wave | bus-rtl | bus-test"
	@echo "  lint | lint-all | parity-check | clean | clean-all"
	@echo "  clean | clean-all"

.PHONY: menuconfig savedefconfig help
