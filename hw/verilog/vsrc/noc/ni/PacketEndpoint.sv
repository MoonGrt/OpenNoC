module PacketIngress #(parameter int DATA_WIDTH=32,NODE_ID_WIDTH=2,NODES=4) (
  input logic clock,reset,
  input logic s_valid,output logic s_ready,input logic[DATA_WIDTH-1:0]s_data,
  input logic[NODE_ID_WIDTH-1:0]s_dest,input logic s_last,
  output logic m_valid,input logic m_ready,output logic[DATA_WIDTH-1:0]m_data,
  output logic[NODE_ID_WIDTH-1:0]m_dest,output logic m_last
);
  logic active;logic[NODE_ID_WIDTH-1:0]dest_q;
  wire[NODE_ID_WIDTH-1:0]effective=active?dest_q:s_dest;
  assign m_valid=s_valid&&(effective<NODES);assign s_ready=m_ready&&(effective<NODES);
  assign m_data=s_data;assign m_dest=effective;assign m_last=s_last;
  always_ff @(posedge clock)begin
    if(reset)begin active<=0;dest_q<='0;end else if(s_valid&&s_ready)begin
      if(!active&&!s_last)begin active<=1;dest_q<=s_dest;end
      if(s_last)active<=0;
    end
  end
endmodule

module PacketEgress #(parameter int DATA_WIDTH=32,NODE_ID_WIDTH=2) (
  input logic s_valid,output logic s_ready,input logic[DATA_WIDTH-1:0]s_data,
  input logic[NODE_ID_WIDTH-1:0]s_src,s_dest,input logic s_last,
  output logic m_valid,input logic m_ready,output logic[DATA_WIDTH-1:0]m_data,
  output logic[NODE_ID_WIDTH-1:0]m_src,m_dest,output logic m_last
);
  assign s_ready=m_ready;assign m_valid=s_valid;assign m_data=s_data;
  assign m_src=s_src;assign m_dest=s_dest;assign m_last=s_last;
endmodule
