FROM php:8.4-fpm-alpine

RUN apk add --no-cache \
    icu-dev \
    zlib-dev \
    libzip-dev \
    inotify-tools

RUN docker-php-ext-install intl opcache pdo_mysql zip

RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');" \
    && php composer-setup.php --install-dir=/usr/bin --filename=composer \
    && php -r "unlink('composer-setup.php');"

WORKDIR /var/www/html
