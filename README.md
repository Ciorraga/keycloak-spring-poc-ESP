# Integración de Keycloak en un proyecto Spring

## Introducción

En esta POC vamos a ver un ejemplo muy básico de como implantar Keycloak como identity manager dentro de una aplicación de ejemplo desarrollada en Spring Boot.

Es importante tener en cuenta que es necesario seguir previamente o haber realizado previamente esta otra https://github.com/Ciorraga/keycloak-postgre-poc-ESP ya que tanto el Realm, como el client, secret  y usuario y contraseña que se usará en la POC que vamos a hacer en este repositorio utilizan los valores que se utilizaron para la POC del enlace anterior.

## Dependencias necesarias

En esta POC haremos uso de Maven como gestor de dependencias, siendo las dependencias necesarias a añadir las siguientes (además de otras que se pueden visualizar en el pom.xml de este repositorio):

```
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-config</artifactId>
    </dependency>

    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-spring-boot-starter</artifactId>
        <version>20.0.3</version>
    </dependency>
```

## Configuración a nivel de application.yml

Para esta prueba se ha sustituido el application.properties por un application.yml en el cual, además del context-path y el puerto, se ha añadido la configuración necesaria de keycloak como es la URL de autenticación, realm, cliente y secret.

```
spring.application.name: poc-spring-keycloak

server:
   port: 8080
   servlet.context-path: /poc-spring-keycloak

keycloak:
    enabled: true
    realm: RealmTest
    auth-server-url: http://localhost:8091
    resource: clientTest
    credentials:
     secret: 4qJL5HrhLiMYOqrwPh6fL0Qpu9jRojid   
```

## Clase de configuración

Lo primero que tendremos que hacer es crear una clase de tipo @Configuration donde, valga la redundancia, se configurará la librería de keycloak.

```
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    @Configuration
    @EnableWebSecurity
    @ComponentScan(basePackageClasses = KeycloakSpringBootConfigResolver.class)
    public class SecurityConfigAuthorization extends KeycloakWebSecurityConfigurerAdapter {
        
        
        @Autowired
        public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
            KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
            keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
            auth.authenticationProvider(keycloakAuthenticationProvider);
        }
    
        @Bean
        public KeycloakConfigResolver KeycloakConfigResolver() {
            return new KeycloakSpringBootConfigResolver();
        }
    
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            super.configure(http);
            http
                .authorizeRequests()
                    .antMatchers("/poc-spring-keycloak/**").authenticated()
                    .anyRequest().permitAll();
        }
    
        @Override
        protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
            return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
        }
    
        @Bean
        @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
        public AccessToken accessToken() {
            return ((KeycloakAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getAccount().getKeycloakSecurityContext().getToken();
        }
        
    }
```

En esta configuración, además, se ha creado un @Bean que nos va a permitir obtener el token y la información en la clase del restController que se detalla en el siguiente punto.

## RestController

Para esta POC se ha definido un RestController muy simple en el cual, cuando se llama al endpoint /getMessage, devolverá un mensaje con con usuario logado acompañado de un código de respuesta 200.

En caso de no funcionar, la respuesta a la petición será un código 401 (Unauthorized).

```
    @RestController
    public class MessageRestController {
        
        @Autowired
        private AccessToken accessToken;
        
        @GetMapping("/getMessage")
        public ResponseEntity<String> getMessageSecurizedByKeycloak(
                @RequestParam(value = "page", defaultValue = "0") int page,
                @RequestParam(value = "size", defaultValue = "20") int size) {
            
            return new ResponseEntity<>	("It works. User logged: " + accessToken.getPreferredUsername(), HttpStatus.OK);	
        }	
    }
```

## Testeo de la aplicación

Para poder probar el endpoint que acabamos de configurar y que está securizado vía Keycloak, tendremos que hacer lo siguiente:

