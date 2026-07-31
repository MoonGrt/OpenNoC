package PacketTypesPkg;
  typedef enum logic[1:0] {
    HEAD=2'b00, BODY=2'b01, TAIL=2'b10, HEAD_TAIL=2'b11
  } flit_kind_t;
  typedef struct packed {
    logic[31:0] data;
    logic[7:0] src;
    logic[7:0] dest;
    logic last;
  } packet_beat_t;
endpackage
