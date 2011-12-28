package lifttest {
package model {

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.http._
import org.bson.types.ObjectId
import net.liftweb.json.JsonDSL._


/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MongoMetaRecord[User] with MongoMetaMegaProtoUser[User] {

  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)

  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email,
  locale, timezone, password)

  //Uncomment to skip email validation (e.g. if you have no Mailer set up)
  //override def skipEmailValidation = true
  
  //Use our extended session
  onLogIn = List(ExtSession.userDidLogin(_))
  onLogOut = List(ExtSession.userDidLogout(_))
}

class User extends MongoRecord[User] with MongoMegaProtoUser[User] {
  def meta = User

  val logger = Logger(classOf[User])

  def userFromStringId(id: String): Box[User] = meta.find(id)

  def findUserByUniqueId(id: String): Box[User] = meta.findAll(("uniqueId" -> id)).headOption
  
  def fromId(id:ObjectId) = meta.find(id)

  override def toString = firstName.is + " " + lastName.is
  
  /**
   * Given a username (probably email address), find the user
   */
  protected def findUserByEmail(email: String): Box[User] = meta.findAll("email",email).headOption

  protected def findUserByUserName(email: String): Box[User] = findUserByEmail(email)

}

}
}
