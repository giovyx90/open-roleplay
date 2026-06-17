'use strict';

const crypto = require('crypto');

/*
 * OTP helpers. The gestionale never stores a password: the in-game plugin
 * generates a 6-digit code, the player types it here, and we verify a hash.
 * Codes are single-use and expire. These functions are pure (clock injected)
 * so they can be unit tested without a database or wall clock.
 */

function generateOtp() {
  // Uniform 6-digit code, 000000–999999, from a CSPRNG.
  const n = crypto.randomInt(0, 1000000);
  return String(n).padStart(6, '0');
}

function hashOtp(uuid, otp) {
  // Bind the hash to the uuid so a leaked hash cannot be replayed for another
  // player. Stored hashes are useless without knowing both factors.
  return crypto.createHash('sha256').update(`${uuid}:${otp}`).digest('hex');
}

/**
 * Validates a submitted OTP against a stored record.
 * @param {object} record { otp_hash, expires_at(ms), used(0|1) } or null
 * @returns {{ok:boolean, reason?:string}}
 */
function verifyOtp(uuid, otp, record, nowMs) {
  if (!record) return { ok: false, reason: 'not_found' };
  if (record.used) return { ok: false, reason: 'used' };
  if (typeof record.expires_at === 'number' && nowMs > record.expires_at) {
    return { ok: false, reason: 'expired' };
  }
  const candidate = hashOtp(uuid, String(otp || ''));
  const a = Buffer.from(candidate);
  const b = Buffer.from(String(record.otp_hash || ''));
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
    return { ok: false, reason: 'mismatch' };
  }
  return { ok: true };
}

module.exports = { generateOtp, hashOtp, verifyOtp };
