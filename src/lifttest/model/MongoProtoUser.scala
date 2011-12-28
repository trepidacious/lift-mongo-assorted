package lifttest{
package model {
  
import net.liftweb.http.S
import net.liftweb.http.S._
import net.liftweb.http.js._
import JsCmds._
import scala.xml.{NodeSeq, Node, Text, Elem}
import scala.xml.transform._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError
//import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.util.Mailer._
import net.liftweb.record.field._
import net.liftweb.proto.{ProtoUser => GenProtoUser}
import net.liftweb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record._

/**
 * ProtoUser is a base class that gives you a "User" that has a first name,
 * last name, email, etc.
 */
trait MongoProtoUser[T <: MongoProtoUser[T]] extends MongoRecord[T] with ObjectIdPk[T] with UserIdAsString {
  self: T =>

  /**
   * Convert the id to a String
   */
  def userIdAsString: String = id.is.toStringMongod
  
  /**
   * The first name field for the User.  You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val firstName = new MyFirstName(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */

  lazy val firstName: StringField[T] = new MyFirstName(this, 32)

  protected class MyFirstName(obj: T, size: Int) extends StringField(obj, size) {
    override def displayName = owner.firstNameDisplayName
    override val fieldId = Some(Text("txtFirstName"))
  }

  /**
   * The string name for the first name field
   */
  def firstNameDisplayName = ??("first.name")

  /**
   * The last field for the User.  You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val lastName = new MyLastName(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val lastName: StringField[T] = new MyLastName(this, 32)

  protected class MyLastName(obj: T, size: Int) extends StringField(obj, size) {
    override def displayName = owner.lastNameDisplayName
    override val fieldId = Some(Text("txtLastName"))
  }

  /**
   * The last name string
   */
  def lastNameDisplayName = ??("last.name")

  /**
   * The email field for the User.  You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val email = new MyEmail(this, 48) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val email: EmailField[T] = new MyEmail(this, 48)

  protected class MyEmail(obj: T, size: Int) extends EmailField(obj, size) {
    private def valUnique(emailValue: ValueType): List[FieldError] = toBoxMyType(emailValue) match {
      case Full(email) => {
        owner.meta.findAll("email", email) match {
					    case Nil => Nil
					    case usr :: Nil if (usr.id.is == owner.id.is) => Nil
					    case _ => Text(S.??("unique.email.address"))
				    }
      }
      case _ => Text(S.??("unique.email.address"))
    }
        
    override def validations = valUnique _ :: super.validations
    override def displayName = owner.emailDisplayName
    override val fieldId = Some(Text("txtEmail"))
  }

  /**
   * The email first name
   */
  def emailDisplayName = ??("email.address")

  /**
   * Use a MongoPasswordField to make passwords work... replaces PasswordField
   */
  lazy val password: MongoPasswordField[T] = new MongoPasswordField[T](this, 8) {
    override def displayName = owner.passwordDisplayName    
  }

    /**
   * The display name for the password field
   */
  def passwordDisplayName = ??("password")

  /**
   * The superuser field for the User.  You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val superUser = new MySuperUser(this) {
   *   println("I am doing something different")
   * }
   * </pre>
   */  
  lazy val superUser: BooleanField[T] = new MySuperUser(this)

  protected class MySuperUser(obj: T) extends BooleanField(obj) {
    override def defaultValue = false
  }

  def niceName: String = (firstName.is, lastName.is, email.is) match {
    case (f, l, e) if f.length > 1 && l.length > 1 => f+" "+l+" ("+e+")"
    case (f, _, e) if f.length > 1 => f+" ("+e+")"
    case (_, l, e) if l.length > 1 => l+" ("+e+")"
    case (_, _, e) => e
  }

  def shortName: String = (firstName.is, lastName.is) match {
    case (f, l) if f.length > 1 && l.length > 1 => f+" "+l
    case (f, _) if f.length > 1 => f
    case (_, l) if l.length > 1 => l
    case _ => email.is
  }

  def niceNameWEmailLink = <a href={"mailto:"+email.is}>{niceName}</a>
}

/**
 * Mix this trait into the the Mapper singleton for User and you
 * get a bunch of user functionality including password reset, etc.
 */
trait MongoMetaMegaProtoUser[ModelType <: MongoMegaProtoUser[ModelType]] extends MongoMetaRecord[ModelType] with ObjectIdPk[ModelType] with GenProtoUser {
  self: ModelType =>

  type TheUserType = ModelType

  /**
   * What's a field pointer for the underlying CRUDify
   */
  type FieldPointerType = Field[_, TheUserType]

  /**
   * Based on a FieldPointer, build a FieldPointerBridge
   */
  protected implicit def buildFieldBridge(from: FieldPointerType): FieldPointerBridge = new MyPointer(from)


  protected class MyPointer(from: FieldPointerType) extends FieldPointerBridge {
    /**
     * What is the display name of this field?
     */
    def displayHtml: NodeSeq = from.displayHtml

    /**
     * Does this represent a pointer to a Password field
     */
    def isPasswordField_? : Boolean = from match {
      case a: MongoPasswordField[_] => true
      case _ => false
    }
  }

  /**
   * Convert an instance of TheUserType to the Bridge trait
   */
  protected implicit def typeToBridge(in: TheUserType): UserBridge = 
    new MyUserBridge(in)

  /**
   * Bridges from TheUserType to methods used in this class
   */
  protected class MyUserBridge(in: TheUserType) extends UserBridge {
    
    /**
     * Convert the user's primary key to a String
     */
    def userIdAsString: String = in.id.toString

    /**
     * Return the user's first name
     */
    def getFirstName: String = in.firstName.is

    /**
     * Return the user's last name
     */
    def getLastName: String = in.lastName.is

    /**
     * Get the user's email
     */
    def getEmail: String = in.email.is

    /**
     * Is the user a superuser
     */
    def superUser_? : Boolean = in.superUser.is

    /**
     * Has the user been validated?
     */
    def validated_? : Boolean = in.validated.is

    /**
     * Does the supplied password match the actual password?
     */
    def testPassword(toTest: Box[String]): Boolean = 
      toTest.map(in.password.isMatch) openOr false

    /**
     * Set the validation flag on the user and return the user
     */
    def setValidated(validation: Boolean): TheUserType =
      in.validated(validation)

    /**
     * Set the unique ID for this user to a new value
     */
    def resetUniqueId(): TheUserType = {
      in.uniqueId.reset()
      in
    }

    /**
     * Return the unique ID for the user
     */
    def getUniqueId(): String = in.uniqueId.is

    /**
     * Validate the user
     */
    def validate: List[FieldError] = in.validate

    /**
     * Given a list of string, set the password
     */
    def setPasswordFromListString(pwd: List[String]): TheUserType = {
      //If we have a pair of matching passwords, set to one of them
      pwd match {
        case x1 :: x2 :: Nil if x1 == x2 => in.password.setPassword(x1)
        case _ => Nil
      }
      in
    }

    /**
     * Save the user to backing store
     */
    def save(): Boolean  = {
      in.save
      true
    }
  }

  /**
   * Given a field pointer and an instance, get the field on that instance
   */
  protected def computeFieldFromPointer(instance: TheUserType, pointer: FieldPointerType): Box[BaseField] = fieldByName(pointer.name, instance)


  /**
   * Given an username (probably email address), find the user
   */
  protected def findUserByUserName(email: String): Box[TheUserType]

  /**
   * Given a unique id, find the user
   */
  protected def findUserByUniqueId(id: String): Box[TheUserType]

  /**
   * Create a new instance of the User
   */
  protected def createNewUserInstance(): TheUserType = self.createRecord

  /**
   * Given a String representing the User ID, find the user
   */
  protected def userFromStringId(id: String): Box[TheUserType]

  /**
   * The list of fields presented to the user at sign-up
   */
  def signupFields: List[FieldPointerType] = List(firstName, 
                                                  lastName, 
                                                  email, 
                                                  locale, 
                                                  timezone,
                                                  password)

  /**
   * The list of fields presented to the user for editing
   */
  def editFields: List[FieldPointerType] = List(firstName, 
                                                lastName, 
                                                email, 
                                                locale, 
                                                timezone)

}

/**
 * ProtoUser is bare bones.  MetaProtoUser contains a bunch
 * more fields including a validated flag, locale, timezone, etc.
 */
trait MongoMegaProtoUser[T <: MongoMegaProtoUser[T]] extends MongoProtoUser[T] {
  self: T =>

  protected class RandomStringField(obj: T, maxLen: Int) extends StringField(obj, maxLen) {
    override def defaultValue = randomString(maxLen)
    def reset() = this(randomString(maxLen))
  }
    
  /**
   * The unique id field for the User. This field
   * is used for validation, lost passwords, etc.
   * You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val uniqueId = new MyUniqueId(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val uniqueId: RandomStringField = new RandomStringField(this, 32)
    
  /**
   * The has the user been validated.
   * You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val validated = new MyValidated(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val validated: BooleanField[T] = new MyValidated(this)

  protected class MyValidated(obj: T) extends BooleanField[T](obj) {
    override def defaultValue = false
    override val fieldId = Some(Text("txtValidated"))
  }

  /**
   * The locale field for the User.
   * You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val locale = new MyLocale(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val locale: LocaleField[T] = new MyLocale(this)

  protected class MyLocale(obj: T) extends LocaleField[T](obj) {
    override def displayName = owner.localeDisplayName
    override val fieldId = Some(Text("txtLocale"))
  }

  /**
   * The time zone field for the User.
   * You can override the behavior
   * of this field:
   * <pre name="code" class="scala">
   * override lazy val timezone = new MyTimeZone(this, 32) {
   *   println("I am doing something different")
   * }
   * </pre>
   */
  lazy val timezone: TimeZoneField[T] = new MyTimeZone(this)

  protected class MyTimeZone(obj: T) extends TimeZoneField[T](obj) {
    override def displayName = owner.timezoneDisplayName
    override val fieldId = Some(Text("txtTimeZone"))
  }

  /**
   * The string for the timezone field
   */
  def timezoneDisplayName = ??("time.zone")

  /**
   * The string for the locale field
   */
  def localeDisplayName = ??("locale")

}

  
}  
}

