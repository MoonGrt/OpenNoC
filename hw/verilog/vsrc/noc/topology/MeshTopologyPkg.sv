package MeshTopologyPkg;
  function automatic int node_id(input int x,input int y,input int width);
    return y*width+x;
  endfunction
  function automatic bit has_east(input int id,input int width);
    return (id%width)!=(width-1);
  endfunction
  function automatic bit has_west(input int id,input int width);
    return (id%width)!=0;
  endfunction
endpackage
