module BusRam #(parameter int ADDR_WIDTH=10,DATA_WIDTH=32,
  localparam int DEPTH=1<<ADDR_WIDTH) (
  input logic clock,reset,
  input logic req_valid,output logic req_ready,input logic req_write,
  input logic[ADDR_WIDTH-1:0]req_addr,input logic[DATA_WIDTH-1:0]req_wdata,
  input logic[DATA_WIDTH/8-1:0]req_strb,
  output logic rsp_valid,input logic rsp_ready,output logic[DATA_WIDTH-1:0]rsp_rdata,
  output logic rsp_error
);
  logic[DATA_WIDTH-1:0]mem[DEPTH];logic[DATA_WIDTH-1:0]read_q;logic pending;
  integer b;
  assign req_ready=!pending||rsp_ready;assign rsp_valid=pending;
  assign rsp_rdata=read_q;assign rsp_error=0;
  always_ff @(posedge clock)begin
    if(reset)pending<=0;else if(req_ready)begin
      pending<=req_valid;
      if(req_valid)begin
        read_q<=mem[req_addr];
        if(req_write)for(b=0;b<DATA_WIDTH/8;b=b+1)
          if(req_strb[b])mem[req_addr][b*8+:8]<=req_wdata[b*8+:8];
      end
    end
  end
endmodule

module BusRom #(parameter int ADDR_WIDTH=10,DATA_WIDTH=32,
  localparam int DEPTH=1<<ADDR_WIDTH) (
  input logic clock,reset,
  input logic req_valid,output logic req_ready,input logic[ADDR_WIDTH-1:0]req_addr,
  output logic rsp_valid,input logic rsp_ready,output logic[DATA_WIDTH-1:0]rsp_rdata
);
  logic[DATA_WIDTH-1:0]mem[DEPTH];logic[DATA_WIDTH-1:0]read_q;logic pending;
  assign req_ready=!pending||rsp_ready;assign rsp_valid=pending;assign rsp_rdata=read_q;
  always_ff @(posedge clock)if(reset)pending<=0;else if(req_ready)begin
    pending<=req_valid;if(req_valid)read_q<=mem[req_addr];
  end
endmodule

module BusUart #(parameter int DATA_WIDTH=32) (
  input logic clock,reset,
  input logic req_valid,output logic req_ready,input logic req_write,
  input logic[1:0]req_addr,input logic[DATA_WIDTH-1:0]req_wdata,
  output logic rsp_valid,input logic rsp_ready,output logic[DATA_WIDTH-1:0]rsp_rdata,
  output logic tx_valid,input logic tx_ready,output logic[7:0]tx_data
);
  logic tx_full,pending;logic[7:0]tx_q;
  assign req_ready=(!pending||rsp_ready)&&(!req_write||req_addr!=0||!tx_full);
  assign rsp_valid=pending;assign rsp_rdata=req_addr==1?{{(DATA_WIDTH-1){1'b0}},!tx_full}:'0;
  assign tx_valid=tx_full;assign tx_data=tx_q;
  always_ff @(posedge clock)begin
    if(reset)begin tx_full<=0;pending<=0;end else begin
      if(tx_full&&tx_ready)tx_full<=0;
      if(pending&&rsp_ready)pending<=0;
      if(req_valid&&req_ready)begin
        pending<=1;
        if(req_write&&req_addr==0)begin tx_full<=1;tx_q<=req_wdata[7:0];end
      end
    end
  end
endmodule
