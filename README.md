# rabbitmq-dlq-management-ui
DLQ管理コンソール埋め込み用。
Spring Bootアプリケーションへ組み込んで利用することを想定しています。

## 準備

下記DDLを実行しておきます。

```sql
DROP TABLE RABBITMQ_MUTEX;
DROP SEQUENCE RABBITMQ_MUTEX_SEQ;
CREATE TABLE RABBITMQ_MUTEX ( MUTEX NUMBER(18) ,CREATED_AT TIMESTAMP );
CREATE SEQUENCE RABBITMQ_MUTEX_SEQ START WITH 1 INCREMENT BY 1 MAXVALUE 99999 CYCLE;
```

## アプリケーションへ組み込み

### ライブラリ追加設定

* Mavenの場合

pom.xmlのdependenciesへ下記を追加します。

```xml
<dependency>
    <groupId>spring.support</groupId>
    <artifactId>dlq-management-ui</artifactId>
    <version>1.0.0</version>
</dependency>
```

* Gradleの場合

build.gradleのdependenciesへ下記を追加します。

```groovy
compile('spring.supprt:dlq-management-ui:1.0.0')
```

* Mavenの場合

pom.xmlのdependenciesへ下記を追加します。

```xml
<dependency>
    <groupId>spring.support</groupId>
    <artifactId>spring-rabbit-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

* Gradleの場合

build.gradleのdependenciesへ下記を追加します。

```groovy
compile('spring.supprt:spring-rabbit-support:1.0.0')
```

### Spring Boot設定

#### パッケージ指定

Spring Bootは、外部ライブラリ内に定義されたBeanを探して勝手にDI登録したりはしないので、
本ライブラリをDI登録対象とするため、Applicationクラスなどに下記アノテーションを追加して、
ライブラリのコンポーネントを登録させます。

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
...

@SpringBootApplication(scanBasePackages = {
  "アプリケーションルートパッケージ", // アプリケーション側のパッケージルート
  "rabbitmq.console"   // DLQ管理コンソールのパッケージルート
})
@EntityScan(basePackages = {
  "rabbitmq.console.repository.entity" // DLQ管理コンソールのJPAエンティティパッケージルート
})
@EnableJpaRepositories(basePackages = {
  "rabbitmq.console.repository" // DLQ管理コンソールのSpring Data JPAリポジトリパッケージルート
})
```
#### プロパティ設定

DLQ管理コンソールは、Spring BootのDB接続用プロパティ(spring.datasource.*)とRabbitMQサーバ接続用プロパティ(spring.rabbitmq.*)および、
DLQ関連の独自プロパティ(dlq.rabbitmq.*)を参照して動作します。

```yaml:application.yaml(例)
spring:
    datasource:
        driver-class-name:  net.sf.log4jdbc.DriverSpy
        url:                jdbc:log4jdbc:oracle:thin:@localhost:1521:xe
        username:           HR
        password:           HR
        schema:             HR
    rabbitmq:
        addresses: localhost:5672,localhost:5673,localhost:5674
        username: user
        password: pass
        virtual-host: /
dlq:
    rabbitmq:
        dead-letter-queue:
            error.queue: backup.on.delete.queue
            error.queue2: backup.on.delete.queue2
            null.queue:
        max-count: 20 # Dead Letterメッセージ一覧表示最大件数
```

### アクセス方法

Spring Bootアプリケーションに組み込んで起動し、下記URLへアクセスします。

```
http://ホスト名:ポート/deadLetterQueues
```
