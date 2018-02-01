@echo off
openssl genrsa -out keypair.pem 2048
openssl rsa -in keypair.pem -outform PEM -pubout -out public.pem
openssl rsa -in keypair.pem -outform PEM -out private.pem
openssl pkcs8 -topk8 -inform PEM -outform DER -in keypair.pem -out private.der -nocrypt
openssl pkcs8 -topk8 -inform PEM -outform PEM -in keypair.pem -out pkcs8.pem -nocrypt

openssl rsautl -decrypt -in crypted4.bin -out crypted3.txt -inkey private.pem

openssl rsautl -encrypt -in crypted4.txt -out crypted4.bin -pubin -inkey public.pem