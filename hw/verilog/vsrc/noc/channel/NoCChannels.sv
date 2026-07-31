module BufferedChannel #(parameter int WIDTH=32, DEPTH=2,
  localparam int PTR_WIDTH=(DEPTH<=1)?1:$clog2(DEPTH),
  localparam int CNT_WIDTH=$clog2(DEPTH+1)) (
  input logic clock,reset,
  input logic in_valid, output logic in_ready, input logic [WIDTH-1:0] in_bits,
  output logic out_valid, input logic out_ready, output logic [WIDTH-1:0] out_bits
);
  logic [WIDTH-1:0] mem[DEPTH];
  logic [PTR_WIDTH-1:0] rd,wr;
  logic [CNT_WIDTH-1:0] count;
  wire push=in_valid&&in_ready, pop=out_valid&&out_ready;
  assign in_ready=count<DEPTH; assign out_valid=count!=0; assign out_bits=mem[rd];
  always_ff @(posedge clock) begin
    if(reset) begin rd<='0;wr<='0;count<='0; end else begin
      if(push) begin mem[wr]<=in_bits;wr<=wr==DEPTH-1?'0:wr+1'b1;end
      if(pop) rd<=rd==DEPTH-1?'0:rd+1'b1;
      case({push,pop}) 2'b10:count<=count+1'b1;2'b01:count<=count-1'b1;default:count<=count;endcase
    end
  end
endmodule

module PipelineChannel #(parameter int WIDTH=32) (
  input logic clock,reset,
  input logic in_valid, output logic in_ready, input logic [WIDTH-1:0] in_bits,
  output logic out_valid, input logic out_ready, output logic [WIDTH-1:0] out_bits
);
  logic full; logic[WIDTH-1:0] data;
  assign in_ready=!full||out_ready;assign out_valid=full;assign out_bits=data;
  always_ff @(posedge clock) if(reset) full<=0; else if(in_ready) begin
    full<=in_valid;if(in_valid)data<=in_bits;
  end
endmodule
