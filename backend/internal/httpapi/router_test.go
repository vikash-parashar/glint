package httpapi

import (
	"context"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"glint/backend/internal/billing"
	"glint/backend/internal/store"
)

type fakeStore struct{}

func (fakeStore) CatalogSince(context.Context, time.Time) ([]store.CatalogItem, error) {
	return []store.CatalogItem{{ID: "frost", Kind: "theme", Name: "Frost"}}, nil
}
func (fakeStore) Wallpapers(context.Context, int, int) ([]store.Wallpaper, error) { return nil, nil }
func (fakeStore) QuoteFor(context.Context, time.Time) (store.Quote, error) {
	return store.Quote{Text: "hi"}, nil
}
func (fakeStore) SavePurchase(context.Context, store.Purchase) error { return nil }

type okVerifier struct{}

func (okVerifier) Verify(context.Context, string, string) (billing.Result, error) {
	return billing.Result{Valid: true, OrderID: "GPA.1"}, nil
}

func newTestHandler() http.Handler {
	return New(Deps{
		Store: fakeStore{}, Verifier: okVerifier{}, Signer: billing.NewSigner([]byte("k")),
		Log: slog.New(slog.NewTextHandler(io.Discard, nil)),
	})
}

func TestCatalog(t *testing.T) {
	rec := httptest.NewRecorder()
	newTestHandler().ServeHTTP(rec, httptest.NewRequest("GET", "/v1/catalog", nil))
	if rec.Code != 200 || !strings.Contains(rec.Body.String(), `"frost"`) {
		t.Fatalf("got %d %s", rec.Code, rec.Body)
	}
}

func TestCatalogBadSince(t *testing.T) {
	rec := httptest.NewRecorder()
	newTestHandler().ServeHTTP(rec, httptest.NewRequest("GET", "/v1/catalog?since=yesterday", nil))
	if rec.Code != 400 {
		t.Fatalf("want 400, got %d", rec.Code)
	}
}

func TestVerifyIssuesLicence(t *testing.T) {
	rec := httptest.NewRecorder()
	body := `{"productId":"pro_lifetime","purchaseToken":"tok","installId":"i1"}`
	newTestHandler().ServeHTTP(rec, httptest.NewRequest("POST", "/v1/purchases/verify", strings.NewReader(body)))
	if rec.Code != 200 || !strings.Contains(rec.Body.String(), `"licence"`) {
		t.Fatalf("got %d %s", rec.Code, rec.Body)
	}
}
