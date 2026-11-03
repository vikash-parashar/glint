package httpapi

import (
	"context"
	"encoding/base64"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"

	"github.com/aws/aws-lambda-go/events"
)

// LambdaAdapter turns an http.Handler into an API Gateway HTTP API (payload v2) handler,
// so routes stay plain net/http and are testable without Lambda.
func LambdaAdapter(h http.Handler) func(context.Context, events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	return func(ctx context.Context, e events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
		var body io.Reader = strings.NewReader(e.Body)
		if e.IsBase64Encoded {
			b, err := base64.StdEncoding.DecodeString(e.Body)
			if err != nil {
				return events.APIGatewayV2HTTPResponse{StatusCode: http.StatusBadRequest}, nil
			}
			body = strings.NewReader(string(b))
		}
		url := e.RawPath
		if e.RawQueryString != "" {
			url += "?" + e.RawQueryString
		}
		req, err := http.NewRequestWithContext(ctx, e.RequestContext.HTTP.Method, url, body)
		if err != nil {
			return events.APIGatewayV2HTTPResponse{StatusCode: http.StatusBadRequest}, nil
		}
		for k, v := range e.Headers {
			req.Header.Set(k, v)
		}
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, req)

		headers := make(map[string]string, len(rec.Header()))
		for k := range rec.Header() {
			headers[k] = rec.Header().Get(k)
		}
		return events.APIGatewayV2HTTPResponse{
			StatusCode: rec.Code,
			Headers:    headers,
			Body:       rec.Body.String(),
		}, nil
	}
}
