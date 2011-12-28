package lifttest.model

trait UserIdAsString {
  /**
   * NOTE - this must be less than 256 characters
   */
  def userIdAsString: String
}