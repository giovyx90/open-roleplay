'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { generateOtp, hashOtp, verifyOtp } = require('../src/auth/otp');

test('generateOtp produces a 6-digit string', () => {
  for (let i = 0; i < 200; i++) {
    const otp = generateOtp();
    assert.match(otp, /^\d{6}$/);
  }
});

test('verifyOtp accepts a fresh matching code', () => {
  const uuid = 'u-1';
  const otp = '123456';
  const record = { otp_hash: hashOtp(uuid, otp), expires_at: Date.now() + 60000, used: 0 };
  assert.deepEqual(verifyOtp(uuid, otp, record, Date.now()), { ok: true });
});

test('verifyOtp rejects wrong code, expired, used and missing', () => {
  const uuid = 'u-1';
  const good = hashOtp(uuid, '123456');
  const now = Date.now();
  assert.equal(verifyOtp(uuid, '000000', { otp_hash: good, expires_at: now + 1000, used: 0 }, now).ok, false);
  assert.equal(verifyOtp(uuid, '123456', { otp_hash: good, expires_at: now - 1, used: 0 }, now).reason, 'expired');
  assert.equal(verifyOtp(uuid, '123456', { otp_hash: good, expires_at: now + 1000, used: 1 }, now).reason, 'used');
  assert.equal(verifyOtp(uuid, '123456', null, now).reason, 'not_found');
});

test('hash is bound to uuid: same code, different player, different hash', () => {
  assert.notEqual(hashOtp('a', '123456'), hashOtp('b', '123456'));
});
