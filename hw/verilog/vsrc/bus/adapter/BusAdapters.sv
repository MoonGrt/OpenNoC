module BusWidthAdapter #(parameter int IN_WIDTH=32,OUT_WIDTH=64) (
  input logic clock,reset,
  input logic s_valid,output logic s_ready,input logic[IN_WIDTH-1:0]s_data,
  output logic m_valid,input logic m_ready,output logic[OUT_WIDTH-1:0]m_data
);
  localparam int RATIO=(OUT_WIDTH>=IN_WIDTH)?OUT_WIDTH/IN_WIDTH:1;
  localparam int CW=(RATIO<=1)?1:$clog2(RATIO);
  logic[OUT_WIDTH-1:0]buffer;logic[CW-1:0]count;logic full;
  generate if(OUT_WIDTH>=IN_WIDTH)begin
    assign s_ready=!full;assign m_valid=full;assign m_data=buffer;
    always_ff @(posedge clock)begin
      if(reset)begin count<=0;full<=0;end else begin
        if(s_valid&&s_ready)begin
          buffer[count*IN_WIDTH+:IN_WIDTH]<=s_data;
          if(count==RATIO-1)begin count<=0;full<=1;end else count<=count+1'b1;
        end
        if(full&&m_ready)full<=0;
      end
    end
  end else begin
    assign s_ready=m_ready;assign m_valid=s_valid;assign m_data=s_data[OUT_WIDTH-1:0];
  end endgenerate
endmodule

module SimpleBusToApb #(parameter int ADDR_WIDTH=32,DATA_WIDTH=32) (
  input logic clock,reset,
  input logic req_valid,output logic req_ready,input logic req_write,
  input logic[ADDR_WIDTH-1:0]req_addr,input logic[DATA_WIDTH-1:0]req_wdata,
  output logic rsp_valid,input logic rsp_ready,output logic[DATA_WIDTH-1:0]rsp_rdata,
  output logic psel,penable,pwrite,output logic[ADDR_WIDTH-1:0]paddr,
  output logic[DATA_WIDTH-1:0]pwdata,input logic pready,
  input logic[DATA_WIDTH-1:0]prdata
);
  typedef enum logic[1:0]{IDLE,SETUP,ACCESS,RESPONSE}state_t;
  state_t state;logic write_q;logic[ADDR_WIDTH-1:0]addr_q;
  logic[DATA_WIDTH-1:0]wdata_q,rdata_q;
  assign req_ready=state==IDLE;assign rsp_valid=state==RESPONSE;assign rsp_rdata=rdata_q;
  assign psel=state==SETUP||state==ACCESS;assign penable=state==ACCESS;
  assign pwrite=write_q;assign paddr=addr_q;assign pwdata=wdata_q;
  always_ff @(posedge clock)begin
    if(reset)state<=IDLE;else case(state)
      IDLE:if(req_valid)begin write_q<=req_write;addr_q<=req_addr;wdata_q<=req_wdata;state<=SETUP;end
      SETUP:state<=ACCESS;
      ACCESS:if(pready)begin rdata_q<=prdata;state<=RESPONSE;end
      RESPONSE:if(rsp_ready)state<=IDLE;
    endcase
  end
endmodule

module BusHost #(parameter int ADDR_WIDTH=32,DATA_WIDTH=32) (
  input logic clock,reset,start,write,
  input logic[ADDR_WIDTH-1:0]address,input logic[DATA_WIDTH-1:0]write_data,
  output logic busy,done,
  output logic req_valid,input logic req_ready,output logic req_write,
  output logic[ADDR_WIDTH-1:0]req_addr,output logic[DATA_WIDTH-1:0]req_wdata,
  input logic rsp_valid,output logic rsp_ready,input logic[DATA_WIDTH-1:0]rsp_rdata,
  output logic[DATA_WIDTH-1:0]read_data
);
  logic sent;
  assign req_valid=busy&&!sent;assign req_write=write;assign req_addr=address;
  assign req_wdata=write_data;assign rsp_ready=busy&&sent;assign done=rsp_valid&&rsp_ready;
  always_ff @(posedge clock)begin
    if(reset)begin busy<=0;sent<=0;read_data<=0;end else begin
      if(start&&!busy)begin busy<=1;sent<=0;end
      if(req_valid&&req_ready)sent<=1;
      if(done)begin busy<=0;sent<=0;read_data<=rsp_rdata;end
    end
  end
endmodule
