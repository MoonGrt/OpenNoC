module VirtualChannel #(parameter int WIDTH=32,DEPTH=2) (
  input logic clock,reset,
  input logic in_valid,output logic in_ready,input logic[WIDTH-1:0]in_bits,
  output logic out_valid,input logic out_ready,output logic[WIDTH-1:0]out_bits
);
  BufferedChannel #(.WIDTH(WIDTH),.DEPTH(DEPTH)) fifo(
    .clock,.reset,.in_valid,.in_ready,.in_bits,.out_valid,.out_ready,.out_bits);
endmodule

module VCAllocator #(parameter int VC_NUM=1,
  localparam int VC_WIDTH=(VC_NUM<=1)?1:$clog2(VC_NUM)) (
  input logic[VC_NUM-1:0]available,
  output logic valid,output logic[VC_WIDTH-1:0]vc
);
  integer i;
  always_comb begin
    valid=0;vc='0;
    for(i=VC_NUM-1;i>=0;i=i-1)if(available[i])begin valid=1;vc=i;end
  end
endmodule
