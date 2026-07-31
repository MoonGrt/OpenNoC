module XYRouting #(parameter int MESH_X=2,NODE_ID_WIDTH=2,ROUTER_ID=0) (
  input logic [NODE_ID_WIDTH-1:0] dest,
  output logic [2:0] port
);
  localparam int LOCAL=0,EAST=1,WEST=2,NORTH=3,SOUTH=4;
  integer dx,dy;
  always_comb begin
    dx=dest%MESH_X;dy=dest/MESH_X;port=LOCAL;
    if(dest==ROUTER_ID)port=LOCAL;
    else if(dx>ROUTER_ID%MESH_X)port=EAST;
    else if(dx<ROUTER_ID%MESH_X)port=WEST;
    else if(dy<ROUTER_ID/MESH_X)port=NORTH;
    else port=SOUTH;
  end
endmodule
