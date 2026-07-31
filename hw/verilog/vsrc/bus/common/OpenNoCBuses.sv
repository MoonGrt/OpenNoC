// OpenNoC-owned protocol declarations.  These modules intentionally use only
// SystemVerilog and are the common public signal contract of all backends.
module OpenNoCAxiStream #(
  parameter int DATA_WIDTH = 32, parameter int DEST_WIDTH = 2
) (
  input logic clock, reset, input logic s_valid, output logic s_ready,
  input logic [DATA_WIDTH-1:0] s_data, input logic [DATA_WIDTH/8-1:0] s_keep,
  input logic s_last, input logic [DEST_WIDTH-1:0] s_dest,
  output logic m_valid, input logic m_ready, output logic [DATA_WIDTH-1:0] m_data,
  output logic [DATA_WIDTH/8-1:0] m_keep, output logic m_last,
  output logic [DEST_WIDTH-1:0] m_dest
);
  logic full;
  logic [DATA_WIDTH-1:0] data_q;
  logic [DATA_WIDTH/8-1:0] keep_q;
  logic last_q;
  logic [DEST_WIDTH-1:0] dest_q;
  always_ff @(posedge clock) begin
    if (reset) begin
      full <= 1'b0;
    end else if (s_ready) begin
      full <= s_valid;
      if (s_valid) begin
        data_q <= s_data;
        keep_q <= s_keep;
        last_q <= s_last;
        dest_q <= s_dest;
      end
    end
  end
  assign s_ready = !full || m_ready;
  assign m_valid = full;
  assign m_data = data_q;
  assign m_keep = keep_q;
  assign m_last = last_q;
  assign m_dest = dest_q;
endmodule

// These endpoint shells keep all protocol pins explicit.  Their transaction
// engines are supplied by the language-specific OpenNoC endpoint packages.
module OpenNoCAxiLite #(
  parameter int ADDR_WIDTH = 32, parameter int DATA_WIDTH = 32
) (
  input logic clock, reset,
  input logic aw_valid, output logic aw_ready, input logic [ADDR_WIDTH-1:0] aw_addr,
  input logic w_valid, output logic w_ready, input logic [DATA_WIDTH-1:0] w_data,
  input logic [DATA_WIDTH/8-1:0] w_strb, output logic b_valid, input logic b_ready,
  output logic [1:0] b_resp, input logic ar_valid, output logic ar_ready,
  input logic [ADDR_WIDTH-1:0] ar_addr, output logic r_valid, input logic r_ready,
  output logic [DATA_WIDTH-1:0] r_data, output logic [1:0] r_resp
);
  assign aw_ready = !reset; assign w_ready = !reset; assign ar_ready = !reset;
  assign b_valid = aw_valid && w_valid; assign b_resp = 2'b00;
  assign r_valid = ar_valid; assign r_data = '0; assign r_resp = 2'b00;
endmodule

// AXI4 compatibility profile: one ID and one outstanding request.  The full
// channel set remains visible so a source/sink can be connected without an
// external AXI package; burst scheduling belongs to the NoC endpoint layer.
module OpenNoCAxi4 #(
  parameter int ADDR_WIDTH = 32, parameter int DATA_WIDTH = 32, parameter int ID_WIDTH = 1
) (
  input logic clock, reset,
  input logic aw_valid, output logic aw_ready, input logic [ID_WIDTH-1:0] aw_id,
  input logic [ADDR_WIDTH-1:0] aw_addr, input logic [7:0] aw_len, input logic [2:0] aw_size,
  input logic w_valid, output logic w_ready, input logic [DATA_WIDTH-1:0] w_data,
  input logic [DATA_WIDTH/8-1:0] w_strb, input logic w_last,
  output logic b_valid, input logic b_ready, output logic [ID_WIDTH-1:0] b_id, output logic [1:0] b_resp,
  input logic ar_valid, output logic ar_ready, input logic [ID_WIDTH-1:0] ar_id,
  input logic [ADDR_WIDTH-1:0] ar_addr, input logic [7:0] ar_len, input logic [2:0] ar_size,
  output logic r_valid, input logic r_ready, output logic [ID_WIDTH-1:0] r_id,
  output logic [DATA_WIDTH-1:0] r_data, output logic [1:0] r_resp, output logic r_last
);
  assign aw_ready = !reset && w_valid && b_ready;
  assign w_ready = !reset && aw_valid && b_ready;
  assign b_valid = !reset && aw_valid && w_valid;
  assign b_id = aw_id; assign b_resp = 2'b00;
  assign ar_ready = !reset && r_ready;
  assign r_valid = !reset && ar_valid;
  assign r_id = ar_id; assign r_data = '0; assign r_resp = 2'b00; assign r_last = 1'b1;
