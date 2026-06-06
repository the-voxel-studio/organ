FROM php:8.4-fpm-alpine

RUN apk add --no-cache \
    icu-dev \
    zlib-dev \
    libzip-dev \
    autoconf \
    g++ \
    make \
    linux-headers \
    openssl-dev \
    php84-pecl-mongodb

RUN docker-php-ext-install intl opcache pdo_mysql zip

# Installation de Redis via PECL pour éviter les dépendances msgpack/igbinary non désirées
RUN pecl install redis \
    && docker-php-ext-enable redis

# Récupération de MongoDB depuis le paquet Alpine (déjà patché pour musl/Alpine)
RUN EXT_DIR=$(php -r "echo ini_get('extension_dir');") \
    && cp /usr/lib/php84/modules/mongodb.so $EXT_DIR/ \
    && docker-php-ext-enable mongodb

RUN apk del autoconf g++ make linux-headers

RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');" \
    && php composer-setup.php --install-dir=/usr/bin --filename=composer \
    && php -r "unlink('composer-setup.php');"

# Préparation des dossiers de cache et permissions pour Doctrine ODM (MongoDB)
RUN mkdir -p var/cache/dev/doctrine/odm/mongodb/Hydrators \
    && mkdir -p var/cache/dev/doctrine/odm/mongodb/Proxies \
    && chown -R www-data:www-data /var/www/html \
    && chmod -R 777 var/cache

WORKDIR /var/www/html

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["php-fpm"]
