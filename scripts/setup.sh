#!/usr/bin/env bash
# OpenNoC dependency installation and environment checks.
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)
MILL_VERSION=1.1.2
VERILATOR_VERSION=v4.216
readonly SCRIPT_DIR REPO_ROOT MILL_VERSION VERILATOR_VERSION

usage() {
  cat <<'EOF'
Usage: scripts/setup.sh <check|deps|java|mill|sbt|verilator|gtkwave|all>
  check      report tools and versions without changing the system
  deps       install native prerequisites on Debian/Ubuntu
  java       install the default OpenJDK
  mill       install Mill 1.1.2 in /usr/local/bin
  sbt        install SBT 1.10.7 in /opt/sbt
  verilator  build and install Verilator 4.216
  gtkwave    install GTKWave
  all        install everything, then run check
EOF
}
need_apt() { command -v apt-get >/dev/null || { echo "setup: apt-get is required" >&2; return 1; }; }
as_root() { if [[ ${EUID} -eq 0 ]]; then "$@"; else command sudo "$@"; fi; }
install_deps() {
  need_apt; as_root apt-get update
  as_root apt-get install -y build-essential autoconf flex bison libfl-dev \
    libtool curl git python3 libncurses-dev
}
install_java() { need_apt; as_root apt-get install -y default-jdk; }
install_gtkwave() { need_apt; as_root apt-get install -y gtkwave; }
install_mill() {
  local work
  work=$(mktemp -d)
  curl -fsSL "https://raw.githubusercontent.com/com-lihaoyi/mill/${MILL_VERSION}/mill" -o "${work}/mill"
  chmod +x "${work}/mill"
  as_root install -m 0755 "${work}/mill" /usr/local/bin/mill
  grep -Fqx "//| mill-version: ${MILL_VERSION}" "${REPO_ROOT}/hw/chisel/build.mill"
  grep -Fqx "//| mill-version: ${MILL_VERSION}" "${REPO_ROOT}/hw/spinal/build.mill"
  rm -rf "${work}"
}
install_sbt() {
  local work archive version=1.10.7
  work=$(mktemp -d); archive="${work}/sbt.tgz"
  curl -fsSL -o "${archive}" "https://github.com/sbt/sbt/releases/download/v${version}/sbt-${version}.tgz"
  tar -xzf "${archive}" -C "${work}"
  as_root install -d /opt/sbt
  as_root cp -a "${work}/sbt/." /opt/sbt/
  as_root ln -sfn /opt/sbt/bin/sbt /usr/local/bin/sbt
  rm -rf "${work}"
}
install_verilator() {
  if command -v verilator >/dev/null &&
     verilator --version | grep -Fq "Verilator 4.216"; then
    echo "Verilator 4.216 is already installed."; return
  fi
  local work
  work=$(mktemp -d)
  git clone --depth 1 --branch "${VERILATOR_VERSION}" \
    https://github.com/verilator/verilator.git "${work}/verilator"
  (
    cd "${work}/verilator"; autoconf; ./configure
    make -j"$(nproc)"; as_root make install
  )
  rm -rf "${work}"
}
check() {
  local missing=0 tool
  for tool in make gcc g++ java python3 mill sbt; do
    if command -v "${tool}" >/dev/null; then
      printf '%-12s %s\n' "${tool}" "$(command -v "${tool}")"
    else printf '%-12s MISSING\n' "${tool}"; missing=1; fi
  done
  if command -v verilator >/dev/null &&
     verilator --version | grep -Fq "Verilator 4.216"; then
    printf '%-12s %s\n' verilator "$(verilator --version)"
  else printf '%-12s MISSING or wrong version (need 4.216)\n' verilator; missing=1; fi
  if command -v gtkwave >/dev/null; then
    printf '%-12s %s\n' gtkwave "$(command -v gtkwave)"
  else printf '%-12s OPTIONAL (needed only for make wave)\n' gtkwave; fi
  return "${missing}"
}
main() {
  case "${1:-}" in
    check) check ;; deps) install_deps ;; java) install_java ;;
    mill) install_mill ;; sbt) install_sbt ;; verilator) install_verilator ;;
    gtkwave) install_gtkwave ;;
    all) install_deps; install_java; install_mill; install_sbt; install_verilator; install_gtkwave; check ;;
    *) usage; return 2 ;;
  esac
}
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then main "$@"; fi
