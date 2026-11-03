// Command api serves the Glint catalogue, wallpaper and licensing API.
// The same binary runs on AWS Lambda (provided.al2023, arm64) or as a plain
// HTTP server for local development with docker compose.
package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/jackc/pgx/v5/pgxpool"

	"glint/backend/internal/billing"
	"glint/backend/internal/httpapi"
	"glint/backend/internal/store"
)

func main() {
	log := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, mustEnv("DATABASE_URL"))
	if err != nil {
		log.Error("db connect", "err", err)
		os.Exit(1)
	}
	defer pool.Close()

	h := httpapi.New(httpapi.Deps{
		Store:    store.New(pool),
		Verifier: billing.NewPlayVerifier(os.Getenv("PLAY_PACKAGE_NAME")),
		Signer:   billing.NewSigner([]byte(os.Getenv("LICENCE_SIGNING_KEY"))),
		Log:      log,
	})

	if os.Getenv("AWS_LAMBDA_RUNTIME_API") != "" {
		lambda.Start(httpapi.LambdaAdapter(h))
		return
	}

	addr := ":" + envOr("PORT", "8080")
	log.Info("listening", "addr", addr)
	srv := &http.Server{Addr: addr, Handler: h, ReadHeaderTimeout: 5 * time.Second}
	if err := srv.ListenAndServe(); err != nil {
		log.Error("server", "err", err)
		os.Exit(1)
	}
}

func mustEnv(k string) string {
	v := os.Getenv(k)
	if v == "" {
		slog.Error("missing env", "key", k)
		os.Exit(1)
	}
	return v
}

func envOr(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
