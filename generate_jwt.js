const jwt = require('jsonwebtoken');

const secret = "TradeFlowSuperSecretKeyForJwtAuthentication2026!";
const payload = {
  sub: "1",
  iat: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60)
};

const token = jwt.sign(payload, secret, { algorithm: 'HS256' });
console.log(token);
