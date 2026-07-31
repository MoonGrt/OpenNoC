module PacketCrossbar #(parameter int PORTS=5,DATA_WIDTH=32,
  NODE_ID_WIDTH=3) (
  input logic clock,reset,
  input logic[PORTS-1:0]in_valid,output logic[PORTS-1:0]in_ready,
  input logic[PORTS*DATA_WIDTH-1:0]in_data,
  input logic[PORTS*NODE_ID_WIDTH-1:0]in_route,input logic[PORTS-1:0]in_last,
  output logic[PORTS-1:0]out_valid,input logic[PORTS-1:0]out_ready,
  output logic[PORTS*DATA_WIDTH-1:0]out_data,output logic[PORTS-1:0]out_last
);
  logic locked[PORTS];logic[NODE_ID_WIDTH-1:0]owner[PORTS],selected[PORTS];
  logic selected_valid[PORTS];integer o,i;
  always_comb begin
    in_ready='0;out_valid='0;out_data='0;out_last='0;
    for(o=0;o<PORTS;o=o+1)begin
      selected[o]=owner[o];selected_valid[o]=0;
      if(locked[o])selected_valid[o]=in_valid[owner[o]];
      else for(i=PORTS-1;i>=0;i=i-1)
        if(in_valid[i]&&in_route[i*NODE_ID_WIDTH+:NODE_ID_WIDTH]==o)begin
          selected[o]=i;selected_valid[o]=1;
        end
      if(selected_valid[o])begin
        out_valid[o]=1;out_data[o*DATA_WIDTH+:DATA_WIDTH]=in_data[selected[o]*DATA_WIDTH+:DATA_WIDTH];
        out_last[o]=in_last[selected[o]];in_ready[selected[o]]=out_ready[o];
      end
    end
  end
  integer n;
  always_ff @(posedge clock)begin
    if(reset)for(n=0;n<PORTS;n=n+1)begin locked[n]<=0;owner[n]<='0;end
    else for(n=0;n<PORTS;n=n+1)begin
      if(!locked[n]&&selected_valid[n])begin locked[n]<=1;owner[n]<=selected[n];end
      if(selected_valid[n]&&out_ready[n]&&in_last[selected[n]])locked[n]<=0;
    end
  end
endmodule
