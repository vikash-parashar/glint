// Package httpapi wires HTTP routes. Uses Go 1.22+ ServeMux method patterns.
package httpapi

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"strconv"
	"time"

	"glint/backend/internal/billing"
	"glint/backend/internal/store"
)

type Store interface {
	CatalogSince(ctx context.Context, since time.Time) ([]store.CatalogItem, error)
	Wallpapers(ctx context.Context, limit, offset int) ([]store.Wallpaper, error)
	QuoteFor(ctx context.Context, day time.Time) (store.Quote, error)
	SavePurchase(ctx context.Context, p store.Purchase) error
}

type Deps struct {
	Store    Store
	Verifier billing.Verifier
	Signer   *billing.Signer
	Log      *slog.Logger
}

func New(d Deps) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusNoContent) })
	mux.HandleFunc("GET /v1/catalog", d.catalog)
	mux.HandleFunc("GET /v1/wallpapers", d.wallpapers)
	mux.HandleFunc("GET /v1/quotes/today", d.quoteToday)
	mux.HandleFunc("POST /v1/purchases/verify", d.verifyPurchase)
	return mux
}

func (d Deps) catalog(w http.ResponseWriter, r *http.Request) {
	since := time.Time{}
	if s := r.URL.Query().Get("since"); s != "" {
		t, err := time.Parse(time.RFC3339, s)
		if err != nil {
			httpError(w, http.StatusBadRequest, "since must be RFC3339")
			return
		}
		since = t
	}
	items, err := d.Store.CatalogSince(r.Context(), since)
	if err != nil {
		d.fail(w, err)
		return
	}
	w.Header().Set("Cache-Control", "public, max-age=300")
	writeJSON(w, http.StatusOK, map[string]any{"items": items, "serverTime": time.Now().UTC()})
}

func (d Deps) wallpapers(w http.ResponseWriter, r *http.Request) {
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	const size = 30
	items, err := d.Store.Wallpapers(r.Context(), size, max(page, 0)*size)
	if err != nil {
		d.fail(w, err)
		return
	}
	w.Header().Set("Cache-Control", "public, max-age=600")
	writeJSON(w, http.StatusOK, map[string]any{"items": items, "page": page})
}

func (d Deps) quoteToday(w http.ResponseWriter, r *http.Request) {
	q, err := d.Store.QuoteFor(r.Context(), time.Now().UTC())
	if err != nil {
		d.fail(w, err)
		return
	}
	w.Header().Set("Cache-Control", "public, max-age=3600")
	writeJSON(w, http.StatusOK, q)
}

type verifyReq struct {
	ProductID     string `json:"productId"`
	PurchaseToken string `json:"purchaseToken"`
	InstallID     string `json:"installId"`
}

func (d Deps) verifyPurchase(w http.ResponseWriter, r *http.Request) {
	var req verifyReq
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<10)).Decode(&req); err != nil ||
		req.ProductID == "" || req.PurchaseToken == "" {
		httpError(w, http.StatusBadRequest, "productId and purchaseToken are required")
		return
	}
	res, err := d.Verifier.Verify(r.Context(), req.ProductID, req.PurchaseToken)
	if err != nil {
		d.fail(w, err)
		return
	}
	if !res.Valid {
		httpError(w, http.StatusPaymentRequired, "purchase not valid")
		return
	}
	if err := d.Store.SavePurchase(r.Context(), store.Purchase{
		ProductID: req.ProductID, Token: req.PurchaseToken, InstallID: req.InstallID,
		OrderID: res.OrderID, ExpiresAt: res.ExpiresAt,
	}); err != nil {
		d.fail(w, err)
		return
	}
	lic, err := d.Signer.Sign(billing.Licence{InstallID: req.InstallID, Tier: "pro", ExpiresAt: res.ExpiresAt})
	if err != nil {
		d.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"licence": lic})
}

func (d Deps) fail(w http.ResponseWriter, err error) {
	d.Log.Error("request failed", "err", err)
	httpError(w, http.StatusInternalServerError, "internal error")
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}

func httpError(w http.ResponseWriter, code int, msg string) {
	writeJSON(w, code, map[string]string{"error": msg})
}
