ROOT := $(abspath ..)
BACKEND := verilog
rtl verilate test bus-rtl bus-test:
	@$(MAKE) -C $(ROOT) BACKEND=$(BACKEND) $@
clean:
	@$(RM) -r $(ROOT)/build/rtl/$(BACKEND) $(ROOT)/build/verilated/$(BACKEND)
clean-all: clean
.PHONY: rtl verilate test bus-rtl bus-test clean clean-all
