{
  inputs = {
    utils.url = "github:numtide/flake-utils";
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };
  outputs = { self, nixpkgs, utils }: utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs {
	inherit system;
	config.allowUnfree = true;
      };
    in
    {
      devShell = pkgs.mkShell {
        buildInputs = with pkgs; [
          jdk
          gradle
	      android-studio
        ];
      };
    }
  );
}
