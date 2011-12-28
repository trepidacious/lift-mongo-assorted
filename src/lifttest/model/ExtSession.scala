package lifttest.model

import net.liftweb._
import mapper._
import util._
import common._

class ExtSession extends MongoProtoExtendedSession[ExtSession] {
  def meta = ExtSession
}

object ExtSession extends ExtSession with MongoMetaProtoExtendedSession[ExtSession] {

  def logUserIn(uid: String) = {
    val user = User.userFromStringId(uid)
    user.foreach(User.logUserIn(_))
    user
  }
  
  def recoverUserId: Box[String] = User.currentUserId
  
  type UserType = User
}
