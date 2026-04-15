FROM php:8.4-fpm-alpine

RUN apk add --no-cache \
    icu-dev \
    zlib-dev \
    libzip-dev \
    inotify-tools

RUN docker-php-ext-install intl opcache pdo_mysql zip

COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html
