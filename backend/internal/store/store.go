// Package store is the Postgres data layer (pgx v5). Swap to sqlc-generated code as queries grow.
package store

import (
	"context"
	"encoding/json"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type CatalogItem struct {
	ID        string          `json:"id"`
	Kind      string          `json:"kind"` // widget_style | theme | font
	Name      string          `json:"name"`
	Pro       bool            `json:"pro"`
	Spec      json.RawMessage `json:"spec"` // WidgetSpec / GlassTheme JSON consumed by the app
	UpdatedAt time.Time       `json:"updatedAt"`
}

type Wallpaper struct {
	ID       string `json:"id"`
	Title    string `json:"title"`
	ThumbURL string `json:"thumbUrl"`
	FullURL  string `json:"fullUrl"`
	Pro      bool   `json:"pro"`
}

type Quote struct {
	Text   string `json:"text"`
	Author string `json:"author"`
}

type Purchase struct {
	ProductID, Token, InstallID, OrderID string
	ExpiresAt                            *time.Time
}

type Store struct{ db *pgxpool.Pool }

func New(db *pgxpool.Pool) *Store { return &Store{db: db} }

func (s *Store) CatalogSince(ctx context.Context, since time.Time) ([]CatalogItem, error) {
	rows, err := s.db.Query(ctx, `
		SELECT id, kind, name, pro, spec, updated_at
		FROM catalog_items WHERE published AND updated_at > $1
		ORDER BY updated_at`, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []CatalogItem
	for rows.Next() {
		var c CatalogItem
		if err := rows.Scan(&c.ID, &c.Kind, &c.Name, &c.Pro, &c.Spec, &c.UpdatedAt); err != nil {
			return nil, err
		}
		out = append(out, c)
	}
	return out, rows.Err()
}

func (s *Store) Wallpapers(ctx context.Context, limit, offset int) ([]Wallpaper, error) {
	rows, err := s.db.Query(ctx, `
		SELECT id, title, thumb_url, full_url, pro
		FROM wallpapers WHERE published
		ORDER BY created_at DESC LIMIT $1 OFFSET $2`, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Wallpaper
	for rows.Next() {
		var w Wallpaper
		if err := rows.Scan(&w.ID, &w.Title, &w.ThumbURL, &w.FullURL, &w.Pro); err != nil {
			return nil, err
		}
		out = append(out, w)
	}
	return out, rows.Err()
}

// QuoteFor picks a stable quote per UTC day so every user sees the same one.
func (s *Store) QuoteFor(ctx context.Context, day time.Time) (Quote, error) {
	var q Quote
	err := s.db.QueryRow(ctx, `
		SELECT text, author FROM quotes
		ORDER BY id OFFSET (($1::date - DATE '2026-01-01') % GREATEST((SELECT count(*) FROM quotes), 1)) LIMIT 1`,
		day).Scan(&q.Text, &q.Author)
	return q, err
}

func (s *Store) SavePurchase(ctx context.Context, p Purchase) error {
	_, err := s.db.Exec(ctx, `
		INSERT INTO purchases (purchase_token, product_id, install_id, order_id, expires_at)
		VALUES ($1, $2, $3, $4, $5)
		ON CONFLICT (purchase_token) DO UPDATE
		SET expires_at = EXCLUDED.expires_at, updated_at = now()`,
		p.Token, p.ProductID, p.InstallID, p.OrderID, p.ExpiresAt)
	return err
}
