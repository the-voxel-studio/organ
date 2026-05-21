# Image de base Node.js
FROM node:20-alpine

# Installation de Angular CLI globalement
RUN npm install -g @angular/cli

# Définition du dossier de travail
WORKDIR /app

# On expose le port 4200 (port par défaut d'Angular)
EXPOSE 4200

# Commande de lancement :
# 1. npm install pour s'assurer que les dépendances sont là
# 2. ng serve avec --host 0.0.0.0 pour être accessible hors du container
# On ajoute --poll 2000 pour forcer la détection des changements sur Windows
CMD ["sh", "-c", "npm install && ng serve --host 0.0.0.0 --poll 2000"]
