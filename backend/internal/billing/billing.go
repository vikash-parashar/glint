// Package billing verifies Google Play purchases and issues signed offline licences.
package billing

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"time"
)

type Result struct {
	Valid     bool
	OrderID   string
	ExpiresAt *time.Time // nil for lifetime (one-time product)
}

type Verifier interface {
	Verify(ctx context.Context, productID, token string) (Result, error)
}

// PlayVerifier will call the Google Play Developer API
// (purchases.products.get / purchases.subscriptionsv2.get) using a service account.
type PlayVerifier struct{ pkg string }

func NewPlayVerifier(pkg string) *PlayVerifier { return &PlayVerifier{pkg: pkg} }

func (v *PlayVerifier) Verify(ctx context.Context, productID, token string) (Result, error) {
	// TODO: implement with google.golang.org/api/androidpublisher/v3 and a service account
	// stored in AWS Secrets Manager. Until then, reject everything.
	return Result{}, errors.New("play verification not implemented")
}

type Licence struct {
	InstallID string     `json:"iid"`
	Tier      string     `json:"tier"`
	ExpiresAt *time.Time `json:"exp,omitempty"`
}

// Signer issues compact HMAC-signed licences: base64(json).base64(sig).
// Move to Ed25519 so the app can verify offline with an embedded public key.
type Signer struct{ key []byte }

func NewSigner(key []byte) *Signer { return &Signer{key: key} }

func (s *Signer) Sign(l Licence) (string, error) {
	if len(s.key) == 0 {
		return "", errors.New("signing key not configured")
	}
	body, err := json.Marshal(l)
	if err != nil {
		return "", err
	}
	mac := hmac.New(sha256.New, s.key)
	mac.Write(body)
	enc := base64.RawURLEncoding
	return enc.EncodeToString(body) + "." + enc.EncodeToString(mac.Sum(nil)), nil
}
