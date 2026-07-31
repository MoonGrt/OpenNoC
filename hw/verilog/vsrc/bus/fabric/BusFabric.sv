module AddressDecoder #(parameter int SLAVES=4,ADDR_WIDTH=32,
  parameter logic[SLAVES*ADDR_WIDTH-1:0] BASE='0,
  parameter logic[SLAVES*ADDR_WIDTH-1:0] MASK='0,
  localparam int INDEX_WIDTH=(SLAVES<=1)?1:$clog2(SLAVES)) (
  input logic[ADDR_WIDTH-1:0]addr,
  output logic[SLAVES-1:0]select,output logic hit,
  output logic[INDEX_WIDTH-1:0]index
);
  integer i;
  always_comb begin
    select='0;hit=0;index='0;
    for(i=0;i<SLAVES;i=i+1)
      if((addr&MASK[i*ADDR_WIDTH+:ADDR_WIDTH])==
         (BASE[i*ADDR_WIDTH+:ADDR_WIDTH]&MASK[i*ADDR_WIDTH+:ADDR_WIDTH]))begin
        select[i]=1;hit=1;index=i;
      end
  end
endmodule

module FabricArbiter #(parameter int MASTERS=4,
  localparam int INDEX_WIDTH=(MASTERS<=1)?1:$clog2(MASTERS)) (
  input logic clock,reset,complete,
  input logic[MASTERS-1:0]request,
  output logic[MASTERS-1:0]grant,output logic valid,
  output logic[INDEX_WIDTH-1:0]index
);
  logic locked;logic[INDEX_WIDTH-1:0]owner,next;integer i,candidate;
  always_comb begin
    grant='0;valid=0;index=owner;candidate=0;
    if(locked)begin grant[owner]=1;valid=request[owner];end else
      for(i=MASTERS-1;i>=0;i=i-1)begin
        candidate=next+i;if(candidate>=MASTERS)candidate=candidate-MASTERS;
        if(request[candidate])begin grant='0;grant[candidate]=1;index=candidate;valid=1;end
      end
  end
  always_ff @(posedge clock)begin
    if(reset)begin locked<=0;owner<='0;next<='0;end else begin
      if(!locked&&valid)begin locked<=1;owner<=index;end
      if(locked&&complete)begin locked<=0;next<=owner==MASTERS-1?'0:owner+1'b1;end
    end
  end
endmodule
