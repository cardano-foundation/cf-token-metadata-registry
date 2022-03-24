import ed25519

privKey, pubKey = ed25519.create_keypair()
print("Private key (32 bytes):", privKey.to_ascii(encoding='hex'))
print("Public key (32 bytes): ", pubKey.to_ascii(encoding='hex'))

msg = b'Message for Ed25519 signing'
signature = privKey.sign(msg, encoding='hex')
print("Signature (64 bytes):", signature)

try:
    pubKey.verify(signature, msg, encoding='hex')
    print("The signature is valid.")
except:
    print("Invalid signature!")


vkey_hex = b"43279D82F5E5E458D7442BFBE0967CBABF40C117D7860DBEC247B43B9D82B4EA"
verifying_key = ed25519.VerifyingKey(vkey_hex, encoding="hex")

skey_hex = b'391651DA96BA0E9C7B930B57C283479305FC152C35C770F2102E5A72BE32FB94'
signing_key = ed25519.SigningKey(skey_hex, encoding="hex")

sig = signing_key.sign(b"hello world", encoding="hex")
print("sig is:", sig)

try:
  verifying_key.verify(sig, b"hello world", encoding="hex")
  print("signature is good")
except ed25519.BadSignatureError:
  print("signature is bad!")
