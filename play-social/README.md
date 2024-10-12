# play-social

Social login implementations for the following service providers:

- Google
- Facebook
- Microsoft
- Twitter
- GitHub
- Amazon

## Usage

To use social logins, you need to obtain a client ID and typically a client secret from the identity provider. Check 
the relevant identity provider's documentation on how to obtain this. Let's assume you have done this.

### Flow

The common login flow is as follows:

1. The user is redirected to the identity provider's login page
1. After a successful login, the identity provider redirects the user back to your application
1. Certain parameters are validated after which your application grants access

This library implements two methods to facilitate this flow for various identity providers:

    trait AuthValidator {
      /** The initial redirect to the identity provider that initiates sign-in.
        */
      def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result]
    
      /** A callback to handle the redirect by the identity provider after login.
        */
      def validateCallback(req: RequestHeader): Future[Result]
    }

### Google

Obtain a client ID and secret:

    val credentials = AuthConf("client_id_here", "client_secret_here")
    
Define a reverse route to the desired redirect callback:

    val callback: Call = ???
    
Define how you deal with successful and failed authentication results:

    val handler: AuthResults[Email] = new AuthResults[Email] {
      override def onAuthenticated(user: Email, req: RequestHeader): Result = ???

      override def onUnauthorized(error: AuthError, req: RequestHeader): Result = ???
    }
    
Configure Google:

    val google = GoogleCodeValidator(OAuthConf(callback, handler, credentials, http))

In your Play controller, define the following action methods:

    // Initiates login
    def startGoogle = Action.async { req =>
      google.start(req)
    }

    // Handles Google's callback
    def callbackGoogle = Action.async { req =>
      google.validateCallback(req)
    }
    
Register the callback URL as a redirect URL in the identity provider's configuration and you should be good to go.

### Facebook

As above, except that configure Facebook using:

    val facebook = FacebookCodeValidator(OAuthConf(callback, handler, credentials, http))

### Microsoft

As above, except:

    val microsoft = MicrosoftCodeValidator(OAuthConf(callback, handler, credentials, http))

### Twitter

As above, except:

    val twitter = TwitterValidator(OAuthConf(callback, handler, credentials, http))

### GitHub

As above, except:

    val github = GitHubCodeValidator(OAuthConf(callback, handler, credentials, http))
