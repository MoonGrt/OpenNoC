#!/usr/bin/env bash
set -euo pipefail

CHOICE=""

error_handler() {
  echo "[error] Installation failed."
  case "$CHOICE" in
    1)
      echo "Please refer to SBT official website:"
      echo "https://www.scala-sbt.org/"
      ;;
    2)
      echo "Please refer to Mill official website:"
      echo "https://mill-build.org/"
      ;;
    *)
      echo "Unknown option."
      ;;
  esac
}

trap error_handler ERR

if [[ $EUID -ne 0 ]]; then
  sudo -v || exit 1
fi

echo "Select installation option:"
echo "1) Install SBT"
echo "2) Install Mill"
read -p "Enter choice [1-2]: " CHOICE

install_java() {
  echo "=== Install Java (OpenJDK 17) ==="
  sudo add-apt-repository -y ppa:openjdk-r/ppa
  sudo apt-get update
  sudo apt-get install -y openjdk-17-jdk
}

install_sbt() {
  read -p "Do you want to install Java (OpenJDK 17)? [y/n]: " install_java_choice
  if [[ "$install_java_choice" =~ ^[Yy]$ ]]; then
    install_java
  fi
  echo "=== Install SBT ==="
  sudo apt update && sudo apt install -y curl
  echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
  echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
  curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add -
  sudo apt-get update && sudo apt-get install -y sbt
}

install_mill() {
  read -p "Do you want to install Java (OpenJDK 17)? [y/n]: " install_java_choice
  if [[ "$install_java_choice" =~ ^[Yy]$ ]]; then
    install_java
  fi
  echo "=== Install Mill ==="
  curl -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.5/mill-dist-1.1.5-mill.sh -o mill
  chmod +x mill && sudo mv mill /usr/local/bin/mill
}

case $CHOICE in
  1)
    install_sbt
    ;;
  2)
    install_mill
    ;;
  *)
    echo "Invalid choice"
    exit 1
    ;;
esac

echo "=== Done ==="
