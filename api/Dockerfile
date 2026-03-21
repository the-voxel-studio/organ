FROM php:8.4-fpm-alpine

RUN apk add --no-cache \
    icu-dev \
    zlib-dev \
    libzip-dev \
    linux-headers \
    $PHPIZE_DEPS

RUN docker-php-ext-install intl opcache pdo_mysql zip

RUN pecl install redis \
    && docker-php-ext-enable redis

COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html