module RandomPacketSource #(parameter int DATA_WIDTH=32,NODE_ID_WIDTH=2,NODES=4) (
  input logic clock,reset,enable,
  output logic valid,input logic ready,output logic[DATA_WIDTH-1:0]data,
  output logic[NODE_ID_WIDTH-1:0]dest,output logic last
);
  logic[31:0]lfsr;
  assign valid=enable;assign data={{(DATA_WIDTH-32){1'b0}},lfsr};
  assign dest=lfsr[NODE_ID_WIDTH-1:0]%NODES;assign last=lfsr[4:3]==0;
  always_ff @(posedge clock)
    if(reset)lfsr<=32'h1;else if(valid&&ready)
      lfsr<={lfsr[30:0],lfsr[31]^lfsr[21]^lfsr[1]^lfsr[0]};
endmodule

module PacketSink #(parameter int DATA_WIDTH=32,NODE_ID_WIDTH=2) (
  input logic clock,reset,valid,output logic ready,
  input logic[DATA_WIDTH-1:0]data,input logic[NODE_ID_WIDTH-1:0]src,dest,
  input logic last,stall,
  output logic[31:0]flit_count,packet_count
);
  assign ready=!stall;
  always_ff @(posedge clock)begin
    if(reset)begin flit_count<=0;packet_count<=0;end
    else if(valid&&ready)begin flit_count<=flit_count+1'b1;if(last)packet_count<=packet_count+1'b1;end
  end
endmodule
