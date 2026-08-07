APP_IMAGE ?= pragmatic-jvm-demo:local
APP_PORT ?= 8081
COMPOSE_RUNTIME = APP_IMAGE="$(APP_IMAGE)" APP_PORT="$(APP_PORT)" docker compose -f compose.runtime.yaml

.PHONY: dev test qa image up down smoke

dev:
	./gradlew bootRun

test:
	./gradlew test

qa:
	./gradlew qa

image:
	./gradlew bootJar
	docker build --tag "$(APP_IMAGE)" .

up:
	$(COMPOSE_RUNTIME) up --detach

down:
	$(COMPOSE_RUNTIME) down

smoke:
	curl --fail --silent --show-error --retry 30 --retry-delay 1 --retry-all-errors \
		"http://localhost:$(APP_PORT)/actuator/health/readiness"
