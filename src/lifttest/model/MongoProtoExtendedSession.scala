package lifttest.model

import net.liftweb._
import net.liftweb.http.provider._
import common._
import util._
import Helpers._
import net.liftweb.record.field._
import net.liftweb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.mongodb.record._
import net.liftweb.http.S
import net.liftweb.json.JsonDSL._
import net.liftweb.http.Req


trait MongoProtoExtendedSession[T <: MongoProtoExtendedSession[T]] extends MongoRecord[T] with ObjectIdPk[T] {
  self: T =>

  protected class RandomStringField(obj: T, maxLen: Int) extends StringField(obj, maxLen) {
    override def defaultValue = randomString(maxLen)
    def reset() = this(randomString(maxLen))
  }
    
  lazy val cookieId:StringField[T] = new RandomStringField(this, 64)
    
  lazy val userId:StringField[T] = new StringField(this, 256)
  
  protected class ExpirationField(obj: T) extends LongField(obj) {
    override def defaultValue = expirationTime 
  }

  def expirationTime: Long = millis + 180.days

  lazy val expiration:LongField[T] = new ExpirationField(this)
}

trait MongoMetaProtoExtendedSession[T <: MongoProtoExtendedSession[T]] extends MongoMetaRecord[T] with ObjectIdPk[T] {
  self: T =>

  def CookieName = "ext_id"
    
  type UserType <: UserIdAsString
  
  /**
   * Try to find a user with specified id. If found, log the user
   * in and return Full(user). Otherwise, return non-Full
   */
  def logUserIn(uid: String): Box[UserType]

  /**
   * Return a Box, which has the id of the currently
   * logged in user, if there is one.
   */
  def recoverUserId: Box[String]

  def userDidLogin(uid: UserType) {
    userDidLogout(Full(uid))
    val inst = createRecord
    inst.userId(uid.userIdAsString).saveTheRecord()
    val cookie = HTTPCookie(CookieName, inst.cookieId.is).
      setMaxAge(((inst.expiration.is - millis) / 1000L).toInt).
      setPath("/")
    S.addCookie(cookie)
  }
  
  def userDidLogout(uid: Box[UserType]) {
    //Delete all cookies, and extended session records
    for (cookie <- S.findCookie(CookieName)) {
      S.deleteCookie(cookie)
      findAll(("cookieId" -> cookie.value.openOr(""))).foreach(_.delete_!)
    }
  }

  /**
   * This does the cookie to User lookup.  In Boot.scala:
   * <code>
    LiftRules.earlyInStateful.append(ExtendedSession.testCookieEarlyInStateful)
   * </code>
   */
  def testCookieEarlyInStateful: Box[Req] => Unit = {
    ignoredReq => {
      (recoverUserId, S.findCookie(CookieName)) match {
        //If no user is logged in, and we have a cookie
        case (Empty, Full(c)) =>
          //Find the extended session for the cookie id
          findAll(("cookieId" -> c.value.openOr(""))).headOption match {
            case Some(es) if es.expiration.is < millis => es.delete_!
            case Some(es) => {
              //Try to log in - if we can't log a user in, session and cookies
              //are incorrect so delete. Note also that logging the user in
              //will recreate the cookie with a new session for next time,
              //so that cookie ids are one-use-only
              if (!logUserIn(es.userId.is).isDefined) {
                es.delete_!
                userDidLogout(Empty)
              }
            }
            case _ =>
          }
        
        case _ =>
      }
    }
  }
}

