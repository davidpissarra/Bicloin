# Guião de Demonstração

## 1. Preparação do sistema

Para testar o sistema e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.

### 1.1. Lançar o *registry*

Para lançar o *ZooKeeper*, ir à pasta `zookeeper/bin` e correr o comando  
`./zkServer.sh start` (Linux) ou `zkServer.cmd` (Windows).

É possível também lançar a consola de interação com o *ZooKeeper*, novamente na pasta `zookeeper/bin` e correr `./zkCli.sh` (Linux) ou `zkCli.cmd` (Windows).

### 1.2. Compilar o projeto

Primeiramente, é necessário compilar e instalar todos os módulos e suas dependências --  *rec*, *hub*, *app*, etc.
Para isso, basta ir à pasta *root* do projeto e correr o seguinte comando:

```sh
$ mvn clean install -DskipTests
```

### 1.3. Lançar e testar o *rec*

Para proceder aos testes, é preciso em primeiro lugar lançar o servidor *rec* .
Para isso basta ir à pasta *rec* e executar:

```sh
$ mvn compile exec:java
```

Este comando vai colocar o *rec* no endereço *localhost* e na porta *8091*.

Para confirmar o funcionamento do servidor com um *ping*, fazer:

```sh
$ cd rec-tester
$ mvn compile exec:java
```

Para executar toda a bateria de testes de integração, fazer:

```sh
$ mvn verify
```

Todos os testes devem ser executados com sucesso.


### 1.4. Lançar e testar o *hub*

Para correr o *hub*, basta entrar na diretoria *hub* do projeto (correr os seguintes comandos):

```sh
$ cd hub
$ mvn compile exec:java
```

**Nota:** Dependendo dos argumentos pretendidos poderá ser necessário alterar os argumentos presentes no *pom.xml* (como por exemplo o argumento *initRec* que irá inicializar o *rec* com os ficheiros *.csv* enviados como argumento).

Para executar toda a bateria de testes de integração do hub, fazer:

```sh
$ cd hub-tester
$ mvn verify
```

Todos os testes devem ser executados com sucesso.

### 1.5. *App*

Iniciar a aplicação com a utilizadora alice:

```sh
$ cd/app/target/appassembler/bin
$ ./app localhost 2181 alice +35191102030 38.7380 -9.3000
```

**Nota:** Para poder correr o script *app* diretamente é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na pasta `target/appassembler/bin/`.

Para importação dos comandos num ficheiro *.txt*:

```sh
$ cd/app/target/appassembler/bin
$ ./app localhost 2181 alice +35191102030 38.7380 -9.3000 < comandos.txt
```

Abrir outra consola, e iniciar a aplicação com o utilizador bruno.

Depois de lançar todos os componentes, tal como descrito acima, já temos o que é necessário para usar o sistema através dos comandos.

# 2. Teste dos comandos

Nesta secção vamos correr os comandos necessários para testar todas as operações do sistema, com a utilizadora alice.
Cada subsecção é respetiva a cada operação presente no *hub*.

## 2.1. *balance*

    > balance

<span style="color:green">alice 0 BIC</span>

## 2.2 *top-up*

    > top-up 10

<span style="color:green">alice 100 BIC</span>

    > top-up 25

<span style="color:red">ERRO Carregamento deve ser entre 1 e 20 Euros.</span>

## 2.3 *tag*

    > tag 38.7376 -9.3031 loc1

<span style="color:green">OK</span>

    > tag 91.7376 -9.3031 loc2

<span style="color:red">ERRO Latitude inválida.</span>

## 2.4 *move*

    > move loc1

<span style="color:green">alice em https://www.google.com/maps/place/38.7376,-9.3031</span>

    > move 58.7576 -9.5051

<span style="color:green">alice em https://www.google.com/maps/place/58.7576,-9.5051</span>

## 2.5 *at*

    > at

<span style="color:green">alice em https://www.google.com/maps/place/58.7576,-9.5051</span>

## 2.6 *scan*

    > scan 4

<span style="color:green">ocea, lat 38.7633, -9.095 long, 20 docas, 2 BIC prémio, 15 bicicletas, a 2212190 metros</span>

