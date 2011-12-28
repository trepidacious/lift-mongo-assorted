# Lift Mongo Assorted Stuff
Various classes for use with Lift and Mongo records

DISCLAIMER - code has been lightly tested, but was written as part of learning to use Lift and Mongo, so please use at your own risk!

lift-mongoauth looks much nicer, I haven't had a chance to try yet though ;)

[lift-mongoauth](https://github.com/eltimn/lift-mongoauth)

[Example lift-mongoauth Project](https://github.com/eltimn/lift-mongo.g8)

# Installation
This isn't a full project - just put the source somewhere appropriate, modifying package as necessary

# Configuration
Depends on use - but you might want to:

Add User.AddUserMenusAfter to an entry in your sitemap, and add this somewhere in Boot:
 
	LiftRules.setSiteMapFunc(() => User.sitemapMutator(sitemap()))

Add ExtSession to Boot:
	LiftRules.earlyInStateful.append(ExtSession.testCookieEarlyInStateful)

Configure Mailer in Boot, to allow for recovering passwords and verifying user emails:
	def configMailer(host: String, user: String, password: String) {
	  // Enable TLS support
	  System.setProperty("mail.smtp.starttls.enable","true");
	  // Set the host name
	  System.setProperty("mail.smtp.host", host) // Enable authentication
	  System.setProperty("mail.smtp.auth", "true") // Provide a means for authentication. Pass it a Can, which can either be Full or Empty
	  Mailer.authenticator = Full(new Authenticator {
	    override def getPasswordAuthentication = new PasswordAuthentication(user, password)
	  })
	}

# Customisation
A User implementation is provided, but you can add modify as necessary

# License
Apache v2.0. See LICENSE.txt