endmodule

module OpenNoCApb4 #(
  parameter int ADDR_WIDTH = 32, parameter int DATA_WIDTH = 32
) (
  input logic clock, reset, input logic psel, penable, pwrite,
  input logic [ADDR_WIDTH-1:0] paddr, input logic [DATA_WIDTH-1:0] pwdata,
  input logic [DATA_WIDTH/8-1:0] pstrb, output logic pready,
  output logic [DATA_WIDTH-1:0] prdata, output logic pslverr
);
  assign pready = !reset; assign prdata = '0; assign pslverr = 1'b0;
endmodule

module OpenNoCAhbLite #(
  parameter int ADDR_WIDTH = 32, parameter int DATA_WIDTH = 32
) (
  input logic clock, reset, input logic hsel, hwrite, hready,
  input logic [1:0] htrans, input logic [2:0] hsize,
  input logic [ADDR_WIDTH-1:0] haddr, input logic [DATA_WIDTH-1:0] hwdata,
  output logic hreadyout, output logic hresp, output logic [DATA_WIDTH-1:0] hrdata
);
  assign hreadyout = !reset; assign hresp = 1'b0; assign hrdata = '0;
endmodule

module OpenNoCTileLinkUL #(
  parameter int ADDR_WIDTH = 32, parameter int DATA_WIDTH = 32, parameter int SOURCE_WIDTH = 4
) (
  input logic clock, reset, input logic a_valid, output logic a_ready,
  input logic [2:0] a_opcode, a_param, a_size, input logic [SOURCE_WIDTH-1:0] a_source,
  input logic [ADDR_WIDTH-1:0] a_address, input logic [DATA_WIDTH/8-1:0] a_mask,
  input logic [DATA_WIDTH-1:0] a_data, output logic d_valid, input logic d_ready,
  output logic [2:0] d_opcode, d_param, d_size, output logic [SOURCE_WIDTH-1:0] d_source,
  output logic [DATA_WIDTH-1:0] d_data, output logic d_denied, d_corrupt
);
  assign a_ready = !reset && d_ready; assign d_valid = a_valid;
  assign d_opcode = a_opcode; assign d_param = '0; assign d_size = a_size;
  assign d_source = a_source; assign d_data = '0; assign d_denied = 1'b0; assign d_corrupt = 1'b0;
endmodule

module OpenNoCWishbone #(
  parameter int ADDR_WIDTH=32, DATA_WIDTH=32
) (
  input logic clock, reset, cyc, stb, we,
  input logic [ADDR_WIDTH-1:0] addr,
  input logic [DATA_WIDTH-1:0] wdata,
  input logic [DATA_WIDTH/8-1:0] sel,
  output logic ack, err,
  output logic [DATA_WIDTH-1:0] rdata
);
  assign ack = !reset && cyc && stb;
  assign err = 1'b0;
  assign rdata = '0;
endmodule

module OpenNoCAvalonMM #(
  parameter int ADDR_WIDTH=32, DATA_WIDTH=32
) (
  input logic clock, reset, read, write,
  input logic [ADDR_WIDTH-1:0] address,
  input logic [DATA_WIDTH-1:0] writedata,
  input logic [DATA_WIDTH/8-1:0] byteenable,
  output logic waitrequest, readdatavalid,
  output logic [DATA_WIDTH-1:0] readdata
);
  assign waitrequest = reset;
  assign readdatavalid = !reset && read;
  assign readdata = '0;
endmodule

module OpenNoCSimpleBus #(
  parameter int ADDR_WIDTH=32, DATA_WIDTH=32
) (
  input logic clock, reset, req, write,
  input logic [ADDR_WIDTH-1:0] addr,
  input logic [DATA_WIDTH-1:0] wdata,
  output logic ready, error,
  output logic [DATA_WIDTH-1:0] rdata
);
  assign ready = !reset && req;
  assign error = 1'b0;
  assign rdata = '0;
endmodule
