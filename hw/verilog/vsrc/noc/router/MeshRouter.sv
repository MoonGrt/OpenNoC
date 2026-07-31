`default_nettype none

module MeshRouter #(
  parameter int ROUTER_ID = 0,
  parameter int MESH_X = 2,
  parameter int MESH_Y = 2,
  parameter int DATA_WIDTH = 32,
  parameter int NODE_ID_WIDTH = 2,
  parameter int BUFFER_DEPTH = 2,
  localparam int PORTS = 5,
  localparam int PTR_WIDTH = (BUFFER_DEPTH <= 1) ? 1 : $clog2(BUFFER_DEPTH),
  localparam int CNT_WIDTH = $clog2(BUFFER_DEPTH + 1)
) (
  input logic clock, input logic reset,
  input logic [PORTS-1:0] in_valid,
  output logic [PORTS-1:0] in_ready,
  input logic [PORTS*DATA_WIDTH-1:0] in_data,
  input logic [PORTS*NODE_ID_WIDTH-1:0] in_src,
  input logic [PORTS*NODE_ID_WIDTH-1:0] in_dest,
  input logic [PORTS-1:0] in_last,
  output logic [PORTS-1:0] out_valid,
  input logic [PORTS-1:0] out_ready,
  output logic [PORTS*DATA_WIDTH-1:0] out_data,
  output logic [PORTS*NODE_ID_WIDTH-1:0] out_src,
  output logic [PORTS*NODE_ID_WIDTH-1:0] out_dest,
  output logic [PORTS-1:0] out_last
);
  localparam int LOCAL=0, EAST=1, WEST=2, NORTH=3, SOUTH=4;
  localparam int ROUTER_X = ROUTER_ID % MESH_X;
  localparam int ROUTER_Y = ROUTER_ID / MESH_X;

  logic [DATA_WIDTH-1:0] data_mem [PORTS][BUFFER_DEPTH];
  logic [NODE_ID_WIDTH-1:0] src_mem [PORTS][BUFFER_DEPTH];
  logic [NODE_ID_WIDTH-1:0] dest_mem [PORTS][BUFFER_DEPTH];
  logic last_mem [PORTS][BUFFER_DEPTH];
  logic [PTR_WIDTH-1:0] rd_ptr[PORTS], wr_ptr[PORTS];
  logic [CNT_WIDTH-1:0] count[PORTS];
  logic locked[PORTS];
  logic [2:0] owner[PORTS], rr[PORTS], selected[PORTS];
  logic selected_valid[PORTS];
  logic [PORTS-1:0] pop;
  integer p, o, k, candidate, dest_value, dest_x, dest_y, route;

  always_comb begin
    candidate = 0; dest_value = 0; dest_x = 0; dest_y = 0; route = LOCAL;
    in_ready = '0; out_valid = '0; out_data = '0;
    out_src = '0; out_dest = '0; out_last = '0; pop = '0;
    for (p=0; p<PORTS; p=p+1)
      in_ready[p] = count[p] < BUFFER_DEPTH;
    for (o=0; o<PORTS; o=o+1) begin
      selected[o] = owner[o];
      selected_valid[o] = 1'b0;
      if (locked[o]) begin
        selected_valid[o] = count[owner[o]] != 0;
      end else begin
        for (k=PORTS-1; k>=0; k=k-1) begin
          candidate = rr[o] + k;
          if (candidate >= PORTS) candidate = candidate - PORTS;
          if (count[candidate] != 0) begin
            dest_value = dest_mem[candidate][rd_ptr[candidate]];
            dest_x = dest_value % MESH_X;
            dest_y = dest_value / MESH_X;
            if (dest_value == ROUTER_ID) route = LOCAL;
            else if (dest_x > ROUTER_X) route = EAST;
            else if (dest_x < ROUTER_X) route = WEST;
            else if (dest_y < ROUTER_Y) route = NORTH;
            else route = SOUTH;
            if (route == o) begin
              selected[o] = candidate[2:0];
              selected_valid[o] = 1'b1;
            end
          end
        end
      end
      if (selected_valid[o]) begin
        out_valid[o] = 1'b1;
        out_data[o*DATA_WIDTH +: DATA_WIDTH] =
          data_mem[selected[o]][rd_ptr[selected[o]]];
        out_src[o*NODE_ID_WIDTH +: NODE_ID_WIDTH] =
          src_mem[selected[o]][rd_ptr[selected[o]]];
        out_dest[o*NODE_ID_WIDTH +: NODE_ID_WIDTH] =
          dest_mem[selected[o]][rd_ptr[selected[o]]];
        out_last[o] = last_mem[selected[o]][rd_ptr[selected[o]]];
        pop[selected[o]] = out_ready[o];
      end
    end
  end

  integer n;
  always_ff @(posedge clock) begin
    if (reset) begin
      for (n=0; n<PORTS; n=n+1) begin
        rd_ptr[n] <= '0; wr_ptr[n] <= '0; count[n] <= '0;
        locked[n] <= 1'b0; owner[n] <= '0; rr[n] <= '0;
      end
    end else begin
      for (n=0; n<PORTS; n=n+1) begin
        if (in_valid[n] && in_ready[n]) begin
          data_mem[n][wr_ptr[n]] <= in_data[n*DATA_WIDTH +: DATA_WIDTH];
          src_mem[n][wr_ptr[n]] <= in_src[n*NODE_ID_WIDTH +: NODE_ID_WIDTH];
          dest_mem[n][wr_ptr[n]] <= in_dest[n*NODE_ID_WIDTH +: NODE_ID_WIDTH];
          last_mem[n][wr_ptr[n]] <= in_last[n];
          wr_ptr[n] <= wr_ptr[n] == BUFFER_DEPTH-1 ? '0 : wr_ptr[n] + 1'b1;
        end
        if (pop[n])
          rd_ptr[n] <= rd_ptr[n] == BUFFER_DEPTH-1 ? '0 : rd_ptr[n] + 1'b1;
        case ({in_valid[n] && in_ready[n],pop[n]})
          2'b10: count[n] <= count[n] + 1'b1;
          2'b01: count[n] <= count[n] - 1'b1;
          default: count[n] <= count[n];
        endcase
      end
      for (n=0; n<PORTS; n=n+1) begin
        if (!locked[n] && selected_valid[n]) begin
          locked[n] <= 1'b1; owner[n] <= selected[n];
        end
        if (selected_valid[n] && out_ready[n] &&
            last_mem[selected[n]][rd_ptr[selected[n]]]) begin
          locked[n] <= 1'b0;
          rr[n] <= selected[n] == PORTS-1 ? '0 : selected[n] + 1'b1;
        end
      end
    end
  end
endmodule

`default_nettype wire