<span style="color:green">istt, lat 38.7372, -9.3023 long, 20 docas, 4 BIC prémio, 12 bicicletas, a 2214905 metros</span>

<span style="color:green">gulb, lat 38.7376, -9.1545 long, 30 docas, 2 BIC prémio, 30 bicicletas, a 2214954 metros</span>

<span style="color:green">ista, lat 38.7369, -9.1366 long, 20 docas, 3 BIC prémio, 19 bicicletas, a 2215044 metros</span>

## 2.7 *info*

    > info istt

<span style="color:green">IST Taguspark, lat 38.7372, -9.3023 long, 20 docas, 4 BIC prémio, 12 bicicletas, 22 levantamentos, 7 devoluções, https://www.google.com/maps/place/38.7372,-9.3023</span>

    > info inexistente

<span style="color:red">ERRO Estação inexistente.</span>

## 2.8 *bike-up*

    > bike-up ista

<span style="color:red">ERRO fora de alcance.</span>

    > move loc1
    alice em https://www.google.com/maps/place/38.7376,-9.3031
    > bike-up istt

<span style="color:green">OK</span>

## 2.9 *bike-down*

    > move 58.1234 53.5235  
    alice em https://www.google.com/maps/place/58.1234,53.5235
    > bike-down ista

<span style="color:red">ERRO fora de alcance.</span>

    > move loc1 
    alice em https://www.google.com/maps/place/38.7376,-9.3031
    > bike-down istt

<span style="color:green">OK</span>

## 2.10 *ping*

Correndo apenas 1 *hub*:

    > ping

<span style="color:green">Servidor hub está ligado.</span> 

## 2.11 *sys-status*

Correndo apenas 1 *hub* e 1 *rec*:

    > sys-status

<span style="color:green">Servidor hub está ligado.</span>

<span style="color:green">Réplica 1 do rec está ligada.</span>

<span style="color:green">Réplica 2 do rec está ligada.</span>

<span style="color:green">Réplica 3 do rec está ligada.</span>

<span style="color:green">Réplica 4 do rec está ligada.</span>

<span style="color:green">Réplica 5 do rec está ligada.</span>

## 3. DEMO: Replicação e tolerância a faltas

### 3.1 *Lançar réplicas do rec*

Instalar dependências:

```sh
$ mvn install -DskipTests
```

Lançar réplica (instância número 1 rec):

```sh
$ cd rec/target/appassembler/bin
$ ./rec localhost 2181 localhost 8091 1
```

Lançar réplica (instância número 2 rec, noutro terminal):

```sh
$ cd rec/target/appassembler/bin
$ ./rec localhost 2181 localhost 8092 2
```

Lançar réplica (instância número 3 rec, noutro terminal):

```sh
$ cd rec/target/appassembler/bin
$ ./rec localhost 2181 localhost 8093 3
```

Lançar réplica (instância número 4 rec, noutro terminal):

```sh
$ cd rec/target/appassembler/bin
$ ./rec localhost 2181 localhost 8094 4
```

Lançar réplica (instância número 5 rec, noutro terminal):

```sh
$ cd rec/target/appassembler/bin
$ ./rec localhost 2181 localhost 8095 5
```

### 3.2 *Lançar servidor do hub (fornecer dados)*

```sh
$ cd hub/target/appassembler/bin
$ ./hub localhost 2181 localhost 8081 1 ../../../src/main/java/pt/tecnico/bicloin/hub/users.csv ../../../src/main/java/pt/tecnico/bicloin/hub/stations.csv initRec
```

### 3.3 *Lançar app*

Iniciar a aplicação com a utilizadora alice:

```sh
$ cd app/target/appassembler/bin
$ ./app localhost 2181 alice +35191102030 38.7380 -9.3000
```

### 3.4 *Fazer interrogações*

Seguir o ponto **2. Teste dos comandos**.

### 3.5 *Tolerância a Faltas*

Para testar a tolerância a faltas clicar Ctrl+Z num dos processos de uma réplica rec.

## 4. Considerações Finais

Estes testes não cobrem tudo, pelo que devem ter sempre em conta os testes de integração e o código.