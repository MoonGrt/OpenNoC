ROOT := $(abspath ..)
BACKEND := chisel
rtl verilate test wave bus-rtl bus-test:
	@$(MAKE) -C $(ROOT) BACKEND=$(BACKEND) $@
clean:
	@$(RM) -r $(ROOT)/build/rtl/$(BACKEND) $(ROOT)/build/verilated/$(BACKEND)
clean-all: clean
	@$(RM) -r out target build .bloop .metals .scala-build
.PHONY: rtl verilate test wave bus-rtl bus-test clean clean-all
