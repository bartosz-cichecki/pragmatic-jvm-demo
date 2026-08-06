APP_IMAGE ?= pragmatic-jvm-demo:local
APP_PORT ?= 8081
COMPOSE_RUNTIME = APP_IMAGE="$(APP_IMAGE)" APP_PORT="$(APP_PORT)" docker compose -f compose.runtime.yaml

.PHONY: dev test image up down smoke

dev:
	./gradlew bootRun

test:
	./gradlew test

image:
	./gradlew bootJar
	docker build --tag "$(APP_IMAGE)" .

up:
	$(COMPOSE_RUNTIME) up --detach

down:
	$(COMPOSE_RUNTIME) down

smoke:
	curl --fail --silent --show-error --retry 30 --retry-delay 1 --retry-connrefused \
		"http://localhost:$(APP_PORT)/actuator/health/readiness"