- Obtener el token del usuario en keycloak vía cURL:
    ```
    curl -d 'client_id=clientTest' -d 'client_secret=4qJL5HrhLiMYOqrwPh6fL0Qpu9jRojid' -d 'username=userTest' -d 'password=userTest' -d 'grant_type=password' 'http://localhost:8091/realms/RealmTest/protocol/openid-connect/token'
    ```

    Con esto, tendremos una salida similar a la siguiente:

    ```
    {"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxV2Rwczhic2ptOHNuOGVEalBacExfeGRvSG5pWHN0bXpFVHlGZEc5dHQ4In0.eyJleHAiOjE2ODE2NDgzNTEsImlhdCI6MTY4MTY0ODA1MSwianRpIjoiZmE2NDM1MDEtNTdkZC00NjljLThlY2UtOGVjMzgzZDYyY2Q5IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDkxL3JlYWxtcy9SZWFsbVRlc3QiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNTQwZTdmNjItZDQ3Ni00MDg3LTk0ZDctZWYzZjQwYzI3MGZmIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50VGVzdCIsInNlc3Npb25fc3RhdGUiOiI0ZjMwZTkxNS01NTFkLTRmYWMtODExNC0wMjJiZWQ3MzczNTciLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiZGVmYXVsdC1yb2xlcy1yZWFsbXRlc3QiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjRmMzBlOTE1LTU1MWQtNGZhYy04MTE0LTAyMmJlZDczNzM1NyIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcnRlc3QiLCJnaXZlbl9uYW1lIjoiIiwiZmFtaWx5X25hbWUiOiIifQ.DFn2GiJL3ztjFJBTHyxEuIkh-cSqzdg6CWLYFntBBXtReDvruaIDT9KvNL_qkcyEGWCayiNZweLrWdzpFkPbT41B-CQKV_nYC7_7OQp3Xnrv6E9-tU4aAmx7dap7HiZaJS6HOlsCywui9qC6PnwYJPPzOX5CGtUwCzrgUDQVhNwlURocdtUNsre_R_VCsNOjzBAz9oR-a_R2vOgKOqM6u04h6DX3QdR3lEPGWW0SoLXeazEgLYmEjH3E0FriqXtUxZ_ElQCRfSPJFRW3mwSSEl-MUTm_Lsm2aIS9PddKQFSZCyiCd6mIZ9vgJH9wBFV3FUE6tfznonelog8Xq6-1wA","expires_in":300,"refresh_expires_in":1800,"refresh_token":"eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlN2MxNWM3Yy05YjJhLTRhOTctOGEwOS1lOWZmMTIzYjlkMDEifQ.eyJleHAiOjE2ODE2NDk4NTEsImlhdCI6MTY4MTY0ODA1MSwianRpIjoiMTE3NDFkZmItYzNlNC00NmM4LTk2OTUtNTM5YTJiMjY3ZGU1IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDkxL3JlYWxtcy9SZWFsbVRlc3QiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwOTEvcmVhbG1zL1JlYWxtVGVzdCIsInN1YiI6IjU0MGU3ZjYyLWQ0NzYtNDA4Ny05NGQ3LWVmM2Y0MGMyNzBmZiIsInR5cCI6IlJlZnJlc2giLCJhenAiOiJjbGllbnRUZXN0Iiwic2Vzc2lvbl9zdGF0ZSI6IjRmMzBlOTE1LTU1MWQtNGZhYy04MTE0LTAyMmJlZDczNzM1NyIsInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjRmMzBlOTE1LTU1MWQtNGZhYy04MTE0LTAyMmJlZDczNzM1NyJ9.pkvkrZoMaH1qTUWpmKMUATOR8-X5rWgS1Qrig8s1mEQ","token_type":"Bearer","not-before-policy":0,"session_state":"4f30e915-551d-4fac-8114-022bed737357","scope":"email profile"}
    ```

- Con el access_token que hemos obtenido en la petición anterior, realizamos la siguiente petición al endpoint de nuestra aplicación informando dicho token como parámetro de cabecera:

    ```
    curl -X GET \
    http://localhost:8080/poc-spring-keycloak/getMessage \
    -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxV2Rwczhic2ptOHNuOGVEalBacExfeGRvSG5pWHN0bXpFVHlGZEc5dHQ4In0.eyJleHAiOjE2ODE2NDgzNTEsImlhdCI6MTY4MTY0ODA1MSwianRpIjoiZmE2NDM1MDEtNTdkZC00NjljLThlY2UtOGVjMzgzZDYyY2Q5IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDkxL3JlYWxtcy9SZWFsbVRlc3QiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNTQwZTdmNjItZDQ3Ni00MDg3LTk0ZDctZWYzZjQwYzI3MGZmIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50VGVzdCIsInNlc3Npb25fc3RhdGUiOiI0ZjMwZTkxNS01NTFkLTRmYWMtODExNC0wMjJiZWQ3MzczNTciLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiZGVmYXVsdC1yb2xlcy1yZWFsbXRlc3QiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjRmMzBlOTE1LTU1MWQtNGZhYy04MTE0LTAyMmJlZDczNzM1NyIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcnRlc3QiLCJnaXZlbl9uYW1lIjoiIiwiZmFtaWx5X25hbWUiOiIifQ.DFn2GiJL3ztjFJBTHyxEuIkh-cSqzdg6CWLYFntBBXtReDvruaIDT9KvNL_qkcyEGWCayiNZweLrWdzpFkPbT41B-CQKV_nYC7_7OQp3Xnrv6E9-tU4aAmx7dap7HiZaJS6HOlsCywui9qC6PnwYJPPzOX5CGtUwCzrgUDQVhNwlURocdtUNsre_R_VCsNOjzBAz9oR-a_R2vOgKOqM6u04h6DX3QdR3lEPGWW0SoLXeazEgLYmEjH3E0FriqXtUxZ_ElQCRfSPJFRW3mwSSEl-MUTm_Lsm2aIS9PddKQFSZCyiCd6mIZ9vgJH9wBFV3FUE6tfznonelog8Xq6-1wA'
    ```

- Si hemos seguido los dos pasos anteriores, tendremos una respuesta correcta de la aplicación con el siguiente resultado:

    ```
    It works. User logged: usertest
    ```