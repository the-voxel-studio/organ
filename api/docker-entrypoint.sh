#!/bin/sh
set -e

# S'assurer que les dossiers de cache et logs existent
mkdir -p var/cache var/log
mkdir -p var/cache/dev/doctrine/odm/mongodb/Hydrators
mkdir -p var/cache/dev/doctrine/odm/mongodb/Proxies
mkdir -p var/cache/prod/doctrine/odm/mongodb/Hydrators
mkdir -p var/cache/prod/doctrine/odm/mongodb/Proxies

# Assurer la possession par www-data et les permissions d'écriture
chown -R www-data:www-data var
chmod -R 777 var

# Si on démarre php-fpm, exécuter la création de schéma en tant que www-data
if [ "$1" = "php-fpm" ]; then
    echo "Running doctrine:mongodb:schema:create as www-data..."
    su -s /bin/sh -c "php bin/console doctrine:mongodb:schema:create --no-interaction" www-data || true
    
    # Ré-assurer les permissions au cas où de nouveaux fichiers de cache/hydrators ont été créés sous d'autres droits
    chown -R www-data:www-data var
    chmod -R 777 var
fi

exec "$@"
