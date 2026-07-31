module BusSmokeTop (
  input logic clock,reset,
  input logic req_valid,output logic req_ready,input logic req_write,
  input logic[7:0]req_addr,input logic[31:0]req_wdata,input logic[3:0]req_strb,
  output logic rsp_valid,input logic rsp_ready,output logic[31:0]rsp_rdata,
  output logic rsp_error,
  output logic tx_valid,input logic tx_ready,output logic[7:0]tx_data
);
  logic active,uart_selected;
  logic ram_req_ready,ram_rsp_valid,ram_rsp_ready,ram_rsp_error;
  logic[31:0]ram_rsp_rdata;
  logic uart_req_ready,uart_rsp_valid,uart_rsp_ready;
  logic[31:0]uart_rsp_rdata;
  wire request_uart=req_addr[7];

  assign req_ready=!active&&(request_uart?uart_req_ready:ram_req_ready);
  assign rsp_valid=active&&(uart_selected?uart_rsp_valid:ram_rsp_valid);
  assign rsp_rdata=uart_selected?uart_rsp_rdata:ram_rsp_rdata;
  assign rsp_error=uart_selected?1'b0:ram_rsp_error;
  assign ram_rsp_ready=active&&!uart_selected&&rsp_ready;
  assign uart_rsp_ready=active&&uart_selected&&rsp_ready;

  BusRam #(.ADDR_WIDTH(7),.DATA_WIDTH(32)) ram(
    .clock,.reset,
    .req_valid(req_valid&&req_ready&&!request_uart),.req_ready(ram_req_ready),
    .req_write,.req_addr(req_addr[6:0]),.req_wdata,.req_strb,
    .rsp_valid(ram_rsp_valid),.rsp_ready(ram_rsp_ready),
    .rsp_rdata(ram_rsp_rdata),.rsp_error(ram_rsp_error));
  BusUart #(.DATA_WIDTH(32)) uart(
    .clock,.reset,
    .req_valid(req_valid&&req_ready&&request_uart),.req_ready(uart_req_ready),
    .req_write,.req_addr(req_addr[3:2]),.req_wdata,
    .rsp_valid(uart_rsp_valid),.rsp_ready(uart_rsp_ready),
    .rsp_rdata(uart_rsp_rdata),.tx_valid,.tx_ready,.tx_data);

  always_ff @(posedge clock)begin
    if(reset)begin active<=0;uart_selected<=0;end else begin
      if(req_valid&&req_ready)begin active<=1;uart_selected<=request_uart;end
      if(rsp_valid&&rsp_ready)active<=0;
    end
  end
endmodule
