.PHONY: dev test build-lambda deploy
dev:            ## Postgres + API locally on :8080
	docker compose up --build
test:
	go test ./...
build-lambda:   ## Used by `sam build` (BuildMethod go1.x) or run manually
	GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -tags lambda.norpc -o bootstrap ./cmd/api
deploy:
	sam build && sam deploy --guided
