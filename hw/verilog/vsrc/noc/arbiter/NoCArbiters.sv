module FixedPriorityArbiter #(parameter int INPUTS=5) (
  input logic [INPUTS-1:0] request,
  output logic [INPUTS-1:0] grant,
  output logic valid
);
  integer i;
  always_comb begin
    grant='0; valid=1'b0;
    for(i=INPUTS-1;i>=0;i=i-1)
      if(request[i]) begin grant='0; grant[i]=1'b1; valid=1'b1; end
  end
endmodule

module RoundRobinArbiter #(parameter int INPUTS=5,
  localparam int INDEX_WIDTH=(INPUTS<=1)?1:$clog2(INPUTS)) (
  input logic clock, reset, advance,
  input logic [INPUTS-1:0] request,
  output logic [INPUTS-1:0] grant,
  output logic valid,
  output logic [INDEX_WIDTH-1:0] grant_index
);
  logic [INDEX_WIDTH-1:0] next;
  integer i,candidate;
  always_comb begin
    grant='0; valid=0; grant_index=next; candidate=0;
    for(i=INPUTS-1;i>=0;i=i-1) begin
      candidate=next+i; if(candidate>=INPUTS) candidate=candidate-INPUTS;
      if(request[candidate]) begin
        grant='0; grant[candidate]=1'b1; grant_index=candidate; valid=1;
      end
    end
  end
  always_ff @(posedge clock)
    if(reset) next<='0;
    else if(advance&&valid) next<=grant_index==INPUTS-1?'0:grant_index+1'b1;
endmodule
