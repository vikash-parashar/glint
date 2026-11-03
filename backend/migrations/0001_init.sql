-- +goose Up
CREATE TABLE catalog_items (
    id          text PRIMARY KEY,
    kind        text NOT NULL CHECK (kind IN ('widget_style', 'theme', 'font')),
    name        text NOT NULL,
    pro         boolean NOT NULL DEFAULT false,
    spec        jsonb NOT NULL,
    published   boolean NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX catalog_items_updated_idx ON catalog_items (updated_at) WHERE published;

CREATE TABLE wallpapers (
    id          text PRIMARY KEY,
    title       text NOT NULL,
    thumb_url   text NOT NULL,
    full_url    text NOT NULL,
    pro         boolean NOT NULL DEFAULT false,
    published   boolean NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE quotes (
    id      bigserial PRIMARY KEY,
    text    text NOT NULL,
    author  text NOT NULL DEFAULT ''
);

CREATE TABLE purchases (
    purchase_token  text PRIMARY KEY,
    product_id      text NOT NULL,
    install_id      text NOT NULL,
    order_id        text,
    expires_at      timestamptz,
    revoked_at      timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

INSERT INTO catalog_items (id, kind, name, pro, spec, published) VALUES
  ('frost',  'theme', 'Frost',  false, '{"tint":"#E8F0FF","opacity":0.18,"cornerDp":28,"accent":"#7CC4FF"}', true),
  ('smoke',  'theme', 'Smoke',  false, '{"tint":"#1A1A1E","opacity":0.55,"cornerDp":28,"accent":"#FFB547"}', true),
  ('aurora', 'theme', 'Aurora', false, '{"tint":"#2B1B4A","opacity":0.40,"cornerDp":32,"accent":"#5CF2C4"}', true);

INSERT INTO quotes (text, author) VALUES
  ('Simplicity is the ultimate sophistication.', 'Leonardo da Vinci'),
  ('Well begun is half done.', 'Aristotle');

-- +goose Down
DROP TABLE purchases, quotes, wallpapers, catalog_items;
