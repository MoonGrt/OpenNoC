package OpenNoCConfigPkg;
  typedef enum logic [1:0] {
    FLIT_HEAD = 2'b00,
    FLIT_BODY = 2'b01,
    FLIT_TAIL = 2'b10,
    FLIT_HEAD_TAIL = 2'b11
  } flit_type_t;
endpackage
