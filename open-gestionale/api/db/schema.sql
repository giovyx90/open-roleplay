-- Open Gestionale — schema lato API bridge.
--
-- Due famiglie di tabelle:
--   1. Tabelle OWNED dal gestionale (sezione 10 del design): sessioni, OTP,
--      preferenze, notizie, stato "letta" delle notifiche. Sono le UNICHE
--      tabelle su cui il gestionale scrive.
--   2. Tabelle di GIOCO in mirror JSON: players / companies / dossiers / globals.
--      Rappresentano i dati che i plugin Paper scrivono durante il gioco. Qui
--      sono documenti JSON con la stessa forma del seed, così un server può
--      popolarli dai propri dati (vedi db/import-seed.js) e gli stessi widget
--      girano identici su demo e produzione. Un deployment reale può sostituire
--      queste tabelle con VIEW sopra le tabelle dei propri plugin.

-- ----------------------------------------------------------------------------
-- Tabelle owned dal gestionale (sola scrittura consentita)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gestionale_sessions (
    id          TEXT PRIMARY KEY,
    player_uuid TEXT NOT NULL,
    token_hash  TEXT NOT NULL,
    created_at  INTEGER NOT NULL,
    expires_at  INTEGER NOT NULL,
    last_used   INTEGER
);

CREATE TABLE IF NOT EXISTS gestionale_otp (
    player_uuid TEXT PRIMARY KEY,
    otp_hash    TEXT NOT NULL,
    created_at  INTEGER NOT NULL,
    expires_at  INTEGER NOT NULL,
    used        INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS gestionale_preferences (
    player_uuid       TEXT PRIMARY KEY,
    theme             TEXT DEFAULT 'auto',
    layout_overrides  TEXT
);

CREATE TABLE IF NOT EXISTS gestionale_news (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    title        TEXT NOT NULL,
    body         TEXT NOT NULL,
    author_uuid  TEXT NOT NULL,
    published_at INTEGER NOT NULL,
    pinned       INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS gestionale_notifications_read (
    player_uuid     TEXT NOT NULL,
    notification_id TEXT NOT NULL,
    read_at         INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, notification_id)
);

-- ----------------------------------------------------------------------------
-- Mirror JSON dei dati di gioco (sola lettura per il gestionale)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS players (
    uuid TEXT PRIMARY KEY,
    doc  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS companies (
    id  TEXT PRIMARY KEY,
    doc TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS dossiers (
    id  TEXT PRIMARY KEY,
    doc TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS globals (
    key TEXT PRIMARY KEY,
    doc TEXT NOT NULL
);
