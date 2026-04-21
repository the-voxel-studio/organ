{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.nodejs # Version complète (pas la slim)
  ];

  shellHook = ''
    # On définit un prefix local temporaire pour éviter les erreurs de permissions
    # et on nettoie le cache npm si besoin au lancement
    export NPM_CONFIG_PREFIX="$PWD/.npx-cache"
    export PATH="$NPM_CONFIG_PREFIX/bin:$PATH"
    
    echo "--- Environnement Nix réparé ---"
    echo "Note : Un dossier .npx-cache sera créé localement mais tu peux le supprimer après usage."
  '';
}
