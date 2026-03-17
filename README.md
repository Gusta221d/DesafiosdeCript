# DesafioCriptAA
Implementacao de um sistema simples de streaming em tempo real (UDP) com extensoes criptograficas por challenge.

# Requisitos
- Java
- VLC

# Configuracao do proxy

Cada folder de challenge tem um `config.properties` dentro da sua pasta.

Os proxies dos challenges tentam encontrar automaticamente o ficheiro em:
- `config.properties`
- `..\config.properties`
- `..\hjUDPproxy\config.properties`
- `..\..\hjUDPproxy\config.properties`

Dentro de cada `config.properties` tem:
-----------------------
remote=127.0.0.1:8888
localdelivery=127.0.0.1:7777
-------------------------

# Challenge1 (AES-GCM)

# Iniciar 

Numa 1 consola:
-----------------------------
cd ..\challenge1
java hjStreamServerAES.java movies/cars.dat localhost 8888
----------------------------------
Numa 2 consola:
-------------------------
cd ..\challenge1
java hjUDPproxyAES
-------------------------

# Challenge2 (ChaCha20-Poly1305)

# Iniciar

Numa 1 consola:
--------------------------
cd ..\challenge2
java hjStreamServer.java movies/cars.dat localhost 8888
------------------------------
Numa 2 consola:
-----------------------------
cd ..\challenge2
java hjUDPproxy
-------------------------------

# Challenge 3 (DPRG + XOR)

# Iniciar

Numa 1 consola:
--------------------------------
cd ..\challenge3
java hjStreamServerDPRG.java movies/cars.dat localhost 8888
-------------------------------------

Noutra consola:
----------------------------------
cd ..\challenge3
java hjUDPproxyDPRG
------------------------------------