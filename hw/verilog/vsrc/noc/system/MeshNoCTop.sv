`default_nettype none

module MeshNoCTop #(
  parameter int MESH_X=2, MESH_Y=2, DATA_WIDTH=32, NODE_ID_WIDTH=2,
  parameter int VC_NUM=1, BUFFER_DEPTH=2,
  localparam int NODES=MESH_X*MESH_Y, PORTS=5
) (
  input logic clock, reset,
  input logic [NODES-1:0] in_valid, output logic [NODES-1:0] in_ready,
  input logic [NODES*DATA_WIDTH-1:0] in_data,
  input logic [NODES*NODE_ID_WIDTH-1:0] in_dest,
  input logic [NODES-1:0] in_last,
  output logic [NODES-1:0] out_valid, input logic [NODES-1:0] out_ready,
  output logic [NODES*DATA_WIDTH-1:0] out_data,
  output logic [NODES*NODE_ID_WIDTH-1:0] out_src,
  output logic [NODES*NODE_ID_WIDTH-1:0] out_dest,
  output logic [NODES-1:0] out_last
);
  localparam int LOCAL=0, EAST=1, WEST=2, NORTH=3, SOUTH=4;
  logic [PORTS-1:0] rv_i[NODES], rr_i[NODES], rv_o[NODES], rr_o[NODES];
  logic [PORTS*DATA_WIDTH-1:0] rd_i[NODES], rd_o[NODES];
  logic [PORTS*NODE_ID_WIDTH-1:0] rs_i[NODES], rt_i[NODES], rs_o[NODES], rt_o[NODES];
  logic [PORTS-1:0] rl_i[NODES], rl_o[NODES];
  logic input_active[NODES];
  logic [NODE_ID_WIDTH-1:0] input_dest_q[NODES];

  genvar g;
  generate for (g=0; g<NODES; g=g+1) begin: routers
    localparam int X=g%MESH_X, Y=g/MESH_X;
    always_comb begin
      rv_i[g]='0; rd_i[g]='0; rs_i[g]='0; rt_i[g]='0; rl_i[g]='0; rr_o[g]='0;
      rv_i[g][LOCAL]=in_valid[g] &&
        ((input_active[g] ? input_dest_q[g] :
          in_dest[g*NODE_ID_WIDTH +: NODE_ID_WIDTH]) < NODES);
      rd_i[g][LOCAL*DATA_WIDTH +: DATA_WIDTH]=in_data[g*DATA_WIDTH +: DATA_WIDTH];
      rs_i[g][LOCAL*NODE_ID_WIDTH +: NODE_ID_WIDTH]=g;
      rt_i[g][LOCAL*NODE_ID_WIDTH +: NODE_ID_WIDTH]=input_active[g] ?
        input_dest_q[g] : in_dest[g*NODE_ID_WIDTH +: NODE_ID_WIDTH];
      rl_i[g][LOCAL]=in_last[g];
      rr_o[g][LOCAL]=out_ready[g];
      if (X < MESH_X-1) begin
        rv_i[g][EAST]=rv_o[g+1][WEST];
        rd_i[g][EAST*DATA_WIDTH +: DATA_WIDTH]=rd_o[g+1][WEST*DATA_WIDTH +: DATA_WIDTH];
        rs_i[g][EAST*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rs_o[g+1][WEST*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rt_i[g][EAST*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rt_o[g+1][WEST*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rl_i[g][EAST]=rl_o[g+1][WEST]; rr_o[g][EAST]=rr_i[g+1][WEST];
      end
      if (X > 0) begin
        rv_i[g][WEST]=rv_o[g-1][EAST];
        rd_i[g][WEST*DATA_WIDTH +: DATA_WIDTH]=rd_o[g-1][EAST*DATA_WIDTH +: DATA_WIDTH];
        rs_i[g][WEST*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rs_o[g-1][EAST*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rt_i[g][WEST*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rt_o[g-1][EAST*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rl_i[g][WEST]=rl_o[g-1][EAST]; rr_o[g][WEST]=rr_i[g-1][EAST];
      end
      if (Y > 0) begin
        rv_i[g][NORTH]=rv_o[g-MESH_X][SOUTH];
        rd_i[g][NORTH*DATA_WIDTH +: DATA_WIDTH]=rd_o[g-MESH_X][SOUTH*DATA_WIDTH +: DATA_WIDTH];
        rs_i[g][NORTH*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rs_o[g-MESH_X][SOUTH*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rt_i[g][NORTH*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rt_o[g-MESH_X][SOUTH*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rl_i[g][NORTH]=rl_o[g-MESH_X][SOUTH]; rr_o[g][NORTH]=rr_i[g-MESH_X][SOUTH];
      end
      if (Y < MESH_Y-1) begin
        rv_i[g][SOUTH]=rv_o[g+MESH_X][NORTH];
        rd_i[g][SOUTH*DATA_WIDTH +: DATA_WIDTH]=rd_o[g+MESH_X][NORTH*DATA_WIDTH +: DATA_WIDTH];
        rs_i[g][SOUTH*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rs_o[g+MESH_X][NORTH*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rt_i[g][SOUTH*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rt_o[g+MESH_X][NORTH*NODE_ID_WIDTH +: NODE_ID_WIDTH];
        rl_i[g][SOUTH]=rl_o[g+MESH_X][NORTH]; rr_o[g][SOUTH]=rr_i[g+MESH_X][NORTH];
      end
    end
    assign in_ready[g]=rr_i[g][LOCAL] &&
      ((input_active[g] ? input_dest_q[g] :
        in_dest[g*NODE_ID_WIDTH +: NODE_ID_WIDTH]) < NODES);
    assign out_valid[g]=rv_o[g][LOCAL];
    assign out_data[g*DATA_WIDTH +: DATA_WIDTH]=rd_o[g][LOCAL*DATA_WIDTH +: DATA_WIDTH];
    assign out_src[g*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rs_o[g][LOCAL*NODE_ID_WIDTH +: NODE_ID_WIDTH];
    assign out_dest[g*NODE_ID_WIDTH +: NODE_ID_WIDTH]=rt_o[g][LOCAL*NODE_ID_WIDTH +: NODE_ID_WIDTH];
    assign out_last[g]=rl_o[g][LOCAL];
    MeshRouter #(.ROUTER_ID(g),.MESH_X(MESH_X),.MESH_Y(MESH_Y),
      .DATA_WIDTH(DATA_WIDTH),.NODE_ID_WIDTH(NODE_ID_WIDTH),
      .BUFFER_DEPTH(BUFFER_DEPTH)) router (
      .clock,.reset,.in_valid(rv_i[g]),.in_ready(rr_i[g]),.in_data(rd_i[g]),
      .in_src(rs_i[g]),.in_dest(rt_i[g]),.in_last(rl_i[g]),
      .out_valid(rv_o[g]),.out_ready(rr_o[g]),.out_data(rd_o[g]),
      .out_src(rs_o[g]),.out_dest(rt_o[g]),.out_last(rl_o[g]));
  end endgenerate

  integer n;
  always_ff @(posedge clock) begin
    if (reset) begin
      for(n=0;n<NODES;n=n+1) begin input_active[n]<=0; input_dest_q[n]<='0; end
    end else for(n=0;n<NODES;n=n+1) if(in_valid[n]&&in_ready[n]) begin
      if(!input_active[n]&&!in_last[n]) begin
        input_active[n]<=1; input_dest_q[n]<=in_dest[n*NODE_ID_WIDTH +: NODE_ID_WIDTH];
      end
      if(in_last[n]) input_active[n]<=0;
    end
  end
endmodule

`default_nettype wire
