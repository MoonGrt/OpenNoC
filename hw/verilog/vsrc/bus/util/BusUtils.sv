module BusCounter #(parameter int WIDTH=32) (
  input logic clock,reset,enable,clear,
  output logic [WIDTH-1:0] value
);
  always_ff @(posedge clock)
    if(reset||clear)value<='0;else if(enable)value<=value+1'b1;
endmodule

module BusPipeline #(parameter int WIDTH=32) (
  input logic clock,reset,
  input logic s_valid,output logic s_ready,input logic[WIDTH-1:0]s_bits,
  output logic m_valid,input logic m_ready,output logic[WIDTH-1:0]m_bits
);
  logic full;logic[WIDTH-1:0]data;
  assign s_ready=!full||m_ready;assign m_valid=full;assign m_bits=data;
  always_ff @(posedge clock)
    if(reset)full<=0;else if(s_ready)begin full<=s_valid;if(s_valid)data<=s_bits;end
endmodule

module BusFifo #(parameter int WIDTH=32,DEPTH=4,
  localparam int PW=(DEPTH<=1)?1:$clog2(DEPTH),CW=$clog2(DEPTH+1)) (
  input logic clock,reset,
  input logic s_valid,output logic s_ready,input logic[WIDTH-1:0]s_bits,
  output logic m_valid,input logic m_ready,output logic[WIDTH-1:0]m_bits,
  output logic[CW-1:0]occupancy
);
  logic[WIDTH-1:0]mem[DEPTH];logic[PW-1:0]rd,wr;
  wire push=s_valid&&s_ready,pop=m_valid&&m_ready;
  assign s_ready=occupancy<DEPTH;assign m_valid=occupancy!=0;assign m_bits=mem[rd];
  always_ff @(posedge clock)begin
    if(reset)begin rd<='0;wr<='0;occupancy<='0;end else begin
      if(push)begin mem[wr]<=s_bits;wr<=wr==DEPTH-1?'0:wr+1'b1;end
      if(pop)rd<=rd==DEPTH-1?'0:rd+1'b1;
      case({push,pop})2'b10:occupancy<=occupancy+1'b1;2'b01:occupancy<=occupancy-1'b1;
        default:occupancy<=occupancy;endcase
    end
  end
endmodule

module BusSkidBuffer #(parameter int WIDTH=32) (
  input logic clock,reset,
  input logic s_valid,output logic s_ready,input logic[WIDTH-1:0]s_bits,
  output logic m_valid,input logic m_ready,output logic[WIDTH-1:0]m_bits
);
  logic held;logic[WIDTH-1:0]data;
  assign s_ready=!held;assign m_valid=held?s_valid|held:s_valid;
  assign m_bits=held?data:s_bits;
  always_ff @(posedge clock)begin
    if(reset)held<=0;
    else begin
      if(!held&&s_valid&&!m_ready)begin held<=1;data<=s_bits;end
      else if(held&&m_ready)held<=0;
    end
  end
endmodule
