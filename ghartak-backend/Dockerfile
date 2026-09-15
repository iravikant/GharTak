FROM php:8.3-cli

# PDO MySQL + common extensions
RUN docker-php-ext-install pdo pdo_mysql opcache

# Composer
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /app

COPY composer.json ./
RUN composer install --no-dev --no-interaction --optimize-autoloader --no-scripts || true

COPY . .
RUN composer install --no-dev --no-interaction --optimize-autoloader --no-scripts

RUN mkdir -p storage/logs && chmod -R 775 storage

# Railway sets $PORT at runtime; default to 8080 for local `docker run`.
ENV PORT=8080
EXPOSE 8080

CMD php -S 0.0.0.0:${PORT} -t public public/router.php
