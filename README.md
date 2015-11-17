# play-scala-jira-api
Scala implementation to the public JIRA API based on Plays Webservice Framework

## Jira OAuth Authentication setup

### 1.Generate Public/Private keys

Generate private key with openssl
```
openssl genrsa -out dummy-genrsa.pem 2048
```

Generate public key with openssl
```
openssl rsa -in dummy-genrsa.pem -pubout
```

### 2. Create incoming Application link
The first step is to register a new consumer in JIRA. This is done through the Application Links administration screens in JIRA. Create a new Application Link.
When creating the Application Link use a placeholder URL or the correct URL to your client, if your client can be reached via HTTP and choose the Generic Application type. After this Application Link has been created, edit the configuration and go to the incoming authentication configuration screen and select OAuth. Enter in this the public key and the consumer key which your client will use when making requests to JIRA.
After you have entered all the information click OK and ensure OAuth authentication is enabled.

### 3. Obtain request token
* Start play-scala-jira-api `activator run`
* Open http://localhost:9000/oauth
* Fill out form with 
** baseUrl of jira instance to connect to
** consumerKey
** privateKey (including start and end tags)

### 4. Obtain validationKey
Follow the provided URL and enter username and password of user you'd like to map to. Fetch validationKey from redirected URL

### 5. Obtain access token
* Enter validationKey into next mask
* Submit form
* Store accessToken for further requests

